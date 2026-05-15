"""Precious-metals scraper for the UAE-vs-India side-by-side module.

Three independent sources are pulled per run:

  * UAE gold:    Khaleej Times daily gold rates table
                 (https://www.khaleejtimes.com/gold-forex)
                 Rate cells use HTML structure:
                   <td>24K</td><td>569.50</td>

  * India:       LiveChennai gold + silver page
                 (https://www.livechennai.com/gold_silverrate.asp)
                 Two server-rendered tables we parse:
                   table #2 — Date | Pure Gold (24k) 1g/8g | Standard Gold (22K) 1g/8g
                   table #4 — Date | Silver 1g | Ready Silver (1Kg)
                 Both tables include the latest ~10 days, so we get
                 today's prices AND the recent history in one fetch.
                 Switched to LiveChennai (Chennai) from BankBazaar
                 (Mumbai) per user request 2026-05-07 — Chennai retail
                 conventions are slightly different but the page is
                 trivially parseable and includes silver natively.

  * UAE silver:  Spot silver from gold-api.com (XAG, USD/oz),
                 converted to AED/g via the fixed UAE peg
                 (1 USD = 3.6725 AED). Khaleej Times does NOT publish
                 a UAE retail silver rate, so this is honest "spot"
                 data — labelled accordingly in the source field.

Output unit: rate per 1 gram in the local currency (AED for UAE,
INR for India). The UI multiplies by 8 to display "per 8 grams" too —
a common purchase weight in Indian jewellery (8 g ≈ 0.69 tola).

The scraper returns a single PreciousMetalsQuote describing UAE +
India side by side; the orchestrator embeds it into rates.json under
the top-level "gold" key (legacy name kept for schema compat — the
field name predates silver being added). See scrapers/run_all.py.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from typing import Optional

from .utils import http_client


KT_URL = "https://www.khaleejtimes.com/gold-forex"
LC_URL = "https://www.livechennai.com/gold_silverrate.asp"
XAG_API_URL = "https://api.gold-api.com/price/XAG"

# v0.32.2: UAE silver primary source now igold.ae's public chart API.
# v0.32.4: UAE gold also moves to igold.ae primary (KT only ships
# 2 history points; igold ships 30).
#
# Both endpoints share the same response shape:
#   { metal, currency, weight, last_price, min/avg/max_price,
#     data: [[ts_ms, price], ...] }
#
# Quirk worth knowing: igold's chart-data endpoint sometimes returns
# `data` array values in MILLIGRAM scale (price ~0.009 for silver,
# ~0.5 for gold) while `last_price` is consistently in GRAM scale
# (price ~9.0 silver, ~540 gold).  Probably an edge-cache state thing.
# Our scrapers detect the scale by comparing data max to last_price
# and rescale by 1000× when needed (see _normalise_igold_scale).
IGOLD_XAG_URL = (
    "https://charts.igold.ae/api/data"
    "?metal=xag&currency=aed&period=monthly&weight=gram"
)
IGOLD_XAU_URL = (
    "https://charts.igold.ae/api/data"
    "?metal=xau&currency=aed&period=monthly&weight=gram&purity=.999"
)

# 22K = 91.67% of 24K by gold content; igold's API returns the same
# value for any `purity` parameter (just spot price), so we derive
# 22K mathematically from 24K.  Real UAE retail 22K runs ~1% above
# the math (jewellery making charge baked in) but for the "indicative
# rates" disclaimer this is honest enough.
KARAT_22_OF_24 = 22.0 / 24.0

# UAE Dirham is hard-pegged to the US Dollar at this rate since 1997.
# We use it to convert spot-price quotes (USD-denominated) into AED.
AED_PER_USD = 3.6725

# 1 troy ounce in grams. Spot precious-metal APIs quote in USD/oz;
# we always display per gram.
GRAMS_PER_TROY_OZ = 31.1034768


@dataclass
class GoldHistoryPoint:
    """One day of precious-metal rate history. Gold-only by name for
    legacy reasons; the same dataclass is also used for silver history
    by ignoring the per_g_22k field (set to None / 0 there)."""
    date: str                  # YYYY-MM-DD
    per_g_24k: float
    per_g_22k: float

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass
class SilverHistoryPoint:
    """One day of silver rate history (single karat — fineness is
    typically 999 / 'sterling' in retail listings)."""
    date: str
    per_g: float

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass
class GoldSide:
    """Per-country gold rate data."""
    currency: str          # "AED" or "INR"
    per_g_24k: Optional[float]
    per_g_22k: Optional[float]
    source: str            # human label ("Khaleej Times", "LiveChennai")
    source_url: str
    status: str = "ok"     # "ok" | "error"
    note: Optional[str] = None
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
class SilverSide:
    """Per-country silver rate data."""
    currency: str
    per_g: Optional[float]
    source: str
    source_url: str
    status: str = "ok"
    note: Optional[str] = None
    history: list = None

    def __post_init__(self):
        if self.history is None:
            self.history = []

    def to_dict(self) -> dict:
        return {
            "currency": self.currency,
            "per_g": self.per_g,
            "source": self.source,
            "source_url": self.source_url,
            "status": self.status,
            "note": self.note,
            "history": [h.to_dict() for h in self.history],
        }


@dataclass
class PreciousMetalsQuote:
    """All four side-by-side: UAE gold, India gold, UAE silver, India silver."""
    uae: GoldSide
    india: GoldSide
    uae_silver: Optional[SilverSide]
    india_silver: Optional[SilverSide]
    fetched_at: str

    def to_dict(self) -> dict:
        return {
            "uae": self.uae.to_dict(),
            "india": self.india.to_dict(),
            "uae_silver": self.uae_silver.to_dict() if self.uae_silver else None,
            "india_silver": self.india_silver.to_dict() if self.india_silver else None,
            "fetched_at": self.fetched_at,
        }


# Backwards-compat alias: orchestrator + earlier consumers expect a
# class named GoldQuote. The new shape has the same uae/india/fetched_at
# keys plus the new silver keys, which older readers will simply ignore.
GoldQuote = PreciousMetalsQuote


def _now_iso() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _parse_float(s: str) -> Optional[float]:
    try:
        return float(s.replace(",", "").strip())
    except (TypeError, ValueError):
        return None


# =====================================================================
# UAE gold — Khaleej Times
# =====================================================================

def _fetch_igold_uae_gold(history_days: int = 30) -> GoldSide:
    """Primary UAE gold source — igold.ae chart-data API.

    v0.32.4: replaces Khaleej Times as primary because KT's HTML
    only ships TODAY's price; we were stitching a tiny "history" of
    2-3 entries from cron snapshots which made the gold sparkline
    sparse and the 30-day stats meaningless.  igold returns ~1440
    sub-day data points spanning the last month — binned to one
    last-of-day observation per UTC calendar day, that's 30 daily
    rates flowing through the same shape the UI already consumes.

    Karat handling: igold's `purity` parameter does not actually
    differentiate output (we tested .999 vs .916 — same value),
    so 24K comes from the API verbatim and 22K is derived
    mathematically (24K × 22/24 = 91.67%).  Real UAE retail 22K
    runs ~1% above the math because of jewellery making charges;
    the global "Rates indicative" disclaimer covers that gap.
    """
    import time
    from collections import OrderedDict

    last_exc: Optional[Exception] = None
    for attempt in range(3):
        if attempt > 0:
            time.sleep(2.0 ** attempt)
        try:
            with http_client() as c:
                r = c.get(
                    IGOLD_XAU_URL,
                    headers={"Accept": "application/json"},
                    timeout=10.0,
                )
                r.raise_for_status()
                payload = r.json()

            raw_data = payload.get("data") or []
            if not raw_data:
                raise RuntimeError("igold gold response has empty data array")

            last_price_raw = float(payload.get("last_price") or 0.0)
            data, _ = _normalise_igold_scale(raw_data, last_price_raw)

            # Bin to one observation per UTC day (last-of-day sample).
            per_day_24k: "OrderedDict[str, float]" = OrderedDict()
            for ts_ms, price_f in data:
                if not 100.0 <= price_f <= 2_000.0:
                    continue
                day = datetime.fromtimestamp(ts_ms / 1000.0, tz=timezone.utc) \
                    .strftime("%Y-%m-%d")
                per_day_24k[day] = round(price_f, 2)

            if not per_day_24k:
                raise RuntimeError("igold gold response had no plausible points")

            history_sorted = sorted(per_day_24k.items())[-history_days:]
            history = [
                GoldHistoryPoint(
                    date=day,
                    per_g_24k=price_24k,
                    per_g_22k=round(price_24k * KARAT_22_OF_24, 2),
                )
                for day, price_24k in history_sorted
            ]

            current_24k = round(last_price_raw or data[-1][1], 2)
            if not 100.0 <= current_24k <= 2_000.0:
                raise RuntimeError(f"igold gold last_price out of range: {current_24k}")
            current_22k = round(current_24k * KARAT_22_OF_24, 2)

            return GoldSide(
                currency="AED",
                per_g_24k=current_24k,
                per_g_22k=current_22k,
                source="igold.ae (Dubai bullion, 24K AED/gram; 22K derived)",
                source_url=IGOLD_XAU_URL,
                status="ok",
                history=history,
            )
        except Exception as exc:
            last_exc = exc

    raise RuntimeError(
        f"igold UAE gold fetch failed after 3 attempts: "
        f"{type(last_exc).__name__}: {last_exc}"
    )


def _fetch_kt_uae_gold() -> GoldSide:
    """Fallback UAE gold source — Khaleej Times HTML.

    Used only when igold.ae is unreachable.  KT parses both 24K AND
    22K from a single page (no derivation needed), but only carries
    today's price — no daily history.  The home card stays
    populated when igold has an outage; the bottom-sheet sparkline
    will be sparse until igold returns.
    """
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
                source="Khaleej Times (fallback)",
                source_url=KT_URL,
                status="error",
                note="Could not parse 24K/22K cells from the page",
            )

        for v in (rate_24k, rate_22k):
            if not 100.0 <= v <= 2000.0:
                return GoldSide(
                    currency="AED",
                    per_g_24k=rate_24k,
                    per_g_22k=rate_22k,
                    source="Khaleej Times (fallback)",
                    source_url=KT_URL,
                    status="error",
                    note=f"Out-of-range value: {v}",
                )

        return GoldSide(
            currency="AED",
            per_g_24k=rate_24k,
            per_g_22k=rate_22k,
            source="Khaleej Times (fallback)",
            source_url=KT_URL,
            status="ok",
        )
    except Exception as exc:
        return GoldSide(
            currency="AED",
            per_g_24k=None,
            per_g_22k=None,
            source="Khaleej Times (fallback)",
            source_url=KT_URL,
            status="error",
            note=f"{type(exc).__name__}: {exc}",
        )


def fetch_uae_gold() -> GoldSide:
    """UAE gold entry point.

    v0.32.4: tries igold.ae first (24K + derived 22K, 30 daily
    history points) and falls back to Khaleej Times (24K + 22K
    parsed natively, no history) only if igold is unreachable
    across 3 retries.  Either path keeps the home card populated.
    """
    try:
        return _fetch_igold_uae_gold()
    except Exception:
        return _fetch_kt_uae_gold()


# =====================================================================
# India gold + silver — LiveChennai
# =====================================================================

# LiveChennai date format: "07/May/2026" → "2026-05-07"
_MONTHS = {
    "Jan": "01", "Feb": "02", "Mar": "03", "Apr": "04",
    "May": "05", "Jun": "06", "Jul": "07", "Aug": "08",
    "Sep": "09", "Oct": "10", "Nov": "11", "Dec": "12",
}


def _normalise_date(s: str) -> Optional[str]:
    """'07/May/2026' → '2026-05-07'. Returns None on parse failure."""
    m = re.match(r"(\d{1,2})/([A-Za-z]{3})/(\d{4})", s.strip())
    if not m:
        return None
    dd, mon, yyyy = m.groups()
    mm = _MONTHS.get(mon.title())
    if not mm:
        return None
    return f"{yyyy}-{mm}-{int(dd):02d}"


def _strip_html_to_cells(table_html: str) -> list:
    """Strip tags, return a flat list of cell text values in order."""
    cleaned = re.sub(r"<[^>]+>", "|", table_html)
    cells = [c.strip() for c in cleaned.split("|") if c.strip()]
    return cells


def _fetch_livechennai_html() -> str:
    """One fetch shared between gold and silver to halve the page-load cost.

    LiveChennai has been observed to occasionally hang on first
    connection from non-Indian IPs (the page is heavy with embedded
    JS and ads). We use a generous 25-second timeout and one
    transparent retry — total worst-case ≈55 sec, still inside the
    8-minute scrape workflow budget.
    """
    last_exc: Optional[Exception] = None
    for attempt in range(2):
        try:
            with http_client(timeout=25.0) as c:
                r = c.get(
                    LC_URL,
                    headers={
                        "User-Agent": (
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                            "AppleWebKit/537.36 (KHTML, like Gecko) "
                            "transfer-rate-bot/1.0"
                        ),
                        "Accept": "text/html",
                        "Accept-Language": "en-IN,en-US;q=0.9,en;q=0.8",
                    },
                )
                r.raise_for_status()
            return r.text
        except Exception as exc:  # noqa: BLE001
            last_exc = exc
            if attempt == 0:
                continue
    raise RuntimeError(
        f"LiveChennai fetch failed after 2 attempts: "
        f"{type(last_exc).__name__}: {last_exc}"
    )


def _extract_tables(html: str) -> list:
    """Return all <table>…</table> bodies in document order."""
    return re.findall(r"<table[^>]*>(.*?)</table>", html, re.DOTALL | re.IGNORECASE)


def fetch_india_gold(html: Optional[str] = None, history_days: int = 10) -> GoldSide:
    """LiveChennai table #2 — Pure Gold (24k) + Standard Gold (22K)."""
    try:
        if html is None:
            html = _fetch_livechennai_html()

        tables = _extract_tables(html)
        if len(tables) < 2:
            return GoldSide(
                currency="INR",
                per_g_24k=None, per_g_22k=None,
                source="LiveChennai",
                source_url=LC_URL,
                status="error",
                note=f"Expected ≥2 tables, found {len(tables)}",
            )

        # Find the gold table by content — robust against table-order changes.
        gold_table = None
        for tbl in tables:
            if re.search(r"24\s*k", tbl, re.IGNORECASE) and re.search(r"22\s*K", tbl, re.IGNORECASE) \
                    and re.search(r"1\s*Gm", tbl, re.IGNORECASE):
                gold_table = tbl
                break
        if gold_table is None:
            return GoldSide(
                currency="INR",
                per_g_24k=None, per_g_22k=None,
                source="LiveChennai",
                source_url=LC_URL,
                status="error",
                note="Gold table (24k + 22K + 1Gm) not found in page",
            )

        cells = _strip_html_to_cells(gold_table)
        # Find the first cell that parses as a date, then iterate rows.
        # Each row: [date, 24k_1g, 24k_8g, 22k_1g, 22k_8g]
        history: list[GoldHistoryPoint] = []
        i = 0
        while i < len(cells) - 4:
            iso = _normalise_date(cells[i])
            if iso is None:
                i += 1
                continue
            g24 = _parse_float(cells[i + 1])
            g22 = _parse_float(cells[i + 3])
            if g24 is not None and g22 is not None and 1000.0 <= g24 <= 50000.0:
                history.append(GoldHistoryPoint(
                    date=iso, per_g_24k=g24, per_g_22k=g22,
                ))
            i += 5  # advance to next row

        if not history:
            return GoldSide(
                currency="INR",
                per_g_24k=None, per_g_22k=None,
                source="LiveChennai",
                source_url=LC_URL,
                status="error",
                note="Could not parse any gold rows from the table",
            )

        # First row is today's rate.
        head = history[0]
        history = history[:history_days]

        return GoldSide(
            currency="INR",
            per_g_24k=head.per_g_24k,
            per_g_22k=head.per_g_22k,
            source="LiveChennai",
            source_url=LC_URL,
            status="ok",
            history=history,
        )
    except Exception as exc:
        return GoldSide(
            currency="INR",
            per_g_24k=None, per_g_22k=None,
            source="LiveChennai",
            source_url=LC_URL,
            status="error",
            note=f"{type(exc).__name__}: {exc}",
        )


