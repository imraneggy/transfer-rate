"""Careem Pay scraper.

Careem Pay's marketing page at careem.com/en-AE/careem-pay shows live rates
for the most-used corridors, including AED -> INR. The page renders rates
client-side from an internal API. We try the API first; if that fails we
fall back to parsing the rendered page.
"""
from __future__ import annotations

import re

from .base import BaseProvider, Quote
from .utils import http_client


_RATE_PATTERN = re.compile(
    r'"sourceCurrency":"AED"[^}]*?"destinationCurrency":"INR"[^}]*?'
    r'"rate":(?P<rate>\d+\.\d+)',
    re.DOTALL,
)


class CareemProvider(BaseProvider):
    id = "careem"
    display_name = "Careem Pay"
    PAGE_URL = "https://www.careem.com/en-AE/careem-pay"

    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        with http_client() as c:
            r = c.get(self.PAGE_URL)
            r.raise_for_status()

        match = _RATE_PATTERN.search(r.text)
        if not match:
            raise RuntimeError(
                "Careem Pay rate not found on page — selector may need update."
            )

        try:
            rate = float(match.group("rate"))
        except ValueError as exc:
            raise RuntimeError(f"Careem rate malformed: {exc}") from exc

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
            delivery_estimate=None,
            url=self.PAGE_URL,
            status="ok",
            fetched_at=self._now_iso(),
        )
