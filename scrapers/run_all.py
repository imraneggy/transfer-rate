"""Orchestrator: runs every provider for every supported corridor.

Output is a single combined JSON document at public/rates.json:

    {
      "schema_version": 2,
      "base": "AED",
      "amount_base": 1000.0,
      "started_at": "...",
      "completed_at": "...",
      "corridors": {
        "INR": [<provider_quote>, ...],
        ...
      }
    }

Design choices:

  1. **Only providers with verified public rate sources are included.**
     Providers without a working scraper are not listed at all - no stubs,
     no estimates. Cleaner UX in the app and less noise.

  2. **Concurrency with hard isolation** - each (provider, corridor) pair
     runs in its own thread with a strict timeout.

  3. **Stale tolerance** - if a (provider, corridor) cell fails this run,
     we keep the previous successful quote (if any) but mark it
     status="stale" with the original `fetched_at`.

  4. **Atomic write** - temp file + os.replace, so readers never see a
     half-written document.

  5. **Manual override layer** - `data/manual-rates.json` can override
     entries (kept for future use, e.g. emergency corrections; currently
     no provider expects manual override since all listed providers have
     working scrapers).
"""
from __future__ import annotations

import argparse
import concurrent.futures as cf
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List

from .base import BaseProvider, Quote, SUPPORTED_TARGETS

# Tier 0 - independent mid-market benchmark (extracted to header in app, not in list)
from .mid_market import MidMarketProvider

# Tier 1 - providers with verified public rate sources (real working scrapers)
from .wise import WiseProvider
from .aspora import AsporaProvider
from .remitly import RemitlyProvider
from .transfergo import TransferGoProvider
from .index_exchange import IndexExchangeProvider
from .orient_exchange import OrientExchangeProvider
from .ahalia import AhaliaProvider
from .al_ansari import AlAnsariProvider
from .al_dahab import AlDahabProvider
from .federal_exchange import FederalExchangeProvider
from .gcc_exchange import GccExchangeProvider
from .lari import LariProvider
from .gold import fetch_gold, GoldHistoryPoint


# Display order. Mid-market first (extracted to header), then real providers.
# Stubs for app-only services (Botim, Comera, e&) and providers without
# discovered public APIs (Al Fardan, Sharaf, Habib, etc.) are intentionally
# NOT in this list - they would just clutter the UI with "estimated"
# placeholders. To re-add a stub, import it from scrapers/<name>.py and
# include it below.
#
# LuLu was dropped in v0.30.6 - their F5 BIG-IP WAF blocks every datacenter
# IP we tried (GitHub Actions, Cloudflare Workers, AWS, Azure, etc).  The
# only reliable path was a self-hosted runner on a residential IP, and the
# operational overhead of keeping that runner online outweighed having
# 1 of 12 providers.  See infra/lulu-proxy/README.md and
# infra/lulu-residential/README.md for the historical context.
def _build_providers() -> List[BaseProvider]:
    """Construct the provider list."""
    return [
        MidMarketProvider(),
        WiseProvider(),
        AsporaProvider(),
        RemitlyProvider(),
        TransferGoProvider(),
        AlAnsariProvider(),
        AlDahabProvider(),
        AhaliaProvider(),
        FederalExchangeProvider(),
        GccExchangeProvider(),
        IndexExchangeProvider(),
        OrientExchangeProvider(),
        LariProvider(),
    ]


PROVIDERS: List[BaseProvider] = _build_providers()

PER_CALL_TIMEOUT_S = 25.0
DEFAULT_AMOUNT = 1000.0


def _now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _run_one(
    provider: BaseProvider, corridor: str, amount: float
) -> Quote:
    """Fetch one (provider, corridor); never raise to caller."""
    try:
        return provider.fetch(target_currency=corridor, amount_base=amount)
    except Exception as exc:  # noqa: BLE001
        return Quote(
            provider_id=provider.id,
            provider_name=provider.display_name,
            base="AED",
            quote=corridor,
            amount_base=amount,
            rate=None,
            fee_base=None,
            received_quote=None,
            effective_rate=None,
            delivery_estimate=None,
            url=None,
            status="error",
            note=f"{type(exc).__name__}: {exc}"[:200],
            fetched_at=_now_iso(),
        )