def fetch_india_silver(html: Optional[str] = None, history_days: int = 10) -> SilverSide:
    """LiveChennai table #4 — Silver 1Gm + Ready Silver (1Kg)."""
    try:
        if html is None:
            html = _fetch_livechennai_html()

        tables = _extract_tables(html)
        # Find the silver table by content. Silver rows have "1 Gm" + a small
        # number (~200-500 INR/g) and a kg column with a 6-digit number.
        silver_table = None
        for tbl in tables:
            if re.search(r"silver", tbl, re.IGNORECASE) \
                    and re.search(r"1\s*Kg", tbl, re.IGNORECASE):
                silver_table = tbl
                break
        if silver_table is None:
            return SilverSide(
                currency="INR",
                per_g=None,
                source="LiveChennai",
                source_url=LC_URL,
                status="error",
                note="Silver table (silver + 1Kg) not found in page",
            )

        cells = _strip_html_to_cells(silver_table)
        # Each row: [date, silver_1g, silver_1kg]
        history: list[SilverHistoryPoint] = []
        i = 0
        while i < len(cells) - 2:
            iso = _normalise_date(cells[i])
            if iso is None:
                i += 1
                continue
            per_g = _parse_float(cells[i + 1])
            if per_g is not None and 50.0 <= per_g <= 5000.0:
                history.append(SilverHistoryPoint(date=iso, per_g=per_g))
            i += 3

        if not history:
            return SilverSide(
                currency="INR",
                per_g=None,
                source="LiveChennai",
                source_url=LC_URL,
                status="error",
                note="Could not parse any silver rows from the table",
            )

        head = history[0]
        history = history[:history_days]

        return SilverSide(
            currency="INR",
            per_g=head.per_g,
            source="LiveChennai",
            source_url=LC_URL,
            status="ok",
            history=history,
        )
    except Exception as exc:
        return SilverSide(
            currency="INR",
            per_g=None,
            source="LiveChennai",
            source_url=LC_URL,
            status="error",
            note=f"{type(exc).__name__}: {exc}",
        )


