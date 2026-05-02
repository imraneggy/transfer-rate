"""Xoom (PayPal) — placeholder."""
from __future__ import annotations
from .base import BaseProvider, Quote


class XoomProvider(BaseProvider):
    id = "xoom"
    display_name = "Xoom (PayPal)"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="xoom.com — parser pending. Owned by PayPal.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
