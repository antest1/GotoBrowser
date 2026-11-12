package com.antest1.gotobrowser.Helpers

import android.content.Context
import com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.Helpers.chs.ChsConfig

/**
 * Coordinates mutual exclusion and runtime state synchronization between
 * Kancolle_to_CHS ([ChsConfig]) and KCCP ([KenPatcher]).
 */
object ModPatchCoordinator {

    /**
     * Synchronizes language patches on startup/reload; CHS takes precedence if both were enabled.
     */
    fun prepareLanguagePatches(context: Context, kenPatcher: KenPatcher) {
        ChsConfig.init(context)
        val mainPref = context.getSharedPreferences(
            context.getString(R.string.preference_key),
            Context.MODE_PRIVATE
        )
        if (ChsConfig.isEnabled() && mainPref.getBoolean(PREF_MOD_KCCP_LANG_PATCH, false)) {
            mainPref.edit().putBoolean(PREF_MOD_KCCP_LANG_PATCH, false).apply()
        }
        kenPatcher.prepare(context)
        ChsConfig.prepare(context)
    }

    /**
     * Invoked when the CHS master switch is toggled in settings.
     */
    fun onChsToggled(context: Context, enabled: Boolean) {
        ChsConfig.init(context)
        ChsConfig.setEnabled(enabled)
        if (enabled) {
            val mainPref = context.getSharedPreferences(
                context.getString(R.string.preference_key),
                Context.MODE_PRIVATE
            )
            if (mainPref.getBoolean(PREF_MOD_KCCP_LANG_PATCH, false)) {
                mainPref.edit().putBoolean(PREF_MOD_KCCP_LANG_PATCH, false).apply()
                KenPatcher().prepare(context)
            }
        }
        ChsConfig.prepare(context)
    }

    /**
     * Invoked when the KCCP patch switch is toggled in settings.
     */
    fun onKccpToggled(context: Context, enabled: Boolean) {
        ChsConfig.init(context)
        if (enabled && ChsConfig.isEnabled()) {
            ChsConfig.setEnabled(false)
            ChsConfig.prepare(context)
        }
        KenPatcher().prepare(context)
    }
}
