package com.antest1.gotobrowser.Activity

import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.webkit.WebView
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import com.antest1.gotobrowser.Constants.PREF_PIP_MODE

class PipController(
    private val activity: BrowserActivity,
    private val mContentView: WebView?,
    private val onPipModeChanged: (Boolean) -> Unit,
    private val hideSystemBars: () -> Unit
) {
    fun setupSmoothPipAnimation() {
        val pipEnabled = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_PIP_MODE, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity.supportsPiPMode() && pipEnabled) {
            val sourceRectHint = Rect()
            mContentView?.getGlobalVisibleRect(sourceRectHint)
            activity.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setSeamlessResizeEnabled(false)
                    .setSourceRectHint(sourceRectHint)
                    .setAutoEnterEnabled(true)
                    .setAspectRatio(Rational(1200, 720))
                    .build()
            )
            mContentView?.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
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
                mContentView?.getGlobalVisibleRect(sourceRectHint)
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

    suspend fun handlePinchToPipGesture(scope: PointerInputScope) {
        val pipEnabled = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_PIP_MODE, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity.supportsPiPMode() && pipEnabled) {
            scope.awaitPointerEventScope {
                while (true) {
                    var zoom = 1f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val changes = event.changes
                        if (changes.any { it.isConsumed }) break

                        val pressedChanges = changes.filter { it.pressed }
                        if (pressedChanges.size < 2) {
                            if (pressedChanges.isEmpty()) break
                            zoom = 1f
                        } else {
                            val p1 = pressedChanges[0]
                            val p2 = pressedChanges[1]
                            val currDist = (p1.position - p2.position).getDistance()
                            val prevDist = (p1.previousPosition - p2.previousPosition).getDistance()

                            if (prevDist > 0) {
                                val scaleDelta = currDist / prevDist
                                zoom *= scaleDelta

                                if (zoom < 0.75f) {
                                    changes.forEach { it.consume() }
                                    val sourceRectHint = Rect()
                                    mContentView?.getGlobalVisibleRect(sourceRectHint)
                                    activity.enterPictureInPictureMode(
                                        PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(1200, 720))
                                            .setSourceRectHint(sourceRectHint)
                                            .build()
                                    )
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
