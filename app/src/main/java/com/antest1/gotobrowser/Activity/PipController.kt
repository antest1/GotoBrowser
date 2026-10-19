package com.antest1.gotobrowser.Activity

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.view.ScaleGestureDetector
import android.webkit.WebView
import com.antest1.gotobrowser.Constants.PREF_PIP_MODE

class PipController(
    private val activity: BrowserActivity,
    private val mContentView: WebView,
    private val onPipModeChanged: (Boolean) -> Unit,
    private val hideSystemBars: () -> Unit
) {
    fun setupSmoothPipAnimation() {
        val pipEnabled = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_PIP_MODE, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity.supportsPiPMode() && pipEnabled) {
            val sourceRectHint = Rect()
            mContentView.getGlobalVisibleRect(sourceRectHint)
            activity.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setSeamlessResizeEnabled(false)
                    .setSourceRectHint(sourceRectHint)
                    .setAutoEnterEnabled(true)
                    .setAspectRatio(Rational(1200, 720))
                    .build()
            )
            mContentView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
                    mContentView.getGlobalVisibleRect(sourceRectHint)
                    activity.setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setSeamlessResizeEnabled(false)
                            .setSourceRectHint(sourceRectHint)
                            .setAutoEnterEnabled(true)
                            .setAspectRatio(Rational(1200, 720))
                            .build()
                    )
                }
            }
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
