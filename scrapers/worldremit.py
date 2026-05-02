"""WorldRemit — placeholder."""
from __future__ import annotations
from .base import BaseProvider, Quote


class WorldRemitProvider(BaseProvider):
    id = "worldremit"
    display_name = "WorldRemit"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="worldremit.com — has a public quote API; parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
