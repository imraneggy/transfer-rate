"""Index Exchange scraper.

Strategy: indexexchange.ae renders the current AED -> INR rate directly
into the homepage HTML inside an element with id="span_rate". The full
page contains a "1 AED = <rate> <currency_code>" sentence that the
backend pre-renders for SEO. We extract the rate by id, anchored on
the span_cu_code that follows it (so we know it's the INR rate, not
some other currency the dropdown might pre-select).

Verified via raw HTML probe 2026-05-02 (rate=25.673940949936).
"""
from __future__ import annotations

import re

from .base import BaseProvider, Quote
from .utils import http_client


# Anchor on the literal HTML structure: <span id="span_rate">RATE</span>
# followed by <span id="span_cu_code">INR</span>. The pattern is tight
# enough to refuse other currencies if Index ever changes the default.
_RE_RATE_INR = re.compile(
    r'id="span_rate">([0-9]+\.[0-9]+)</span>\s*<span\s+id="span_cu_code">\s*INR\s*</span>',
    re.DOTALL,
)


class IndexExchangeProvider(BaseProvider):
    id = "index_exchange"
    display_name = "Index Exchange"
    PAGE_URL = "https://www.indexexchange.ae/"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        if target_currency != "INR":
            raise RuntimeError(
                f"Index Exchange scraper currently handles AED->INR only "
                f"(homepage default); AED->{target_currency} would need a "
                f"different request shape."
            )

        with http_client() as c:
            r = c.get(self.PAGE_URL)
            r.raise_for_status()

        m = _RE_RATE_INR.search(r.text)
        if not m:
            raise RuntimeError(
                "Index Exchange: AED->INR rate pattern not matched. "
                "Their homepage may have changed structure or default "
                "currency selection."
            )

        try:
            rate = float(m.group(1))
        except ValueError as exc:
            raise RuntimeError(f"Index Exchange rate malformed: {exc}") from exc

        if not 10.0 <= rate <= 50.0:
            raise RuntimeError(f"Index Exchange rate out of plausible range: {rate}")

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote="INR",
            amount_base=amount_base,
            rate=rate,
            fee_base=None,
            received_quote=rate * amount_base,
            effective_rate=rate,
            delivery_estimate="2 working days (per Index page)",
            url=self.PAGE_URL,
            status="ok",
            note="Indicative rate from indexexchange.ae homepage.",
            fetched_at=self._now_iso(),
        )
