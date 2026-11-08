package com.antest1.gotobrowser.Activity

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.ScaleGestureDetector
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.antest1.gotobrowser.Constants.PREF_PIP_MODE

class PipController(
    private val activity: BrowserActivity,
    private val mContentView: WebView,
    private val onPipModeChanged: (Boolean) -> Unit,
    private val hideSystemBars: () -> Unit
) {
    private var layoutListenerAdded = false

    fun setupSmoothPipAnimation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !activity.supportsPiPMode()) return
        updatePipParams()
        // Register once; the listener re-reads the setting, so turning PiP off also disables auto-enter.
        if (!layoutListenerAdded) {
            layoutListenerAdded = true
            mContentView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
                    updatePipParams()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun updatePipParams() {
        // Layout changes keep firing while the activity is being torn down.
        if (activity.isFinishing || activity.isDestroyed) return
        val pipEnabled = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_PIP_MODE, false)
        val sourceRectHint = Rect()
        mContentView.getGlobalVisibleRect(sourceRectHint)
        try {
            activity.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setSeamlessResizeEnabled(false)
                    .setSourceRectHint(sourceRectHint)
                    .setAutoEnterEnabled(pipEnabled)
                    .setAspectRatio(Rational(1200, 720))
                    .build()
            )
        } catch (e: IllegalStateException) {
            // The system may no longer know the activity (e.g. its task was removed).
            Log.e("GOTO", "setPictureInPictureParams failed: ${e.message}")
        }
    }

    fun onUserLeaveHint() {
        val pipEnabled = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_PIP_MODE, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity.supportsPiPMode() && pipEnabled) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val sourceRectHint = Rect()
                mContentView.getGlobalVisibleRect(sourceRectHint)
                activity.enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(1200, 720))
                        .setSourceRectHint(sourceRectHint)
                        .build()
                )
            }
        }
    }

    fun handlePictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        onPipModeChanged(isInPictureInPictureMode)
        if (!isInPictureInPictureMode) {
            hideSystemBars()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupPinchToPipGesture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !activity.supportsPiPMode()) return

        var zoom = 1f
        val detector = ScaleGestureDetector(
            activity,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    zoom = 1f
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val pipEnabled = activity.getSharedPreferences("shared_pref", 0)
                        .getBoolean(PREF_PIP_MODE, false)
                    if (!pipEnabled) return false

                    zoom *= detector.scaleFactor
                    if (zoom < 0.75f) {
                        zoom = 1f
                        val sourceRectHint = Rect()
                        mContentView.getGlobalVisibleRect(sourceRectHint)
                        activity.enterPictureInPictureMode(
                            PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(1200, 720))
                                .setSourceRectHint(sourceRectHint)
                                .build()
                        )
                    }
                    return true
                }
            }
        )
        mContentView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }
    }
}
