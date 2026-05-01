"""Orchestrator: runs every provider scraper, writes public/rates.json.

Design choices for security and reliability:

  1. **Concurrency with hard isolation** — each scraper runs in its own
     thread with a strict timeout. One slow or hostile provider cannot
     hang the run.

  2. **No partial overwrite** — we collect ALL quotes (success or error),
     then write rates.json atomically. Readers (the Android app) never see
     a half-written file because we write to a temp path and rename.

  3. **Stale tolerance** — if a provider fails in this run, we keep the
     previous successful Quote (if available) but mark it status="stale"
     with the original `fetched_at`. The app shows stale rates with a
     warning rather than dropping the provider entirely.

  4. **Bounded log noise** — exceptions are caught per provider and
     surfaced in the JSON, never printed in a way that exposes secrets
     (we have none, but this is defense-in-depth).
"""
from __future__ import annotations

import argparse
import concurrent.futures as cf
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import List

from .base import BaseProvider, Quote
from .aspora import AsporaProvider
from .botim import BotimProvider
from .careem import CareemProvider
from .comera import ComeraProvider
from .eand import EandProvider
from .lulu import LuluProvider
from .remitly import RemitlyProvider
from .wise import WiseProvider


# Order here is the default display order in the app when rates are equal.
PROVIDERS: List[BaseProvider] = [
    WiseProvider(),
    RemitlyProvider(),
    LuluProvider(),
    AsporaProvider(),
    CareemProvider(),
    EandProvider(),
    BotimProvider(),
    ComeraProvider(),
]

PER_PROVIDER_TIMEOUT_S = 25.0
DEFAULT_AMOUNT_AED = 1000.0


def _now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _run_one(provider: BaseProvider, amount_aed: float) -> Quote:
    """Run a single provider, never raise to caller."""
    try:
        return provider.fetch_inr(amount_aed=amount_aed)
    except Exception as exc:  # noqa: BLE001 — we want to catch everything
        return Quote(
            provider_id=provider.id,
            provider_name=provider.display_name,
            base="AED",
            quote="INR",
            amount_base=amount_aed,
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
    fresh: List[Quote], previous_path: Path
) -> List[Quote]:
    """If a provider errored this run but has a previous successful quote,
    keep the previous quote with status='stale' rather than show error."""
    if not previous_path.exists():
        return fresh
    try:
        prev = json.loads(previous_path.read_text(encoding="utf-8"))
        prev_by_id = {p["provider_id"]: p for p in prev.get("providers", [])}
    except (json.JSONDecodeError, KeyError, TypeError):
        return fresh

    merged: List[Quote] = []
    for q in fresh:
        if q.status == "error" and q.provider_id in prev_by_id:
            old = prev_by_id[q.provider_id]
            if old.get("status") == "ok":
                merged.append(
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
                        fetched_at=old.get("fetched_at", q.fetched_at),
                    )
                )
                continue
        merged.append(q)
    return merged


def main() -> int:
    parser = argparse.ArgumentParser(description="Run all rate scrapers")
    parser.add_argument(
        "--amount", type=float, default=DEFAULT_AMOUNT_AED,
        help="Amount in AED to quote (default: 1000)",
    )
    parser.add_argument(
        "--out", type=Path,
        default=Path("public/rates.json"),
        help="Output JSON path",
    )
    args = parser.parse_args()

    started = _now_iso()

    quotes: List[Quote] = []
    with cf.ThreadPoolExecutor(max_workers=len(PROVIDERS)) as pool:
        futures = {
            pool.submit(_run_one, p, args.amount): p for p in PROVIDERS
        }
        for fut in cf.as_completed(futures, timeout=PER_PROVIDER_TIMEOUT_S * 2):
            try:
                quotes.append(fut.result(timeout=PER_PROVIDER_TIMEOUT_S))
            except cf.TimeoutError:
                p = futures[fut]
                quotes.append(
                    Quote(
                        provider_id=p.id,
                        provider_name=p.display_name,
                        base="AED",
                        quote="INR",
                        amount_base=args.amount,
                        rate=None, fee_base=None, received_quote=None,
                        effective_rate=None, delivery_estimate=None,
                        url=None, status="error",
                        note=f"Hard timeout after {PER_PROVIDER_TIMEOUT_S}s",
                        fetched_at=_now_iso(),
                    )
                )

    # Merge with previous successful runs to preserve stale-tolerance.
    quotes = _merge_with_previous(quotes, args.out)

    # Stable display order: keep the order defined in PROVIDERS.
    order = {p.id: i for i, p in enumerate(PROVIDERS)}
    quotes.sort(key=lambda q: order.get(q.provider_id, 99))

    payload = {
        "schema_version": 1,
        "base": "AED",
        "quote": "INR",
        "amount_base": args.amount,
        "started_at": started,
        "completed_at": _now_iso(),
        "providers": [q.to_dict() for q in quotes],
    }

    args.out.parent.mkdir(parents=True, exist_ok=True)
    tmp = args.out.with_suffix(args.out.suffix + ".tmp")
    tmp.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    os.replace(tmp, args.out)

    ok = sum(1 for q in quotes if q.status == "ok")
    err = sum(1 for q in quotes if q.status == "error")
    stale = sum(1 for q in quotes if q.status == "stale")
    inv = sum(1 for q in quotes if q.status == "investigating")
    print(
        f"wrote {args.out}: ok={ok} stale={stale} error={err} "
        f"investigating={inv} total={len(quotes)}"
    )
    # Exit non-zero only if EVERYTHING failed — partial failure is normal.
    return 0 if (ok + stale) > 0 else 1


if __name__ == "__main__":
    sys.exit(main())
