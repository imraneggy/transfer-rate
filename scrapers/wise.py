"""Wise scraper.

Strategy: Wise exposes a public live-rates endpoint at
https://wise.com/rates/live which returns the current mid-market rate as a
small JSON document. This is the "real exchange rate" Wise advertises in
all marketing. We surface it as Wise's quote.

Note on fees: Wise charges a transparent percentage fee on top of the
mid-market rate. Their fee depends on the send amount and is published
behind a separate quote endpoint that requires more parameters. For the
list view we present the mid-market rate (which IS what Wise charges before
their fee); a future enhancement could call the quote endpoint to surface
the post-fee effective rate.

This is the *reference* scraper — copy its structure when adding new ones.
"""
from __future__ import annotations

from .base import BaseProvider, Quote
from .utils import http_client


class WiseProvider(BaseProvider):
    id = "wise"
    display_name = "Wise"
    LIVE_URL = "https://wise.com/rates/live"
    PRODUCT_URL = "https://wise.com/gb/send-money/send-money-to-india"

    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        params = {
            "source": "AED",
            "target": "INR",
            "length": 1,
            "resolution": "hourly",
            "unit": "day",
        }
        with http_client() as c:
            r = c.get(self.LIVE_URL, params=params)
            r.raise_for_status()
            data = r.json()

        # Defensive parsing — every field check is explicit so a schema
        # change produces a clear error, not silently-wrong data.
        try:
            rate = float(data["value"])
            if data.get("source") != "AED" or data.get("target") != "INR":
                raise RuntimeError("Wise returned wrong currency pair")
        except (KeyError, TypeError, ValueError) as exc:
            raise RuntimeError(
                f"Wise schema unrecognised: {type(exc).__name__}: {exc}"
            ) from exc

        # Mid-market rate. Wise's actual fee is not exposed by this endpoint;
        # we leave fee_base / effective_rate as None to be honest about
        # what we do and don't know, rather than fabricate a number.
        received = rate * amount_aed
        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote="INR",
            amount_base=amount_aed,
            rate=rate,
            fee_base=None,
            received_quote=received,
            effective_rate=None,  # unknown without fee
            delivery_estimate="within minutes",
            url=self.PRODUCT_URL,
            status="ok",
            note="Mid-market rate; provider charges a transparent fee on top.",
            fetched_at=self._now_iso(),
        )
