"""Lari Exchange scraper.

Strategy: Lari Exchange (lariexchange.com) is an ASP.NET WebForms site
that renders today's rates server-side as a static HTML table. The same
rate table appears 32 times across the homepage in different visual
contexts (header carousel, sidebar, footer mini-table, etc.) - every
instance contains the same authoritative AED -> X rate.

We just GET the homepage and regex-extract the first occurrence of:
  <td>INR</td>
  <td class="...Currency(Red|Green)..." style="...">
    <i class='fa fa-sort-down|sort-up'></i>
    <RATE>
  </td>

No __VIEWSTATE roundtrip, no AJAX postback - the rate is in the initial
HTML payload. This is the cheapest scraper we have.

Discovery (recorded 2026-05-04):
  - HTML pattern: <td> INR </td><td class="...Currency(Red|Green)...">
    <i class='fa fa-sort-down'></i> 25.75 </td>
  - 59 currencies in their dropdown; ~32 visible in static rate tables
  - Site has Cloudflare-style cookies but no JS challenge for GETs
  - Rate updates appear to be cron-driven (server-side regen of HTML)

Verified live 2026-05-04: AED -> INR ~25.75
"""
from __future__ import annotations

import os
import re
from pathlib import Path

import certifi
import httpx

from .base import BaseProvider, Quote

# Cert chain bundle for lariexchange.com — used when the system / certifi
# trust store rejects Lari's incomplete chain.  If this file does not
# exist, we fall back to certifi.where() (which works for most clients
# at this point; the v0.4 + v0.13 era of "verify=False" is no longer
# necessary on most runners).  The "fix lari TLS" path is therefore:
#   1. Prefer the explicit chain file if shipped: scrapers/certs/lari-chain.pem
#   2. Otherwise verify against certifi (correct, default-secure)
#   3. Never silently disable verification.
_LARI_CHAIN = Path(__file__).resolve().parent / "certs" / "lari-chain.pem"


class LariProvider(BaseProvider):
    id = "lari"
    display_name = "Lari Exchange"
    PAGE_URL = "https://www.lariexchange.com/"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        # TLS: verify against the bundled chain if present, else fall back
        # to certifi.  v0.28: was previously `verify=False`, which let any
        # MITM substitute the "rate" with arbitrary values and the result
        # would be committed to rates.json verbatim — see security audit
        # finding P1#3.  If the GHA runner ever genuinely cannot verify
        # Lari's chain again, drop the up-to-date chain into
        # scrapers/certs/lari-chain.pem and re-run.
        verify_target: str | bool
        if _LARI_CHAIN.exists():
            verify_target = str(_LARI_CHAIN)
        else:
            verify_target = certifi.where()

        with httpx.Client(
            timeout=20.0,
            follow_redirects=True,
            verify=verify_target,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/120.0.0.0 Safari/537.36"
                ),
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language": "en-US,en;q=0.9",
            },
        ) as client:
            r = client.get(self.PAGE_URL)
            r.raise_for_status()
            html = r.text

        # Pattern: <td>{CODE}</td> <td class="...Currency(Red|Green)..." ...>
        #            [optional <i class='fa fa-sort-(up|down)'></i>] {RATE} </td>
        # The CurrencyRed/Green and sort icon indicate movement vs previous
        # fetch - we don't surface that signal but capture rate value only.
        pattern = re.compile(
            r'<td[^>]*>\s*' + re.escape(target_currency) + r'\s*</td>\s*'
            r'<td[^>]*class="[^"]*Currency(?:Red|Green)[^"]*"[^>]*>\s*'
            r"(?:<i[^>]*class='fa fa-sort-(?:up|down)'[^>]*></i>\s*)?"
            r'(\d{1,3}\.\d{2,5})\s*</td>',
            re.IGNORECASE | re.DOTALL,
        )
        m = pattern.search(html)
        if not m:
            raise RuntimeError(
                f"Lari: could not find AED -> {target_currency} rate in page HTML. "
                f"The static rate table format may have changed."
            )

        try:
            rate = float(m.group(1))
        except (TypeError, ValueError) as exc:
            raise RuntimeError(f"Lari: rate value malformed: {exc}") from exc

        # Generic plausibility window (any corridor).
        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Lari: rate out of plausible range: {rate}")

        # Tighter corridor-specific window for AED→INR — the real rate has
        # been in 22..30 for the past decade.  This catches a poisoned
        # response that *looks* numeric (e.g. an attacker substituting
        # "1.0000" or "999.99") even when the broad bound would accept it.
        # If we add other corridors later, generalise this to a corridor
        # → (lo, hi) lookup table.
        if target_currency == "INR" and not 20.0 <= rate <= 32.0:
            raise RuntimeError(
                f"Lari: AED→INR rate {rate} outside plausible 20..32 window"
            )

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote=target_currency,
            amount_base=amount_base,
            rate=rate,
            fee_base=None,            # Fees vary by branch; not surfaced in homepage table
            received_quote=rate * amount_base,
            effective_rate=rate,
            delivery_estimate="branch / online transfer",
            url=self.PAGE_URL,
            status="ok",
            note="Static homepage rate table.",
            fetched_at=self._now_iso(),
        )
