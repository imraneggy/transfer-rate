"""MoneyGram — placeholder."""
from __future__ import annotations

from .base import BaseProvider, Quote


class MoneyGramProvider(BaseProvider):
    id = "moneygram"
    display_name = "MoneyGram"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="moneygram.com — parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
