package com.transferrate.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Schema v2: a single document carries quotes for all (AED -> X) corridors.
 *
 * Top-level shape:
 *   {
 *     "schema_version": 2,
 *     "base": "AED",
 *     "amount_base": 1000.0,
 *     "started_at":  "...",
 *     "completed_at": "...",
 *     "corridors": {
 *       "INR": [<provider_quote>, ...],
 *       "USD": [<provider_quote>, ...],
 *       ...
 *     }
 *   }
 *
 * Schema v2 is a breaking change from v1 (which had a flat `providers`
 * list). The reader rejects v1 with a clear message rather than partial
 * decode.
 */
@Serializable
data class RatesDocument(
    @SerialName("schema_version") val schemaVersion: Int,
    val base: String,
    @SerialName("amount_base") val amountBase: Double,
    @SerialName("started_at") val startedAt: String,
    @SerialName("completed_at") val completedAt: String,
    val corridors: Map<String, List<ProviderQuote>>,
)

@Serializable
data class ProviderQuote(
    @SerialName("provider_id") val providerId: String,
    @SerialName("provider_name") val providerName: String,
    val base: String,
    val quote: String,
    @SerialName("amount_base") val amountBase: Double,
    val rate: Double? = null,
    @SerialName("fee_base") val feeBase: Double? = null,
    @SerialName("received_quote") val receivedQuote: Double? = null,
    @SerialName("effective_rate") val effectiveRate: Double? = null,
    @SerialName("delivery_estimate") val deliveryEstimate: String? = null,
    val url: String? = null,
    val status: String,
    val note: String? = null,
    @SerialName("promo_rate") val promoRate: Double? = null,
    @SerialName("promo_note") val promoNote: String? = null,
    @SerialName("fetched_at") val fetchedAt: String,
)

/** Sanity-check a parsed document. Bounded ranges before UI rendering. */
fun RatesDocument.validate(): RatesDocument {
    require(schemaVersion == 2) { "Unsupported schema version: $schemaVersion (expected 2)" }
    require(base == "AED") { "Unexpected base currency: $base" }
    require(amountBase in 1.0..1_000_000.0) { "Amount out of range" }
    corridors.forEach { (code, providers) ->
        require(code.length == 3) { "Corridor code must be 3-letter: $code" }
        providers.forEach { p ->
            p.rate?.let { require(it in 0.0001..10000.0) { "Rate out of range for ${p.providerId} in $code" } }
            p.effectiveRate?.let { require(it in 0.0001..10000.0) }
            p.promoRate?.let { require(it in 0.0001..10000.0) }
            p.feeBase?.let { require(it in 0.0..100_000.0) }
        }
    }
    return this
}

/** Display metadata for each supported currency. Symbol + full name. */
data class CurrencyInfo(val code: String, val symbol: String, val flag: String, val name: String)

val CURRENCIES: Map<String, CurrencyInfo> = listOf(
    CurrencyInfo("INR", "₹",   "🇮🇳", "Indian Rupee"),
    CurrencyInfo("PKR", "₨",   "🇵🇰", "Pakistani Rupee"),
    CurrencyInfo("PHP", "₱",   "🇵🇭", "Philippine Peso"),
    CurrencyInfo("BDT", "৳",   "🇧🇩", "Bangladeshi Taka"),
    CurrencyInfo("EGP", "E£",  "🇪🇬", "Egyptian Pound"),
    CurrencyInfo("USD", "$",   "🇺🇸", "US Dollar"),
    CurrencyInfo("EUR", "€",   "🇪🇺", "Euro"),
    CurrencyInfo("GBP", "£",   "🇬🇧", "Pound Sterling"),
    CurrencyInfo("NPR", "रू",  "🇳🇵", "Nepalese Rupee"),
    CurrencyInfo("LKR", "Rs",  "🇱🇰", "Sri Lankan Rupee"),
).associateBy { it.code }
