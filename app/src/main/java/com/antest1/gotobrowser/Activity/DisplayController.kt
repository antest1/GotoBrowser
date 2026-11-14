package com.antest1.gotobrowser.Activity

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.antest1.gotobrowser.Constants.*
import com.antest1.gotobrowser.R
import kotlin.math.abs

/**
 * In split screen or a freeform window, where the app cannot rotate the display. A fixed
 * orientation there only letterboxes the app inside its window, and some systems also stop
 * the split from being resized, so no orientation is requested then. PiP is left out: it
 * keeps the request so that expanding it goes straight back to landscape.
 */
private val Activity.isInResizableWindow: Boolean
    get() = isInMultiWindowMode && !isInPictureInPictureMode

class DisplayController(
    private val activity: BrowserActivity,
    private val mContentView: WebView,
    /** Divider margin (dp) to keep above and below the game. */
    private val onMultiwinMarginChanged: (topDp: Int, bottomDp: Int) -> Unit
) {
    fun updateMultiwindowMargin() {
        val enabled = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_MULTIWIN_MARGIN, false)
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !activity.isInMultiWindowMode) {
            onMultiwinMarginChanged(0, 0)
            return
        }

        val windowRect = Rect()
        val screenRect = Rect()
        val decorView = activity.window.decorView
        decorView.getWindowVisibleDisplayFrame(windowRect)
        decorView.getGlobalVisibleRect(screenRect)

        val isFreeform = windowRect.top != screenRect.top &&
                windowRect.bottom != screenRect.bottom &&
                windowRect.left != screenRect.left &&
                windowRect.right != screenRect.right

        val margin = BrowserActivity.MULTIWIN_MARGIN_DP
        val center = (screenRect.top + screenRect.bottom) / 2
        when {
            isFreeform -> onMultiwinMarginChanged(0, 0)
            // Bottom split: the divider is above the window.
            windowRect.top > center -> onMultiwinMarginChanged(margin, 0)
            // Top split: the divider is below the window.
            windowRect.bottom < center -> onMultiwinMarginChanged(0, margin)
            else -> onMultiwinMarginChanged(0, 0)
        }
    }

    /** Whether the orientation request is currently dropped (see [isInResizableWindow]). */
    private var orientationReleased = activity.isInResizableWindow

    /**
     * The request dropped on entering split screen, put back as it was on leaving, so a locked
     * reverse landscape is not recomputed from the rotation at that moment. Null when there is
     * nothing to put back (started in split screen, or the settings changed meanwhile).
     */
    private var heldOrientation: Int? = null

    /**
     * Drops or restores the orientation request when the window enters or leaves split screen.
     * Other mode changes, like going in and out of PiP, leave the current request alone.
     */
    fun onWindowModeChanged() {
        val resizable = activity.isInResizableWindow
        if (resizable == orientationReleased) return
        val held = heldOrientation
        when {
            resizable -> {
                heldOrientation = activity.requestedOrientation
                orientationReleased = true
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            held != null -> {
                heldOrientation = null
                orientationReleased = false
                activity.requestedOrientation = held
            }
            else -> requestOrientationThenLock()
        }
    }

    /** Set while a saved lock waits for the window to settle (see [requestOrientationThenLock]). */
    private var lockPending = false

    /**
     * Requests the orientation without the saved lock first, and locks once the window has
     * settled into it (see [onOrientationSettled]). Locking right away would lock to the
     * rotation the launcher or the split screen left behind, not the side the device is held at.
     */
    fun requestOrientationThenLock() {
        updateOrientationLock(applyLock = false)
        lockPending = !orientationReleased &&
                activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_LOCKMODE, false)
    }

    /**
     * Applies a pending lock. Called when the window gains focus or its configuration changes;
     * with landscape on, it waits until the window is actually in landscape.
     */
    fun onOrientationSettled() {
        if (!lockPending) return
        val landscape = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_LANDSCAPE, true)
        if (landscape && activity.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) return
        updateOrientationLock()
    }

    fun updateOrientationLock(applyLock: Boolean = true) {
        lockPending = false
        heldOrientation = null
        orientationReleased = activity.isInResizableWindow
        if (orientationReleased) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            return
        }
        val isLockMode = applyLock && activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_LOCKMODE, false)
        if (activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_LANDSCAPE, true)) {
            activity.requestedOrientation = if (isLockMode) {
                val rot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION")
                    activity.windowManager.defaultDisplay.rotation
                }
                if (rot == Surface.ROTATION_270) {
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            } else ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        } else {
            activity.requestedOrientation = if (isLockMode) ActivityInfo.SCREEN_ORIENTATION_LOCKED
            else ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    fun applyKeyboardSetting() {
        val enabled = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_KEYBOARD, true)
        mContentView.isFocusableInTouchMode = enabled
        mContentView.isFocusable = enabled
        mContentView.descendantFocusability =
            if (enabled) ViewGroup.FOCUS_AFTER_DESCENDANTS else ViewGroup.FOCUS_BLOCK_DESCENDANTS
    }

    fun applyFpsLimit() {
        val prefKey = activity.getString(R.string.preference_key)
        val sharedPref = activity.getSharedPreferences(prefKey, Context.MODE_PRIVATE)
        val fpsLimitStr = sharedPref.getString(PREF_FPS_LIMIT, DEFAULT_FPS_LIMIT) ?: DEFAULT_FPS_LIMIT
        val targetFps = if (fpsLimitStr == DEFAULT_FPS_LIMIT) 0f else (fpsLimitStr.toFloatOrNull() ?: 0f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val window = activity.window
            if (targetFps > 0f) {
                val displayManager = activity.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
                val modes = display?.supportedModes
                val targetMode = modes?.firstOrNull { mode ->
                    abs(mode.refreshRate - targetFps) < 1.0f
                }
                if (targetMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = targetMode.modeId
                    window.attributes = params
                }
            } else {
                val params = window.attributes
                params.preferredDisplayModeId = 0
                window.attributes = params
            }
        }
    }

    fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
