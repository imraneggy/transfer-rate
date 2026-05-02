"""Federal Exchange scraper.

Strategy: federalexchange.ae is a WordPress site with currency cards
rendered server-side into the homepage HTML. Each currency card has
the structure:

    <div class='currency-item'>
      <div class='currency-name'>INDIAN RUPEE</div>
      <div class='currency-info'>
        <p class='currency-rate'>25.77 INR 🇮🇳</p>
      </div>
    </div>

We anchor on the literal currency NAME followed by the rate element
so we can support multiple corridors deterministically.

Verified live 2026-05-02: AED -> INR = 25.77.

Note: Federal Exchange has Abu Dhabi presence and is a meaningful
addition for AD-based users. They appear to be a smaller chain than
Al Ansari/LuLu but cover the same UAE-India corridor.
"""
from __future__ import annotations

import re

from .base import BaseProvider, Quote
from .utils import http_client


# Map our 3-letter currency code to the human label Federal uses on their
# cards. Add more as we expand corridors.
_CURRENCY_LABEL = {
    "INR": "INDIAN RUPEE",
    "PKR": "PAKISTANI RUPEE",
    "PHP": "PHILIPPINE PESO",
    "BDT": "BANGLADESHI TAKA",
    "EGP": "EGYPTIAN POUND",
    "NPR": "NEPALI RUPEE",
    "LKR": "SRI LANKAN RUPEE",
    "USD": "US DOLLAR",
    "EUR": "EURO",
    "GBP": "POUND STERLING",
}


def _build_pattern(label: str) -> re.Pattern:
    # Find the currency-name div with this label, then the FIRST
    # currency-rate <p> after it. DOTALL because there are newlines
    # between the divs.
    return re.compile(
        rf"<div\s+class=['\"]currency-name['\"]>\s*{re.escape(label)}\s*</div>"
        rf"\s*<div\s+class=['\"]currency-info['\"]>\s*"
        rf"<p\s+class=['\"]currency-rate['\"]>\s*([0-9]+\.[0-9]+)",
        re.DOTALL | re.IGNORECASE,
    )


class FederalExchangeProvider(BaseProvider):
    id = "federal_exchange"
    display_name = "Federal Exchange"
    PAGE_URL = "https://www.federalexchange.ae/"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        label = _CURRENCY_LABEL.get(target_currency)
        if not label:
            raise RuntimeError(
                f"Federal Exchange: no known label for {target_currency}. "
                f"Add to _CURRENCY_LABEL in scrapers/federal_exchange.py."
            )

        with http_client() as c:
            r = c.get(self.PAGE_URL)
            r.raise_for_status()

        m = _build_pattern(label).search(r.text)
        if not m:
            raise RuntimeError(
                f"Federal Exchange: '{label}' card not found on page. "
                f"Their homepage structure may have changed."
            )

        try:
            rate = float(m.group(1))
        except (TypeError, ValueError) as exc:
            raise RuntimeError(f"Federal Exchange rate malformed: {exc}") from exc

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Federal Exchange rate out of plausible range: {rate}")

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
            note="Daily rate from Federal Exchange homepage card.",
            fetched_at=self._now_iso(),
        )
