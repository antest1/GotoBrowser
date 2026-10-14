package com.antest1.gotobrowser.Activity

import android.content.Context
import android.content.pm.ActivityInfo
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

class DisplayController(
    private val activity: BrowserActivity,
    private val mContentView: WebView?,
    private val onMultiwinMarginChanged: (Int) -> Unit
) {
    fun updateMultiwindowMargin() {
        val enabled = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_MULTIWIN_MARGIN, false)
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !activity.isInMultiWindowMode) {
            onMultiwinMarginChanged(0)
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

        val margin = if (isFreeform) {
            0
        } else {
            val center = (screenRect.top + screenRect.bottom) / 2
            when {
                windowRect.top > center -> BrowserActivity.MULTIWIN_MARGIN_DP
                windowRect.bottom < center -> BrowserActivity.MULTIWIN_MARGIN_DP
                else -> 0
            }
        }
        onMultiwinMarginChanged(margin)
    }

    fun updateOrientationLock() {
        val isLockMode = activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_LOCKMODE, false)
        if (activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_LANDSCAPE, false)) {
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
        if (activity.getSharedPreferences("shared_pref", 0).getBoolean(PREF_KEYBOARD, true)) return
        mContentView?.isFocusableInTouchMode = false
        mContentView?.isFocusable = false
        mContentView?.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
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
