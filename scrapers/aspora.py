"""Aspora scraper (formerly Vance).

Aspora is a UAE -> India focused remittance app. Their landing page displays
the current rate prominently. This is one of the simpler scrapers because
the page is purpose-built for this exact corridor.

NOTE FOR CONTRIBUTORS: this parser is intentionally minimal until the page
structure is verified live. Run `python -m scrapers.aspora` to see the raw
extraction; if it returns status="error", inspect the actual HTML and update
the selector below. See CONTRIBUTING.md for the workflow.
"""
from __future__ import annotations

import re

from .base import BaseProvider, Quote
from .utils import http_client


# Conservative pattern: we look for a clearly-labeled rate display. This MUST
# be tightened to the actual page structure — broad regexes are how aggregators
# silently report wrong numbers.
_RATE_PATTERN = re.compile(
    r'data-rate="(?P<rate>\d+\.\d+)"\s+data-pair="AED-INR"',
    re.IGNORECASE,
)


class AsporaProvider(BaseProvider):
    id = "aspora"
    display_name = "Aspora"
    PAGE_URL = "https://www.aspora.com"

    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        with http_client() as c:
            r = c.get(self.PAGE_URL)
            r.raise_for_status()

        match = _RATE_PATTERN.search(r.text)
        if not match:
            raise RuntimeError(
                "Aspora rate selector did not match — page structure may have "
                "changed. Update _RATE_PATTERN in scrapers/aspora.py."
            )

        try:
            rate = float(match.group("rate"))
        except ValueError as exc:
            raise RuntimeError(f"Aspora rate malformed: {exc}") from exc

        # Aspora typically advertises zero fees for the AE->IN corridor.
        # We surface that explicitly rather than leave it None.
        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote="INR",
            amount_base=amount_aed,
            rate=rate,
            fee_base=0.0,
            received_quote=rate * amount_aed,
            effective_rate=rate,
            delivery_estimate="within minutes",
            url=self.PAGE_URL,
            status="ok",
            fetched_at=self._now_iso(),
        )
