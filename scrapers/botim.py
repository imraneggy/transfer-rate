"""Botim Pay — placeholder."""
from __future__ import annotations

from .base import BaseProvider, Quote


class BotimProvider(BaseProvider):
    id = "botim"
    display_name = "Botim Pay"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="App-only service, no public rate endpoint yet.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
