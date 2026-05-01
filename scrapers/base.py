from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from typing import Optional


@dataclass
class Quote:
    """A single rate quote from one provider.

    All monetary fields are in their native currency (AED for fees, INR for received).
    Any field a scraper cannot reliably extract should be left as None — partial
    data is more useful than no data, and the app treats None as "not shown."
    """
    provider_id: str
    provider_name: str
    base: str                      # "AED"
    quote: str                     # "INR"
    amount_base: float             # amount user is sending (e.g. 1000.0 AED)
    rate: Optional[float]          # advertised/gross rate (1 AED = X INR)
    fee_base: Optional[float]      # provider fee, in AED
    received_quote: Optional[float]  # what recipient actually gets, in INR
    effective_rate: Optional[float]  # received_quote / amount_base — the truth
    delivery_estimate: Optional[str]  # human-readable, e.g. "within minutes"
    url: Optional[str]             # deep link or rate page
    status: str = "ok"             # "ok" | "stale" | "error" | "investigating"
    note: Optional[str] = None     # error message or context, shown only on non-ok
    fetched_at: str = ""           # ISO 8601 UTC timestamp

    def to_dict(self) -> dict:
        return asdict(self)


class BaseProvider(ABC):
    """Abstract scraper interface. Each provider lives in its own module.

    Concrete subclasses must implement fetch_inr() at minimum. They should
    raise any exception they can't handle internally — the orchestrator
    catches and converts these to status='error' Quote records, so one
    flaky provider never breaks the run.
    """
    id: str = ""           # short slug, e.g. "wise"
    display_name: str = "" # human name shown in app, e.g. "Wise"

    @abstractmethod
    def fetch_inr(self, amount_aed: float = 1000.0) -> Quote:
        """Return current AED -> INR quote for the given send amount."""
        raise NotImplementedError

    def _now_iso(self) -> str:
        return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    def _stub(self, note: str, status: str = "investigating") -> Quote:
        """Helper for providers we haven't reverse-engineered yet."""
        return Quote(
            provider_id=self.id,
            provider_name=self.display_name,
            base="AED",
            quote="INR",
            amount_base=1000.0,
            rate=None,
            fee_base=None,
            received_quote=None,
            effective_rate=None,
            delivery_estimate=None,
            url=None,
            status=status,
            note=note,
            fetched_at=self._now_iso(),
        )
