#!/usr/bin/env python3
"""
Prune APK assets from old GitHub Releases on this repository.

WHY:
    Every release ships 4 APK variants (3 ABI-split + 1 universal) at
    ~3.3 MB each = ~13 MB per release.  At 17 historic releases we were
    sitting on 221 MB of redundant binaries — only the latest is useful
    to users, older ones exist only to preserve changelog history.

WHAT IT DOES:
    1. Lists all releases on the repo.
    2. Sorts by semver tag (newest first).
    3. Keeps APK assets on the latest --keep releases (default: 2 — the
       current release + one fallback for users mid-side-load).
    4. For every older release: deletes only `.apk` assets; the release
       itself (and its changelog/notes) stays so the version history
       remains browsable on the GitHub Releases page.
    5. Non-APK assets are never touched.  Pre-release / draft releases
       are skipped (no automatic destruction of unfinished work).

USAGE:
    Set GH_TOKEN with a token that has `contents: write` permission on
    the repo, then:

        python3 prune_release_apks.py --keep 2

    In GitHub Actions (with the workflow's default `GITHUB_TOKEN`):

        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: python3 .github/scripts/prune_release_apks.py --keep 2

    The script is idempotent — running it twice in a row deletes
    nothing on the second pass.

RATE LIMITS:
    Uses the authenticated GitHub REST API (5000 req/hr).  Each release
    consumes 1 list call + N delete calls where N = number of APK
    assets on that release (typically 4).  17 historic releases pruning
    down to 2 = ~64 DELETEs — well under the hourly budget.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request


REPO = "imraneggy/transfer-rate"
API_ROOT = "https://api.github.com"


def gh_request(method: str, path: str, token: str) -> object | None:
    """Send an authenticated GitHub API request and return parsed JSON.

    Returns None for 204 No Content (DELETE responses).  Raises on any
    non-2xx status so the workflow surface fails clearly rather than
    silently skipping.
    """
    url = f"{API_ROOT}{path}"
    req = urllib.request.Request(
        url,
        method=method,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "transfer-rate-prune-script",
        },
    )
    with urllib.request.urlopen(req) as response:
        if response.status == 204:
            return None
        return json.loads(response.read().decode("utf-8"))


def list_all_releases(token: str) -> list[dict]:
    """Page through every release on the repo (default 30/page, max 100)."""
    releases: list[dict] = []
    page = 1
    while True:
        chunk = gh_request(
            "GET",
            f"/repos/{REPO}/releases?per_page=100&page={page}",
            token,
        )
        if not chunk:
            break
        releases.extend(chunk)  # type: ignore[arg-type]
        if len(chunk) < 100:  # type: ignore[arg-type]
            break
        page += 1
    return releases


def semver_key(release: dict) -> tuple[int, int, int, str]:
    """Sort key for a release: (major, minor, patch, pre-release suffix).

    Tags that don't parse as semver sort to the bottom so they never
    accidentally end up in the "keep latest" window.  Pre-release
    suffixes (e.g. -alpha, -beta) sort lower than the GA tag with the
    same major.minor.patch.
    """
    tag = release.get("tag_name", "") or ""
    match = re.match(r"^v?(\d+)\.(\d+)\.(\d+)(?:[-.](.*))?$", tag)
    if not match:
        return (-1, -1, -1, "")
    major, minor, patch, suffix = match.groups()
    return (int(major), int(minor), int(patch), suffix or "~~~")


def delete_asset(asset: dict, token: str, dry_run: bool) -> None:
    """Delete a single release asset by ID."""
    asset_id = asset["id"]
    name = asset.get("name", "?")
    size_mb = asset.get("size", 0) / 1024 / 1024
    prefix = "[DRY-RUN] would delete" if dry_run else "Deleting"
    print(f"  {prefix}: {name} ({size_mb:.2f} MB)")
    if dry_run:
        return
    try:
        gh_request("DELETE", f"/repos/{REPO}/releases/assets/{asset_id}", token)
    except urllib.error.HTTPError as exc:
        # 404 = already gone; treat as success so re-runs are idempotent.
        if exc.code == 404:
            print(f"    (already gone, skipping)")
            return
        raise


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--keep",
        type=int,
        default=2,
        help="Number of latest releases to keep APK assets on (default: 2).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="List what would be deleted without actually deleting.",
    )
    args = parser.parse_args()

    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not token:
        print("ERROR: set GH_TOKEN or GITHUB_TOKEN in the environment", file=sys.stderr)
        return 2

    if args.keep < 1:
        print("ERROR: --keep must be >= 1 (don't delete the current release)", file=sys.stderr)
        return 2

    releases = list_all_releases(token)
    # Skip drafts entirely — they're in-progress work and the user
    # probably doesn't want them pruned automatically.
    releases = [r for r in releases if not r.get("draft", False)]
    releases.sort(key=semver_key, reverse=True)

    if not releases:
        print("No releases found — nothing to prune.")
        return 0

    keep = releases[: args.keep]
    prune = releases[args.keep :]

    print(f"Found {len(releases)} release(s)")
    print(f"Keeping APKs on {len(keep)} latest:")
    for r in keep:
        apk_count = sum(1 for a in r.get("assets", []) if a["name"].endswith(".apk"))
        print(f"  - {r['tag_name']} ({apk_count} APKs)")
    print(f"Pruning APKs from {len(prune)} older release(s):")

    total_freed_mb = 0.0
    total_deleted = 0
    for release in prune:
        apk_assets = [a for a in release.get("assets", []) if a["name"].endswith(".apk")]
        if not apk_assets:
            continue
        size_mb = sum(a.get("size", 0) for a in apk_assets) / 1024 / 1024
        print(f"  {release['tag_name']} ({len(apk_assets)} APKs, {size_mb:.1f} MB)")
        for asset in apk_assets:
            delete_asset(asset, token, args.dry_run)
            total_deleted += 1
            total_freed_mb += asset.get("size", 0) / 1024 / 1024

    verb = "Would free" if args.dry_run else "Freed"
    print(f"\n{verb}: {total_freed_mb:.1f} MB across {total_deleted} APK asset(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
