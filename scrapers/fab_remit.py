"""FAB Remit (First Abu Dhabi Bank) — placeholder."""
from __future__ import annotations
from .base import BaseProvider, Quote


class FabRemitProvider(BaseProvider):
    id = "fab_remit"
    display_name = "FAB Remit"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="bankfab.com — bank remittance product. Parser pending.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
