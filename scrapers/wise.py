"""Wise scraper — multi-corridor.

Wise's public live-rates endpoint at https://wise.com/rates/live accepts
any 3-letter source/target pair and returns the current mid-market rate
as a small JSON document. We loop across our SUPPORTED_TARGETS and emit
one Quote per corridor.

Wise's transparent fee on top of the mid-market rate is exposed through
a separate quote endpoint that requires more parameters; for the list
view we present the mid-market rate (Wise's marketing rate) and leave
fee/effective_rate as None to be honest about what we know.
"""
from __future__ import annotations

from .base import BaseProvider, Quote
from .utils import http_client


class WiseProvider(BaseProvider):
    id = "wise"
    display_name = "Wise"
    LIVE_URL = "https://wise.com/rates/live"
    PRODUCT_URL = "https://wise.com"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        params = {
            "source": "AED",
            "target": target_currency,
            "length": 1,
            "resolution": "hourly",
            "unit": "day",
        }
        with http_client() as c:
            r = c.get(self.LIVE_URL, params=params)
            r.raise_for_status()
            data = r.json()

        try:
            rate = float(data["value"])
            if data.get("source") != "AED" or data.get("target") != target_currency:
                raise RuntimeError(
                    f"Wise returned wrong pair: source={data.get('source')} "
                    f"target={data.get('target')}"
                )
        except (KeyError, TypeError, ValueError) as exc:
            raise RuntimeError(
                f"Wise schema unrecognised: {type(exc).__name__}: {exc}"
            ) from exc

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote=target_currency,
            amount_base=amount_base,
            rate=rate,
            fee_base=None,
            received_quote=rate * amount_base,
            effective_rate=None,
            delivery_estimate="within minutes",
            url=self.PRODUCT_URL,
            status="ok",
            note="Mid-market rate; provider charges a transparent fee on top.",
            fetched_at=self._now_iso(),
        )
