"""Western Union — placeholder."""
from __future__ import annotations

from .base import BaseProvider, Quote


class WesternUnionProvider(BaseProvider):
    id = "western_union"
    display_name = "Western Union"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="westernunion.com/ae — has a public quote API but requires "
                 "rotating IDs. Parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
