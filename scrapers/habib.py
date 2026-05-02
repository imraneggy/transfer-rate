"""Habib Exchange — placeholder."""
from __future__ import annotations
from .base import BaseProvider, Quote


class HabibProvider(BaseProvider):
    id = "habib"
    display_name = "Habib Exchange"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="habibexchange.com — parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
