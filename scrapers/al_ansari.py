"""Al Ansari Exchange — placeholder.

One of the largest UAE exchange houses (founded 1966). Their rate page
exists at alansariexchange.com/exchange-rates but uses heavy JS rendering;
parser pending contributor work. Discovery hint: look for embedded
JSON in the page or a backend `/api/rates` endpoint.
"""
from __future__ import annotations

from .base import BaseProvider, Quote


class AlAnsariProvider(BaseProvider):
    id = "al_ansari"
    display_name = "Al Ansari Exchange"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        return self._stub(
            note="alansariexchange.com — parser pending. Major UAE exchange house "
                 "(founded 1966).",
            target_currency=target_currency,
            amount_base=amount_base,
        )
