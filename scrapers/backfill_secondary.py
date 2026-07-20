#!/usr/bin/env python3
"""One-shot backfill of the per-country gold/silver 30-day history.

Why this exists
---------------
`gold.secondary[CUR]` (per-corridor retail gold + silver from
livepriceofgold.com) only ever exposes *today's* price — the upstream page
has no historical series.  The rolling sidecars under
`data/secondary_{gold,silver}/` therefore only fill one day per cron run, so
a freshly-shipped corridor shows "Building…" for ~30 days before its trend
line has enough points to draw.

Rather than wait a month, we reconstruct a plausible-and-honest 30-day series
from data sources that *do* carry history:

    local_retail_per_g(day) = (gold_spot_usd_per_oz(day) / 31.1034768)
                              x  usd_to_local_fx(day)
                              x  premium

where `premium` is anchored so the most-recent reconstructed day equals the
real retail value we already have for today.  The day-to-day *movement* comes
entirely from real COMEX gold/silver futures (GC=F / SI=F) and real daily FX
(Yahoo Finance) — only the local retail premium/tax wedge is held constant,
which is a small second-order effect next to spot and FX.

The reconstructed series is written into the same sidecar files the scheduled
scraper maintains, so subsequent cron runs simply extend it (the merge in
`run_all.py` does `rolling.setdefault(date, ...)`, preserving these points).
Today's row is forced to the exact real value so nothing drifts.

Run once, locally:  python -m scrapers.backfill_secondary
It rewrites the sidecars AND patches public/rates.json's history in place.
"""
from __future__ import annotations

import json
import sys
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

G_PER_OZT = 31.1034768  # grams per troy ounce

REPO = Path(__file__).resolve().parent.parent
RATES = REPO / "public" / "rates.json"
GOLD_DIR = REPO / "data" / "secondary_gold"
SILVER_DIR = REPO / "data" / "secondary_silver"

MAX_DAYS = 30

# Corridors carried in gold.secondary and how to get USD->CUR for each.
#   ("direct", "USDXXX=X")  -> Yahoo quote is already USD->CUR
#   ("invert", "XXXUSD=X")  -> Yahoo quote is CUR->USD, invert it
#   ("one",    None)        -> USD corridor, factor is 1.0
FX = {
    "INR": ("direct", "USDINR=X"),
    "PKR": ("direct", "USDPKR=X"),
    "PHP": ("direct", "USDPHP=X"),
    "BDT": ("direct", "USDBDT=X"),
    "EGP": ("direct", "USDEGP=X"),
    "NPR": ("direct", "USDNPR=X"),
    "LKR": ("direct", "USDLKR=X"),
    "EUR": ("invert", "EURUSD=X"),
    "GBP": ("invert", "GBPUSD=X"),
    "USD": ("one", None),
}


def _yahoo_daily(symbol: str) -> dict[str, float]:
    """Return {YYYY-MM-DD: close} for the last ~40 daily bars."""
    url = (
        f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}"
        "?range=45d&interval=1d"
    )
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    d = json.loads(urllib.request.urlopen(req, timeout=30).read())
    r = d["chart"]["result"][0]
    ts = r["timestamp"]
    close = r["indicators"]["quote"][0]["close"]
    out: dict[str, float] = {}
    for t, c in zip(ts, close):
        if c is None:
            continue
        day = datetime.fromtimestamp(t, tz=timezone.utc).date().isoformat()
        out[day] = float(c)  # last write per calendar day wins
    return out


def _usd_to_cur_series(cur: str) -> dict[str, float]:
    mode, sym = FX[cur]
    if mode == "one":
        # Constant 1.0 keyed on the gold-spot dates (filled by caller).
        return {}
    raw = _yahoo_daily(sym)
    if mode == "direct":
        return raw
    return {d: (1.0 / v) for d, v in raw.items() if v}


