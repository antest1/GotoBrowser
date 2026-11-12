package com.antest1.gotobrowser.Helpers.chs

import android.content.Context
import android.content.SharedPreferences

/**
 * 简中汉化独立配置管理
 * 持有独立的 SharedPreferences 实例 (chs_prefs)，仅负责自身模块的配置读写与运行态准备。
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

    private lateinit var sharedPref: SharedPreferences

    @Volatile
    private var preparedEnabled: Boolean = false

    @Volatile
    private var preparedResPatchEnabled: Boolean = true

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
     * 热路径读取：是否在当前会话启用了简中汉化补丁
     */
    fun isPatcherEnabled(): Boolean = preparedEnabled

    /**
     * 热路径读取：是否启用了贴图与字体替换
     */
    fun isResPatchActive(): Boolean = preparedEnabled && preparedResPatchEnabled

    /**
     * 在浏览器启动或页面刷新前同步最新配置状态并构建资源索引
     */
    fun prepare(context: Context) {
        ensureInitialized(context)
        val enabled = sharedPref.getBoolean(KEY_ENABLED, false)
        preparedEnabled = enabled
        preparedResPatchEnabled = sharedPref.getBoolean(KEY_ENABLE_RES_PATCH, true)
        if (enabled) {
            ChsHackResolver.rebuildIndexIfNeeded(context)
        }
    }

    /**
     * 设置简中汉化总开关（仅操作自身配置；跨 Mod 互斥由 ModPatchCoordinator 协调）
     */
    fun setEnabled(enabled: Boolean) {
        sharedPref.edit().putBoolean(KEY_ENABLED, enabled).apply()
        preparedEnabled = enabled
    }

    fun getBoolean(key: String, defValue: Boolean = true): Boolean {
        val defaultVal = if (key == KEY_ENABLED) false else defValue
        return sharedPref.getBoolean(key, defaultVal)
    }

    fun setBoolean(key: String, value: Boolean) {
        if (key == KEY_ENABLED) {
            setEnabled(value)
            return
        }
        sharedPref.edit().putBoolean(key, value).apply()
        if (key == KEY_ENABLE_RES_PATCH) {
            preparedResPatchEnabled = value
        }
    }

    fun getString(key: String, defValue: String = ""): String {
        return sharedPref.getString(key, defValue) ?: defValue
    }

    fun setString(key: String, value: String) {
        sharedPref.edit().putString(key, value).apply()
    }

    fun getApiOptions(): ApiOptions {
        return ApiOptions(
            enableApiQuest = sharedPref.getBoolean(KEY_API_QUEST, true),
            enableApiShip = sharedPref.getBoolean(KEY_API_SHIP, true),
            enableApiMission = sharedPref.getBoolean(KEY_API_MISSION, true),
            enableApiFurniture = sharedPref.getBoolean(KEY_API_FURNITURE, true),
            enableApiMapInfo = sharedPref.getBoolean(KEY_API_MAPINFO, true),
            enableApiPictureBook = sharedPref.getBoolean(KEY_API_PICTUREBOOK, true),
            enableApiFlavor = sharedPref.getBoolean(KEY_API_FLAVOR, true),
            enableApiItem = sharedPref.getBoolean(KEY_API_ITEM, true)
        )
    }

    /**
     * 解析当前生效的远程仓库文件下载根地址（末尾固定带 '/'）
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

    fun isTextOnlyDownloadMode(): Boolean {
        return sharedPref.getString(KEY_DOWNLOAD_MODE, DOWNLOAD_MODE_FULL) == DOWNLOAD_MODE_TEXT_ONLY
    }

    fun getLocalVersion(): String {
        return sharedPref.getString(KEY_LOCAL_VERSION, "") ?: ""
    }

    fun setLocalVersion(version: String) {
        sharedPref.edit().putString(KEY_LOCAL_VERSION, version).apply()
    }
}
