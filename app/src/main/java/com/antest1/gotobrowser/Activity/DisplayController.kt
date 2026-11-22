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
 * In a freeform (pop-up) window. The windowing mode is not public API, but the configuration
 * prints it from Android 9; on older versions a freeform window counts as split screen.
 */
private val Activity.isInFreeformWindow: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            resources.configuration.toString().contains("mWindowingMode=freeform")

/**
 * In split screen, where the app cannot rotate the display. A fixed orientation there only
 * letterboxes the app inside its window, and some systems also stop the split from being
 * resized, so no orientation is requested then. Freeform windows keep the request: some
 * systems shape the window after it, and open it in portrait without it. PiP keeps it too,
 * so that expanding it goes straight back to landscape.
 */
private val Activity.isInSplitScreen: Boolean
    get() = isInMultiWindowMode && !isInPictureInPictureMode && !isInFreeformWindow

class DisplayController(
    private val activity: BrowserActivity,
    /** The game WebView, created after this controller (see BrowserActivity.onCreate). */
    private val contentView: () -> WebView?,
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

    /** Whether the orientation request is currently dropped (see [isInSplitScreen]). */
    private var orientationReleased = activity.isInSplitScreen

    /** Last seen [isInFreeformWindow], to notice moves between split screen and freeform. */
    private var inFreeform = activity.isInFreeformWindow

    /**
     * Moving between split screen and a freeform window changes neither the multi-window nor
     * the PiP mode, only the configuration, so the window mode is checked again from there.
     */
    fun onConfigurationChanged() {
        val freeform = activity.isInFreeformWindow
        if (freeform == inFreeform) return
        inFreeform = freeform
        onWindowModeChanged()
    }

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
        val split = activity.isInSplitScreen
        if (split == orientationReleased) return
        val held = heldOrientation
        when {
            split -> {
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
        orientationReleased = activity.isInSplitScreen
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
        val view = contentView() ?: return
        val enabled = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_KEYBOARD, true)
        view.isFocusableInTouchMode = enabled
        view.isFocusable = enabled
        view.descendantFocusability =
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
