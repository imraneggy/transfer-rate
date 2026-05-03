"""Standalone LuLu rate injector for the self-hosted-runner workflow.

Why this exists
---------------
LuLu's rate API runs on TCP port 9443 which GitHub-hosted runners block,
and Cloudflare Workers also can't reach non-standard ports. The only
practical path to include LuLu in the live rates is to run a fetch from
a residential IP — your home box, an Indian/UAE VPS with a residential
ISP, or any other always-on machine with unrestricted egress.

This script is what the self-hosted-runner workflow runs:

    1. Read public/rates.json (the JSON the GitHub-hosted scraper just
       published)
    2. Fetch LuLu's live rate via the existing LuluProvider (which uses
       the working port-9443 endpoint)
    3. Insert / update the LuLu entry in the corridors.INR list at a
       stable position (right after TransferGo, index 5)
    4. Atomically rewrite public/rates.json

The wrapping workflow then commits and pushes the file. Because the
GitHub-hosted scrape commits rates.json too, both schedules race and
the latest commit wins — both write the LuLu entry idempotently, so
the merge is safe.

Run as:

    python -m scrapers.lulu_inject [--rates-path public/rates.json]
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Optional

from .lulu import LuluProvider


LULU_INSERT_INDEX = 5  # after mid_market, wise, aspora, remitly, transfergo


def _quote_to_entry(q) -> dict:
    """Serialize a Quote dataclass to the rates.json entry shape."""
    return q.to_dict()


def inject(rates_path: Path) -> int:
    if not rates_path.exists():
        print(f"error: {rates_path} does not exist", file=sys.stderr)
        return 2

    try:
        doc = json.loads(rates_path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as exc:
        print(f"error: cannot read {rates_path}: {exc}", file=sys.stderr)
        return 2

    inr_list = (doc.get("corridors") or {}).get("INR")
    if not isinstance(inr_list, list):
        print("error: corridors.INR missing or not a list", file=sys.stderr)
        return 2

    # Fetch LuLu (this is the residential-IP-only call)
    try:
        quote = LuluProvider().fetch(target_currency="INR", amount_base=doc.get("amount_base", 1000.0))
    except Exception as exc:  # noqa: BLE001
        print(f"warning: LuLu fetch failed: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 1

    new_entry = _quote_to_entry(quote)

    # Replace existing LuLu entry if present, otherwise insert at the
    # canonical index. We don't modify any other entry.
    existing_idx: Optional[int] = next(
        (i for i, e in enumerate(inr_list) if e.get("provider_id") == "lulu"),
        None,
    )
    if existing_idx is not None:
        inr_list[existing_idx] = new_entry
        action = f"updated at index {existing_idx}"
    else:
        target = min(LULU_INSERT_INDEX, len(inr_list))
        inr_list.insert(target, new_entry)
        action = f"inserted at index {target}"

    # Atomic write
    tmp = rates_path.with_suffix(rates_path.suffix + ".tmp")
    tmp.write_text(json.dumps(doc, indent=2) + "\n", encoding="utf-8")
    os.replace(tmp, rates_path)

    print(f"lulu rate {quote.rate}: {action} in {rates_path}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Inject LuLu rate into rates.json")
    parser.add_argument(
        "--rates-path", type=Path, default=Path("public/rates.json"),
        help="Path to rates.json to update (default: public/rates.json)",
    )
    args = parser.parse_args()
    return inject(args.rates_path)


if __name__ == "__main__":
    sys.exit(main())
