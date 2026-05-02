"""Wall Street Exchange — placeholder."""
from __future__ import annotations

from .base import BaseProvider, Quote


class WallStreetProvider(BaseProvider):
    id = "wall_street"
    display_name = "Wall Street Exchange"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="wallstreet.ae — parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
