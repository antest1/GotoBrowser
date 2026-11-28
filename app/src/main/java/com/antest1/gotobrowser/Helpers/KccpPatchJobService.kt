package com.antest1.gotobrowser.Helpers

import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.antest1.gotobrowser.R

/**
 * Runs a KCCP patch download as a user-initiated data transfer job (Android 14+), so the
 * download and install go on when the app is left, shown by [KccpPatchNotification]. The
 * download itself is [KccpPatchDownloader]'s.
 */
class KccpPatchJobService : JobService() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        val downloader = KccpPatchDownloader.shared
        val title = downloader.activeTitle() ?: return false
        val context = applicationContext
        KccpPatchNotification.createChannel(context)
        // A user-initiated job must show its notification right away.
        setNotification(
            params, KccpPatchNotification.NOTIFICATION_ID,
            KccpPatchNotification.progress(context, title, getString(R.string.patch_download_checking), null, true),
            // Left in place when the job ends, so it can show the result.
            JOB_END_NOTIFICATION_POLICY_DETACH
        )
        val observer = KccpPatchNotification.Observer(context, title)
        downloader.addObserver(observer)
        val started = downloader.runFor(KccpPatchDownloader.Runner.JOB) {
            mainHandler.post { downloader.removeObserver(observer) }
            jobFinished(params, false)
        }
        if (!started) {
            downloader.removeObserver(observer)
            context.getSystemService(NotificationManager::class.java)?.cancel(KccpPatchNotification.NOTIFICATION_ID)
        }
        return started
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // Also called after the app cancelled the job itself; the download is stopped then.
        val byUser = Build.VERSION.SDK_INT >= 31 && params.stopReason == JobParameters.STOP_REASON_USER
        KccpPatchDownloader.shared.stopBySystem(byUser)
        // The archive cannot be resumed, so a retry would start over; the user decides.
        return false
    }

    // The download follows the network on its own; without this the system logs a warning on
    // every network change.
    override fun onNetworkChanged(params: JobParameters) {}

    companion object {
        private const val TAG = "GOTO-KCCP"
        private const val JOB_NAMESPACE = "kccp_patch"
        private const val JOB_ID = 1

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
    }
}
