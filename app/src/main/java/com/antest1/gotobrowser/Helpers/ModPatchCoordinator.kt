package com.antest1.gotobrowser.Helpers

import android.content.Context
import com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.Helpers.chs.ChsConfig

/**
 * 跨语言补丁协调器：负责简中汉化 (ChsConfig) 与英/印尼语补丁 (KenPatcher) 的互斥策略与状态同步。
 * 由 BrowserViewModel.prepareModPatchers() 在启动/重载时调用，以及 SettingsViewModel 在用户切换开关时调用。
 */
object ModPatchCoordinator {

    /**
     * 在浏览器启动或页面刷新时同步并准备语言类补丁（若两者同时处于开启状态，优先保留简中汉化并关闭 KCCP）
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
     * 当用户在设置中切换简中汉化总开关时调用
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
     * 当用户在设置中切换 KCCP 语言补丁开关时调用
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
