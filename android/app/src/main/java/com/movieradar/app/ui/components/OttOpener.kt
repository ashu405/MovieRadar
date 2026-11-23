package com.movieradar.app.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.movieradar.app.domain.OttLinks
import com.movieradar.app.domain.Provider

/**
 * Opens a streaming service for [title]:
 * 0. the exact title link (from the proxy) in the service's app, or in the browser,
 * 1. else the service's app with its search link (if the app handles it),
 * 2. else the app's home screen (title copied to clipboard so you can paste it in search),
 * 3. else the website in the browser.
 */
object OttOpener {
    /**
     * @param exactUrl the service's own title URL from the proxy, when available. It opens the app
     *   straight on the title (Netflix movies start playing). Without it we fall back to search.
     */
    fun open(context: Context, provider: Provider, title: String, fallbackLink: String?, exactUrl: String? = null, isMovie: Boolean = true) {
        val exact = exactUrl?.let { OttLinks.exactTarget(provider.id, it, isMovie) }
        if (exact != null && openExact(context, exact.packageName, exact.searchUrl)) return

        val target = OttLinks.targetFor(provider.id, title, fallbackLink)
        if (target == null) {
            Toast.makeText(context, "No link available for ${provider.name}", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = target.searchUrl.toUri()
        val pm = context.packageManager
        target.packageName?.let { pkg ->
            val inApp = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (inApp.resolveActivity(pm) != null && start(context, inApp)) return
            pm.getLaunchIntentForPackage(pkg)?.let { launch ->
                copy(context, title)
                Toast.makeText(context, "Opening ${provider.name} — title copied, paste it in search", Toast.LENGTH_LONG).show()
                if (start(context, launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) return
            }
        }
        if (!start(context, Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }

    /** Title URL in the service's app if it's installed and accepts it, else any handler (browser). */
    private fun openExact(context: Context, pkg: String?, url: String): Boolean {
        val uri = url.toUri()
        if (pkg != null) {
            val inApp = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (inApp.resolveActivity(context.packageManager) != null && start(context, inApp)) return true
        }
        return start(context, Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openYoutube(context: Context, key: String) {
        start(context, Intent(Intent.ACTION_VIEW, "https://www.youtube.com/watch?v=$key".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent); true
    } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }

    private fun copy(context: Context, text: String) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("title", text))
    }
}
