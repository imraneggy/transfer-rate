"""Aspora scraper.

Aspora's public /forex/rates endpoint returns a JSON object keyed by
SOURCE currency, with values being the rate to INR. Schema (verified
2026-05-02):
  {"AED": 25.81, "EUR": 111.15, "GBP": 128.73, ...}

This means Aspora exposes multiple SOURCE currencies, all pointing at
INR. From our app's perspective (sending AED), only the AED entry is
relevant. If/when we add support for sending FROM other currencies,
the API is already there.

Currently restricted to AED -> INR. For other AED-> targets, the
provider has no public endpoint we've identified; we raise and the
orchestrator records status='error' for those corridors.
"""
from __future__ import annotations

from typing import Optional

from .base import BaseProvider, Quote
from .utils import http_client


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

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        if target_currency != "INR":
            raise RuntimeError(
                f"Aspora currently exposes only AED->INR; AED->{target_currency} "
                f"not supported."
            )

        last_exc: Optional[Exception] = None
        for host in _HOSTS:
            try:
                with http_client() as c:
                    r = c.get(
                        f"https://{host}/forex/rates",
                        headers={
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

        if not 10.0 <= rate <= 50.0:
            raise RuntimeError(f"Aspora rate out of plausible range: {rate}")

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote="INR",
            amount_base=amount_base,
            rate=rate,
            fee_base=0.0,
            received_quote=rate * amount_base,
            effective_rate=rate,
            delivery_estimate="within minutes",
            url=self.PRODUCT_URL,
            status="ok",
            fetched_at=self._now_iso(),
        )
