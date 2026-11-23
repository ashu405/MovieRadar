package com.movieradar.app

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import com.movieradar.app.alerts.AlertNotifier
import com.movieradar.app.ui.DetailsRoute
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.movieradar.app.data.local.LibraryRepository
import com.movieradar.app.ui.MovieRadarRoot
import com.movieradar.app.ui.components.LocalWatchedKeys
import javax.inject.Inject
import com.movieradar.app.ui.theme.MovieRadarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var library: LibraryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        super.onCreate(savedInstanceState)
        openRequest.value = intent.toDetailsRoute()
        setContent {
            val watchedKeys by library.watchedKeys.collectAsState(initial = emptySet())
            CompositionLocalProvider(LocalWatchedKeys provides watchedKeys) {
                MovieRadarTheme { MovieRadarRoot(openRequest = openRequest.value, onOpenHandled = { openRequest.value = null }) }
            }
        }
    }

    /** A tapped alert while the app is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toDetailsRoute()?.let { openRequest.value = it }
    }

    private val openRequest = mutableStateOf<DetailsRoute?>(null)

    private fun Intent?.toDetailsRoute(): DetailsRoute? {
        val type = this?.getStringExtra(AlertNotifier.EXTRA_TYPE) ?: return null
        val id = getIntExtra(AlertNotifier.EXTRA_ID, 0).takeIf { it > 0 } ?: return null
        return if (type == "movie" || type == "tv") DetailsRoute(type, id) else null
    }
}
