"""TransferGo scraper.

TransferGo exposes a simple, public, undocumented FX rates endpoint at
`https://my.transfergo.com/api/fx-rates?from=AED&to=INR` returning:

    {"from":"AED","to":"INR","rate":25.84357,"fromAmount":1,"toAmount":25.84}

The endpoint requires no authentication and accepts standard browser
headers. It returns the indicative customer-facing rate before fees.

Note that TransferGo's actual quotation API (which also factors in their
fee tiers) is `/api/booking/quotes`, but it requires a country pair plus
amount and isn't strictly necessary for our comparison purpose — the
indicative rate from `/api/fx-rates` is what they advertise on their
landing pages and is consistent with what the booking flow shows.

Verified live 2026-05-02: AED -> INR = 25.84357.
"""
from __future__ import annotations

from .base import BaseProvider, Quote
from .utils import http_client


class TransferGoProvider(BaseProvider):
    id = "transfergo"
    display_name = "TransferGo"
    PAGE_URL = "https://www.transfergo.com/en-ae/send-money-from-united-arab-emirates/aed/india/inr"
    API_URL = "https://my.transfergo.com/api/fx-rates"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        params = {"from": "AED", "to": target_currency}
        headers = {
            "Accept": "application/json",
            "Origin": "https://www.transfergo.com",
            "Referer": "https://www.transfergo.com/",
        }

        with http_client() as c:
            r = c.get(self.API_URL, params=params, headers=headers)
            r.raise_for_status()
            data = r.json()

        rate = data.get("rate")
        if not isinstance(rate, (int, float)):
            raise RuntimeError(
                f"TransferGo response missing numeric `rate`. "
                f"Got: {data!r}"
            )

        rate = float(rate)
        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"TransferGo rate out of plausible range: {rate}")

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote=target_currency,
            amount_base=amount_base,
            rate=rate,
            fee_base=None,  # Fee depends on payment method + speed; not in this endpoint
            received_quote=rate * amount_base,
            effective_rate=rate,
            delivery_estimate="bank deposit, varies",
            url=self.PAGE_URL,
            status="ok",
            note="Indicative rate from TransferGo public fx-rates API.",
            fetched_at=self._now_iso(),
        )