# =====================================================================
# UAE silver — spot price from gold-api.com → AED via UAE peg
# =====================================================================

def _normalise_igold_scale(
    data: list, last_price: float
) -> tuple[list[tuple[int, float]], float]:
    """Detect and correct igold's intermittent milligram-vs-gram scale
    mismatch in the `data` array.  Returns (rescaled_data, scale_factor).

    igold's `last_price` is consistently in AED/gram; the `data` array
    sometimes comes back in AED/milligram (max ~0.01 silver, ~0.6 gold).
    When `max(data)` is ~1000× smaller than `last_price`, we multiply
    every data point by 1000 to bring it back to gram scale.

    Returns the data normalised + the scale factor applied (1.0 = no
    change, 1000.0 = milligram scaled up).  Caller can log the factor
    for diagnostic visibility.
    """
    if not data:
        return [], 1.0
    plausible = [
        (int(ts), float(p))
        for ts, p in data
        if p is not None and float(p) > 0
    ]
    if not plausible:
        return [], 1.0
    max_data = max(p for _, p in plausible)
    scale_factor = 1.0
    if last_price > 0 and max_data > 0:
        ratio = last_price / max_data
        if 500.0 <= ratio <= 5_000.0:
            scale_factor = 1000.0
    if scale_factor != 1.0:
        plausible = [(ts, p * scale_factor) for ts, p in plausible]
    return plausible, scale_factor


