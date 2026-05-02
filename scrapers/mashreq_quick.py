"""Mashreq QuickRemit — placeholder."""
from __future__ import annotations
from .base import BaseProvider, Quote


class MashreqQuickProvider(BaseProvider):
    id = "mashreq_quick"
    display_name = "Mashreq QuickRemit"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="mashreqbank.com — bank remittance product. Parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
