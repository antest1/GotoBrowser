package com.antest1.gotobrowser.Helpers.chs

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.security.MessageDigest

/**
 * 简中汉化包远端/本地清单模型与增量差分计算
 * 同时兼容仓库根部的 manifest.json (sha256) 与 GitHub Trees API (git blob sha)
 */
object ChsManifest {
    private val gson = Gson()

    data class FileEntry(
        val path: String,
        val size: Long,
        val hash: String, // sha256 或 git blob sha
        val hashType: String = "sha256" // "sha256" 或 "git_sha"
    ) {
        val isTextFile: Boolean
            get() = path.startsWith("text/")

        val isCacheFile: Boolean
            get() = path.startsWith("cache/")
    }

    data class ManifestData(
        val version: String,
        val files: List<FileEntry>
    )

    data class DiffResult(
        val remoteVersion: String,
        val toDownload: List<FileEntry>,
        val upToDateCount: Int,
        val totalDownloadBytes: Long
    ) {
        val hasUpdates: Boolean
            get() = toDownload.isNotEmpty()
    }

    /**
     * 解析 manifest.json 内容
     */
    fun parseManifestJson(rawJson: String?): ManifestData? {
        val el = ChsJsonCleaner.safeParseJson(rawJson) ?: return null
        if (!el.isJsonObject) return null
        val obj = el.asJsonObject

        val version = obj.optStr("version")
            .ifEmpty { obj.optStr("commit") }
            .ifEmpty { obj.optStr("sha") }
            .ifEmpty { "unknown" }

        val filesArr = obj.getAsJsonArray("files") ?: return null
        val entries = ArrayList<FileEntry>(filesArr.size())
        for (item in filesArr) {
            if (!item.isJsonObject) continue
            val fObj = item.asJsonObject
            val path = fObj.optStr("path").trim().removePrefix("/")
            if (!isSupportedRepoPath(path)) continue
            val size = fObj.optLong("size", 0L)
            val sha256 = fObj.optStr("sha256")
            val sha = fObj.optStr("sha").ifEmpty { fObj.optStr("hash") }
            val hash = sha256.ifEmpty { sha }
            val hashType = if (sha256.isNotEmpty()) "sha256" else "git_sha"
            if (hash.isNotEmpty()) {
                entries.add(FileEntry(path = path, size = size, hash = hash, hashType = hashType))
            }
        }
        return ManifestData(version = version, files = entries)
    }

    /**
     * 解析 GitHub Git Trees API (?recursive=1) 响应作为无缝回退
     */
    fun parseGithubTreeJson(rawJson: String?): ManifestData? {
        val el = ChsJsonCleaner.safeParseJson(rawJson) ?: return null
        if (!el.isJsonObject) return null
        val obj = el.asJsonObject

        val treeSha = obj.optStr("sha").take(10).ifEmpty { "main" }
        val treeArr = obj.getAsJsonArray("tree") ?: return null
        val entries = ArrayList<FileEntry>()
        for (item in treeArr) {
            if (!item.isJsonObject) continue
            val tObj = item.asJsonObject
            if (tObj.optStr("type") != "blob") continue
            val path = tObj.optStr("path").trim().removePrefix("/")
            if (!isSupportedRepoPath(path)) continue
            val size = tObj.optLong("size", 0L)
            val sha = tObj.optStr("sha")
            if (sha.isNotEmpty()) {
                entries.add(FileEntry(path = path, size = size, hash = sha, hashType = "git_sha"))
            }
        }
        return ManifestData(version = treeSha, files = entries)
    }

    fun isSupportedRepoPath(path: String): Boolean {
        if (path.contains("..")) return false
        if (path.startsWith("text/text/subtitles/")) return false // GotoBrowser 使用自带字幕，无需下载字幕子目录
        return path.startsWith("text/") || path.startsWith("cache/")
    }

    /**
     * 计算远端清单与本地目录/本地清单的增量差分
     */
    fun computeDiff(
        remote: ManifestData,
        localRootDir: File,
        localManifest: Map<String, String>,
        textOnly: Boolean
    ): DiffResult {
        val candidates = if (textOnly) {
            remote.files.filter { it.isTextFile }
        } else {
            remote.files.filter { it.isTextFile || it.isCacheFile }
        }

        val toDownload = ArrayList<FileEntry>()
        var upToDate = 0
        var totalBytes = 0L

        for (entry in candidates) {
            val localFile = File(localRootDir, entry.path)
            val recordedHash = localManifest[entry.path]
            val isSameHash = recordedHash != null && recordedHash.equals(entry.hash, ignoreCase = true)
            val isFileValid = localFile.isFile && localFile.length() > 0L &&
                (entry.size <= 0L || localFile.length() == entry.size)

            if (isSameHash && isFileValid) {
                upToDate++
            } else {
                toDownload.add(entry)
                if (entry.size > 0L) {
                    totalBytes += entry.size
                }
            }
        }

        return DiffResult(
            remoteVersion = remote.version,
            toDownload = toDownload,
            upToDateCount = upToDate,
            totalDownloadBytes = totalBytes
        )
    }

    fun loadLocalManifestMap(manifestFile: File): MutableMap<String, String> {
        val map = LinkedHashMap<String, String>()
        if (!manifestFile.isFile) return map
        try {
            val el = ChsJsonCleaner.safeParseJson(manifestFile.readText(Charsets.UTF_8))
            if (el != null && el.isJsonObject) {
                val filesObj = el.asJsonObject.getAsJsonObject("files") ?: el.asJsonObject
                for ((k, v) in filesObj.entrySet()) {
                    if (v != null && v.isJsonPrimitive) {
                        map[k] = v.asString
                    }
                }
            }
        } catch (_: Exception) {
        }
        return map
    }

    fun saveLocalManifestMap(manifestFile: File, version: String, filesMap: Map<String, String>) {
        val tempFile = File(manifestFile.parentFile, "${manifestFile.name}.tmp")
        try {
            manifestFile.parentFile?.mkdirs()
            val root = JsonObject().apply {
                addProperty("version", version)
                val fObj = JsonObject()
                for ((k, v) in filesMap) {
                    fObj.addProperty(k, v)
                }
                add("files", fObj)
            }
            tempFile.writeText(gson.toJson(root), Charsets.UTF_8)
            if (!tempFile.renameTo(manifestFile)) {
                tempFile.copyTo(manifestFile, overwrite = true)
                tempFile.delete()
            }
        } catch (_: Exception) {
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (_: Exception) {
            }
        }
    }

    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { fis ->
            val buf = ByteArray(16384)
            while (true) {
                val r = fis.read(buf)
                if (r <= 0) break
                digest.update(buf, 0, r)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun JsonObject.optStr(key: String): String {
        val el = this.get(key) ?: return ""
        return if (el.isJsonPrimitive) el.asString else ""
    }

    private fun JsonObject.optLong(key: String, def: Long): Long {
        val el = this.get(key) ?: return def
        return if (el.isJsonPrimitive) {
            try { el.asLong } catch (_: Exception) { def }
        } else def
    }
}
