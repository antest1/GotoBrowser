package com.antest1.gotobrowser.chs

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.antest1.gotobrowser.Activity.PatchDownloadState
import com.antest1.gotobrowser.Activity.SettingsStatusHost
import com.antest1.gotobrowser.Helpers.KcUtils
import com.antest1.gotobrowser.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 简中汉化包 (PLUS) 增量检查、并发下载、SHA-256 校验与清理工具
 * 支持从 manifest.json 或 GitHub Trees API 自动获取远端清单，并支持多镜像源切换。
 */
class ChsDownloader(
    private val httpClient: OkHttpClient = KcUtils.getSharedHttpClient().newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GOTO-CHS"
        private const val GITHUB_TREES_API =
            "https://api.github.com/repos/asaico/Kancolle_to_CHS/git/trees/main?recursive=1"
        private const val MAX_MANIFEST_BYTES = 16L * 1024 * 1024 // 16 MB 清单响应大小上限
        private const val MAX_SINGLE_FILE_BYTES = 32L * 1024 * 1024 // 32 MB 单文件下载大小上限
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val workerExecutor = Executors.newSingleThreadExecutor()

    /**
     * 在宿主 ViewModel 销毁时关闭后台工作线程
     */
    fun shutdown() {
        workerExecutor.shutdownNow()
    }

    @Volatile
    private var lastDiffResult: ChsManifest.DiffResult? = null

    /**
     * 检查汉化包远端更新并在主线程更新设置页状态
     */
    fun checkUpdate(context: Context, onStatusResult: (summary: String, canDownload: Boolean) -> Unit) {
        val appContext = context.applicationContext
        ChsConfig.init(appContext)
        if (!ChsConfig.isEnabled()) {
            mainHandler.post {
                onStatusResult(appContext.getString(R.string.chs_status_disabled), false)
            }
            return
        }

        mainHandler.post {
            onStatusResult(appContext.getString(R.string.patch_download_checking), false)
        }

        workerExecutor.execute {
            try {
                val remote = fetchRemoteManifest()
                if (remote == null || remote.files.isEmpty()) {
                    val hasLocal = ChsPaths.hasInstalledData(appContext)
                    val localVer = ChsConfig.getLocalVersion()
                    val fallbackMsg = if (hasLocal) {
                        appContext.getString(R.string.chs_status_offline_installed, localVer.ifEmpty { "local" })
                    } else {
                        appContext.getString(R.string.chs_status_check_failed)
                    }
                    mainHandler.post { onStatusResult(fallbackMsg, true) }
                    return@execute
                }

                val rootDir = ChsPaths.getRootDir(appContext)
                val localMap = ChsManifest.loadLocalManifestMap(ChsPaths.getLocalManifestFile(appContext))
                val textOnly = ChsConfig.isTextOnlyDownloadMode()
                val diff = ChsManifest.computeDiff(remote, rootDir, localMap, textOnly)
                lastDiffResult = diff

                val summary = if (!ChsPaths.hasInstalledData(appContext)) {
                    appContext.getString(
                        R.string.chs_status_not_installed_format,
                        remote.version,
                        diff.toDownload.size,
                        formatSize(diff.totalDownloadBytes)
                    )
                } else if (diff.hasUpdates) {
                    appContext.getString(
                        R.string.chs_status_update_available_format,
                        remote.version,
                        diff.toDownload.size,
                        formatSize(diff.totalDownloadBytes)
                    )
                } else {
                    appContext.getString(R.string.chs_status_up_to_date_format, remote.version)
                }

                mainHandler.post {
                    onStatusResult(summary, true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "检查简中汉化更新失败: ${e.message}")
                mainHandler.post {
                    onStatusResult(appContext.getString(R.string.chs_status_check_failed), true)
                }
            }
        }
    }

    /**
     * 执行增量下载更新，复用 GotoBrowser 的 PatchDownloadState 展示 Material 3 进度弹窗
     */
    fun startDownload(
        host: SettingsStatusHost,
        onCompleteSummary: (summary: String, canDownload: Boolean) -> Unit
    ) {
        val appContext = host.getContext()?.applicationContext ?: return
        mainHandler.post {
            host.setPatchDownloadState(
                PatchDownloadState.Running(
                    appContext.getString(R.string.patch_download_checking),
                    null
                )
            )
        }

        workerExecutor.execute {
            try {
                ChsConfig.init(appContext)
                val rootDir = ChsPaths.getRootDir(appContext)
                val manifestFile = ChsPaths.getLocalManifestFile(appContext)
                val localMap = ChsManifest.loadLocalManifestMap(manifestFile)
                val textOnly = ChsConfig.isTextOnlyDownloadMode()

                val remote = fetchRemoteManifest()
                    ?: throw IOException(appContext.getString(R.string.chs_status_check_failed))
                val diff = ChsManifest.computeDiff(remote, rootDir, localMap, textOnly)

                if (diff.toDownload.isEmpty()) {
                    ChsConfig.setLocalVersion(remote.version)
                    ChsManifest.saveLocalManifestMap(manifestFile, remote.version, localMap)
                    ChsDataRepository.invalidateCache()
                    ChsHackResolver.invalidateIndex()
                    ChsConfig.prepare(appContext)

                    mainHandler.post {
                        host.setPatchDownloadState(PatchDownloadState.Idle)
                        host.showPatchMessage(appContext.getString(R.string.patch_download_updated))
                        onCompleteSummary(
                            appContext.getString(R.string.chs_status_up_to_date_format, remote.version),
                            true
                        )
                    }
                    return@execute
                }

                val baseUrl = ChsConfig.resolveRawBaseUrl()
                downloadEntriesIncrementally(
                    baseUrl = baseUrl,
                    entries = diff.toDownload,
                    rootDir = rootDir,
                    localMap = localMap,
                    manifestFile = manifestFile,
                    remoteVersion = remote.version,
                    onProgress = { current, total, fileName, downloadedBytes ->
                        val fraction = if (total > 0) current.toFloat() / total.toFloat() else null
                        val msg = String.format(
                            Locale.US,
                            "%s (%d/%d)\n%s",
                            fileName,
                            current,
                            total,
                            formatSize(downloadedBytes)
                        )
                        mainHandler.post {
                            host.setPatchDownloadState(PatchDownloadState.Running(msg, fraction))
                        }
                    }
                )

                ChsConfig.setLocalVersion(remote.version)
                ChsManifest.saveLocalManifestMap(manifestFile, remote.version, localMap)
                ChsDataRepository.invalidateCache()
                ChsHackResolver.invalidateIndex()
                ChsConfig.prepare(appContext)

                mainHandler.post {
                    host.setPatchDownloadState(PatchDownloadState.Idle)
                    host.showPatchMessage(appContext.getString(R.string.patch_download_updated))
                    onCompleteSummary(
                        appContext.getString(R.string.chs_status_up_to_date_format, remote.version),
                        true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载简中汉化包异常: ${e.message}")
                mainHandler.post {
                    host.setPatchDownloadState(PatchDownloadState.Idle)
                    host.showPatchMessage(
                        appContext.getString(R.string.patch_download_error) + ": " + (e.message ?: "")
                    )
                    onCompleteSummary(appContext.getString(R.string.patch_download_failed), true)
                }
            }
        }
    }

    /**
     * 供单元测试与正式环境共用的增量下载执行器（支持断点持久化与 SHA-256 原子替换）
     */
    fun downloadEntriesIncrementally(
        baseUrl: String,
        entries: List<ChsManifest.FileEntry>,
        rootDir: File,
        localMap: MutableMap<String, String>,
        manifestFile: File,
        remoteVersion: String,
        onProgress: (completed: Int, total: Int, currentFile: String, totalBytes: Long) -> Unit = { _, _, _, _ -> }
    ) {
        val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val total = entries.size
        val completedCount = AtomicInteger(0)
        val totalDownloadedBytes = AtomicLong(0L)

        // 使用 4 线程并发加速大量小文件下载
        val pool = Executors.newFixedThreadPool(4)
        val firstError = java.util.concurrent.atomic.AtomicReference<Exception?>(null)

        try {
            val futures = entries.map { entry ->
                pool.submit {
                    if (firstError.get() != null) return@submit
                    val targetFile = File(rootDir, entry.path)
                    val tempFile = File(rootDir, "${entry.path}.part")
                    try {
                        tempFile.parentFile?.mkdirs()

                        val fileUrl = normalizedBase + entry.path
                        downloadSingleFile(fileUrl, tempFile)

                        if (entry.hashType == "sha256" && entry.hash.length == 64) {
                            val actualSha256 = ChsManifest.sha256Hex(tempFile)
                            if (!actualSha256.equals(entry.hash, ignoreCase = true)) {
                                tempFile.delete()
                                throw IOException("SHA-256 校验不匹配: ${entry.path}")
                            }
                        }

                        targetFile.parentFile?.mkdirs()
                        if (targetFile.exists() && !targetFile.delete()) {
                            throw IOException("无法替换目标文件: ${entry.path}")
                        }
                        if (!tempFile.renameTo(targetFile)) {
                            tempFile.copyTo(targetFile, overwrite = true)
                            tempFile.delete()
                        }

                        val bytes = totalDownloadedBytes.addAndGet(targetFile.length())
                        val done = completedCount.incrementAndGet()
                        synchronized(localMap) {
                            localMap[entry.path] = entry.hash
                            if (done % 25 == 0 || done == total) {
                                ChsManifest.saveLocalManifestMap(manifestFile, remoteVersion, localMap)
                            }
                        }
                        onProgress(done, total, entry.path.substringAfterLast('/'), bytes)
                    } catch (e: Exception) {
                        try {
                            if (tempFile.exists()) tempFile.delete()
                        } catch (_: Exception) {
                        }
                        firstError.compareAndSet(null, e)
                    }
                }
            }
            for (f in futures) {
                f.get()
            }
        } finally {
            pool.shutdownNow()
        }

        synchronized(localMap) {
            ChsManifest.saveLocalManifestMap(manifestFile, remoteVersion, localMap)
        }

        firstError.get()?.let { throw it }
    }

    /**
     * 删除本地已下载的简中汉化数据
     */
    fun deletePatchData(
        host: SettingsStatusHost,
        onCompleteSummary: (summary: String, canDownload: Boolean) -> Unit
    ) {
        val appContext = host.getContext()?.applicationContext ?: return
        workerExecutor.execute {
            ChsConfig.init(appContext)
            val rootDir = ChsPaths.getRootDir(appContext)
            if (rootDir.exists()) {
                rootDir.deleteRecursively()
            }
            ChsConfig.setLocalVersion("")
            ChsDataRepository.invalidateCache()
            ChsHackResolver.invalidateIndex()

            mainHandler.post {
                host.showPatchMessage(appContext.getString(R.string.patch_deleted))
                checkUpdate(appContext, onCompleteSummary)
            }
        }
    }

    /**
     * 优先获取远端 manifest.json；若返回 404 或不可用，自动回退到 GitHub Trees API
     */
    fun fetchRemoteManifest(): ChsManifest.ManifestData? {
        val baseUrl = ChsConfig.resolveRawBaseUrl()
        val manifestUrl = "${baseUrl}manifest.json"
        try {
            val raw = fetchUrlString(manifestUrl)
            if (!raw.isNullOrBlank()) {
                ChsManifest.parseManifestJson(raw)?.takeIf { it.files.isNotEmpty() }?.let {
                    return it
                }
            }
        } catch (_: Exception) {
        }

        // 回退到 GitHub Trees API (?recursive=1)
        return try {
            val treeRaw = fetchUrlString(GITHUB_TREES_API)
            ChsManifest.parseGithubTreeJson(treeRaw)?.takeIf { it.files.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "回退获取 GitHub Trees API 失败: ${e.message}")
            null
        }
    }

    private fun fetchUrlString(url: String): String? {
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body ?: return null
            val contentLen = body.contentLength()
            if (contentLen > MAX_MANIFEST_BYTES) {
                throw IOException("Manifest exceeds maximum allowed size ($contentLen bytes): $url")
            }
            body.byteStream().use { input ->
                val out = java.io.ByteArrayOutputStream(
                    if (contentLen in 1..65536) contentLen.toInt() else 32768
                )
                val buffer = ByteArray(16384)
                var totalRead = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    totalRead += read
                    if (totalRead > MAX_MANIFEST_BYTES) {
                        throw IOException("Manifest stream exceeds maximum allowed size: $url")
                    }
                    out.write(buffer, 0, read)
                }
                return out.toString(Charsets.UTF_8.name())
            }
        }
    }

    private fun downloadSingleFile(url: String, destFile: File) {
        val req = Request.Builder().url(url).build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: $url")
            }
            val body = resp.body ?: throw IOException("Empty body: $url")
            val contentLen = body.contentLength()
            if (contentLen > MAX_SINGLE_FILE_BYTES) {
                throw IOException("File exceeds maximum allowed size ($contentLen bytes): $url")
            }
            FileOutputStream(destFile).use { fos ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(16384)
                    var totalRead = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        totalRead += read
                        if (totalRead > MAX_SINGLE_FILE_BYTES) {
                            throw IOException("File stream exceeds maximum allowed size: $url")
                        }
                        fos.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        return if (kb < 1024.0) {
            String.format(Locale.US, "%.1f KB", kb)
        } else {
            String.format(Locale.US, "%.2f MB", kb / 1024.0)
        }
    }
}
