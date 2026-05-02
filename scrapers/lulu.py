"""LuLu Exchange scraper.

Strategy: LuLu's homepage hydrates rates client-side from
https://lieservices.luluone.com:9443/liveccyrates with a Gravitee API
key embedded in their JavaScript (key is public — every browser visitor
sends the same one). The endpoint returns ALL currencies for the
configured 'aglcid' (location id) in a single response.

Discovery process (recorded for future maintainers):
  1. Homepage HTML had `<p>0</p>inr` placeholder, JS-rendered.
  2. Inspected /wp-content/themes/lulu_exchange/assets/js/custom.js,
     found `currencyURLArr` const mapping country slug -> request payload.
  3. UAE entry: `{"activityType":"rates.get","aglcid":784278,"instype":"LR"}`
     where "LR" means "live rate" (vs "FC" for foreign currency display).
  4. Found request URL `https://lieservices.luluone.com:9443/liveccyrates`
     and the public API key `x-gravitee-api-key: 94cfe79a-ec6a-...`.
  5. Response shape:
       {"code":..., "message":..., "payload":{"rates":[
         {"frmccy":"AED", "toccy":"INR", "rate":25.73, "sellrate":...,
          "buyrate":..., "ccyname":"INDIAN RUPEE",
          "svcprovcd":"LULUXAEDX###"}, ...
       ]}}

Note on rate semantics:
  * `rate` and `sellrate` are equal — this is the rate the customer GETS
    when sending AED to INR (i.e., 1 AED = 25.73 INR is paid out to the
    recipient).
  * `buyrate` is the wholesale buy-side, irrelevant for our display.

Verified live 2026-05-02: AED -> INR = 25.7300.
"""
from __future__ import annotations

import urllib.parse

from .base import BaseProvider, Quote
from .utils import http_client


class LuluProvider(BaseProvider):
    id = "lulu"
    display_name = "LuLu Money"
    PAGE_URL = "https://www.luluexchange.com/"

    # API host runs on port 9443 (Gravitee API gateway).
    API_HOST = "https://lieservices.luluone.com:9443"
    API_PATH = "/liveccyrates"

    # UAE location id (aglcid) embedded in their JS:
    AGLCID_UAE = 784278

    # Public Gravitee API key embedded in their JavaScript — every browser
    # visitor sends this exact key. Replaying it from a server is the
    # functional equivalent of a browser visit.
    GRAVITEE_KEY = "94cfe79a-ec6a-4f11-96c1-d12a928ad3f1"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        # Build the encoded payload they put in the query string
        payload_obj = (
            f'{{"activityType":"rates.get",'
            f'"aglcid":{self.AGLCID_UAE},'
            f'"instype":"LR"}}'
        )
        url = (
            f"{self.API_HOST}{self.API_PATH}"
            f"?payload={urllib.parse.quote(payload_obj)}"
        )
        headers = {
            "x-gravitee-api-key": self.GRAVITEE_KEY,
            "Origin": "https://www.luluexchange.com",
            "Referer": "https://www.luluexchange.com/",
        }

        # Use the centralized retry-with-backoff helper. LuLu's port 9443
        # is firewalled from GitHub Actions runners, so retries don't
        # actually help in CI — but they do help on flaky residential
        # networks where the initial connect occasionally drops.
        from .utils import get_with_retry
        try:
            r = get_with_retry(
                url, headers=headers, timeout=20.0, max_attempts=3,
            )
            data = r.json()
        except Exception as exc:
            raise RuntimeError(
                f"LuLu API request failed: {type(exc).__name__}: {exc}"
            ) from exc

        rates = (data.get("payload") or {}).get("rates") or []
        if not isinstance(rates, list):
            raise RuntimeError(
                f"LuLu response missing payload.rates list. Keys: "
                f"{list(data.keys())}"
            )

        match = next(
            (
                row for row in rates
                if isinstance(row, dict)
                and row.get("frmccy") == "AED"
                and row.get("toccy") == target_currency
            ),
            None,
        )
        if match is None:
            available = sorted({
                row.get("toccy") for row in rates
                if isinstance(row, dict) and row.get("frmccy") == "AED"
            })
            raise RuntimeError(
                f"LuLu has no AED->{target_currency}. Available targets: {available}"
            )

        try:
            rate = float(match.get("sellrate") or match.get("rate"))
        except (TypeError, ValueError) as exc:
            raise RuntimeError(f"LuLu rate value malformed: {exc}") from exc

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"LuLu rate out of plausible range: {rate}")

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote=target_currency,
            amount_base=amount_base,
            rate=rate,
            fee_base=None,  # fee structure varies by amount/channel
            received_quote=rate * amount_base,
            effective_rate=rate,  # fee unknown from this endpoint
            delivery_estimate=None,
            url=self.PAGE_URL,
            status="ok",
            note="Live rate via lieservices.luluone.com (LR/sell side).",
            fetched_at=self._now_iso(),
        )
