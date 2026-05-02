"""Al Ansari Exchange scraper.

Strategy: Al Ansari is a WordPress site with a custom plugin
`aln-currency-convertor` that POSTs to wp-admin/admin-ajax.php with a
WP nonce for CSRF protection. We:

  1. Fetch the homepage to extract `CC_Ajax_Object` (contains ajax_nonce
     valid for the duration of the WP session).
  2. POST `convert_action` with their internal numeric currency codes
     and the discovered nonce.

Discovery (recorded for future maintainers):
  - JS file: /wp-content/plugins/aln-currency-convertor/js/convertor.js
  - Defines $.ajax POST to admin-ajax.php with:
      action=convert_action, currfrom=<int>, currto=<int>,
      cntcode=<int>, amt=<n>, security=<nonce>, trtype=<code>
  - Internal currency codes (NOT ISO 4217):
      AED=91, USD=92, INR=26, PHP=49, PKR=27, EGP=19, JOD=20, BDT=59
  - trtype values supported per corridor:
      BT = Bank Transfer    (works for AED->INR)
      CP = Cash Pickup
      EFT, RM, DC = others (some corridor-specific)
  - Response shape (success):
      {"amount":"25.733", "get_rate":"25.7069", "status_msg":"SUCCESS"}

Verified live 2026-05-02: AED -> INR (BT) = 25.7069.
"""
from __future__ import annotations

import json
import re
from typing import Optional

import httpx

from .base import BaseProvider, Quote
from .utils import http_client


# Al Ansari's internal numeric currency codes — discovered from
# data-toccy / data-fromccy attributes on their currency selector.
_CCY_CODES = {
    "AED": 91,
    "USD": 92,
    "INR": 26,
    "PHP": 49,
    "PKR": 27,
    "EGP": 19,
    "JOD": 20,
    "BDT": 59,
    # Add more as you see them in the page HTML.
}

# Country codes — same numeric scheme. cntcode is the destination's code.
_CNT_CODES = {
    "INR": 26,  # India
    "PHP": 49,
    "PKR": 27,
    "EGP": 19,
    "BDT": 59,
}


class AlAnsariProvider(BaseProvider):
    id = "al_ansari"
    display_name = "Al Ansari Exchange"
    PAGE_URL = "https://alansariexchange.com/"
    AJAX_URL = "https://alansariexchange.com/wp-admin/admin-ajax.php"

    # Reuse a session so cookies + nonce stay together. One scraper run
    # = one homepage fetch + one AJAX call.
    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        if target_currency not in _CCY_CODES:
            raise RuntimeError(
                f"Al Ansari: no internal currency code for {target_currency}"
            )

        client = httpx.Client(
            timeout=15.0,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "transfer-rate-bot/1.0"
                ),
                "Accept-Language": "en-US,en;q=0.9",
            },
            follow_redirects=True,
        )

        try:
            # Step 1: fetch homepage, extract CC_Ajax_Object
            home = client.get(self.PAGE_URL)
            home.raise_for_status()
            m = re.search(
                r'CC_Ajax_Object\s*=\s*(\{[^}]+\})', home.text
            )
            if not m:
                raise RuntimeError(
                    "Al Ansari: CC_Ajax_Object not found in homepage. "
                    "Plugin may have changed."
                )
            try:
                cc = json.loads(m.group(1))
                ajax_url = cc.get("ajax_url") or self.AJAX_URL
                nonce = cc.get("ajax_nonce")
            except json.JSONDecodeError as exc:
                raise RuntimeError(
                    f"Al Ansari CC_Ajax_Object malformed: {exc}"
                ) from exc
            if not nonce:
                raise RuntimeError("Al Ansari: ajax_nonce missing")

            # Step 2: POST the convert_action with numeric codes.
            # trtype=BT (Bank Transfer) is the canonical working type for
            # AED -> INR. CP (Cash Pickup) often returns "no data found"
            # for this corridor.
            r = client.post(
                ajax_url,
                data={
                    "action": "convert_action",
                    "currfrom": str(_CCY_CODES["AED"]),
                    "currto":   str(_CCY_CODES[target_currency]),
                    "cntcode":  str(_CNT_CODES.get(target_currency, _CCY_CODES[target_currency])),
                    "amt": "1",
                    "security": nonce,
                    "trtype": "BT",
                },
                headers={
                    "Origin": "https://alansariexchange.com",
                    "Referer": "https://alansariexchange.com/",
                    "X-Requested-With": "XMLHttpRequest",
                },
            )
            r.raise_for_status()
            data = r.json()
        finally:
            client.close()

        if data.get("status_msg") != "SUCCESS":
            raise RuntimeError(
                f"Al Ansari rate call failed: status={data.get('status_msg')}, "
                f"detail={data.get('status_msg_detail')}"
            )

        # `get_rate` is the rate (1 AED = X target).
        # `amount` is the value of `amt * get_rate` plus their applied
        # spread / processing — slightly higher than get_rate. We use
        # `get_rate` as the published rate.
        try:
            rate = float(data["get_rate"])
        except (KeyError, TypeError, ValueError) as exc:
            raise RuntimeError(f"Al Ansari rate malformed: {exc}") from exc

        if not 0.0001 <= rate <= 10000.0:
            raise RuntimeError(f"Al Ansari rate out of range: {rate}")

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote=target_currency,
            amount_base=amount_base,
            rate=rate,
            fee_base=None,  # fee depends on amount/channel, not in this response
            received_quote=rate * amount_base,
            effective_rate=rate,
            delivery_estimate="bank transfer",
            url=self.PAGE_URL,
            status="ok",
            note="Bank Transfer rate (BT) via WP-Ajax convertor.",
            fetched_at=self._now_iso(),
        )
