"""InstaReM (Nium) — placeholder."""
from __future__ import annotations
from .base import BaseProvider, Quote


class InstaRemProvider(BaseProvider):
    id = "instarem"
    display_name = "InstaReM"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="instarem.com — parser pending. Owned by Nium.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
