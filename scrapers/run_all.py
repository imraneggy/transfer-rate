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
        "USD": [<provider_quote>, ...],
        ...
      }
    }

Design choices:

  1. **Concurrency with hard isolation** — each (provider, corridor) pair
     runs in its own thread with a strict timeout. One slow provider
     cannot hang the run.

  2. **Stale tolerance** — if a (provider, corridor) cell fails this
     run, we keep the previous successful quote (if any) but mark it
     status="stale" with the original `fetched_at`.

  3. **Atomic write** — temp file + os.replace, so readers never see
     a half-written document.

  4. **Investigation stubs always present** — every corridor lists every
     provider, even when most of them are stubs. Users see the full
     coverage roadmap, contributors see the targets to fix.
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

# Tier 1 — working scrapers (real data)
from .wise import WiseProvider
from .aspora import AsporaProvider
from .remitly import RemitlyProvider

# Tier 2 — stubbed (placeholder, parser pending)
from .lulu import LuluProvider
from .careem import CareemProvider
from .botim import BotimProvider
from .comera import ComeraProvider
from .eand import EandProvider

# Tier 3 — additional UAE exchange houses, bank remit products,
# and global remitters (all stubbed; parsers pending contributor work)
from .al_ansari import AlAnsariProvider
from .al_fardan import AlFardanProvider
from .unimoni import UnimoniProvider
from .sharaf import SharafProvider
from .joyalukkas import JoyalukkasProvider
from .gcc_exchange import GccExchangeProvider
from .index_exchange import IndexExchangeProvider
from .wall_street import WallStreetProvider
from .lari import LariProvider
from .orient import OrientProvider
from .al_razouki import AlRazoukiProvider
from .habib import HabibProvider
from .western_union import WesternUnionProvider
from .moneygram import MoneyGramProvider
from .worldremit import WorldRemitProvider
from .instarem import InstaRemProvider
from .xoom import XoomProvider
# Bank remittance products
from .emirates_nbd import EmiratesNbdProvider
from .fab_remit import FabRemitProvider
from .mashreq_quick import MashreqQuickProvider


# Display order: working providers first, then UAE exchange houses,
# then global remitters, then bank-remit products, then app-only services.
PROVIDERS: List[BaseProvider] = [
    # --- Working ---
    WiseProvider(),
    AsporaProvider(),
    RemitlyProvider(),
    # --- Major UAE exchange houses (stubs) ---
    LuluProvider(),
    AlAnsariProvider(),
    AlFardanProvider(),
    UnimoniProvider(),
    SharafProvider(),
    JoyalukkasProvider(),
    GccExchangeProvider(),
    IndexExchangeProvider(),
    WallStreetProvider(),
    LariProvider(),
    OrientProvider(),
    AlRazoukiProvider(),
    HabibProvider(),
    # --- Global remitters (stubs) ---
    WesternUnionProvider(),
    MoneyGramProvider(),
    WorldRemitProvider(),
    InstaRemProvider(),
    XoomProvider(),
    # --- Bank remittance products (stubs) ---
    EmiratesNbdProvider(),
    FabRemitProvider(),
    MashreqQuickProvider(),
    # --- App-only services (stubs) ---
    CareemProvider(),
    EandProvider(),
    BotimProvider(),
    ComeraProvider(),
]

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

    # Tolerate either schema v1 (flat providers list) or v2 (corridors map)
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


def main() -> int:
    parser = argparse.ArgumentParser(description="Run scrapers across all corridors")
    parser.add_argument("--amount", type=float, default=DEFAULT_AMOUNT)
    parser.add_argument("--out", type=Path, default=Path("public/rates.json"))
    parser.add_argument(
        "--corridors", nargs="+", default=list(SUPPORTED_TARGETS),
        help="Subset of corridors to run (default: all)",
    )
    args = parser.parse_args()

    started = _now_iso()

    # Build all (provider, corridor) work units.
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

    fresh = _merge_with_previous(fresh, args.out)

    # Stable display order within each corridor: keep PROVIDERS order.
    order = {p.id: i for i, p in enumerate(PROVIDERS)}
    for c in fresh:
        fresh[c].sort(key=lambda q: order.get(q.provider_id, 99))

    payload = {
        "schema_version": 2,
        "base": "AED",
        "amount_base": args.amount,
        "started_at": started,
        "completed_at": _now_iso(),
        "corridors": {c: [q.to_dict() for q in fresh[c]] for c in args.corridors},
    }

    args.out.parent.mkdir(parents=True, exist_ok=True)
    tmp = args.out.with_suffix(args.out.suffix + ".tmp")
    tmp.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    os.replace(tmp, args.out)

    # Summary
    total = sum(len(v) for v in fresh.values())
    ok = sum(1 for v in fresh.values() for q in v if q.status == "ok")
    err = sum(1 for v in fresh.values() for q in v if q.status == "error")
    stale = sum(1 for v in fresh.values() for q in v if q.status == "stale")
    inv = sum(1 for v in fresh.values() for q in v if q.status == "investigating")
    print(
        f"wrote {args.out}: corridors={len(fresh)} total={total} "
        f"ok={ok} stale={stale} error={err} investigating={inv}"
    )
    return 0 if (ok + stale) > 0 else 1


if __name__ == "__main__":
    sys.exit(main())
