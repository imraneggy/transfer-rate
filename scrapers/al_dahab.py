"""Al Dahab Exchange scraper.

Strategy: aldahabexchange.ae is a WordPress + Elementor site that
renders the daily rate ticker directly into the HTML inside a
`<div class="currecymarquee">` element. No JS hydration, no AJAX —
the rate is in the page that anyone fetches anonymously.

Discovery (recorded for future maintainers):
  - URL: https://aldahabexchange.ae/
  - HTML excerpt:
        <div class="currecymarquee"><ul>
          <li>AED/INR = 25.79</li>
          <li>AED/PHP = 16.72</li>
          <li>AED/PKR = 75.90</li>
          <li>AED/NPR = 41.37</li>
          <li>AED/LKR = 86.93</li>
        </ul></div>
  - The marquee is updated by site staff (server-side); ticker
    contents change throughout the day as their boards update.

Verified live 2026-05-02: AED -> INR = 25.79.
"""
from __future__ import annotations

import re

from .base import BaseProvider, Quote
from .utils import http_client


# Anchor on the literal "AED/<TARGET> = " ticker text. The pattern is
# tight enough to refuse any bare "25.79" elsewhere in the page; it
# must follow a literal currency-pair label.
def _build_pattern(target: str) -> re.Pattern:
    return re.compile(
        rf'AED\s*/\s*{re.escape(target)}\s*=\s*([0-9]+\.[0-9]+)',
        re.IGNORECASE,
    )


class AlDahabProvider(BaseProvider):
    id = "al_dahab"
    display_name = "Al Dahab Exchange"
    PAGE_URL = "https://aldahabexchange.ae/"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        with http_client() as c:
            r = c.get(self.PAGE_URL)
            r.raise_for_status()

        m = _build_pattern(target_currency).search(r.text)
        if not m:
            raise RuntimeError(
                f"Al Dahab: AED/{target_currency} not in marquee. "
                f"Their ticker may not include this corridor today."
            )

        try:
            rate = float(m.group(1))
        except (TypeError, ValueError) as exc:
            raise RuntimeError(f"Al Dahab rate malformed: {exc}") from exc

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Al Dahab rate out of plausible range: {rate}")

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
            url=self.PAGE_URL,
            status="ok",
            note="Daily ticker rate from Al Dahab Exchange homepage.",
            fetched_at=self._now_iso(),
        )
