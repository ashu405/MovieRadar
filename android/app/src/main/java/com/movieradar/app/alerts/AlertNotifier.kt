package com.movieradar.app.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.movieradar.app.MainActivity
import com.movieradar.app.R
import com.movieradar.app.domain.AlertRules
import com.movieradar.app.ui.components.TmdbImage
import kotlinx.coroutines.withTimeoutOrNull

object AlertNotifier {
    const val CHANNEL_ID = "streaming_alerts"
    const val EXTRA_TYPE = "open_type"
    const val EXTRA_ID = "open_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Streaming alerts", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "When a title you follow starts streaming" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    suspend fun show(context: Context, a: NewArrival) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val t = a.title
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_TYPE, t.type.path)
            .putExtra(EXTRA_ID, t.id)
        val pi = PendingIntent.getActivity(
            context, t.id * 2 + t.type.ordinal, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(AlertRules.headline(a.providers))
            .setContentText(t.title)
            .setLargeIcon(poster(context, t.posterPath))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(t.id * 2 + t.type.ordinal, n)
        } catch (_: SecurityException) { /* permission revoked meanwhile */ }
    }

    /** Poster as the large icon; skipped if it doesn't load within 10 s. */
    private suspend fun poster(context: Context, path: String?): Bitmap? {
        val url = TmdbImage.poster(path) ?: return null
        return withTimeoutOrNull(10_000) {
            val result = context.imageLoader.execute(ImageRequest.Builder(context).data(url).allowHardware(false).build())
            (result as? SuccessResult)?.image?.toBitmap()
        }
    }
}
