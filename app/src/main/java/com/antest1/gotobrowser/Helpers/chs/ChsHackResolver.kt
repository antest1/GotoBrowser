package com.antest1.gotobrowser.Helpers.chs

import android.content.Context
import android.util.Log
import android.webkit.WebResourceResponse
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

/**
 * Resolves .hack image and font overrides from the isolated CHS cache directory.
 *
 * Maintains an in-memory request-path index built lazily on WebView interception threads
 * so original GotoBrowser cache files are never modified.
 */
object ChsHackResolver {
    private const val TAG = "GOTO-CHS"
    private val HACK_SUFFIX_REGEX = Regex("""\.hack(\.[^./\\]+)$""", RegexOption.IGNORE_CASE)

    private val indexLock = Any()

    @Volatile
    private var pathIndex: Map<String, File> = emptyMap()

    @Volatile
    private var fontNameIndex: Map<String, File> = emptyMap()

    @Volatile
    private var indexedCacheRoot: String = ""

    @Volatile
    private var indexBuilt: Boolean = false

    fun invalidateIndex() {
        synchronized(indexLock) {
            indexBuilt = false
            indexedCacheRoot = ""
            pathIndex = emptyMap()
            fontNameIndex = emptyMap()
        }
    }

    /**
     * Rebuilds the lookup maps from [cacheDir] synchronously (also invoked directly by tests).
     */
    fun rebuildIndexFromDir(cacheDir: File) {
        synchronized(indexLock) {
            if (!cacheDir.isDirectory) {
                pathIndex = emptyMap()
                fontNameIndex = emptyMap()
                indexedCacheRoot = cacheDir.absolutePath
                indexBuilt = true
                return
            }

            if (indexBuilt && cacheDir.absolutePath == indexedCacheRoot) {
                return
            }

            val newPathMap = HashMap<String, File>(3096)
            val newFontMap = HashMap<String, File>(8)

            val basePath = cacheDir.canonicalFile.path
            cacheDir.walkTopDown().forEach { file ->
                if (!file.isFile || file.length() <= 0L) return@forEach
                val relPath = file.canonicalFile.path
                    .removePrefix(basePath)
                    .replace('\\', '/')
                    .let { if (it.startsWith("/")) it else "/$it" }

                val normalizedPath = normalizeHackPath(relPath)
                newPathMap[normalizedPath] = file
                // Also index the raw .hack path as an exact-match fallback.
                newPathMap[relPath] = file

                if (normalizedPath.endsWith(".woff2", ignoreCase = true)) {
                    val fileName = normalizedPath.substringAfterLast('/')
                    newFontMap[fileName] = file
                }
            }

            pathIndex = newPathMap
            fontNameIndex = newFontMap
            indexedCacheRoot = cacheDir.absolutePath
            indexBuilt = true
            Log.i(TAG, "Indexed CHS asset overrides: ${newPathMap.size / 2} files")
        }
    }

    /**
     * Strips the ".hack" marker and query string, e.g.
     * `/kcs2/img/port/port_main.hack.png?v=1` -> `/kcs2/img/port/port_main.png`.
     */
    fun normalizeHackPath(path: String): String {
        val clean = path.substringBefore('?').replace('\\', '/')
        val withSlash = if (clean.startsWith("/")) clean else "/$clean"
        return HACK_SUFFIX_REGEX.replace(withSlash, "$1")
    }

    /**
     * Looks up a matching local override file using lock-free snapshot maps.
     */
    fun findHackFile(requestPath: String?): File? {
        if (requestPath.isNullOrEmpty()) return null
        val clean = normalizeHackPath(requestPath)
        val currentPaths = pathIndex
        currentPaths[clean]?.takeIf { it.isFile && it.canRead() }?.let { return it }

        if (clean.endsWith("/html/maintenance.png")) {
            currentPaths["/kcs/html/maintenance.png"]?.takeIf { it.isFile && it.canRead() }?.let { return it }
        }
        if (clean.endsWith(".woff2", ignoreCase = true)) {
            val fileName = clean.substringAfterLast('/')
            fontNameIndex[fileName]?.takeIf { it.isFile && it.canRead() }?.let { return it }
        }
        return null
    }

    /**
     * Lazily indexes on the WebView interception thread and serves matching CHS asset overrides.
     */
    @JvmStatic
    fun resolveWebResponse(context: Context, requestPath: String?): WebResourceResponse? {
        if (!ChsConfig.isPatcherEnabled()) return null
        if (!indexBuilt) {
            rebuildIndexFromDir(ChsPaths.getCacheDir(context))
        }
        val file = findHackFile(requestPath) ?: return null
        return try {
            val mimeType = guessMimeType(file.name)
            val encoding = if (mimeType.startsWith("application/json")) "utf-8" else "binary"
            WebResourceResponse(mimeType, encoding, BufferedInputStream(FileInputStream(file)))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read CHS override asset ($requestPath): ${e.message}")
            null
        }
    }

    private fun guessMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".woff2") -> "application/font-woff2"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".css") -> "text/css"
            else -> "application/octet-stream"
        }
    }
}
