"""Mid-market reference rate — fallback chain.

Sources, tried in order:
  1. Google Finance (https://www.google.com/finance/quote/AED-<TARGET>)
     Best match for what users see when they Google "1 AED in INR".
     But: Google often blocks datacenter IPs (GitHub Actions runners),
     serving captchas or different page structure. So we treat it as
     a "best effort" first source.
  2. Open ExchangeRate (https://open.er-api.com/v6/latest/AED)
     Public API designed for server-to-server use; reliable from any
     IP. Slightly different number than Google (different sampling).
     Used as the durable fallback.

Either source produces an objective benchmark independent of any
remittance provider's quote. The `note` field records which source
actually returned the rate, so users (and the runbook) can tell.
"""
from __future__ import annotations

import re
from typing import Optional, Tuple

from .base import BaseProvider, Quote
from .utils import http_client


def _google_finance_pattern(target: str) -> re.Pattern:
    return re.compile(
        rf'"AED / {re.escape(target)}",\d+,null,\[([0-9]+\.[0-9]+),'
    )


class MidMarketProvider(BaseProvider):
    id = "mid_market"
    display_name = "Mid-Market Reference"

    # Per-instance cache: open.er-api returns ALL currencies in one call,
    # so cache the full table for the duration of one orchestrator run.
    _erapi_cache: Optional[dict] = None

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        rate, source = self._fetch_rate(target_currency)

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
            url=None,
            status="ok",
            note=f"Reference rate from {source}.",
            fetched_at=self._now_iso(),
        )

    # --- Source 1: Google Finance (best for user mental model) -------------

    def _fetch_google(self, target_currency: str) -> float:
        url = f"https://www.google.com/finance/quote/AED-{target_currency}"
        with http_client(timeout=8.0) as c:
            r = c.get(
                url,
                headers={
                    "User-Agent": (
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        "AppleWebKit/537.36 (KHTML, like Gecko) "
                        "Chrome/130.0.0.0 Safari/537.36"
                    ),
                    "Accept-Language": "en-US,en;q=0.9",
                    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                },
            )
            r.raise_for_status()

        m = _google_finance_pattern(target_currency).search(r.text)
        if not m:
            raise RuntimeError("Google Finance: rate pattern not found")
        rate = float(m.group(1))
        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Google Finance rate out of range: {rate}")
        return rate

    # --- Source 2: Open ExchangeRate (reliable fallback) -------------------

    def _fetch_erapi(self, target_currency: str) -> float:
        if self._erapi_cache is None:
            with http_client(timeout=8.0) as c:
                r = c.get("https://open.er-api.com/v6/latest/AED")
                r.raise_for_status()
                data = r.json()
            if data.get("result") != "success":
                raise RuntimeError(
                    f"open.er-api.com response not OK: {data.get('result')}"
                )
            rates = data.get("rates")
            if not isinstance(rates, dict):
                raise RuntimeError("open.er-api.com response missing 'rates'")
            self._erapi_cache = rates

        rate = self._erapi_cache.get(target_currency)
        if rate is None:
            raise RuntimeError(
                f"open.er-api.com has no AED->{target_currency}"
            )
        rate_f = float(rate)
        if not 0.0001 <= rate_f <= 10000.0:
            raise RuntimeError(f"Open ExchangeRate rate out of range: {rate_f}")
        return rate_f

    # --- Fallback chain ----------------------------------------------------

    def _fetch_rate(self, target_currency: str) -> Tuple[float, str]:
        """Try Google Finance first; fall back to Open ExchangeRate."""
        google_err: Optional[str] = None
        try:
            return self._fetch_google(target_currency), "Google Finance"
        except Exception as exc:  # noqa: BLE001
            google_err = f"{type(exc).__name__}: {exc}"

        try:
            return self._fetch_erapi(target_currency), "Open ExchangeRate"
        except Exception as exc:  # noqa: BLE001
            raise RuntimeError(
                f"All mid-market sources failed. "
                f"Google: {google_err}. "
                f"Open ExchangeRate: {type(exc).__name__}: {exc}"
            ) from exc
