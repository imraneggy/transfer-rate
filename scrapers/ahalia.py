"""Ahalia Exchange scraper.

ahaliaexchange.com renders an inline JavaScript variable on the homepage
that the WordPress currency-calculator plugin reads to display rates:

    var cc_data = {
        "ajax_url":"https://ahaliaexchange.com/wp-content/plugins/...",
        "inr":"25.67",
        "pkr":"76.07",
        "bdt":"33.38",
        "npr":"39.89",
        "lkr":"86.96",
        "phpr":"16.68"
    };

The rates are present in the static HTML (no JS execution required), so
we can extract them with a single regex anchored on the property key.

Rate semantics: these are the customer-facing transfer rates (i.e., how
many units of the target currency the recipient gets for 1 AED). The
HTML table on the same page displays a different (lower) value which
appears to be the TT-buy rate — we use cc_data because it tracks the
mid-market more closely and matches what their calculator widget shows
to a customer.

Note: Ahalia uses "phpr" (not "php") as the Philippine Peso key.

Verified live 2026-05-02: AED -> INR = 25.67.
"""
from __future__ import annotations

import re

from .base import BaseProvider, Quote
from .utils import http_client


# Map our 3-letter currency code to Ahalia's cc_data key.
_CC_KEY = {
    "INR": "inr",
    "PKR": "pkr",
    "BDT": "bdt",
    "NPR": "npr",
    "LKR": "lkr",
    "PHP": "phpr",  # quirk: 'phpr' not 'php'
}


def _build_pattern(key: str) -> re.Pattern:
    # Match the JSON-style "key":"value" inside the cc_data object.
    return re.compile(
        rf'"{re.escape(key)}"\s*:\s*"([0-9]+\.[0-9]+)"',
    )


class AhaliaProvider(BaseProvider):
    id = "ahalia"
    display_name = "Ahalia Exchange"
    PAGE_URL = "https://ahaliaexchange.com/"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        cc_key = _CC_KEY.get(target_currency)
        if not cc_key:
            raise RuntimeError(
                f"Ahalia: no cc_data key for {target_currency}. "
                f"Add to _CC_KEY in scrapers/ahalia.py."
            )

        with http_client() as c:
            r = c.get(self.PAGE_URL)
            r.raise_for_status()

        m = _build_pattern(cc_key).search(r.text)
        if not m:
            raise RuntimeError(
                f"Ahalia: cc_data {cc_key!r} not found on homepage. "
                f"Their inline JS structure may have changed."
            )

        try:
            rate = float(m.group(1))
        except (TypeError, ValueError) as exc:
            raise RuntimeError(f"Ahalia rate malformed: {exc}") from exc

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Ahalia rate out of plausible range: {rate}")

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
            note="Customer transfer rate from cc_data inline JS.",
            fetched_at=self._now_iso(),
        )
