"""Unimoni (formerly UAE Exchange) — placeholder."""
from __future__ import annotations

from .base import BaseProvider, Quote


class UnimoniProvider(BaseProvider):
    id = "unimoni"
    display_name = "Unimoni"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="unimoni.ae — parser pending. Formerly UAE Exchange.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
