"""Aspora scraper.

Aspora is a UAE / UK / EU / US -> India focused remittance app. Their
landing-page widget hydrates client-side from an undocumented JSON
endpoint at `/forex/rates` on a region-specific host.

Discovery process (recorded for future maintainers):
  1. The homepage HTML embeds an Astro Island for CurrencyConverter; the
     static markup contains "₹--" rather than a live rate.
  2. The CurrencyConverter JS imports a service `g` that does
       fetch(`https://${HOST}/forex/rates`)
     keyed by region (api-z1 = GB, z2 = AE, z3 = US, z4 = EU).
  3. The endpoint returns
       {"AED": 25.81, "EUR": 111.15, "GBP": 128.73, ... }
     where each value is the all-in INR rate for sending 1 unit of that
     source currency to India. Default fee is zero (flatFee=[0,0] in the
     widget props), so this is the headline rate the user sees.
  4. `api-z1` is a global fallback that responds for all currencies; the
     other region hosts may not resolve from outside their region. We
     try z1 first.

If Aspora ever moves the endpoint, this will return status="error" with
the response code or exception class, NOT a guessed value.
"""
from __future__ import annotations

from typing import Optional

from .base import BaseProvider, Quote
from .utils import http_client


# Region hosts in fallback order. z1 is the UK mirror that responds globally;
# the others are kept as fallbacks if z1 ever changes posture.
_HOSTS = (
    "api-z1.aspora.com",
    "api-z2.aspora.com",
    "api-z3.aspora.com",
    "api-z4.aspora.com",
)


class AsporaProvider(BaseProvider):
    id = "aspora"
    display_name = "Aspora"
    PRODUCT_URL = "https://www.aspora.com/ae"

    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        last_exc: Optional[Exception] = None
        for host in _HOSTS:
            try:
                with http_client() as c:
                    r = c.get(
                        f"https://{host}/forex/rates",
                        headers={
                            # The endpoint is browser-oriented; sending Origin
                            # and Referer makes us look like a legitimate
                            # widget call rather than a bot probe.
                            "Origin": "https://www.aspora.com",
                            "Referer": "https://www.aspora.com/ae",
                        },
                    )
                    r.raise_for_status()
                    data = r.json()
                break
            except Exception as exc:  # noqa: BLE001 — try next host
                last_exc = exc
                continue
        else:
            raise RuntimeError(
                f"All Aspora region hosts failed. Last error: "
                f"{type(last_exc).__name__}: {last_exc}"
            )

        if not isinstance(data, dict) or "AED" not in data:
            raise RuntimeError(
                f"Aspora /forex/rates response missing AED key. "
                f"Keys: {list(data.keys()) if isinstance(data, dict) else type(data).__name__}"
            )

        try:
            rate = float(data["AED"])
        except (TypeError, ValueError) as exc:
            raise RuntimeError(f"Aspora AED value malformed: {exc}") from exc

        # Sanity bound: AED -> INR has historically traded ~22-26. A value
        # outside [10, 50] is almost certainly a wire-format change we should
        # surface as an error rather than display.
        if not 10.0 <= rate <= 50.0:
            raise RuntimeError(f"Aspora rate out of plausible range: {rate}")

        # Default Aspora widget shows zero fee for the AED corridor (flatFee
        # prop = 0). The headline rate IS the effective rate the user gets.
        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote="INR",
            amount_base=amount_aed,
            rate=rate,
            fee_base=0.0,
            received_quote=rate * amount_aed,
            effective_rate=rate,
            delivery_estimate="within minutes",
            url=self.PRODUCT_URL,
            status="ok",
            note=None,
            fetched_at=self._now_iso(),
        )
