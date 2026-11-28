package com.antest1.gotobrowser.Helpers

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.antest1.gotobrowser.Activity.PatchDownloadState
import com.antest1.gotobrowser.R

/**
 * Runs a KCCP patch download in a data sync foreground service where user-initiated jobs are
 * not available (before Android 14), so the download and install go on when the app is left,
 * shown by [KccpPatchNotification]. The download itself is [KccpPatchDownloader]'s.
 */
class KccpPatchService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloader = KccpPatchDownloader.shared
        val context = applicationContext
        val title = downloader.activeTitle() ?: getString(R.string.settings_mod_kantaien_download)
        KccpPatchNotification.createChannel(context)
        // A service started this way must be in the foreground within seconds, even when it
        // turns out to have nothing to run.
        ServiceCompat.startForeground(
            this, KccpPatchNotification.NOTIFICATION_ID,
            KccpPatchNotification.progress(context, title, getString(R.string.patch_download_checking), null, true),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        )
        val notification = KccpPatchNotification.Observer(context, title)
        val observer = object : KccpPatchDownloader.Observer {
            override fun onDownloadStateChanged(state: PatchDownloadState) {
                notification.onDownloadStateChanged(state)
            }

            override fun onDownloadFinished(message: String, result: KccpPatchDownloader.Result) {
                // Out of the foreground first, so the result is a notification the user can
                // dismiss; the cleanup that follows needs no protection.
                ServiceCompat.stopForeground(this@KccpPatchService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                notification.onDownloadFinished(message, result)
            }
        }
        downloader.addObserver(observer)
        val started = downloader.runFor(KccpPatchDownloader.Runner.SERVICE) {
            mainHandler.post {
                downloader.removeObserver(observer)
                stopSelf(startId)
            }
        }
        if (!started) {
            downloader.removeObserver(observer)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
        // A download cut off with the process is not picked up again.
        return START_NOT_STICKY
    }

    // The system's time limit for data sync services (Android 15), reached only when this
    // service runs where the job could not be used.
    override fun onTimeout(startId: Int, fgsType: Int) {
        KccpPatchDownloader.shared.stopBySystem(byUser = false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    companion object {
        private const val TAG = "GOTO-KCCP"

        /** Starts the service for the download being started; false when the system refuses it. */
        fun start(context: Context): Boolean = try {
            ContextCompat.startForegroundService(context, Intent(context, KccpPatchService::class.java))
            true
        } catch (e: RuntimeException) {
            // E.g. a foreground service may not be started from the background.
            Log.w(TAG, "Could not start the patch download service: ${e.message}")
            false
        }
    }
}