def _fetch_igold_uae_silver(history_days: int = 30) -> SilverSide:
    """Primary UAE silver source — igold.ae's public chart-data API.

    Returns AED-per-gram with a `history_days`-day daily history (the
    monthly endpoint returns ~1440 data points covering the last month,
    sampled at ~30-min intervals).  We bin to one observation per
    calendar day by taking the LAST point of each UTC day, mirroring
    how the LiveChennai gold/silver scraper records its history.

    Why this fills a real gap: Khaleej Times — our UAE gold source —
    doesn't publish a daily silver page.  v0.23 through v0.32.1 worked
    around this with a spot-price + AED-peg derivation that had no
    history at all; the home card showed "spot only" and the bottom
    sheet showed an apologetic "no daily history" placeholder.  igold
    is a Dubai-based bullion dealer publishing AED-denominated silver
    with a public JSON feed designed for their own chart widget; we
    use the same feed and shape it into the SilverSide we already use.

    Response shape:
        {
          "metal":"xag", "currency":"aed", "weight":"gram",
          "last_price": 9.30, "min_price": ..., "avg_price": ...,
          "max_price": ..., "data": [[ts_ms, price], ...]
        }
    """
    import time
    from collections import OrderedDict

    last_exc: Optional[Exception] = None
    for attempt in range(3):
        if attempt > 0:
            time.sleep(2.0 ** attempt)
        try:
            with http_client() as c:
                r = c.get(
                    IGOLD_XAG_URL,
                    headers={"Accept": "application/json"},
                    timeout=10.0,
                )
                r.raise_for_status()
                payload = r.json()

            raw_data = payload.get("data") or []
            if not raw_data:
                raise RuntimeError("igold response has empty data array")

            # v0.32.4: normalise potential milligram-scale `data` arrays.
            last_price_raw = float(payload.get("last_price") or 0.0)
            data, _ = _normalise_igold_scale(raw_data, last_price_raw)

            # Bin to one point per UTC calendar day; take the last
            # observation of each day (closing-style sample).
            per_day: "OrderedDict[str, float]" = OrderedDict()
            for ts_ms, price_f in data:
                if not 0.5 <= price_f <= 2_000.0:
                    continue  # ignore out-of-range outliers
                day = datetime.fromtimestamp(ts_ms / 1000.0, tz=timezone.utc) \
                    .strftime("%Y-%m-%d")
                per_day[day] = round(price_f, 2)

            if not per_day:
                raise RuntimeError("igold response had no plausible points")

            history_sorted = sorted(per_day.items())[-history_days:]
            history = [
                SilverHistoryPoint(date=day, per_g=price)
                for day, price in history_sorted
            ]

            current_per_g = round(last_price_raw or data[-1][1], 2)
            if not 0.5 <= current_per_g <= 2_000.0:
                raise RuntimeError(f"igold last_price out of range: {current_per_g}")

            return SilverSide(
                currency="AED",
                per_g=current_per_g,
                source="igold.ae (Dubai bullion, AED/gram)",
                source_url=IGOLD_XAG_URL,
                status="ok",
                history=history,
            )
        except Exception as exc:
            last_exc = exc

    raise RuntimeError(
        f"igold UAE silver fetch failed after 3 attempts: "
        f"{type(last_exc).__name__}: {last_exc}"
    )


