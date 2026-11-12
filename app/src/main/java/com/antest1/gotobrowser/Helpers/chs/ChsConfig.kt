package com.antest1.gotobrowser.Helpers.chs

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration store for the Kancolle_to_CHS localization module.
 * Uses a dedicated SharedPreferences file ("chs_prefs") isolated from GotoBrowser core prefs.
 */
object ChsConfig {
    const val PREFS_NAME = "chs_prefs"

    const val KEY_ENABLED = "pref_chs_enabled"
    const val KEY_DATA_SOURCE = "pref_chs_data_source"
    const val KEY_CUSTOM_SOURCE_URL = "pref_chs_custom_source_url"
    const val KEY_LOCAL_VERSION = "pref_chs_local_version"

    const val SOURCE_GITHUB_RAW = "github_raw"
    const val SOURCE_JSDELIVR = "jsdelivr"
    const val SOURCE_GHPROXY = "ghproxy"
    const val SOURCE_CUSTOM = "custom"

    const val OFFICIAL_GITHUB_URL = "https://github.com/asaico/Kancolle_to_CHS"

    const val URL_GITHUB_RAW = "https://raw.githubusercontent.com/asaico/Kancolle_to_CHS/main/"
    const val URL_JSDELIVR = "https://testingcf.jsdelivr.net/gh/asaico/Kancolle_to_CHS@main/"
    const val URL_GHPROXY = "https://mirror.ghproxy.com/https://raw.githubusercontent.com/asaico/Kancolle_to_CHS/main/"

    /**
     * Preference keys whose changes require a browser reload to take effect.
     */
    val REFRESH_KEYS: Set<String> = setOf(KEY_ENABLED)

    private lateinit var sharedPref: SharedPreferences

    @Volatile
    private var preparedEnabled: Boolean = false

    fun init(context: Context) {
        sharedPref = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun ensureInitialized(context: Context) {
        if (!::sharedPref.isInitialized) {
            init(context)
        }
    }

    fun isEnabled(): Boolean {
        return sharedPref.getBoolean(KEY_ENABLED, false)
    }

    /**
     * Fast-path state checked during script injection and WebView resource interception.
     */
    fun isPatcherEnabled(): Boolean = preparedEnabled

    /**
     * Alias for WebView asset override checks (text/API/asset patching toggle together).
     */
    fun isResPatchActive(): Boolean = preparedEnabled

    /**
     * Syncs runtime flags before page load without performing filesystem I/O on the main thread.
     */
    fun prepare(context: Context) {
        ensureInitialized(context)
        preparedEnabled = sharedPref.getBoolean(KEY_ENABLED, false)
    }

    /**
     * Updates the master enable switch. Cross-mod mutual exclusion is handled by ModPatchCoordinator.
     */
    fun setEnabled(enabled: Boolean) {
        sharedPref.edit().putBoolean(KEY_ENABLED, enabled).apply()
        preparedEnabled = enabled
    }

    fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        val defaultVal = if (key == KEY_ENABLED) false else defValue
        return sharedPref.getBoolean(key, defaultVal)
    }

    fun setBoolean(key: String, value: Boolean) {
        if (key == KEY_ENABLED) {
            setEnabled(value)
            return
        }
        sharedPref.edit().putBoolean(key, value).apply()
    }

    fun getString(key: String, defValue: String = ""): String {
        return sharedPref.getString(key, defValue) ?: defValue
    }

    fun setString(key: String, value: String) {
        sharedPref.edit().putString(key, value).apply()
    }

    /**
     * Resolves the active raw file base URL (always terminated with '/').
     */
    fun resolveRawBaseUrl(): String {
        return when (sharedPref.getString(KEY_DATA_SOURCE, SOURCE_GITHUB_RAW)) {
            SOURCE_JSDELIVR -> URL_JSDELIVR
            SOURCE_GHPROXY -> URL_GHPROXY
            SOURCE_CUSTOM -> {
                val custom = sharedPref.getString(KEY_CUSTOM_SOURCE_URL, "")?.trim().orEmpty()
                if (custom.isEmpty()) URL_GITHUB_RAW
                else if (custom.endsWith("/")) custom else "$custom/"
            }
            else -> URL_GITHUB_RAW
        }
    }

    fun getLocalVersion(): String {
        return sharedPref.getString(KEY_LOCAL_VERSION, "") ?: ""
    }

    fun setLocalVersion(version: String) {
        sharedPref.edit().putString(KEY_LOCAL_VERSION, version).apply()
    }
}
