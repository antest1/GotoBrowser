package com.antest1.gotobrowser.Helpers

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.antest1.gotobrowser.Activity.PatchDownloadState
import com.antest1.gotobrowser.R

/**
 * Runs a KCCP patch download as a user-initiated data transfer job (Android 14+), so the
 * download and install go on when the app is left, shown by a notification with a Cancel
 * action. The download itself is [KccpPatchDownloader]'s.
 */
class KccpPatchJobService : JobService() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        val downloader = KccpPatchDownloader.shared
        val title = downloader.activeTitle() ?: return false
        val context = applicationContext
        createChannel(context)
        // A user-initiated job must show its notification right away.
        setNotification(
            params, NOTIFICATION_ID,
            progressNotification(context, title, getString(R.string.patch_download_checking), null, true),
            // Left in place when the job ends, so it can show the result.
            JOB_END_NOTIFICATION_POLICY_DETACH
        )
        val observer = NotificationObserver(context, title)
        downloader.addObserver(observer)
        val started = downloader.runJob {
            mainHandler.post { downloader.removeObserver(observer) }
            jobFinished(params, false)
        }
        if (!started) {
            downloader.removeObserver(observer)
            context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        }
        return started
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // Also called after the app cancelled the job itself; the download is stopped then.
        val byUser = Build.VERSION.SDK_INT >= 31 && params.stopReason == JobParameters.STOP_REASON_USER
        KccpPatchDownloader.shared.stopForJob(byUser)
        // The archive cannot be resumed, so a retry would start over; the user decides.
        return false
    }

    // The download follows the network on its own; without this the system logs a warning on
    // every network change.
    override fun onNetworkChanged(params: JobParameters) {}

    /** Mirrors the download on the job's notification. */
    private class NotificationObserver(
        private val context: Context,
        private val title: String
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
            manager?.notify(NOTIFICATION_ID, progressNotification(context, title, state.message, state.progress, cancellable))
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

    companion object {
        private const val TAG = "GOTO-KCCP"
        private const val JOB_NAMESPACE = "kccp_patch"
        private const val JOB_ID = 1
        private const val NOTIFICATION_ID = 0x4B43 // "KC"
        private const val CHANNEL_ID = "NOTI_CHANNEL_PATCH_DOWNLOAD"
        private const val UPDATE_INTERVAL_MS = 1000L

        /**
         * Schedules the job that runs the download [KccpPatchDownloader] is starting. False
         * before Android 14, or when the system does not take it; the download then runs in
         * the app. Call while the app is visible.
         */
        fun schedule(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < 34) return false
            val scheduler = scheduler(context) ?: return false
            if (!scheduler.canRunUserInitiatedJobs()) return false
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, KccpPatchJobService::class.java))
                .setUserInitiated(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
            return try {
                scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS
            } catch (e: RuntimeException) {
                Log.w(TAG, "Could not schedule the patch download job: ${e.message}")
                false
            }
        }

        /** Cancels the job, whether it runs or still waits for the network. */
        fun cancel(context: Context) {
            if (Build.VERSION.SDK_INT < 34) return
            scheduler(context)?.cancel(JOB_ID)
        }

        private fun scheduler(context: Context): JobScheduler? {
            if (Build.VERSION.SDK_INT < 34) return null
            // A namespace of its own, so the id never meets the jobs of libraries.
            return context.getSystemService(JobScheduler::class.java)?.forNamespace(JOB_NAMESPACE)
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.settings_mod_kantaien_download),
                NotificationManager.IMPORTANCE_LOW
            )
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        private fun progressNotification(
            context: Context,
            title: String,
            message: String,
            progress: Float?,
            cancellable: Boolean
        ) = NotificationCompat.Builder(context, CHANNEL_ID)
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
    }
}