def _fetch_spot_xag_uae_silver() -> SilverSide:
    """Fallback UAE silver source — international spot XAG converted
    via the UAE Dirham/USD peg.  This is the v0.23–v0.32.1 primary;
    kept as a safety net so the silver column never goes blank if
    igold.ae has an outage.  No history (the spot endpoint is just a
    point-in-time price), but at least the home card stays populated.
    """
    import time

    last_exc: Optional[Exception] = None
    for attempt in range(3):
        if attempt > 0:
            time.sleep(2.0 ** attempt)
        try:
            with http_client() as c:
                r = c.get(
                    XAG_API_URL,
                    headers={"Accept": "application/json"},
                    timeout=8.0,
                )
                r.raise_for_status()
                data = r.json()

            spot_usd_oz = float(data.get("price", 0.0))
            if not 1.0 <= spot_usd_oz <= 1_000.0:
                raise RuntimeError(f"Spot silver out of range: {spot_usd_oz}")

            per_g_aed = (spot_usd_oz / GRAMS_PER_TROY_OZ) * AED_PER_USD
            per_g_aed = round(per_g_aed, 2)

            return SilverSide(
                currency="AED",
                per_g=per_g_aed,
                source="Spot (gold-api.com → AED peg, fallback)",
                source_url=XAG_API_URL,
                status="ok",
            )
        except Exception as exc:
            last_exc = exc

    return SilverSide(
        currency="AED",
        per_g=None,
        source="Spot (gold-api.com → AED peg, fallback)",
        source_url=XAG_API_URL,
        status="error",
        note=f"both igold + spot fallback failed; last: "
             f"{type(last_exc).__name__}: {last_exc}",
    )


