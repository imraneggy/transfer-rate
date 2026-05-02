"""Remitly scraper.

Remitly's UAE landing page at /ae/en/india server-side renders both an
everyday rate and a first-transfer promo rate inside an embedded JSON
blob (`merchandisingFacts`).

Currently scoped to AED -> INR. Other AED-> corridors would require
parsing different per-country landing pages with potentially different
HTML structures; that's contributor work, not architectural change.
"""
from __future__ import annotations

import re

from .base import BaseProvider, Quote
from .utils import http_client


_RE_BLOCK = re.compile(r'"merchandisingFacts":\{[^}]+\}')
_RE_EVERYDAY = re.compile(r'"everydayRateAsLowAs":"([0-9.]+)"')
_RE_EFFECTIVE = re.compile(r'"effectiveRateAsLowAs":"([0-9.]+)"')
_RE_FEES = re.compile(r'"feesAsLowAs":"([0-9.]+)"')
_RE_MIN_AMT = re.compile(r'"bestRateMinSendAmount":"([0-9.]+)"')


class RemitlyProvider(BaseProvider):
    id = "remitly"
    display_name = "Remitly"
    PAGE_URL = "https://www.remitly.com/ae/en/india"

    def fetch(self, target_currency: str = "INR", amount_base: float = 1000.0) -> Quote:
        if target_currency != "INR":
            raise RuntimeError(
                f"Remitly scraper currently only handles AED->INR. "
                f"AED->{target_currency} would need a separate corridor page."
            )

        with http_client() as c:
            r = c.get(
                self.PAGE_URL,
                headers={
                    "User-Agent": (
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        "AppleWebKit/537.36 (KHTML, like Gecko) "
                        "transfer-rate-bot/1.0 "
                        "(+https://github.com/imraneggy/transfer-rate)"
                    ),
                },
            )
            r.raise_for_status()

        block = _RE_BLOCK.search(r.text)
        if not block:
            raise RuntimeError(
                "Remitly merchandisingFacts JSON block not found on page — "
                "page structure may have changed."
            )

        m_every = _RE_EVERYDAY.search(block.group(0))
        m_eff = _RE_EFFECTIVE.search(block.group(0))
        m_fees = _RE_FEES.search(block.group(0))
        m_min = _RE_MIN_AMT.search(block.group(0))

        if not m_every:
            raise RuntimeError(
                "Remitly everydayRateAsLowAs missing from merchandisingFacts"
            )

        try:
            everyday = float(m_every.group(1))
            effective = float(m_eff.group(1)) if m_eff else None
            fees = float(m_fees.group(1)) if m_fees else None
            min_send = float(m_min.group(1)) if m_min else None
        except (TypeError, ValueError) as exc:
            raise RuntimeError(f"Remitly value malformed: {exc}") from exc

        if not 10.0 <= everyday <= 50.0:
            raise RuntimeError(f"Remitly everyday rate out of range: {everyday}")

        promo_rate = effective if (effective and abs(effective - everyday) > 0.001) else None
        promo_note = None
        if promo_rate is not None:
            if min_send and min_send > 0:
                promo_note = f"First transfer ≥ AED {min_send:.0f}"
            else:
                promo_note = "First transfer only"

        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote="INR",
            amount_base=amount_base,
            rate=everyday,
            fee_base=fees,
            received_quote=everyday * amount_base,
            effective_rate=everyday,
            delivery_estimate="cash pickup, bank deposit, or UPI",
            url=self.PAGE_URL,
            status="ok",
            promo_rate=promo_rate,
            promo_note=promo_note,
            fetched_at=self._now_iso(),
        )
