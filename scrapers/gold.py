"""Gold rate scraper for the UAE-vs-India comparison module.

Two independent sources are pulled in parallel:

  * UAE:   Khaleej Times daily gold rates table
           (https://www.khaleejtimes.com/gold-forex)
           Rate cells use HTML structure:  <td>24K</td><td>556.00</td>

  * India: BankBazaar national gold rate page
           (https://www.bankbazaar.com/gold-rate-india.html)
           Rate is in an inline-JSON history series with the daily
           per-gram price as: "22K_1G":13935,"24K_1G":14632

Output unit: rate per 1 gram in the local currency. The UI multiplies
by 8 to display "per 8 grams" as well — a common purchase weight in
Indian jewelry, where 8 g ≈ 0.69 tola.

The scraper returns a single GoldQuote describing both UAE and India
side by side; the orchestrator embeds it into rates.json under a
top-level "gold" key (see scrapers/run_all.py).
"""
from __future__ import annotations

import re
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from typing import Optional

from .utils import http_client


KT_URL = "https://www.khaleejtimes.com/gold-forex"
BB_URL = "https://www.bankbazaar.com/gold-rate-india.html"


@dataclass
class GoldHistoryPoint:
    """One day of gold rate history."""
    date: str                  # YYYY-MM-DD
    per_g_24k: float
    per_g_22k: float

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass
class GoldSide:
    """Per-country gold rate data."""
    currency: str          # "AED" or "INR"
    per_g_24k: Optional[float]
    per_g_22k: Optional[float]
    source: str            # human label for the source ("Khaleej Times" etc.)
    source_url: str
    status: str = "ok"     # "ok" | "error"
    note: Optional[str] = None
    # Recent daily history (newest first). Empty if upstream doesn't
    # supply one — the orchestrator may then fold this in from a
    # locally-maintained rolling file.
    history: list = None

    def __post_init__(self):
        if self.history is None:
            self.history = []

    def to_dict(self) -> dict:
        return {
            "currency": self.currency,
            "per_g_24k": self.per_g_24k,
            "per_g_22k": self.per_g_22k,
            "source": self.source,
            "source_url": self.source_url,
            "status": self.status,
            "note": self.note,
            "history": [h.to_dict() for h in self.history],
        }


@dataclass
class GoldQuote:
    uae: GoldSide
    india: GoldSide
    fetched_at: str

    def to_dict(self) -> dict:
        return {
            "uae": self.uae.to_dict(),
            "india": self.india.to_dict(),
            "fetched_at": self.fetched_at,
        }


def _now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _parse_float(s: str) -> Optional[float]:
    try:
        return float(s.replace(",", "").strip())
    except (TypeError, ValueError):
        return None


def fetch_uae_gold() -> GoldSide:
    """Khaleej Times publishes a Dubai gold rates table server-side."""
    try:
        with http_client() as c:
            r = c.get(
                KT_URL,
                headers={
                    "User-Agent": (
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        "AppleWebKit/537.36 (KHTML, like Gecko) "
                        "transfer-rate-bot/1.0"
                    ),
                    "Accept": "text/html",
                },
            )
            r.raise_for_status()
        text = r.text

        def cell(carat: str) -> Optional[float]:
            m = re.search(
                rf"{carat}</td>\s*<td[^>]*>\s*([0-9,]+\.?[0-9]*)",
                text,
                re.IGNORECASE,
            )
            return _parse_float(m.group(1)) if m else None

        rate_24k = cell("24K")
        rate_22k = cell("22K")

        if rate_24k is None or rate_22k is None:
            return GoldSide(
                currency="AED",
                per_g_24k=rate_24k,
                per_g_22k=rate_22k,
                source="Khaleej Times",
                source_url=KT_URL,
                status="error",
                note="Could not parse 24K/22K cells from the page",
            )

        # Sanity bounds (per-gram AED): UAE gold has ranged ~150 to ~700
        # over the last decade; outside that and we've definitely
        # mis-parsed.
        for v in (rate_24k, rate_22k):
            if not 100.0 <= v <= 2000.0:
                return GoldSide(
                    currency="AED",
                    per_g_24k=rate_24k,
                    per_g_22k=rate_22k,
                    source="Khaleej Times",
                    source_url=KT_URL,
                    status="error",
                    note=f"Out-of-range value: {v}",
                )

        return GoldSide(
            currency="AED",
            per_g_24k=rate_24k,
            per_g_22k=rate_22k,
            source="Khaleej Times",
            source_url=KT_URL,
            status="ok",
        )
    except Exception as exc:
        return GoldSide(
            currency="AED",
            per_g_24k=None,
            per_g_22k=None,
            source="Khaleej Times",
            source_url=KT_URL,
            status="error",
            note=f"{type(exc).__name__}: {exc}",
        )


