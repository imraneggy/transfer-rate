"""Botim Pay — placeholder.

Botim's BotimPay/BotimSend feature is mobile-app-only with no public web
rate display. Adding a working scraper requires reverse-engineering their
mobile API, which is fragile and ToS-sensitive.

This stub keeps Botim visible in the app (with a "Coming soon" status)
so users know the project is aware of it. If you discover a public
endpoint, replace this stub — see CONTRIBUTING.md.
"""
from __future__ import annotations

from .base import BaseProvider, Quote


class BotimProvider(BaseProvider):
    id = "botim"
    display_name = "Botim Pay"

    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        return self._stub(
            note="App-only service, no public rate endpoint yet. "
                 "Tracking issue welcome."
        )
