package com.antest1.gotobrowser.Helpers.chs

import android.content.Context
import java.io.File

/**
 * Local storage layout for Kancolle_to_CHS under externalFilesDir/chs_patch,
 * isolated from GotoBrowser's native cache and KCCP data.
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
     * Returns true when core text dictionary or API translation JSON files exist locally.
     */
    fun hasInstalledData(context: Context): Boolean {
        val apiDir = getApiDir(context)
        val textDir = getTextDictDir(context)
        val hasApi = apiDir.isDirectory && (apiDir.listFiles()?.any { it.name.endsWith(".json") } == true)
        val hasText = textDir.isDirectory && (textDir.listFiles()?.any { it.name.endsWith(".json") } == true)
        return hasApi || hasText
    }
}
