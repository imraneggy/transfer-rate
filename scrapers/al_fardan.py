"""Al Fardan Exchange — placeholder."""
from __future__ import annotations

from .base import BaseProvider, Quote


class AlFardanProvider(BaseProvider):
    id = "al_fardan"
    display_name = "Al Fardan Exchange"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="alfardanexchange.com — parser pending. UAE exchange house since 1971.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
