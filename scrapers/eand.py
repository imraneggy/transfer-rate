"""e& Money — placeholder.

e& Money (the Etisalat brand) provides remittance rates inside its app.
Public marketing pages occasionally surface a rate but the format is
unstable. Until a stable public source is identified, this remains a stub.
"""
from __future__ import annotations

from .base import BaseProvider, Quote


class EandProvider(BaseProvider):
    id = "eand"
    display_name = "e& Money"

    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        return self._stub(
            note="No stable public rate page yet. Contributions welcome."
        )
