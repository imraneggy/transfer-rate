"""LuLu Money — placeholder.

The previously-tried URL `lulumoney.com/exchange-rates` returns 404.
LuLu may have moved their public rate ticker behind app-only or behind
a different path. Until contributor identifies a stable public source,
this remains a stub.
"""
from __future__ import annotations

from .base import BaseProvider, Quote


class LuluProvider(BaseProvider):
    id = "lulu"
    display_name = "LuLu Money"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="Public rate page not yet identified. Discovery candidates: "
                 "lulumoney.com, lulufinancialholdings.com, /api/rates.",
            target_currency=target_currency,
            amount_base=amount_base,
        )
