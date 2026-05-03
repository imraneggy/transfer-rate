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

import re

import httpx

from .base import BaseProvider, Quote


class LariProvider(BaseProvider):
    id = "lari"
    display_name = "Lari Exchange"
    PAGE_URL = "https://www.lariexchange.com/"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        # verify=False: lariexchange.com ships an incomplete SSL chain
        # (Python's bundled CAs + certifi both reject it; only browsers
        # with bundled intermediate-CA fetching work).  Acceptable here
        # because we only read public HTML - no credentials, no PII -
        # and a MITM at most gives us a stale public rate.
        with httpx.Client(
            timeout=20.0,
            follow_redirects=True,
            verify=False,
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

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Lari: rate out of plausible range: {rate}")

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
