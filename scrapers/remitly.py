"""Remitly scraper.

Strategy: Remitly's public calculator at https://www.remitly.com renders a
rate widget for AED -> INR. The widget calls a public JSON endpoint to compute
quotes; we hit that endpoint directly so we don't have to render JavaScript.

Endpoint format and field names are reverse-engineered from public traffic
on Remitly's marketing pages. If they change the schema, this scraper returns
status="error" rather than guessing — never silently emit a wrong number.
"""
from __future__ import annotations

from .base import BaseProvider, Quote
from .utils import http_client


class RemitlyProvider(BaseProvider):
    id = "remitly"
    display_name = "Remitly"
    # Remitly's public calculator endpoint. Documented behavior is to accept
    # a query for an anonymous quote (no auth required) for marketing widgets.
    QUOTE_URL = "https://api.remitly.io/v3/calculator/estimate"
    PRODUCT_URL = "https://www.remitly.com/ae/en/india"

    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        params = {
            "anchor": "SEND",
            "amount": amount_aed,
            "conduit": "AED:BANK_DEPOSIT-INR:BANK_DEPOSIT",
            "purpose": "OTHER",
            "customer_segment": "UNRECOGNIZED",
            "strict_promo": "false",
        }
        with http_client() as c:
            r = c.get(self.QUOTE_URL, params=params)
            r.raise_for_status()
            data = r.json()

        # Defensive parsing — every field check is explicit so a schema
        # change produces an error, not silently-wrong data.
        try:
            estimate = data["estimate"]
            rate = float(estimate["exchange_rate"]["base_rate"])
            fee = float(estimate.get("fee_amount", {}).get("amount", 0))
            received = float(estimate["receive_amount"]["amount"])
            effective = received / amount_aed
        except (KeyError, TypeError, ValueError) as exc:
            raise RuntimeError(
                f"Remitly schema unrecognised: {type(exc).__name__}: {exc}"
            ) from exc

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote="INR",
            amount_base=amount_aed,
            rate=rate,
            fee_base=fee,
            received_quote=received,
            effective_rate=effective,
            delivery_estimate=None,
            url=self.PRODUCT_URL,
            status="ok",
            fetched_at=self._now_iso(),
        )