def fetch_uae_silver() -> SilverSide:
    """UAE silver entry point.

    v0.32.2: tries igold.ae first (AED-denominated with daily history),
    falls back to the international spot + AED-peg derivation only if
    igold is unreachable across 3 attempts.  This way the silver
    column always populates and we get history when the primary works.
    """
    try:
        return _fetch_igold_uae_silver()
    except Exception:
        # Primary unreachable — degrade gracefully to spot-only.
        return _fetch_spot_xag_uae_silver()


# =====================================================================
# Orchestration entry point
# =====================================================================

def fetch_gold() -> PreciousMetalsQuote:
    """Combine UAE + India gold AND silver into a single quote.

    Note the function name `fetch_gold` is kept for backward compat —
    the orchestrator calls it under that name. The returned shape has
    silver fields appended; older Android clients that only parse the
    gold sub-keys keep working unchanged.
    """
    # Single shared HTML pull for the LiveChennai page (gold + silver).
    try:
        lc_html = _fetch_livechennai_html()
    except Exception:
        lc_html = None  # let the per-side functions fail individually

    return PreciousMetalsQuote(
        uae=fetch_uae_gold(),
        india=fetch_india_gold(html=lc_html),
        uae_silver=fetch_uae_silver(),
        india_silver=fetch_india_silver(html=lc_html),
        fetched_at=_now_iso(),
    )