def _merge_with_previous(
    fresh: Dict[str, List[Quote]], previous_path: Path
) -> Dict[str, List[Quote]]:
    """Preserve last good quote when a fresh fetch errored."""
    if not previous_path.exists():
        return fresh
    try:
        prev = json.loads(previous_path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return fresh

    prev_corridors: Dict[str, Dict[str, dict]] = {}
    if isinstance(prev.get("corridors"), dict):
        for corridor, items in prev["corridors"].items():
            if isinstance(items, list):
                prev_corridors[corridor] = {p.get("provider_id"): p for p in items if isinstance(p, dict)}
    elif isinstance(prev.get("providers"), list):
        prev_corridors["INR"] = {p.get("provider_id"): p for p in prev["providers"] if isinstance(p, dict)}

    merged: Dict[str, List[Quote]] = {}
    for corridor, quotes in fresh.items():
        merged[corridor] = []
        prev_lookup = prev_corridors.get(corridor, {})
        for q in quotes:
            if q.status == "error" and q.provider_id in prev_lookup:
                old = prev_lookup[q.provider_id]
                if old.get("status") == "ok":
                    merged[corridor].append(
                        Quote(
                            provider_id=old["provider_id"],
                            provider_name=old["provider_name"],
                            base=old["base"],
                            quote=old["quote"],
                            amount_base=old["amount_base"],
                            rate=old.get("rate"),
                            fee_base=old.get("fee_base"),
                            received_quote=old.get("received_quote"),
                            effective_rate=old.get("effective_rate"),
                            delivery_estimate=old.get("delivery_estimate"),
                            url=old.get("url"),
                            status="stale",
                            note=f"Last good fetch: {old.get('fetched_at')}; "
                                 f"current error: {q.note}",
                            promo_rate=old.get("promo_rate"),
                            promo_note=old.get("promo_note"),
                            fetched_at=old.get("fetched_at", q.fetched_at),
                        )
                    )
                    continue
            merged[corridor].append(q)
    return merged


def _apply_manual_rates(
    fresh: Dict[str, List[Quote]], manual_path: Path
) -> Dict[str, List[Quote]]:
    """Override entries with manually-entered rates from data/manual-rates.json.

    Currently overrides only `status="investigating"` entries. Since v0.7
    removed all stubs, this layer is dormant unless a contributor adds
    a stub provider back. Kept for future flexibility.
    """
    if not manual_path.exists():
        return fresh
    try:
        manual = json.loads(manual_path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return fresh
    if manual.get("schema_version") != 1:
        return fresh
    rates_map = manual.get("rates")
    if not isinstance(rates_map, dict):
        return fresh

    for corridor, quotes in fresh.items():
        for i, q in enumerate(quotes):
            if q.status != "investigating":
                continue
            entry = rates_map.get(q.provider_id, {}).get(corridor)
            if not isinstance(entry, dict):
                continue
            try:
                rate = float(entry["rate"])
            except (KeyError, TypeError, ValueError):
                continue
            if not 0.0001 <= rate <= 10000.0:
                continue
            fee = entry.get("fee_aed")
            try:
                fee = float(fee) if fee is not None else None
            except (TypeError, ValueError):
                fee = None
            fetched_at = entry.get("fetched_at") or _now_iso()
            note = entry.get("note") or "Manually entered rate"

            quotes[i] = Quote(
                provider_id=q.provider_id,
                provider_name=q.provider_name,
                base="AED",
                quote=corridor,
                amount_base=q.amount_base,
                rate=rate,
                fee_base=fee,
                received_quote=rate * q.amount_base
                              - (fee * rate if fee else 0.0),
                effective_rate=rate,
                delivery_estimate=q.delivery_estimate,
                url=q.url,
                status="manual",
                note=note,
                fetched_at=fetched_at,
            )
    return fresh


def _append_to_history(
    payload: dict, history_path: Path, max_age_days: int = 10
) -> None:
    """Append today's rates into a rolling history file.

    Schema:
        {
          "schema_version": 1,
          "updated_at": "...",
          "providers": {
            "<provider_id>": [
              {"t": "2026-05-01T08:00:00Z", "rate": 25.81},
              ...
            ]
          }
        }

    Pruned to the last `max_age_days` days. App renders a sparkline per
    provider from this. INR-only for now (the only corridor with
    multiple working providers); when other corridors come online,
    extend to a {corridor: {provider: [...]}} two-level map.
    """
    from datetime import timedelta
    history: dict = {"schema_version": 1, "providers": {}}
    if history_path.exists():
        try:
            loaded = json.loads(history_path.read_text(encoding="utf-8"))
            if loaded.get("schema_version") == 1 and isinstance(loaded.get("providers"), dict):
                history = loaded
        except (json.JSONDecodeError, OSError):
            pass

    completed_at = payload.get("completed_at") or _now_iso()
    inr = payload.get("corridors", {}).get("INR", [])
    for q in inr:
        if q.get("status") not in ("ok", "manual"):
            continue
        rate = q.get("rate")
        pid = q.get("provider_id")
        if not pid or not isinstance(rate, (int, float)) or rate <= 0:
            continue
        bucket = history["providers"].setdefault(pid, [])
        # Avoid adjacent duplicates from manual re-triggers
        if bucket and bucket[-1].get("t") == completed_at:
            continue
        bucket.append({"t": completed_at, "rate": float(rate)})

    cutoff = datetime.now(timezone.utc) - timedelta(days=max_age_days)
    cutoff_iso = cutoff.strftime("%Y-%m-%dT%H:%M:%SZ")
    for pid in list(history["providers"].keys()):
        history["providers"][pid] = [
            e for e in history["providers"][pid]
            if e.get("t", "") >= cutoff_iso
        ]
        # Drop empty buckets
        if not history["providers"][pid]:
            del history["providers"][pid]

    history["updated_at"] = _now_iso()

    history_path.parent.mkdir(parents=True, exist_ok=True)
    tmp = history_path.with_suffix(history_path.suffix + ".tmp")
    tmp.write_text(json.dumps(history, indent=2) + "\n", encoding="utf-8")
    os.replace(tmp, history_path)


def _merge_uae_gold_history(
    side: dict, history_path: Path, max_days: int = 30
) -> dict:
    """Maintain a rolling UAE gold history file and fold it into `side`.

    Original v0.x design: Khaleej Times only exposes today's UAE gold
    rate, so we kept our own daily series stitched from cron snapshots
    so the app could show a 30-day chart.

    v0.32.5 fix: as of v0.32.4 the primary scraper (igold.ae) returns
    its own 30-day history.  This function used to BLINDLY OVERWRITE
    side["history"] with the rolling-file content, throwing away
    igold's 30 points and replacing them with the 2-3 cron-snapshot
    entries that pre-dated the swap.  Now:

      1. Seed the rolling dict with whatever side["history"] already
         contains (igold's 30 days when primary works).
      2. Layer the rolling file on top (preserves cron-snapshot
         continuity for outage windows when igold is down).
      3. Append today's reading.
      4. Dedupe by date (latest in-memory wins), prune to max_days,
         persist, fold back into side.

    This way the union of "igold history + accumulated cron snapshots"
    is what the app sees - fullness when primary works, graceful
    degradation when primary doesn't.
    """
    from datetime import date as _date

    # 1. Seed from whatever the scraper provided (igold's 30 days when
    #    the primary path worked; KT fallback's empty list otherwise).
    rolling: dict = {}
    for entry in side.get("history", []) or []:
        d = entry.get("date")
        p22 = entry.get("per_g_22k")
        p24 = entry.get("per_g_24k")
        if d and p22 is not None and p24 is not None:
            rolling[d] = {"22k": float(p22), "24k": float(p24)}

    # 2. Layer the cron-snapshot rolling file on top (preserves
    #    continuity across deploys; older entries from previous
    #    primary-source versions stay until pruned by age).
    if history_path.exists():
        try:
            loaded = json.loads(history_path.read_text(encoding="utf-8"))
            if isinstance(loaded, dict):
                for d, v in loaded.items():
                    if isinstance(v, dict) and "22k" in v and "24k" in v:
                        # Don't overwrite scraper-provided values -
                        # the scraper output is fresher (sub-day),
                        # the rolling file is yesterday's tick.
                        rolling.setdefault(d, v)
        except (json.JSONDecodeError, OSError):
            pass

    # 3. Append today's reading if both karats are present.
    today = _date.today().isoformat()
    if (
        side.get("status") == "ok"
        and side.get("per_g_22k") is not None
        and side.get("per_g_24k") is not None
    ):
        # Today's value from this run wins over any pre-existing
        # entry for today (catch the latest cron tick).
        rolling[today] = {
            "22k": float(side["per_g_22k"]),
            "24k": float(side["per_g_24k"]),
        }

    # 4. Prune to the most recent `max_days`.
    keep = sorted(rolling.keys(), reverse=True)[:max_days]
    pruned = {d: rolling[d] for d in keep}

    # Persist.
    history_path.parent.mkdir(parents=True, exist_ok=True)
    tmp = history_path.with_suffix(history_path.suffix + ".tmp")
    tmp.write_text(json.dumps(pruned, indent=2) + "\n", encoding="utf-8")
    os.replace(tmp, history_path)

    # Fold into the side payload (newest first).
    side["history"] = [
        {"date": d, "per_g_22k": pruned[d]["22k"], "per_g_24k": pruned[d]["24k"]}
        for d in sorted(pruned.keys(), reverse=True)
    ]
    return side


def main() -> int:
    parser = argparse.ArgumentParser(description="Run scrapers across all corridors")
    parser.add_argument("--amount", type=float, default=DEFAULT_AMOUNT)
    parser.add_argument("--out", type=Path, default=Path("public/rates.json"))
    parser.add_argument(
        "--history", type=Path, default=Path("public/history.json"),
        help="Rolling history file (last 10 days of rates per provider)",
    )
    parser.add_argument(
        "--gold-history", type=Path, default=Path("data/uae_gold_history.json"),
        help="Locally-maintained UAE gold history (Khaleej Times only "
             "shows today; we keep our own series)",
    )
    parser.add_argument(
        "--corridors", nargs="+", default=list(SUPPORTED_TARGETS),
        help="Subset of corridors to run (default: all)",
    )
    parser.add_argument(
        "--manual", type=Path, default=Path("data/manual-rates.json"),
        help="Path to manual rates JSON (overrides for stub providers)",
    )
    args = parser.parse_args()

    started = _now_iso()

    work = [(p, c) for c in args.corridors for p in PROVIDERS]
    fresh: Dict[str, List[Quote]] = {c: [] for c in args.corridors}

    with cf.ThreadPoolExecutor(max_workers=min(20, len(work))) as pool:
        futures = {
            pool.submit(_run_one, p, c, args.amount): (p, c) for p, c in work
        }
        for fut in cf.as_completed(futures, timeout=PER_CALL_TIMEOUT_S * 4):
            p, c = futures[fut]
            try:
                quote = fut.result(timeout=PER_CALL_TIMEOUT_S)
            except cf.TimeoutError:
                quote = Quote(
                    provider_id=p.id,
                    provider_name=p.display_name,
                    base="AED", quote=c,
                    amount_base=args.amount,
                    rate=None, fee_base=None, received_quote=None,
                    effective_rate=None, delivery_estimate=None,
                    url=None, status="error",
                    note=f"Hard timeout after {PER_CALL_TIMEOUT_S}s",
                    fetched_at=_now_iso(),
                )
            fresh[c].append(quote)

    fresh = _apply_manual_rates(fresh, args.manual)
    fresh = _merge_with_previous(fresh, args.out)

    order = {p.id: i for i, p in enumerate(PROVIDERS)}
    for c in fresh:
        fresh[c].sort(key=lambda q: order.get(q.provider_id, 99))

    # Gold module - pulled separately from the FX corridors and embedded
    # at the top level. Failures here are isolated: rates.json publishes
    # with `gold: null` if the gold scrape fails entirely.
    gold_payload = None
    try:
        gold = fetch_gold()
        gold_dict = gold.to_dict()
        # Maintain UAE history locally (KT only shows today)
        gold_dict["uae"] = _merge_uae_gold_history(
            gold_dict["uae"], args.gold_history
        )
        gold_payload = gold_dict
    except Exception as exc:
        print(f"warning: gold module failed: {exc}", file=sys.stderr)

    payload = {
        "schema_version": 2,
        "base": "AED",
        "amount_base": args.amount,
        "started_at": started,
        "completed_at": _now_iso(),
        "corridors": {c: [q.to_dict() for q in fresh[c]] for c in args.corridors},
        "gold": gold_payload,
    }

    args.out.parent.mkdir(parents=True, exist_ok=True)
    tmp = args.out.with_suffix(args.out.suffix + ".tmp")
    tmp.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    os.replace(tmp, args.out)

    # Append to rolling history (7-day window). The app fetches this
    # separately and renders sparklines per provider.
    try:
        _append_to_history(payload, args.history)
    except Exception as exc:
        # Non-fatal - rate.json is the primary artifact; history is bonus.
        print(f"warning: history update failed: {exc}", file=sys.stderr)

    total = sum(len(v) for v in fresh.values())
    ok = sum(1 for v in fresh.values() for q in v if q.status == "ok")
    err = sum(1 for v in fresh.values() for q in v if q.status == "error")
    stale = sum(1 for v in fresh.values() for q in v if q.status == "stale")
    inv = sum(1 for v in fresh.values() for q in v if q.status == "investigating")
    man = sum(1 for v in fresh.values() for q in v if q.status == "manual")
    print(
        f"wrote {args.out}: corridors={len(fresh)} total={total} "
        f"ok={ok} manual={man} stale={stale} error={err} investigating={inv}"
    )
    return 0 if (ok + stale + man) > 0 else 1


if __name__ == "__main__":
    sys.exit(main())
