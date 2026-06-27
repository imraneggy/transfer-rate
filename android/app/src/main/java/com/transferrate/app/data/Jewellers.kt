package com.transferrate.app.data

/**
 * UAE gold jewellers shown in the gold sheet's "Jewellers" directory.
 *
 * IMPORTANT — this is a directory, NOT a rate comparison.  The UAE retail
 * gold rate is uniform across jewellers: the Dubai Gold & Jewellery Group
 * (DGJG) publishes one daily AED/gram rate that every shop displays.
 * Verified 2026-06-27: Malabar UAE's own rate API returned 24K = 490.7524,
 * byte-identical to the aggregator rate the app already shows.  So listing a
 * per-jeweller gold price would just repeat the same number five times.
 *
 * Instead this links each jeweller's official daily-rate page (one tap) and
 * the UI notes that making charges — the real differentiator between shops —
 * vary by design and store and aren't published in any reliable feed.
 *
 * All [ratePageUrl]s verified HTTP 200 on 2026-06-27 and are https:// so the
 * value is safe to hand to Intent.ACTION_VIEW.  [id] doubles as the bundled
 * logo lookup key: drop res/drawable-nodpi/logo_<id>.png to replace the
 * initials avatar with a real logo, no code change (see ProviderAvatar).
 *
 * Trademark note: jeweller names/logos are trademarks of their owners, used
 * here for nominative identification in a comparison context only.
 */
data class Jeweller(
    val id: String,
    val name: String,
    val ratePageUrl: String,
)

val JEWELLERS: List<Jeweller> = listOf(
    Jeweller(
        id = "joy_alukkas",
        name = "Joy Alukkas",
        ratePageUrl = "https://www.joyalukkas.com/ae/goldrate",
    ),
    Jeweller(
        id = "malabar",
        name = "Malabar Gold & Diamonds",
        ratePageUrl = "https://www.malabargoldanddiamonds.com/ae/goldprice",
    ),
    Jeweller(
        id = "kalyan",
        name = "Kalyan Jewellers",
        ratePageUrl = "https://www.kalyanjewellers.net/gold-rate/Gold-Rate-Today",
    ),
    Jeweller(
        id = "sky",
        name = "Sky Jewellery",
        ratePageUrl = "https://www.skyjewellery.com/gold-rate",
    ),
    Jeweller(
        id = "damas",
        name = "Damas Jewellery",
        ratePageUrl = "https://www.damasjewellery.com/",
    ),
)
