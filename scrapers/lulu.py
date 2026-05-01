"""LuLu Money / LuLu Exchange scraper.

LuLu Financial Holdings publishes daily rates on lulumoney.com. The page
embeds a JSON blob the client-side ticker reads from. We fetch the page,
extract the embedded data, and read the AED -> INR rate.

This scraper deliberately uses a conservative parsing approach: it tries a
specific extraction pattern; if the pattern fails it surfaces a clear error
rather than fall back to "guess from any number on the page."
"""
from __future__ import annotations

import json
import re

from .base import BaseProvider, Quote
from .utils import http_client


# Matches a JSON-ish array of rate rows embedded in the page.
# We deliberately do NOT do generic "find any number" parsing — wrong rate is
# worse than no rate.
_RATES_BLOB = re.compile(
    r"window\.__INITIAL_RATES__\s*=\s*(\[.*?\]);", re.DOTALL
)


class LuluProvider(BaseProvider):
    id = "lulu"
    display_name = "LuLu Money"
    PAGE_URL = "https://www.lulumoney.com/exchange-rates"
    PRODUCT_URL = "https://www.lulumoney.com"

    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        with http_client() as c:
            r = c.get(self.PAGE_URL)
            r.raise_for_status()

        match = _RATES_BLOB.search(r.text)
        if not match:
            raise RuntimeError("LuLu rates blob not found on page")

        try:
            rows = json.loads(match.group(1))
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"LuLu rates blob malformed: {exc}") from exc

        # Find the INR row. LuLu lists each currency with iso code + rate.
        inr_row = next(
            (row for row in rows if row.get("currency") == "INR"),
            None,
        )
        if not inr_row:
            raise RuntimeError("LuLu INR row missing")

        try:
            rate = float(inr_row["rate"])
        except (KeyError, TypeError, ValueError) as exc:
            raise RuntimeError(f"LuLu INR rate malformed: {exc}") from exc

        # LuLu commonly waives fees for online instant transfers above a
        # threshold; we cannot know the exact fee without authenticating, so
        # leave it None rather than guess.
        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote="INR",
            amount_base=amount_aed,
            rate=rate,
            fee_base=None,
            received_quote=rate * amount_aed,
            effective_rate=rate,
            delivery_estimate="instant - 24h",
            url=self.PRODUCT_URL,
            status="ok",
            fetched_at=self._now_iso(),
        )
