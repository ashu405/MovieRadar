package com.movieradar.app.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.movieradar.app.alerts.AlertNotifier

/**
 * Returns a click handler for 🔔 Notify me. Turning it on asks for notification permission first
 * (Android 13+); the alert is saved either way, and a toast explains if notifications are blocked.
 */
@Composable
fun rememberNotifyToggle(isOn: Boolean, onChange: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val change by rememberUpdatedState(onChange)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        change(true)
        Toast.makeText(
            context,
            if (granted) "We'll notify you when it starts streaming" else "Alert saved, but notifications are blocked in Android settings",
            Toast.LENGTH_LONG,
        ).show()
    }
    return {
        when {
            isOn -> change(false)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !AlertNotifier.canNotify(context) ->
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> {
                change(true)
                Toast.makeText(context, "We'll notify you when it starts streaming", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
