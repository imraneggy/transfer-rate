"""GCC Exchange scraper.

Strategy: GCC publishes a public REST endpoint at
`gccexchange.com/media/index.php/exchangerate/getexchangerate` that
returns the full rate table for all currencies in one GET. No auth.

Discovery (recorded for future maintainers):
  - JS file: /assets/js/gccexchangecustom.js
  - Defines `var apicall = "https://" + host + "/media/index.php/
      exchangerate/getexchangerate"` and calls it via $.ajax.
  - Response shape:
      {"result":..., "totalrows":13, "exchangerate":[
          {"CurrencyCode":"INR", "CurrencyName":"INDIAN RUPEES",
           "ExchangeRate":"0.038744"}, ...
      ]}
  - **Critical detail**: `ExchangeRate` is INVERTED — it stores
    AED-per-1-target (i.e. how much AED to buy 1 unit of target).
    The displayed rate is `1 / ExchangeRate`. The frontend JS does
    `var resultval = 1 / arrayexchangerate[i].ExchangeRate`. We do
    the same.

Verified live 2026-05-02: AED → INR = 25.8104 (computed from
ExchangeRate=0.038744).
"""
from __future__ import annotations

from .base import BaseProvider, Quote
from .utils import http_client


class GccExchangeProvider(BaseProvider):
    id = "gcc_exchange"
    display_name = "GCC Exchange"
    PAGE_URL = "https://www.gccexchange.com/"
    API_URL = "https://www.gccexchange.com/media/index.php/exchangerate/getexchangerate"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        with http_client() as c:
            r = c.get(
                self.API_URL,
                headers={
                    "Origin": "https://www.gccexchange.com",
                    "Referer": "https://www.gccexchange.com/",
                },
            )
            r.raise_for_status()
            data = r.json()

        rates = data.get("exchangerate")
        if not isinstance(rates, list):
            raise RuntimeError(
                f"GCC: response missing 'exchangerate' list. "
                f"Keys: {list(data.keys())}"
            )

        match = next(
            (
                row for row in rates
                if isinstance(row, dict)
                and row.get("CurrencyCode") == target_currency
            ),
            None,
        )
        if match is None:
            available = sorted({
                row.get("CurrencyCode") for row in rates if isinstance(row, dict)
            })
            raise RuntimeError(
                f"GCC: no entry for {target_currency}. Available: {available}"
            )

        try:
            inv_rate = float(match["ExchangeRate"])
        except (TypeError, ValueError) as exc:
            raise RuntimeError(f"GCC ExchangeRate malformed: {exc}") from exc

        if inv_rate <= 0:
            raise RuntimeError(f"GCC ExchangeRate non-positive: {inv_rate}")

        # GCC stores inverse: AED-per-1-target. Flip to get target-per-1-AED.
        rate = 1.0 / inv_rate

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"GCC computed rate out of range: {rate}")

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
            note="Bank-transfer rate from GCC public API.",
            fetched_at=self._now_iso(),
        )
