"""Emirates NBD DirectRemit — placeholder."""
from __future__ import annotations
from .base import BaseProvider, Quote


class EmiratesNbdProvider(BaseProvider):
    id = "emirates_nbd"
    display_name = "Emirates NBD DirectRemit"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="emiratesnbd.com — bank remittance product. Parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
