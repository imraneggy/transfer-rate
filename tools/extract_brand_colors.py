"""Extract dominant brand colors from each bundled provider logo PNG.

The provider logos in `res/drawable-nodpi/logo_<provider>.png` were
favicon-derived from each provider's official site at bundle time, so
they are authoritative.  We:

    1. Read each PNG into RGBA.
    2. Discard transparent / near-transparent / near-white / near-black
       pixels (white margins, black text on white logos, alpha edges).
    3. K-means cluster the remaining pixels into 4 groups.
    4. Pick the largest non-near-grey cluster as the dominant brand
       colour.  ("Near grey" filter avoids mistaking neutral logo
       backgrounds for brand colour.)
    5. Compute the light-mode and dark-mode tint a BEST card should
       use:
         * Light tint  = brand colour mixed 25% into white.
         * Dark tint   = brand colour mixed 25% into deep navy
                         (#0F1720 — the app's dark surface).
       Both tints keep the appropriate `onSurface` text APCA-legible
       (verified by the validator on the v0.29.3 palette).

Output: prints a Kotlin map literal that can be pasted into
`ProviderBrand.kt`.  Optionally writes it directly to that file with
`--write`.

Usage:
    python tools/extract_brand_colors.py
    python tools/extract_brand_colors.py --write
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np
from PIL import Image


REPO = Path(__file__).resolve().parent.parent
LOGO_DIR = REPO / "android" / "app" / "src" / "main" / "res" / "drawable-nodpi"
PROVIDER_BRAND_KT = (
    REPO / "android" / "app" / "src" / "main" / "java" / "com" / "transferrate"
    / "app" / "ui" / "ProviderBrand.kt"
)


# Pixels to drop before clustering, by criteria:
ALPHA_THRESHOLD = 64        # < 25% opacity
WHITE_MAX = 240             # any channel above this on all three = "near white"
BLACK_MAX = 16              # all channels below this = "near black"
GREY_TOLERANCE = 12         # if max(rgb) - min(rgb) < this, it's a neutral grey


def filter_brand_pixels(img: Image.Image) -> np.ndarray:
    """Return an (N, 3) array of brand-candidate RGB pixels."""
    rgba = np.array(img.convert("RGBA"))
    h, w, _ = rgba.shape
    px = rgba.reshape(-1, 4)

    alpha = px[:, 3]
    r, g, b = px[:, 0], px[:, 1], px[:, 2]
    rgb_max = np.maximum(np.maximum(r, g), b)
    rgb_min = np.minimum(np.minimum(r, g), b)

    mask = (
        (alpha >= ALPHA_THRESHOLD)
        & ~((r >= WHITE_MAX) & (g >= WHITE_MAX) & (b >= WHITE_MAX))   # not near-white
        & ~((r <= BLACK_MAX) & (g <= BLACK_MAX) & (b <= BLACK_MAX))   # not near-black
        & ((rgb_max - rgb_min) >= GREY_TOLERANCE)                     # not neutral grey
    )
    return px[mask, :3]


def kmeans(samples: np.ndarray, k: int = 4, max_iter: int = 30, seed: int = 7) -> tuple[np.ndarray, np.ndarray]:
    """Tiny k-means on RGB.  Returns (centroids, label-array).

    Plenty good for picking a few dominant clusters from a couple
    hundred thousand pixels.  ~50 ms total per logo.
    """
    rng = np.random.default_rng(seed)
    n = len(samples)
    if n == 0:
        return np.empty((0, 3)), np.empty(0, dtype=int)
    init_idx = rng.choice(n, size=min(k, n), replace=False)
    centroids = samples[init_idx].astype(np.float32)

    for _ in range(max_iter):
        # Assign each sample to the nearest centroid (squared Euclidean).
        d = ((samples[:, None, :].astype(np.float32) - centroids[None, :, :]) ** 2).sum(-1)
        labels = d.argmin(axis=1)
        # Update centroids.
        new_centroids = np.array([
            samples[labels == c].mean(axis=0) if (labels == c).any() else centroids[c]
            for c in range(len(centroids))
        ])
        if np.allclose(new_centroids, centroids, atol=0.5):
            centroids = new_centroids
            break
        centroids = new_centroids
    return centroids, labels


def hex_of(rgb: tuple[float, float, float]) -> str:
    r, g, b = (int(round(c)) for c in rgb)
    return f"#{r:02X}{g:02X}{b:02X}"


def mix_into(brand_rgb: tuple[float, float, float], target_rgb: tuple[int, int, int], frac: float) -> str:
    """Blend brand colour into a target (white or dark navy)."""
    out = tuple(target_rgb[i] * (1 - frac) + brand_rgb[i] * frac for i in range(3))
    return hex_of(out)


def extract_for(logo_path: Path) -> tuple[str, str, str]:
    """Return (raw-brand-hex, light-mode-tint-hex, dark-mode-tint-hex)."""
    img = Image.open(logo_path)
    px = filter_brand_pixels(img)
    if len(px) == 0:
        # Fallback: image was all-grey or transparent; use neutral indigo
        return "#7676FF", "#CDD9FF", "#2A2D5C"
    centroids, labels = kmeans(px, k=4)
    counts = np.bincount(labels, minlength=len(centroids))
    # Pick the most-saturated centroid among the top-2 by population, so
    # we don't accidentally pick a near-grey large cluster from a logo
    # that has lots of muted background.
    order = counts.argsort()[::-1]
    top = order[:2]
    def saturation(c):
        cmax = max(c)
        cmin = min(c)
        return (cmax - cmin) / max(cmax, 1.0)
    pick = max(top, key=lambda i: saturation(centroids[i]))
    brand_rgb = tuple(centroids[pick])

    raw_hex = hex_of(brand_rgb)
    # v0.29.6: bumped light-mode mix 22% -> 32% so the BEST card
    # actually looks tinted in light mode (was so pale you could barely
    # tell which provider had won).  Still keeps onSurface text
    # APCA-legible — verified by tools/validate_color_palette.py.
    light_hex = mix_into(brand_rgb, (255, 255, 255), frac=0.32)
    # Dark tint unchanged at 45% mix into the dark surface — already
    # had enough presence in dark mode.
    dark_hex = mix_into(brand_rgb, (0x1F, 0x2F, 0x41), frac=0.45)
    return raw_hex, light_hex, dark_hex


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--write",
        action="store_true",
        help="Overwrite ProviderBrand.kt with the computed values",
    )
    args = parser.parse_args()

    logos = sorted(LOGO_DIR.glob("logo_*.png"))
    if not logos:
        print(f"No logos found in {LOGO_DIR}", file=sys.stderr)
        return 2

    print(f"Found {len(logos)} provider logos in {LOGO_DIR}")
    print()

    results: list[tuple[str, str, str, str]] = []  # (id, raw, light, dark)
    for logo in logos:
        provider_id = logo.stem.replace("logo_", "")
        raw, light, dark = extract_for(logo)
        print(f"  {provider_id:<20} raw={raw}  light={light}  dark={dark}")
        results.append((provider_id, raw, light, dark))

    print()
    print("Kotlin map literal (paste into ProviderBrand.kt):")
    print()
    indent = "        "
    for pid, raw, light, dark in results:
        print(f'{indent}"{pid}"{" " * (20 - len(pid))} -> if (isDark) Color(0xFF{dark[1:]}) else Color(0xFF{light[1:]})  // raw {raw}')

    if args.write:
        write_provider_brand(results)
        print()
        print(f"Wrote {PROVIDER_BRAND_KT}")

    return 0


def write_provider_brand(results: list[tuple[str, str, str, str]]) -> None:
    """Render a fresh ProviderBrand.kt from the computed values."""
    lines = [
        "package com.transferrate.app.ui",
        "",
        "import androidx.compose.material3.MaterialTheme",
        "import androidx.compose.runtime.Composable",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.graphics.luminance",
        "",
        "/**",
        " * Provider brand-colour mapping used to tint the BEST card so the",
        " * winning provider's identity reads at a glance.",
        " *",
        " * **Generated by `tools/extract_brand_colors.py`** from the bundled",
        " * `res/drawable-nodpi/logo_<provider>.png` PNGs (which were derived",
        " * from each provider's official favicon at bundle time, so they ARE",
        " * the canonical brand colours — no hand-picking, no guessing).  The",
        " * extractor:",
        " *",
        " *   1. drops transparent / near-white / near-black / near-grey pixels,",
        " *   2. k-means clusters the remaining RGB pixels into 4 groups,",
        " *   3. picks the most-saturated of the top-2 by population,",
        " *   4. blends 22% into white for the light-mode tint, 45% into",
        " *      `#1F2F41` (the dark surface) for the dark-mode tint.",
        " *",
        " * The blends keep `MaterialTheme.colorScheme.onSurface` text APCA",
        " * Body-legible on top — verified by `tools/validate_color_palette.py`.",
        " *",
        " * Trademark posture: provider names and logos are trademarks of",
        " * their respective owners.  This map is used here for nominative",
        " * identification in a comparison context, not branding or",
        " * endorsement.  Re-run the extractor whenever a provider logo PNG",
        " * is updated to keep the tints in sync with the brand.",
        " */",
        "@Composable",
        "internal fun bestCardTintFor(providerId: String): Color {",
        "    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f",
        "    return when (providerId.lowercase()) {",
    ]
    for pid, raw, light, dark in results:
        pad = " " * (20 - len(pid))
        lines.append(
            f'        "{pid}"{pad} -> if (isDark) Color(0xFF{dark[1:]}) else Color(0xFF{light[1:]})  // brand {raw}'
        )
    lines.extend([
        "        // Fallback: prior dual-tone indigo (used when a provider is",
        "        // newly registered without a bundled logo PNG).",
        "        else                  -> if (isDark) Color(0xFF241776) else Color(0xFFA3ACFF)",
        "    }",
        "}",
        "",
    ])
    PROVIDER_BRAND_KT.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    sys.exit(main())
