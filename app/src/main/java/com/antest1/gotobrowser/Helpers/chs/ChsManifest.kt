package com.antest1.gotobrowser.Helpers.chs

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.security.MessageDigest

/**
 * Remote/local manifest model and incremental diff calculation for Kancolle_to_CHS.
 * Supports both root manifest.json (SHA-256) and GitHub Git Trees API fallback (git blob SHA-1).
 */
object ChsManifest {
    private val gson = Gson()

    data class FileEntry(
        val path: String,
        val size: Long,
        val hash: String, // 64-char sha256 or 40-char git blob sha-1
        val hashType: String = "sha256" // "sha256" or "git_sha"
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
        val toDelete: List<String> = emptyList(),
        val upToDateCount: Int,
        val totalDownloadBytes: Long
    ) {
        val hasUpdates: Boolean
            get() = toDownload.isNotEmpty() || toDelete.isNotEmpty()
    }

    /**
     * Parses repository manifest.json payload.
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
     * Parses GitHub Git Trees API (?recursive=1) response as fallback when manifest.json is unavailable.
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
        if (path.isEmpty() || path.contains("..") || path.contains('\\') || path.contains('\u0000')) {
            return false
        }
        // GotoBrowser uses its own subtitle system; skip subtitle sub-directory.
        if (path.startsWith("text/text/subtitles/")) return false
        return path.startsWith("text/") || path.startsWith("cache/")
    }

    /**
     * Resolves [relPath] safely inside [rootDir], returning null if path traversal is attempted.
     */
    fun safeResolveUnderRoot(rootDir: File, relPath: String): File? {
        if (!isSupportedRepoPath(relPath)) return null
        return try {
            val canonicalRoot = rootDir.canonicalPath
            val prefix = if (canonicalRoot.endsWith(File.separator)) canonicalRoot else canonicalRoot + File.separator
            val candidate = File(rootDir, relPath)
            if (candidate.canonicalPath.startsWith(prefix)) candidate else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Computes incremental diff between [remote] manifest and local directory / recorded hashes.
     */
    fun computeDiff(
        remote: ManifestData,
        localRootDir: File,
        localManifest: Map<String, String>
    ): DiffResult {
        val candidates = remote.files.filter { it.isTextFile || it.isCacheFile }

        val toDownload = ArrayList<FileEntry>()
        var upToDate = 0
        var totalBytes = 0L

        for (entry in candidates) {
            val localFile = File(localRootDir, entry.path)
            val recordedHash = localManifest[entry.path]
            val isFileValid = localFile.isFile && localFile.length() > 0L &&
                (entry.size <= 0L || localFile.length() == entry.size)
            val isSameHash = when {
                !isFileValid -> false
                recordedHash != null && recordedHash.equals(entry.hash, ignoreCase = true) -> true
                // Handle migration between 40-char git_sha and 64-char sha256 without re-downloading valid files.
                recordedHash != null && recordedHash.length != entry.hash.length ->
                    verifyFileHash(localFile, entry.hash, entry.hashType)
                else -> false
            }

            if (isSameHash) {
                upToDate++
            } else {
                toDownload.add(entry)
                if (entry.size > 0L) {
                    totalBytes += entry.size
                }
            }
        }

        val remotePathSet = candidates.map { it.path }.toSet()
        val toDelete = ArrayList<String>()
        for (localPath in localManifest.keys) {
            if (isSupportedRepoPath(localPath) && !remotePathSet.contains(localPath)) {
                toDelete.add(localPath)
            }
        }

        return DiffResult(
            remoteVersion = remote.version,
            toDownload = toDownload,
            toDelete = toDelete,
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

    fun sha256Hex(file: File): String = digestFileHex(file, "SHA-256")

    fun gitBlobSha1Hex(file: File): String {
        val header = "blob ${file.length()}\u0000".toByteArray(Charsets.UTF_8)
        return digestFileHex(file, "SHA-1", header)
    }

    /**
     * Verifies [file] against [expectedHash] using [hashType] ("sha256" or "git_sha").
     */
    fun verifyFileHash(file: File, expectedHash: String, hashType: String): Boolean {
        if (!file.isFile || expectedHash.isBlank()) return false
        val actual = when (hashType.lowercase()) {
            "sha256" -> if (expectedHash.length == 64) sha256Hex(file) else return false
            "git_sha" -> if (expectedHash.length == 40) gitBlobSha1Hex(file) else return false
            else -> return false
        }
        return actual.equals(expectedHash, ignoreCase = true)
    }

    private fun digestFileHex(file: File, algorithm: String, header: ByteArray? = null): String {
        val digest = MessageDigest.getInstance(algorithm)
        if (header != null) {
            digest.update(header)
        }
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
