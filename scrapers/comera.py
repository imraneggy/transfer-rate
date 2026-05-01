"""Comera (e& International) — placeholder.

Comera is e& Money's UAE remittance app. Rates are shown only after sign-in
in the mobile app; no public rate widget exists at time of writing.

If a public endpoint becomes available, replace this stub with a real
fetcher following the structure of scrapers/wise.py.
"""
from __future__ import annotations

from .base import BaseProvider, Quote


class ComeraProvider(BaseProvider):
    id = "comera"
    display_name = "Comera"

    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        return self._stub(
            note="App-only service (e& International), no public rate "
                 "endpoint yet."
        )
