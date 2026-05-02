"""Careem Pay — placeholder.

The previously-tried URL careem.com/en-AE/careem-pay returns 404.
Careem Pay appears to be primarily app-based; their marketing site
structure changes frequently. Until contributor identifies a stable
source, this remains a stub.
"""
from __future__ import annotations

from .base import BaseProvider, Quote


class CareemProvider(BaseProvider):
    id = "careem"
    display_name = "Careem Pay"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="Public rate page not yet identified. Careem Pay is mostly "
                 "in-app; potentially scrape from careem.com/pay marketing pages.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
