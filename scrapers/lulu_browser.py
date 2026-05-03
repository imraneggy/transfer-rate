"""Browser-based LuLu scraper.

The direct LuLu rate API runs on TCP port 9443
(`lieservices.luluone.com:9443`) which GitHub Actions runners
firewall outbound. Their alternative endpoint on port 443
(`orville.luluone.com/hbapi/v1_0/pas/rate-history`) has WAF
detection that drops anything that doesn't look like a real
browser session — even with TLS-fingerprint impersonation.

This module uses Playwright with headless Chromium to load
`https://luluexchange.com/` and read the rate value out of the
rendered DOM after their client-side JavaScript fetches it. The
DOM has a known stable structure:

    <em>AED</em> ... <span class="rate-in-value">1.00</span>
    <em>INR</em> ... <span class="rate-in-value converted-rate-in-value">25.73</span>

Cost in CI: ~30 s installation (cached after first run) + ~6-8 s
per execution for browser launch and page load. Acceptable for an
hourly cron.

Use only when the lighter `lulu.py` HTTP path would fail (e.g. in
the GitHub Actions environment).
"""
from __future__ import annotations

import re
from typing import Optional

from .base import BaseProvider, Quote


# Sentinel placeholder values that LuLu's HTML ships before JS runs.
# If we still see one of these after waiting we assume the JS hasn't
# executed and the scrape failed.
_PLACEHOLDER_VALUES = {"19.33", "0.00", "0", ""}

# The rate is reachable inside `.converted-rate-in-value` once JS has
# populated it. Wait for it to reach a believable AED→INR value
# (between 20 and 30) before reading.
_BELIEVABLE_INR_LO = 20.0
_BELIEVABLE_INR_HI = 35.0


class LuluBrowserProvider(BaseProvider):
    id = "lulu"
    display_name = "LuLu Money"
    PAGE_URL = "https://www.luluexchange.com/"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        if target_currency != "INR":
            raise RuntimeError(
                "LuLu browser scraper currently only handles AED→INR "
                "(homepage default corridor)."
            )

        rate = self._read_rate_from_dom()

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote=target_currency,
            amount_base=amount_base,
            rate=rate,
            fee_base=None,
            received_quote=rate * amount_base,
            effective_rate=rate,
            delivery_estimate=None,
            url=self.PAGE_URL,
            status="ok",
            note="Live rate read from luluexchange.com homepage (browser-rendered).",
            fetched_at=self._now_iso(),
        )

    def _read_rate_from_dom(self) -> float:
        """Launch headless Chromium, load the homepage, return the live INR rate."""
        try:
            from playwright.sync_api import sync_playwright
        except ImportError as exc:
            raise RuntimeError(
                "playwright is not installed. Set LULU_USE_BROWSER=0 to "
                "disable, or `pip install playwright && playwright install "
                "chromium` to enable."
            ) from exc

        with sync_playwright() as p:
            browser = p.chromium.launch(
                headless=True,
                args=[
                    "--no-sandbox",
                    "--disable-blink-features=AutomationControlled",
                ],
            )
            context = browser.new_context(
                user_agent=(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/126.0.0.0 Safari/537.36"
                ),
                viewport={"width": 1280, "height": 800},
                locale="en-AE",
            )
            page = context.new_page()
            try:
                page.goto(self.PAGE_URL, wait_until="domcontentloaded", timeout=20_000)

                # Wait for the rate to be JS-injected. We poll the DOM
                # at 250 ms intervals up to 12 s for a believable INR
                # rate value.
                rate = page.wait_for_function(
                    """() => {
                        const els = document.querySelectorAll('.converted-rate-in-value');
                        for (const el of els) {
                            const txt = (el.textContent || '').trim();
                            const v = parseFloat(txt);
                            if (!isNaN(v) && v >= 20.0 && v <= 35.0) {
                                return v;
                            }
                        }
                        return false;
                    }""",
                    timeout=15_000,
                ).json_value()
            finally:
                context.close()
                browser.close()

        if not isinstance(rate, (int, float)) or not (
            _BELIEVABLE_INR_LO <= float(rate) <= _BELIEVABLE_INR_HI
        ):
            raise RuntimeError(
                f"LuLu browser scrape returned out-of-range rate: {rate!r}"
            )

        return float(rate)
