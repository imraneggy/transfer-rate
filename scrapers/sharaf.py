"""Sharaf Exchange — placeholder."""
from __future__ import annotations

from .base import BaseProvider, Quote


class SharafProvider(BaseProvider):
    id = "sharaf"
    display_name = "Sharaf Exchange"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="sharafexchange.ae — parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
