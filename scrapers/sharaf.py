"""Sharaf Exchange scraper.

Sharaf Exchange publishes its board rates on a public JSON endpoint used by
its own website's rate widget:

    https://www.sharafexchange.ae/engine/wp-json/v1/currency-exchange-rates

The response contains one row per currency with a direct AED -> currency
exchange rate (unlike GCC Exchange's inverted currency -> AED quotes) and a
per-row "last_update" timestamp. No JavaScript execution, account, API key,
or session cookie is required.

Verified live 2026-06-12: AED -> INR = 25.7600.
"""
from __future__ import annotations

from typing import Any

from .base import BaseProvider, Quote
from .utils import get_with_retry


class SharafProvider(BaseProvider):
    id = "sharaf"
    display_name = "Sharaf Exchange"
    PAGE_URL = "https://www.sharafexchange.ae/"
    API_URL = "https://www.sharafexchange.ae/engine/wp-json/v1/currency-exchange-rates"

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
                "Transfer rate from Sharaf Exchange public rates endpoint. "
                f"Provider timestamp: {provider_timestamp}."
            ),
            fetched_at=self._now_iso(),
        )


def _extract_rate(payload: Any, target_currency: str) -> tuple[float, str]:
    if not isinstance(payload, dict):
        raise RuntimeError("Sharaf Exchange: response is not a JSON object")

    data = payload.get("data")
    if not isinstance(data, dict):
        raise RuntimeError("Sharaf Exchange: response missing data object")

    rows = data.get("details")
    if not isinstance(rows, list):
        raise RuntimeError("Sharaf Exchange: response missing data.details array")

    wanted = target_currency.upper()
    for row in rows:
        if not isinstance(row, dict):
            continue
        if str(row.get("currency_code", "")).strip().upper() != wanted:
            continue

        raw_rate = str(row.get("exchange_rate", "")).strip().replace(",", "")
        try:
            rate = float(raw_rate)
        except ValueError as exc:
            raise RuntimeError(f"Sharaf Exchange {wanted} rate malformed: {raw_rate!r}") from exc

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Sharaf Exchange {wanted} rate out of plausible range: {rate}")

        timestamp = row.get("last_update") or "unknown"
        return rate, str(timestamp)

    raise RuntimeError(f"Sharaf Exchange: {wanted} not found in data.details")
