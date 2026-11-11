package com.antest1.gotobrowser.chs

import android.content.Context
import android.content.SharedPreferences
import com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH
import com.antest1.gotobrowser.Helpers.KenPatcher
import com.antest1.gotobrowser.R

/**
 * 简中汉化 (PLUS) 独立配置管理
 * 使用独立的 SharedPreferences 文件 (chs_prefs)，并与 KCCP 英/印尼语补丁保持互斥。
 */
object ChsConfig {
    const val PREFS_NAME = "chs_prefs"

    const val KEY_ENABLED = "pref_chs_enabled"
    const val KEY_ENABLE_TEXT_DICT = "pref_chs_text_dict"
    const val KEY_ENABLE_TEXT_REGEX = "pref_chs_text_regex"
    const val KEY_ENABLE_RES_PATCH = "pref_chs_res_patch"

    // 8 个 KCSAPI 响应合流细粒度子开关
    const val KEY_API_QUEST = "pref_chs_api_quest"
    const val KEY_API_SHIP = "pref_chs_api_ship"
    const val KEY_API_MISSION = "pref_chs_api_mission"
    const val KEY_API_FURNITURE = "pref_chs_api_furniture"
    const val KEY_API_MAPINFO = "pref_chs_api_mapinfo"
    const val KEY_API_PICTUREBOOK = "pref_chs_api_picturebook"
    const val KEY_API_FLAVOR = "pref_chs_api_flavor"
    const val KEY_API_ITEM = "pref_chs_api_item"

    // 数据源与下载模式配置
    const val KEY_DATA_SOURCE = "pref_chs_data_source"
    const val KEY_CUSTOM_SOURCE_URL = "pref_chs_custom_source_url"
    const val KEY_DOWNLOAD_MODE = "pref_chs_download_mode"
    const val KEY_LOCAL_VERSION = "pref_chs_local_version"

    const val SOURCE_GITHUB_RAW = "github_raw"
    const val SOURCE_JSDELIVR = "jsdelivr"
    const val SOURCE_GHPROXY = "ghproxy"
    const val SOURCE_CUSTOM = "custom"

    const val DOWNLOAD_MODE_FULL = "full"
    const val DOWNLOAD_MODE_TEXT_ONLY = "text_only"

    const val OFFICIAL_REPO_OWNER = "asaico"
    const val OFFICIAL_REPO_NAME = "Kancolle_to_CHS"
    const val OFFICIAL_BRANCH = "main"
    const val OFFICIAL_GITHUB_URL = "https://github.com/asaico/Kancolle_to_CHS"

    const val URL_GITHUB_RAW = "https://raw.githubusercontent.com/asaico/Kancolle_to_CHS/main/"
    const val URL_JSDELIVR = "https://testingcf.jsdelivr.net/gh/asaico/Kancolle_to_CHS@main/"
    const val URL_GHPROXY = "https://mirror.ghproxy.com/https://raw.githubusercontent.com/asaico/Kancolle_to_CHS/main/"

    /**
     * 所有会触发页面重载才生效的汉化配置键集合
     */
    @JvmField
    val REFRESH_KEYS: Set<String> = setOf(
        KEY_ENABLED,
        KEY_ENABLE_TEXT_DICT,
        KEY_ENABLE_TEXT_REGEX,
        KEY_ENABLE_RES_PATCH,
        KEY_API_QUEST,
        KEY_API_SHIP,
        KEY_API_MISSION,
        KEY_API_FURNITURE,
        KEY_API_MAPINFO,
        KEY_API_PICTUREBOOK,
        KEY_API_FLAVOR,
        KEY_API_ITEM
    )

    data class ApiOptions(
        val enableApiQuest: Boolean = true,
        val enableApiShip: Boolean = true,
        val enableApiMission: Boolean = true,
        val enableApiFurniture: Boolean = true,
        val enableApiMapInfo: Boolean = true,
        val enableApiPictureBook: Boolean = true,
        val enableApiFlavor: Boolean = true,
        val enableApiItem: Boolean = true
    ) {
        val anyEnabled: Boolean
            get() = enableApiQuest || enableApiShip || enableApiMission ||
                enableApiFurniture || enableApiMapInfo || enableApiPictureBook ||
                enableApiFlavor || enableApiItem
    }

    @Volatile
    private var preparedEnabled: Boolean = false

    @Volatile
    private var preparedResPatchEnabled: Boolean = true

