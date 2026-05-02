"""Al Razouki Exchange — placeholder."""
from __future__ import annotations
from .base import BaseProvider, Quote


class AlRazoukiProvider(BaseProvider):
    id = "al_razouki"
    display_name = "Al Razouki Exchange"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="alrazouki.ae — parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
