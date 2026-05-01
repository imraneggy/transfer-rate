package com.transferrate.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format mirrors the JSON produced by `scrapers/run_all.py`. Keep these
 * data classes in sync with `scrapers/base.py::Quote`.
 *
 * Security notes:
 *   - `@Serializable` classes are validated on parse; mismatches throw.
 *   - All numeric fields are nullable so a missing/garbled field doesn't
 *     fail the whole document.
 *   - There is intentionally no field that could carry executable content
 *     (no URLs are auto-opened without user click).
 */
@Serializable
data class RatesDocument(
    @SerialName("schema_version") val schemaVersion: Int,
    val base: String,
    val quote: String,
    @SerialName("amount_base") val amountBase: Double,
    @SerialName("started_at") val startedAt: String,
    @SerialName("completed_at") val completedAt: String,
    val providers: List<ProviderQuote>,
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
    @SerialName("fetched_at") val fetchedAt: String,
)

/**
 * Sanity-check a parsed document. We refuse obviously implausible values
 * before they reach the UI — defense-in-depth against a poisoned JSON file.
 */
fun RatesDocument.validate(): RatesDocument {
    require(schemaVersion == 1) { "Unsupported schema version: $schemaVersion" }
    require(base == "AED" && quote == "INR") { "Unexpected currency pair" }
    require(amountBase in 1.0..1_000_000.0) { "Amount out of range" }
    providers.forEach { p ->
        p.rate?.let { require(it in 0.1..1000.0) { "Rate out of range for ${p.providerId}" } }
        p.effectiveRate?.let { require(it in 0.1..1000.0) }
        p.feeBase?.let { require(it in 0.0..100_000.0) }
    }
    return this
}