    @JvmStatic
    fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun isEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ENABLED, false)
    }

    /**
     * 热路径读取：是否在当前会话启用了简中汉化补丁
     */
    @JvmStatic
    fun isPatcherEnabled(): Boolean = preparedEnabled

    /**
     * 热路径读取：是否启用了贴图与字体替换
     */
    @JvmStatic
    fun isResPatchActive(): Boolean = preparedEnabled && preparedResPatchEnabled

    /**
     * 在浏览器启动或页面刷新前同步最新配置状态并构建资源索引
     */
    @JvmStatic
    fun prepare(context: Context) {
        val p = prefs(context)
        val enabled = p.getBoolean(KEY_ENABLED, false)
        preparedEnabled = enabled
        preparedResPatchEnabled = p.getBoolean(KEY_ENABLE_RES_PATCH, true)
        if (enabled) {
            enforceMutualExclusionWithKccp(context, true)
            ChsHackResolver.rebuildIndexIfNeeded(context)
        }
    }

    /**
     * 设置汉化总开关，并在开启时自动关闭 KCCP 英/印尼语补丁
     */
    @JvmStatic
    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        preparedEnabled = enabled
        if (enabled) {
            enforceMutualExclusionWithKccp(context, true)
        }
    }

    /**
     * 当开启简中汉化时自动关闭 KCCP；当开启 KCCP 时自动关闭简中汉化
     */
    @JvmStatic
    fun enforceMutualExclusionWithKccp(context: Context, chsJustEnabled: Boolean) {
        val mainPref = context.getSharedPreferences(
            context.getString(R.string.preference_key),
            Context.MODE_PRIVATE
        )
        if (chsJustEnabled) {
            if (mainPref.getBoolean(PREF_MOD_KCCP_LANG_PATCH, false)) {
                mainPref.edit().putBoolean(PREF_MOD_KCCP_LANG_PATCH, false).apply()
                KenPatcher().prepare(context)
            }
        } else {
            if (mainPref.getBoolean(PREF_MOD_KCCP_LANG_PATCH, false) && isEnabled(context)) {
                prefs(context).edit().putBoolean(KEY_ENABLED, false).apply()
                preparedEnabled = false
            }
        }
    }

    @JvmStatic
    fun getBoolean(context: Context, key: String, defValue: Boolean = true): Boolean {
        val defaultVal = if (key == KEY_ENABLED) false else defValue
        return prefs(context).getBoolean(key, defaultVal)
    }

    @JvmStatic
    fun setBoolean(context: Context, key: String, value: Boolean) {
        if (key == KEY_ENABLED) {
            setEnabled(context, value)
            return
        }
        prefs(context).edit().putBoolean(key, value).apply()
        if (key == KEY_ENABLE_RES_PATCH) {
            preparedResPatchEnabled = value
        }
    }

    @JvmStatic
    fun getString(context: Context, key: String, defValue: String = ""): String {
        return prefs(context).getString(key, defValue) ?: defValue
    }

    @JvmStatic
    fun setString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    @JvmStatic
    fun getApiOptions(context: Context): ApiOptions {
        val p = prefs(context)
        return ApiOptions(
            enableApiQuest = p.getBoolean(KEY_API_QUEST, true),
            enableApiShip = p.getBoolean(KEY_API_SHIP, true),
            enableApiMission = p.getBoolean(KEY_API_MISSION, true),
            enableApiFurniture = p.getBoolean(KEY_API_FURNITURE, true),
            enableApiMapInfo = p.getBoolean(KEY_API_MAPINFO, true),
            enableApiPictureBook = p.getBoolean(KEY_API_PICTUREBOOK, true),
            enableApiFlavor = p.getBoolean(KEY_API_FLAVOR, true),
            enableApiItem = p.getBoolean(KEY_API_ITEM, true)
        )
    }

    /**
     * 解析当前生效的远程仓库文件下载根地址（末尾固定带 '/'）
     */
    @JvmStatic
    fun resolveRawBaseUrl(context: Context): String {
        val p = prefs(context)
        return when (p.getString(KEY_DATA_SOURCE, SOURCE_GITHUB_RAW)) {
            SOURCE_JSDELIVR -> URL_JSDELIVR
            SOURCE_GHPROXY -> URL_GHPROXY
            SOURCE_CUSTOM -> {
                val custom = p.getString(KEY_CUSTOM_SOURCE_URL, "")?.trim().orEmpty()
                if (custom.isEmpty()) URL_GITHUB_RAW
                else if (custom.endsWith("/")) custom else "$custom/"
            }
            else -> URL_GITHUB_RAW
        }
    }

    @JvmStatic
    fun isTextOnlyDownloadMode(context: Context): Boolean {
        return prefs(context).getString(KEY_DOWNLOAD_MODE, DOWNLOAD_MODE_FULL) == DOWNLOAD_MODE_TEXT_ONLY
    }

    @JvmStatic
    fun getLocalVersion(context: Context): String {
        return prefs(context).getString(KEY_LOCAL_VERSION, "") ?: ""
    }

    @JvmStatic
    fun setLocalVersion(context: Context, version: String) {
        prefs(context).edit().putString(KEY_LOCAL_VERSION, version).apply()
    }
}