# BankBazaar inline-JSON pattern. They embed full multi-city history
# in `cityPrices`; each entry has shape:
#   {"date":"2026-05-02","cityId":1,"prices":{"22K_1G":13935,"24K_1G":14632}}
# We anchor on the city-1 series (Mumbai, by their cityId convention)
# and take its most recent entry as today's rate, plus the next 30
# entries as the history.
_BB_HISTORY = re.compile(
    r'"date"\s*:\s*"(\d{4}-\d{2}-\d{2})"\s*,\s*"cityId"\s*:\s*1\s*,'
    r'\s*"prices"\s*:\s*\{\s*"22K_1G"\s*:\s*(\d+)\s*,\s*"24K_1G"\s*:\s*(\d+)'
)


def fetch_india_gold(history_days: int = 30) -> GoldSide:
    """BankBazaar exposes daily 22K/24K rates plus a history series."""
    try:
        with http_client() as c:
            r = c.get(
                BB_URL,
                headers={
                    "User-Agent": (
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        "AppleWebKit/537.36 (KHTML, like Gecko) "
                        "transfer-rate-bot/1.0"
                    ),
                    "Accept": "text/html",
                },
            )
            r.raise_for_status()
        text = r.text

        # Extract city-1 history series. The first match is today's rate.
        matches = list(_BB_HISTORY.finditer(text))
        if not matches:
            return GoldSide(
                currency="INR",
                per_g_24k=None,
                per_g_22k=None,
                source="BankBazaar",
                source_url=BB_URL,
                status="error",
                note="Could not find cityId:1 history series in page JSON",
            )

        # Build de-duplicated history (date -> latest entry); BankBazaar
        # sometimes repeats the same date in the page payload.
        seen: dict = {}
        for m in matches:
            d = m.group(1)
            if d not in seen:
                seen[d] = (float(m.group(2)), float(m.group(3)))

        sorted_dates = sorted(seen.keys(), reverse=True)
        latest_date = sorted_dates[0]
        rate_22k, rate_24k = seen[latest_date]

        # Sanity bounds (per-gram INR): India 24K gold has ranged
        # ~3000 to ~25000 historically; outside that = mis-parse.
        for v in (rate_24k, rate_22k):
            if not 1000.0 <= v <= 50000.0:
                return GoldSide(
                    currency="INR",
                    per_g_24k=rate_24k,
                    per_g_22k=rate_22k,
                    source="BankBazaar",
                    source_url=BB_URL,
                    status="error",
                    note=f"Out-of-range value: {v}",
                )

        history = [
            GoldHistoryPoint(date=d, per_g_22k=seen[d][0], per_g_24k=seen[d][1])
            for d in sorted_dates[:history_days]
        ]

        return GoldSide(
            currency="INR",
            per_g_24k=rate_24k,
            per_g_22k=rate_22k,
            source="BankBazaar",
            source_url=BB_URL,
            status="ok",
            history=history,
        )
    except Exception as exc:
        return GoldSide(
            currency="INR",
            per_g_24k=None,
            per_g_22k=None,
            source="BankBazaar",
            source_url=BB_URL,
            status="error",
            note=f"{type(exc).__name__}: {exc}",
        )


def fetch_gold() -> GoldQuote:
    """Combine UAE + India gold rates into a single GoldQuote."""
    return GoldQuote(
        uae=fetch_uae_gold(),
        india=fetch_india_gold(),
        fetched_at=_now_iso(),
    )
