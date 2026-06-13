"""Orient Exchange scraper.

Orient Exchange publishes transfer rates on a public JSON endpoint used by
its own rates page:

    https://www.orientexchange.com/Orient/GetExchangeRates

The response contains one row per target currency with customer-facing
transfer rates, e.g. INR Rate="25.7599" and a provider timestamp. No
JavaScript execution, account, API key, or session cookie is required.

Verified live 2026-06-13: AED -> INR = 25.7599.
"""
from __future__ import annotations

from typing import Any

from .base import BaseProvider, Quote
from .utils import get_with_retry


class OrientExchangeProvider(BaseProvider):
    id = "orient_exchange"
    display_name = "Orient Exchange"
    PAGE_URL = "https://www.orientexchange.com/Orient/CurrencyRates"
    API_URL = "https://www.orientexchange.com/Orient/GetExchangeRates"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        response = get_with_retry(
            self.API_URL,
            headers={
                "Accept": "application/json, text/plain, */*",
                "Referer": self.PAGE_URL,
            },
        )
        payload = response.json()
        rate, provider_timestamp = _extract_rate(payload, target_currency)

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
            note=(
                "Transfer rate from Orient public rates endpoint. "
                f"Provider timestamp: {provider_timestamp}."
            ),
            fetched_at=self._now_iso(),
        )


def _extract_rate(payload: Any, target_currency: str) -> tuple[float, str]:
    if not isinstance(payload, dict):
        raise RuntimeError("Orient Exchange: response is not a JSON object")

    rows = payload.get("rateList")
    if not isinstance(rows, list):
        raise RuntimeError("Orient Exchange: response missing rateList array")

    wanted = target_currency.upper()
    for row in rows:
        if not isinstance(row, dict):
            continue
        if str(row.get("CurrencyCode", "")).upper() != wanted:
            continue

        raw_rate = str(row.get("Rate", "")).strip().replace(",", "")
        try:
            rate = float(raw_rate)
        except ValueError as exc:
            raise RuntimeError(f"Orient Exchange {wanted} rate malformed: {raw_rate!r}") from exc

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Orient Exchange {wanted} rate out of plausible range: {rate}")

        timestamp = (
            row.get("LastUpdatedOnDateFormattedString")
            or payload.get("lastUpdatedOnDate")
            or "unknown"
        )
        return rate, str(timestamp)

    raise RuntimeError(f"Orient Exchange: {wanted} not found in rateList")