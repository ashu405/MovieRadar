package com.movieradar.app

import android.app.Application
import com.movieradar.app.alerts.AlertNotifier
import com.movieradar.app.alerts.AlertWorker
import com.movieradar.app.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MovieRadarApp : Application() {
    @Inject lateinit var settings: SettingsRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AlertNotifier.ensureChannel(this)
        // Keep the daily "now streaming" check in step with Settings → Streaming alerts.
        scope.launch {
            settings.alertsEnabled.collect { on ->
                if (on) AlertWorker.schedule(this@MovieRadarApp) else AlertWorker.cancel(this@MovieRadarApp)
            }
        }
    }
}
