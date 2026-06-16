package com.transferrate.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Per-device user personalization stored in SharedPreferences.
 * No account required — everything lives locally and can be cleared
 * by the user at any time via Android Settings → Apps → Transfer Rate.
 */
class UserProfile(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        FILE_NAME, Context.MODE_PRIVATE,
    )

    private val _displayName = MutableStateFlow(
        prefs.getString(KEY_DISPLAY_NAME, null),
    )
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _preferredAmount = MutableStateFlow(
        prefs.getFloat(KEY_PREFERRED_AMOUNT, DEFAULT_AMOUNT).toDouble(),
    )
    val preferredAmount: StateFlow<Double> = _preferredAmount.asStateFlow()

    private val _preferredCurrency = MutableStateFlow(
        prefs.getString(KEY_PREFERRED_CURRENCY, "INR") ?: "INR",
    )
    val preferredCurrency: StateFlow<String> = _preferredCurrency.asStateFlow()

    private val _favoriteProviders = MutableStateFlow(loadFavoriteProviders())
    val favoriteProviders: StateFlow<Set<String>> = _favoriteProviders.asStateFlow()

    fun setDisplayName(name: String?) {
        val trimmed = name?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit().run {
            if (trimmed == null) remove(KEY_DISPLAY_NAME) else putString(KEY_DISPLAY_NAME, trimmed)
            apply()
        }
        _displayName.value = trimmed
    }

    fun setPreferredAmount(amount: Double) {
        if (amount !in 1.0..1_000_000.0) return
        prefs.edit().putFloat(KEY_PREFERRED_AMOUNT, amount.toFloat()).apply()
        _preferredAmount.value = amount
    }

    fun setPreferredCurrency(code: String) {
        prefs.edit().putString(KEY_PREFERRED_CURRENCY, code).apply()
        _preferredCurrency.value = code
    }

    fun toggleFavoriteProvider(providerId: String) {
        val current = _favoriteProviders.value.toMutableSet()
        if (providerId in current) current.remove(providerId) else current.add(providerId)
        prefs.edit().putStringSet(KEY_FAVORITE_PROVIDERS, current).apply()
        _favoriteProviders.value = current
    }

    fun isFavorite(providerId: String): Boolean = providerId in _favoriteProviders.value

    private fun loadFavoriteProviders(): Set<String> =
        prefs.getStringSet(KEY_FAVORITE_PROVIDERS, emptySet()) ?: emptySet()

    companion object {
        private const val FILE_NAME = "transfer-rate-profile"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PREFERRED_AMOUNT = "preferred_amount"
        private const val KEY_PREFERRED_CURRENCY = "preferred_currency"
        private const val KEY_FAVORITE_PROVIDERS = "favorite_providers"
        private const val DEFAULT_AMOUNT = 1000f
    }
}
