"""Comera (e& International) — placeholder."""
from __future__ import annotations

from .base import BaseProvider, Quote


class ComeraProvider(BaseProvider):
    id = "comera"
    display_name = "Comera"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="App-only service (e& International), no public rate endpoint yet.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
