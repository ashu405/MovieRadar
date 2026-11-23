package com.movieradar.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.movieradar.app.domain.OttService
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.movieradar.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val regionKey = stringPreferencesKey("region")
    private val adultKey = booleanPreferencesKey("show_adult")
    private val alertsKey = booleanPreferencesKey("streaming_alerts")
    private val myServicesKey = stringSetPreferencesKey("my_services")

    /** Settings → My services (used by 🎲 Surprise me "Any of mine"). Empty = not set. */
    val myServices: Flow<Set<OttService>> = context.dataStore.data
        .map { prefs -> prefs[myServicesKey].orEmpty().mapNotNull { n -> OttService.entries.firstOrNull { it.name == n } }.toSet() }
        .distinctUntilChanged()

    suspend fun setMyServices(services: Set<OttService>) {
        context.dataStore.edit { it[myServicesKey] = services.map { s -> s.name }.toSet() }
    }

    /** Settings → "Streaming alerts". On by default. */
    val alertsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[alertsKey] ?: true }
        .distinctUntilChanged()

    suspend fun setAlertsEnabled(on: Boolean) {
        context.dataStore.edit { it[alertsKey] = on }
    }

    /**
     * Settings → "Show 18+ titles". Off by default. Always false in the MoviesRadar (standard) app,
     * whatever an older install may have stored.
     */
    val showAdult: Flow<Boolean> =
        if (!BuildConfig.ADULT_ENABLED) flowOf(false)
        else context.dataStore.data.map { it[adultKey] ?: false }.distinctUntilChanged()

    suspend fun setShowAdult(on: Boolean) {
        context.dataStore.edit { it[adultKey] = on }
    }

    /** ISO-3166 region used for OTT availability, age ratings and releases. Defaults to India. */
    val region: Flow<String> = context.dataStore.data
        .map { it[regionKey] ?: DEFAULT_REGION }
        .distinctUntilChanged()

    suspend fun setRegion(code: String) {
        require(code.matches(Regex("^[A-Z]{2}$"))) { "Invalid region code" }
        context.dataStore.edit { it[regionKey] = code }
    }

    companion object {
        const val DEFAULT_REGION = "IN"
    }
}
