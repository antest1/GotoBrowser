package com.antest1.gotobrowser.Helpers.chs

import android.content.Context
import java.io.File

/**
 * 简中汉化 (PLUS) 本地数据存储路径管理
 * 与 GotoBrowser 原有缓存和 KCCP 补丁完全隔离，存放在 externalFilesDir/chs_patch 下。
 */
object ChsPaths {
    const val ROOT_FOLDER_NAME = "chs_patch"
    const val LOCAL_MANIFEST_FILE = "manifest.local.json"

    fun getRootDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, ROOT_FOLDER_NAME)
    }

    fun getTextDictDir(context: Context): File = File(getRootDir(context), "text/text")

    fun getTextRegexDir(context: Context): File = File(getRootDir(context), "text/text_regex")

    fun getApiDir(context: Context): File = File(getRootDir(context), "text/api")

    fun getCacheDir(context: Context): File = File(getRootDir(context), "cache")

    fun getLocalManifestFile(context: Context): File = File(getRootDir(context), LOCAL_MANIFEST_FILE)

    /**
     * 检查是否至少已下载核心文本或 API 字典数据
     */
    fun hasInstalledData(context: Context): Boolean {
        val apiDir = getApiDir(context)
        val textDir = getTextDictDir(context)
        val hasApi = apiDir.isDirectory && (apiDir.listFiles()?.any { it.name.endsWith(".json") } == true)
        val hasText = textDir.isDirectory && (textDir.listFiles()?.any { it.name.endsWith(".json") } == true)
        return hasApi || hasText
    }
}
