package com.antest1.gotobrowser.chs

import android.content.Context
import android.util.Log
import android.webkit.WebResourceResponse
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * 简中汉化 (PLUS) .hack 贴图与中文字体资源映射器
 * 在内存中维护请求路径 -> 本地 hack 文件的 HashMap 索引，命中时直接返回 WebResourceResponse，
 * 不写入 GotoBrowser 原生缓存目录，确保关闭汉化后立即恢复原版资源且无残留。
 */
object ChsHackResolver {
    private const val TAG = "GOTO-CHS"
    private val HACK_SUFFIX_REGEX = Regex("""\.hack(\.[^./\\]+)$""", RegexOption.IGNORE_CASE)

    private val pathIndex = ConcurrentHashMap<String, File>()
    private val fontNameIndex = ConcurrentHashMap<String, File>()

    @Volatile
    private var indexedCacheRoot: String = ""

    @Volatile
    private var indexedLastModified: Long = -1L

    @JvmStatic
    fun invalidateIndex() {
        indexedCacheRoot = ""
        indexedLastModified = -1L
        pathIndex.clear()
        fontNameIndex.clear()
    }

    @JvmStatic
    fun rebuildIndexIfNeeded(context: Context) {
        val cacheDir = ChsPaths.getCacheDir(context)
        rebuildIndexFromDir(cacheDir)
    }

    /**
     * 从指定 cache 目录构建请求路径映射表（可供单元测试直接调用）
     */
    @JvmStatic
    fun rebuildIndexFromDir(cacheDir: File) {
        if (!cacheDir.isDirectory) {
            pathIndex.clear()
            fontNameIndex.clear()
            indexedCacheRoot = cacheDir.absolutePath
            indexedLastModified = 0L
            return
        }

        val dirStamp = computeDirTreeStamp(cacheDir)
        if (cacheDir.absolutePath == indexedCacheRoot && dirStamp == indexedLastModified && pathIndex.isNotEmpty()) {
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
            // 同时支持原始带 .hack 的路径
            newPathMap[relPath] = file

            if (normalizedPath.endsWith(".woff2", ignoreCase = true)) {
                val fileName = normalizedPath.substringAfterLast('/')
                newFontMap[fileName] = file
            }
        }

        pathIndex.clear()
        pathIndex.putAll(newPathMap)
        fontNameIndex.clear()
        fontNameIndex.putAll(newFontMap)

        indexedCacheRoot = cacheDir.absolutePath
        indexedLastModified = dirStamp
        try {
            Log.i(TAG, "已构建简中贴图/字体索引: ${newPathMap.size / 2} 个文件")
        } catch (_: Throwable) {
        }
    }

    /**
     * 汇总 cache 目录及其全部子目录的修改时间戳，以便检测任意子目录下的文件增删
     */
    private fun computeDirTreeStamp(cacheDir: File): Long {
        var stamp = cacheDir.lastModified()
        cacheDir.walkTopDown().forEach { f ->
            if (f.isDirectory) {
                stamp = stamp * 31L + f.lastModified()
            }
        }
        return stamp
    }

    /**
     * 将 /kcs2/img/port/port_main.hack.png 规范化为 /kcs2/img/port/port_main.png
     */
    @JvmStatic
    fun normalizeHackPath(path: String): String {
        val clean = path.substringBefore('?').replace('\\', '/')
        val withSlash = if (clean.startsWith("/")) clean else "/$clean"
        return HACK_SUFFIX_REGEX.replace(withSlash, "$1")
    }

    /**
     * 根据请求路径查找对应的本地 hack 文件（仅在内存中 O(1) 查找）
     */
    @JvmStatic
    fun findHackFile(requestPath: String?): File? {
        if (requestPath.isNullOrEmpty()) return null
        val clean = normalizeHackPath(requestPath)
        pathIndex[clean]?.takeIf { it.isFile && it.canRead() }?.let { return it }

        // 兼容 maintenance.png 等短路径匹配
        if (clean.endsWith("/html/maintenance.png")) {
            pathIndex["/kcs/html/maintenance.png"]?.takeIf { it.isFile && it.canRead() }?.let { return it }
        }
        if (clean.endsWith(".woff2", ignoreCase = true)) {
            val fileName = clean.substringAfterLast('/')
            fontNameIndex[fileName]?.takeIf { it.isFile && it.canRead() }?.let { return it }
        }
        return null
    }

    /**
     * 若简中贴图/字体替换已启用且存在匹配的本地 hack 文件，则直接返回 WebResourceResponse；
     * 否则返回 null，交由 GotoBrowser 原生流程处理。
     */
    @JvmStatic
    fun resolveWebResponse(context: Context, requestPath: String?): WebResourceResponse? {
        if (!ChsConfig.isResPatchActive()) return null
        if (indexedCacheRoot.isEmpty()) {
            rebuildIndexIfNeeded(context)
        }
        val file = findHackFile(requestPath) ?: return null
        return try {
            val mimeType = guessMimeType(file.name)
            val encoding = if (mimeType.startsWith("application/json")) "utf-8" else "binary"
            WebResourceResponse(mimeType, encoding, BufferedInputStream(FileInputStream(file)))
        } catch (e: Exception) {
            Log.w(TAG, "读取 hack 资源失败 ($requestPath): ${e.message}")
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
