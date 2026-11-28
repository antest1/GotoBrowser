package com.antest1.gotobrowser.Helpers

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.antest1.gotobrowser.Activity.PatchDownloadState
import com.antest1.gotobrowser.R

/** The notification that shows a KCCP patch download, with a Cancel action. */
object KccpPatchNotification {
    const val NOTIFICATION_ID = 0x4B43 // "KC"
    private const val CHANNEL_ID = "NOTI_CHANNEL_PATCH_DOWNLOAD"
    private const val UPDATE_INTERVAL_MS = 1000L

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.settings_mod_kantaien_download),
            NotificationManager.IMPORTANCE_LOW
        )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /**
     * The progress notification. [timeoutMs] takes it down when no update follows in time,
     * for a download whose app may be frozen or killed in the background.
     */
    fun progress(
        context: Context,
        title: String,
        message: String,
        progress: Float?,
        cancellable: Boolean,
        timeoutMs: Long? = null
    ): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.download_icon)
        .setContentTitle(title)
        .setContentText(message.lines().joinToString("  "))
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setProgress(100, ((progress ?: 0f) * 100).toInt(), progress == null)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setContentIntent(openAppIntent(context))
        .apply {
            timeoutMs?.let { setTimeoutAfter(it) }
            if (cancellable) {
                addAction(0, context.getString(R.string.action_cancel), PendingIntent.getBroadcast(
                    context, 0, Intent(context, CancelReceiver::class.java), PendingIntent.FLAG_IMMUTABLE
                ))
            }
        }
        .build()

    private fun openAppIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * Mirrors a download on the notification, and leaves its result there when the app is in
     * the background. Without the notification permission nothing shows.
     */
    class Observer(
        private val context: Context,
        private val title: String,
        private val timeoutMs: Long? = null
    ) : KccpPatchDownloader.Observer {
        private val manager = context.getSystemService(NotificationManager::class.java)
        private var lastPostAt = 0L
        private var lastCancellable: Boolean? = null

        override fun onDownloadStateChanged(state: PatchDownloadState) {
            if (state !is PatchDownloadState.Running) return
            val cancellable = state.onCancel != null
            val now = SystemClock.elapsedRealtime()
            // Updates coming faster than this are dropped by the system anyway.
            if (now - lastPostAt < UPDATE_INTERVAL_MS && cancellable == lastCancellable) return
            lastPostAt = now
            lastCancellable = cancellable
            manager?.notify(NOTIFICATION_ID, progress(context, title, state.message, state.progress, cancellable, timeoutMs))
        }

        override fun onDownloadFinished(message: String, result: KccpPatchDownloader.Result) {
            // A cancelled download needs no word; in the app the settings sheet shows the result.
            if (result == KccpPatchDownloader.Result.CANCELLED || isAppInForeground()) {
                manager?.cancel(NOTIFICATION_ID)
                return
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.download_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true)
                .build()
            manager?.notify(NOTIFICATION_ID, notification)
        }

        private fun isAppInForeground(): Boolean {
            val info = ActivityManager.RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(info)
            return info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    /** Takes the Cancel action of the notification. */
    class CancelReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            KccpPatchDownloader.shared.cancelDownload()
        }
    }
}
