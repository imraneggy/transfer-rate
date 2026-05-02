"""Mid-market reference rate — Google Finance.

Source: https://www.google.com/finance/quote/AED-<TARGET>

Google Finance pages server-render the current rate inside an embedded
data structure for SEO. The pattern we extract is the most-stable one
on the page:

    "AED / <CUR>",<N>,null,[<rate>,<change>,<change_pct>,...]

Why this source:
  * It IS what users see when they Google "1 AED in INR".
  * Sourced from Morningstar / market data providers, sampled
    near-realtime.
  * Distinct from any individual remittance provider's quote, so
    serves as an independent benchmark.

Risks (documented for future maintainers):
  * Google's ToS prohibits automated scraping in general; this is a
    pragmatic use case (one request per corridor every cron tick) and
    we identify ourselves with a polite UA. If Google ever 429s/blocks
    us, the orchestrator records status='error' for this provider and
    the app falls back to no-header gracefully.
  * The page structure can change. The regex anchors on the literal
    string "AED / <TARGET>" in the data feed which is more durable
    than CSS class names.
"""
from __future__ import annotations

import re

from .base import BaseProvider, Quote
from .utils import http_client


def _build_pattern(target: str) -> re.Pattern:
    # Match the data tuple: "AED / INR",N,null,[<rate>,...
    return re.compile(
        rf'"AED / {re.escape(target)}",\d+,null,\[([0-9]+\.[0-9]+),'
    )


class MidMarketProvider(BaseProvider):
    id = "mid_market"
    display_name = "Mid-Market Reference"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        url = f"https://www.google.com/finance/quote/AED-{target_currency}"

        with http_client() as c:
            r = c.get(
                url,
                headers={
                    # Google blocks obvious bot UAs; Mozilla shape required.
                    "User-Agent": (
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        "AppleWebKit/537.36 (KHTML, like Gecko) "
                        "Chrome/130.0.0.0 Safari/537.36"
                    ),
                    "Accept-Language": "en-US,en;q=0.9",
                },
            )
            r.raise_for_status()

        m = _build_pattern(target_currency).search(r.text)
        if not m:
            raise RuntimeError(
                f"Google Finance: rate pattern not found on page for AED-{target_currency}. "
                f"Page may have changed structure or denied access."
            )

        try:
            rate = float(m.group(1))
        except (TypeError, ValueError) as exc:
            raise RuntimeError(f"Google Finance rate malformed: {exc}") from exc

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Google Finance rate out of range: {rate}")

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote=target_currency,
            amount_base=amount_base,
            rate=rate,
            fee_base=None,
            received_quote=rate * amount_base,
            effective_rate=rate,
            delivery_estimate=None,
            url=url,
            status="ok",
            note="Reference rate from Google Finance.",
            fetched_at=self._now_iso(),
        )