def backfill() -> None:
    doc = json.loads(RATES.read_text(encoding="utf-8"))
    secondary = doc.get("gold", {}).get("secondary", {})
    if not secondary:
        sys.exit("rates.json has no gold.secondary — nothing to backfill")

    print("fetching spot history (GC=F gold, SI=F silver)...")
    gold_spot = _yahoo_daily("GC=F")   # USD / troy oz
    silver_spot = _yahoo_daily("SI=F")  # USD / troy oz
    print(f"  gold days={len(gold_spot)} silver days={len(silver_spot)}")

    GOLD_DIR.mkdir(parents=True, exist_ok=True)
    SILVER_DIR.mkdir(parents=True, exist_ok=True)

    for cur, side in secondary.items():
        if cur not in FX:
            print(f"  {cur}: no FX mapping, skipped")
            continue
        gold = side.get("gold") or {}
        silver = side.get("silver") or {}
        anchor_date = None
        if gold.get("history"):
            anchor_date = gold["history"][0].get("date")
        anchor_date = anchor_date or datetime.now(timezone.utc).date().isoformat()

        fx = _usd_to_cur_series(cur)
        if FX[cur][0] == "one":
            fx = {d: 1.0 for d in gold_spot}

        # ---- GOLD ----
        g24_today = gold.get("per_g_24k")
        g22_today = gold.get("per_g_22k")
        gold_rows: dict[str, dict[str, float]] = {}
        if g24_today and gold_spot and fx:
            ratio22 = (g22_today / g24_today) if g22_today else (22.0 / 24.0)
            days = sorted(set(gold_spot) & set(fx))
            if days:
                anchor_day = days[-1]
                derived_anchor = (gold_spot[anchor_day] / G_PER_OZT) * fx[anchor_day]
                k = g24_today / derived_anchor if derived_anchor else 0.0
                for d in days[-MAX_DAYS:]:
                    per24 = (gold_spot[d] / G_PER_OZT) * fx[d] * k
                    gold_rows[d] = {"24k": round(per24, 2),
                                    "22k": round(per24 * ratio22, 2)}
                # Force the real value onto the anchor (today) row exactly.
                gold_rows[anchor_date] = {"24k": round(g24_today, 2),
                                          "22k": round(g22_today, 2)}

        # ---- SILVER ----
        s_today = silver.get("per_g")
        silver_rows: dict[str, float] = {}
        if s_today and silver_spot and fx:
            days = sorted(set(silver_spot) & set(fx))
            if days:
                anchor_day = days[-1]
                derived_anchor = (silver_spot[anchor_day] / G_PER_OZT) * fx[anchor_day]
                k = s_today / derived_anchor if derived_anchor else 0.0
                for d in days[-MAX_DAYS:]:
                    silver_rows[d] = round((silver_spot[d] / G_PER_OZT) * fx[d] * k, 2)
                silver_rows[anchor_date] = round(s_today, 2)

        # ---- persist sidecars (same shape the scraper merge expects) ----
        gk = sorted(gold_rows.keys(), reverse=True)[:MAX_DAYS]
        gold_side = {d: gold_rows[d] for d in sorted(gk)}
        (GOLD_DIR / f"{cur}.json").write_text(
            json.dumps(gold_side, indent=2) + "\n", encoding="utf-8")

        sk = sorted(silver_rows.keys(), reverse=True)[:MAX_DAYS]
        silver_side = {d: silver_rows[d] for d in sorted(sk)}
        (SILVER_DIR / f"{cur}.json").write_text(
            json.dumps(silver_side, indent=2) + "\n", encoding="utf-8")

        # ---- patch rates.json history in place (newest first) ----
        gold["history"] = [
            {"date": d, "per_g_22k": gold_rows[d]["22k"],
             "per_g_24k": gold_rows[d]["24k"]}
            for d in sorted(gold_rows.keys(), reverse=True)
        ]
        silver["history"] = [
            {"date": d, "per_g": silver_rows[d]}
            for d in sorted(silver_rows.keys(), reverse=True)
        ]
        print(f"  {cur}: gold {len(gold_rows)}d  silver {len(silver_rows)}d")

    RATES.write_text(json.dumps(doc, indent=2) + "\n", encoding="utf-8")
    print("done. sidecars + public/rates.json updated.")


if __name__ == "__main__":
    backfill()
