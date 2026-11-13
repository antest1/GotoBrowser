package com.antest1.gotobrowser.Helpers.chs

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.antest1.gotobrowser.Activity.PatchDownloadState
import com.antest1.gotobrowser.Activity.SettingsStatusHost
import com.antest1.gotobrowser.R
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Handles update checks, concurrent incremental downloads, Release archive extraction,
 * cryptographic hash verification, and cleanup for Kancolle_to_CHS.
 */
class ChsDownloader(
    private val httpClient: OkHttpClient = createDefaultHttpClient()
) {
    companion object {
        private const val TAG = "GOTO-CHS"
        private const val GITHUB_TREES_API =
            "https://api.github.com/repos/asaico/Kancolle_to_CHS/git/trees/main?recursive=1"
        private const val MAX_MANIFEST_BYTES = 16L * 1024 * 1024
        private const val MAX_SINGLE_FILE_BYTES = 32L * 1024 * 1024
        private const val MAX_ARCHIVE_BYTES = 1024L * 1024 * 1024
        private const val GITHUB_RELEASES_LATEST =
            "https://github.com/asaico/Kancolle_to_CHS/releases/latest"
        private const val CONCURRENCY = 8
        private const val BUFFER_SIZE = 32768

        private fun createDefaultHttpClient(): OkHttpClient {
            val dispatcher = Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 16
            }
            val pool = ConnectionPool(16, 5, TimeUnit.MINUTES)
            return OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(pool)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    private val mainHandler: Handler by lazy {
        try {
            Handler(Looper.getMainLooper())
        } catch (_: Throwable) {
            Handler(Looper.myLooper() ?: Looper.getMainLooper())
        }
    }
    private val workerExecutor = Executors.newSingleThreadExecutor()

    private val isCancelled = AtomicBoolean(false)
    @Volatile
    private var activeDownloadPool: ExecutorService? = null
    @Volatile
    private var currentDownloadHost: SettingsStatusHost? = null
    @Volatile
    private var currentOnComplete: ((summary: String) -> Unit)? = null

    /** Shuts down background executors and cancels any active download. */
    fun shutdown() {
        cancelDownload()
        workerExecutor.shutdownNow()
    }

    /** Cancels an in-flight download or archive extraction. */
    fun cancelDownload() {
        if (!isCancelled.compareAndSet(false, true)) {
            return
        }
        try {
            httpClient.dispatcher.cancelAll()
        } catch (_: Exception) {}
        try {
            activeDownloadPool?.shutdownNow()
        } catch (_: Exception) {}

        val host = currentDownloadHost
        val onComplete = currentOnComplete
        val appContext = host?.getContext()?.applicationContext

        mainHandler.post {
            host?.setPatchDownloadState(PatchDownloadState.Idle)
            if (appContext != null) {
                host.showPatchMessage(appContext.getString(R.string.patch_download_cancelled))
                onComplete?.invoke(appContext.getString(R.string.patch_download_cancelled))
            }
        }
    }

    /** Checks remote updates and posts status summary on the main thread. */
    fun checkUpdate(context: Context, onStatusResult: (summary: String) -> Unit) {
        val appContext = context.applicationContext
        ChsConfig.init(appContext)
        if (!ChsConfig.isEnabled()) {
            mainHandler.post {
                onStatusResult(appContext.getString(R.string.chs_status_disabled))
            }
            return
        }

        mainHandler.post {
            onStatusResult(appContext.getString(R.string.patch_download_checking))
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
                    mainHandler.post { onStatusResult(fallbackMsg) }
                    return@execute
                }

                val rootDir = ChsPaths.getRootDir(appContext)
                val localMap = ChsManifest.loadLocalManifestMap(ChsPaths.getLocalManifestFile(appContext))
                val diff = ChsManifest.computeDiff(remote, rootDir, localMap)

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
                    onStatusResult(summary)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check CHS update: ${e.message}")
                mainHandler.post {
                    onStatusResult(appContext.getString(R.string.chs_status_check_failed))
                }
            }
        }
    }

    /** Runs incremental patch update using GotoBrowser PatchDownloadState progress dialog. */
    fun startDownload(
        host: SettingsStatusHost,
        onCompleteSummary: (summary: String) -> Unit
    ) {
        val appContext = host.getContext()?.applicationContext ?: return
        isCancelled.set(false)
        currentDownloadHost = host
        currentOnComplete = onCompleteSummary

        val modTitle = appContext.getString(R.string.settings_mod_chs_download)

        mainHandler.post {
            host.setPatchDownloadState(
                PatchDownloadState.Running(
                    message = appContext.getString(R.string.patch_download_checking),
                    progress = null,
                    title = modTitle,
                    onCancel = { cancelDownload() }
                )
            )
        }

        workerExecutor.execute {
            try {
                ChsConfig.init(appContext)
                val rootDir = ChsPaths.getRootDir(appContext)
                val manifestFile = ChsPaths.getLocalManifestFile(appContext)
                val localMap = ChsManifest.loadLocalManifestMap(manifestFile)

                if (isCancelled.get()) return@execute

                val remote = fetchRemoteManifest()
                    ?: throw IOException(appContext.getString(R.string.chs_status_check_failed))

                if (isCancelled.get()) return@execute

                val diff = ChsManifest.computeDiff(remote, rootDir, localMap)

                // Delete orphaned/stale files removed from upstream repo
                for (relPath in diff.toDelete) {
                    if (deleteStaleFile(rootDir, relPath)) {
                        localMap.remove(relPath)
                    }
                }

                if (diff.toDownload.isEmpty()) {
                    ChsConfig.setLocalVersion(remote.version)
                    ChsManifest.saveLocalManifestMap(manifestFile, remote.version, localMap)
                    ChsDataRepository.invalidateCache()
                    ChsHackResolver.invalidateIndex()
                    ChsConfig.prepare(appContext)

                    mainHandler.post {
                        if (!isCancelled.get()) {
                            host.setPatchDownloadState(PatchDownloadState.Idle)
                            host.showPatchMessage(appContext.getString(R.string.patch_download_updated))
                            onCompleteSummary(
                                appContext.getString(R.string.chs_status_up_to_date_format, remote.version)
                            )
                        }
                    }
                    return@execute
                }

                if (isCancelled.get()) return@execute

                val baseUrl = ChsConfig.resolveRawBaseUrl()
                downloadEntriesIncrementally(
                    baseUrl = baseUrl,
                    entries = diff.toDownload,
                    rootDir = rootDir,
                    localMap = localMap,
                    manifestFile = manifestFile,
                    remoteVersion = remote.version,
                    onProgress = { current, total, fileName, downloadedBytes ->
                        if (isCancelled.get()) return@downloadEntriesIncrementally
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
                            if (!isCancelled.get()) {
                                host.setPatchDownloadState(
                                    PatchDownloadState.Running(
                                        message = msg,
                                        progress = fraction,
                                        title = modTitle,
                                        onCancel = { cancelDownload() }
                                    )
                                )
                            }
                        }
                    }
                )

                if (isCancelled.get()) return@execute

                ChsConfig.setLocalVersion(remote.version)
                ChsManifest.saveLocalManifestMap(manifestFile, remote.version, localMap)
                ChsDataRepository.invalidateCache()
                ChsHackResolver.invalidateIndex()
                ChsConfig.prepare(appContext)

                mainHandler.post {
                    if (!isCancelled.get()) {
                        host.setPatchDownloadState(PatchDownloadState.Idle)
                        host.showPatchMessage(appContext.getString(R.string.patch_download_updated))
                        onCompleteSummary(
                            appContext.getString(R.string.chs_status_up_to_date_format, remote.version)
                        )
                    }
                }
            } catch (e: Exception) {
                if (isCancelled.get()) {
                    Log.i(TAG, "CHS download cancelled by user")
                    return@execute
                }
                Log.e(TAG, "CHS incremental download failed: ${e.message}")
                mainHandler.post {
                    host.setPatchDownloadState(PatchDownloadState.Idle)
                    host.showPatchMessage(appContext.getString(R.string.chs_download_failed))
                    onCompleteSummary(appContext.getString(R.string.chs_download_failed_summary))
                }
            } finally {
                currentDownloadHost = null
                currentOnComplete = null
            }
        }
    }

    /**
     * Downloads the latest official release archive, extracts it, and reconciles against incremental updates.
     */
    fun startDownloadReleaseArchive(
        host: SettingsStatusHost,
        onCompleteSummary: (summary: String) -> Unit
    ) {
        val appContext = host.getContext()?.applicationContext ?: return
        isCancelled.set(false)
        currentDownloadHost = host
        currentOnComplete = onCompleteSummary

        val title = appContext.getString(R.string.settings_mod_chs_download_archive_option)

        mainHandler.post {
            host.setPatchDownloadState(
                PatchDownloadState.Running(
                    message = appContext.getString(R.string.patch_download_checking),
                    progress = null,
                    title = title,
                    onCancel = { cancelDownload() }
                )
            )
        }

        workerExecutor.execute {
            var archivePartFile: File? = null
            var archiveZipFile: File? = null
            try {
                ChsConfig.init(appContext)
                val rootDir = ChsPaths.getRootDir(appContext)
                rootDir.mkdirs()

                if (isCancelled.get()) return@execute

                // 1. Fetch latest release tag
                val tag = fetchLatestReleaseTag()
                    ?: throw IOException(appContext.getString(R.string.chs_status_check_failed))

                if (isCancelled.get()) return@execute

                val candidateUrls = resolveReleaseArchiveUrls(tag)
                archiveZipFile = File(rootDir, "release_$tag.zip")
                archivePartFile = File(rootDir, "release_$tag.zip.part")

                // 2. Stream-download release archive with progress updates
                downloadArchiveFile(
                    candidateUrls = candidateUrls,
                    destFile = archivePartFile,
                    onProgress = { downloaded, total ->
                        if (isCancelled.get()) return@downloadArchiveFile
                        val fraction = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null
                        val msg = if (total > 0L) {
                            String.format(Locale.US, "%s / %s", formatSize(downloaded), formatSize(total))
                        } else {
                            formatSize(downloaded)
                        }
                        mainHandler.post {
                            if (!isCancelled.get()) {
                                host.setPatchDownloadState(
                                    PatchDownloadState.Running(
                                        message = msg,
                                        progress = fraction,
                                        title = title,
                                        onCancel = { cancelDownload() }
                                    )
                                )
                            }
                        }
                    }
                )

                if (isCancelled.get()) {
                    archivePartFile.delete()
                    return@execute
                }

                if (!archivePartFile.renameTo(archiveZipFile)) {
                    archivePartFile.copyTo(archiveZipFile, overwrite = true)
                    archivePartFile.delete()
                }

                // 3. Extract zip archive with progress updates
                mainHandler.post {
                    if (!isCancelled.get()) {
                        host.setPatchDownloadState(
                            PatchDownloadState.Running(
                                message = appContext.getString(R.string.patch_download_extracting),
                                progress = null,
                                title = title,
                                onCancel = { cancelDownload() }
                            )
                        )
                    }
                }

                extractZipArchive(archiveZipFile, rootDir) { extractedCount, fileName ->
                    if (isCancelled.get()) return@extractZipArchive
                    val msg = String.format(
                        Locale.US,
                        "%s (%d)\n%s",
                        appContext.getString(R.string.patch_download_extracting),
                        extractedCount,
                        fileName
                    )
                    mainHandler.post {
                        if (!isCancelled.get()) {
                            host.setPatchDownloadState(
                                PatchDownloadState.Running(
                                    message = msg,
                                    progress = null,
                                    title = title,
                                    onCancel = { cancelDownload() }
                                )
                            )
                        }
                    }
                }

                if (isCancelled.get()) return@execute

                archiveZipFile.delete()

                // Verify extracted files against remote hashes before checking remaining incremental updates.
                mainHandler.post {
                    if (!isCancelled.get()) {
                        host.setPatchDownloadState(
                            PatchDownloadState.Running(
                                message = appContext.getString(R.string.patch_download_checking),
                                progress = null,
                                title = title,
                                onCancel = { cancelDownload() }
                            )
                        )
                    }
                }

                val manifestFile = ChsPaths.getLocalManifestFile(appContext)
                val localMap = ChsManifest.loadLocalManifestMap(manifestFile)

                val remote = fetchRemoteManifest()
                if (remote != null) {
                    reconcileExtractedFilesWithRemote(remote, rootDir, localMap)
                    val diff = ChsManifest.computeDiff(remote, rootDir, localMap)
                    if (diff.hasUpdates) {
                        // Record release tag so remaining incremental files still trigger an update prompt.
                        ChsConfig.setLocalVersion(tag)
                        ChsManifest.saveLocalManifestMap(manifestFile, tag, localMap)
                    } else {
                        ChsConfig.setLocalVersion(remote.version)
                        ChsManifest.saveLocalManifestMap(manifestFile, remote.version, localMap)
                    }
                    ChsDataRepository.invalidateCache()
                    ChsHackResolver.invalidateIndex()
                    ChsConfig.prepare(appContext)

                    mainHandler.post {
                        host.setPatchDownloadState(PatchDownloadState.Idle)
                        if (diff.hasUpdates) {
                            host.showPatchMessage(appContext.getString(R.string.chs_archive_extracted_with_updates))
                            onCompleteSummary(
                                appContext.getString(
                                    R.string.chs_status_update_available_format,
                                    remote.version,
                                    diff.toDownload.size,
                                    formatSize(diff.totalDownloadBytes)
                                )
                            )
                        } else {
                            host.showPatchMessage(appContext.getString(R.string.chs_archive_extracted_up_to_date))
                            onCompleteSummary(
                                appContext.getString(R.string.chs_status_up_to_date_format, remote.version)
                            )
                        }
                    }
                } else {
                    val baseVersion = "$tag (release)"
                    // When remote manifest is unreachable, compute git blob SHA-1 hashes
                    // for all extracted files and persist to local manifest map.
                    rootDir.walkTopDown().filter { it.isFile }.forEach { f ->
                        val relPath = f.relativeTo(rootDir).path.replace('\\', '/')
                        if (relPath.startsWith("text/") || relPath.startsWith("cache/")) {
                            localMap[relPath] = ChsManifest.gitBlobSha1Hex(f)
                        }
                    }
                    ChsConfig.setLocalVersion(baseVersion)
                    ChsManifest.saveLocalManifestMap(manifestFile, baseVersion, localMap)
                    ChsDataRepository.invalidateCache()
                    ChsHackResolver.invalidateIndex()
                    ChsConfig.prepare(appContext)

                    mainHandler.post {
                        host.setPatchDownloadState(PatchDownloadState.Idle)
                        host.showPatchMessage(appContext.getString(R.string.patch_download_updated))
                        onCompleteSummary(
                            appContext.getString(R.string.chs_status_offline_installed, baseVersion)
                        )
                    }
                }
            } catch (e: Exception) {
                archivePartFile?.delete()
                archiveZipFile?.delete()
                if (isCancelled.get()) {
                    Log.i(TAG, "CHS release archive download cancelled by user")
                    return@execute
                }
                Log.e(TAG, "CHS release archive download failed: ${e.message}")
                mainHandler.post {
                    host.setPatchDownloadState(PatchDownloadState.Idle)
                    host.showPatchMessage(appContext.getString(R.string.chs_download_failed))
                    onCompleteSummary(appContext.getString(R.string.chs_download_failed_summary))
                }
            } finally {
                currentDownloadHost = null
                currentOnComplete = null
            }
        }
    }

    /**
     * Verifies extracted archive files against [remote] hashes before updating [localMap].
     * Any missing, size-mismatched, or hash-mismatched file is excluded so it will be repaired
     * by incremental download. Files deleted upstream are removed from disk.
     */
    fun reconcileExtractedFilesWithRemote(
        remote: ChsManifest.ManifestData,
        rootDir: File,
        localMap: MutableMap<String, String>
    ) {
        val remotePaths = remote.files.map { it.path }.toSet()
        val iterator = localMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!remotePaths.contains(entry.key) && deleteStaleFile(rootDir, entry.key)) {
                iterator.remove()
            }
        }

        for (entry in remote.files) {
            val f = File(rootDir, entry.path)
            val sizeMatches = entry.size <= 0L || f.length() == entry.size
            if (f.isFile && sizeMatches && ChsManifest.verifyFileHash(f, entry.hash, entry.hashType)) {
                localMap[entry.path] = entry.hash
            } else {
                localMap.remove(entry.path)
            }
        }
    }

    /**
     * Safely deletes an obsolete file inside [rootDir]. Returns true if the manifest key
     * should be removed (deleted, already absent, or unsafe path rejected without disk access).
     */
    private fun deleteStaleFile(rootDir: File, relPath: String): Boolean {
        val dead = ChsManifest.safeResolveUnderRoot(rootDir, relPath)
        if (dead == null) {
            try { Log.w(TAG, "Skipped unsafe stale path: $relPath") } catch (_: Throwable) {}
            return true
        }
        return !dead.exists() || dead.delete()
    }

    /**
     * Downloads files concurrently, verifying either SHA-256 or git_sha before atomic replacement.
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
        val lastReportTime = AtomicLong(0L)

        val pool = Executors.newFixedThreadPool(CONCURRENCY)
        activeDownloadPool = pool
        val firstError = AtomicReference<Exception?>(null)

        try {
            val futures = entries.map { entry ->
                pool.submit {
                    if (isCancelled.get() || firstError.get() != null) return@submit
                    val targetFile = File(rootDir, entry.path)
                    val tempFile = File(rootDir, "${entry.path}.part")
                    try {
                        tempFile.parentFile?.mkdirs()

                        val fileUrl = normalizedBase + entry.path
                        downloadSingleFile(fileUrl, tempFile)

                        if (isCancelled.get()) {
                            tempFile.delete()
                            return@submit
                        }

                        if (!ChsManifest.verifyFileHash(tempFile, entry.hash, entry.hashType)) {
                            tempFile.delete()
                            throw IOException("Hash mismatch (${entry.hashType}): ${entry.path}")
                        }

                        if (isCancelled.get()) {
                            tempFile.delete()
                            return@submit
                        }

                        targetFile.parentFile?.mkdirs()
                        if (targetFile.exists() && !targetFile.delete()) {
                            throw IOException("Failed to replace target file: ${entry.path}")
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

                        // Throttle UI progress updates to at most once every 100ms except on completion.
                        val now = System.currentTimeMillis()
                        val last = lastReportTime.get()
                        if (done == total || now - last >= 100L) {
                            if (lastReportTime.compareAndSet(last, now) || done == total) {
                                onProgress(done, total, entry.path.substringAfterLast('/'), bytes)
                            }
                        }
                    } catch (e: Exception) {
                        try {
                            if (tempFile.exists()) tempFile.delete()
                        } catch (_: Exception) {
                        }
                        if (!isCancelled.get()) {
                            firstError.compareAndSet(null, e)
                        }
                    }
                }
            }
            for (f in futures) {
                try {
                    f.get()
                } catch (_: Exception) {
                }
                if (isCancelled.get()) break
            }
        } finally {
            activeDownloadPool = null
            pool.shutdownNow()
        }

        if (isCancelled.get()) {
            return
        }

        synchronized(localMap) {
            ChsManifest.saveLocalManifestMap(manifestFile, remoteVersion, localMap)
        }

        firstError.get()?.let { throw it }
    }

    /** Deletes locally cached localization files. */
    fun deletePatchData(
        host: SettingsStatusHost,
        onCompleteSummary: (summary: String) -> Unit
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
     * Fetches manifest.json from the selected mirror; falls back to GitHub Trees API (unproxied)
     * when manifest.json is missing or unreachable.
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

        // Fall back to GitHub Git Trees API (?recursive=1) which provides git blob SHA-1 hashes.
        return try {
            val treeRaw = fetchUrlString(GITHUB_TREES_API)
            ChsManifest.parseGithubTreeJson(treeRaw)?.takeIf { it.files.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Trees API fallback failed: ${e.message}")
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
            body.byteStream().buffered(BUFFER_SIZE).use { input ->
                val out = java.io.ByteArrayOutputStream(
                    if (contentLen in 1..65536) contentLen.toInt() else BUFFER_SIZE
                )
                val buffer = ByteArray(BUFFER_SIZE)
                var totalRead = 0L
                while (true) {
                    if (isCancelled.get()) {
                        throw IOException("Download cancelled")
                    }
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
            destFile.parentFile?.mkdirs()
            BufferedOutputStream(FileOutputStream(destFile), BUFFER_SIZE).use { fos ->
                body.byteStream().buffered(BUFFER_SIZE).use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var totalRead = 0L
                    while (true) {
                        if (isCancelled.get()) {
                            throw IOException("Download cancelled")
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        totalRead += read
                        if (totalRead > MAX_SINGLE_FILE_BYTES) {
                            throw IOException("File stream exceeds maximum allowed size: $url")
                        }
                        fos.write(buffer, 0, read)
                    }
                    fos.flush()
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

    fun fetchLatestReleaseTag(): String? {
        val req = Request.Builder().url(GITHUB_RELEASES_LATEST).head().build()
        val noRedirectClient = httpClient.newBuilder().followRedirects(false).build()
        return try {
            noRedirectClient.newCall(req).execute().use { resp ->
                val location = resp.header("Location") ?: ""
                val tag = location.substringAfterLast('/').trim()
                tag.takeIf { it.isNotEmpty() && it != "latest" }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveReleaseArchiveUrls(tag: String): List<String> {
        val isGhProxy = ChsConfig.getString(ChsConfig.KEY_DATA_SOURCE, ChsConfig.SOURCE_GITHUB_RAW) == ChsConfig.SOURCE_GHPROXY
        val directRaw = "https://github.com/asaico/Kancolle_to_CHS/releases/download/$tag/Kancolle_to_CHS_$tag.zip"
        val fallbackRaw = "https://github.com/asaico/Kancolle_to_CHS/archive/refs/tags/$tag.zip"
        return if (isGhProxy) {
            listOf(
                "https://mirror.ghproxy.com/$directRaw",
                "https://mirror.ghproxy.com/$fallbackRaw",
                directRaw,
                fallbackRaw
            )
        } else {
            listOf(directRaw, fallbackRaw)
        }
    }

    private fun downloadArchiveFile(
        candidateUrls: List<String>,
        destFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) {
        var lastException: Exception? = null
        for (url in candidateUrls) {
            if (isCancelled.get()) return
            try {
                val req = Request.Builder().url(url).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IOException("HTTP ${resp.code}: $url")
                    }
                    val body = resp.body ?: throw IOException("Empty body: $url")
                    val totalBytes = body.contentLength()
                    if (totalBytes > MAX_ARCHIVE_BYTES) {
                        throw IOException("Archive size exceeds limit: $totalBytes")
                    }
                    destFile.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(destFile), BUFFER_SIZE).use { fos ->
                        body.byteStream().buffered(BUFFER_SIZE).use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var totalRead = 0L
                            var lastReportTime = 0L
                            while (true) {
                                if (isCancelled.get()) {
                                    throw IOException("Download cancelled")
                                }
                                val read = input.read(buffer)
                                if (read == -1) break
                                totalRead += read
                                if (totalRead > MAX_ARCHIVE_BYTES) {
                                    throw IOException("Archive stream exceeds limit")
                                }
                                fos.write(buffer, 0, read)

                                val now = System.currentTimeMillis()
                                if (now - lastReportTime >= 100L) {
                                    lastReportTime = now
                                    onProgress(totalRead, totalBytes)
                                }
                            }
                            fos.flush()
                            onProgress(totalRead, totalBytes)
                        }
                    }
                }
                // Archive downloaded successfully.
                return
            } catch (e: Exception) {
                destFile.delete()
                if (isCancelled.get()) throw e
                lastException = e
            }
        }
        throw lastException ?: IOException("Failed to download release archive from all candidate URLs")
    }

    fun extractZipArchive(
        zipFile: File,
        targetDir: File,
        onProgress: (extractedCount: Int, fileName: String) -> Unit
    ): Int {
        var count = 0
        val buffer = ByteArray(BUFFER_SIZE)
        val canonicalDest = targetDir.canonicalPath + File.separator

        java.util.zip.ZipInputStream(java.io.BufferedInputStream(java.io.FileInputStream(zipFile), BUFFER_SIZE)).use { zis ->
            var entry: java.util.zip.ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (isCancelled.get()) {
                    throw IOException("Extraction cancelled")
                }
                val name = entry.name.replace('\\', '/')
                val normalizedPath = when {
                    name.startsWith("cache/") || name.startsWith("text/") -> name
                    name.contains("/cache/") -> name.substring(name.indexOf("/cache/") + 1)
                    name.contains("/text/") -> name.substring(name.indexOf("/text/") + 1)
                    else -> ""
                }

                if (normalizedPath.isNotEmpty() && !normalizedPath.startsWith("text/text/subtitles/")) {
                    val outFile = File(targetDir, normalizedPath)
                    val canonicalOut = outFile.canonicalPath
                    if (!canonicalOut.startsWith(canonicalDest)) {
                        throw SecurityException("Zip Slip detected: $name")
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).buffered(BUFFER_SIZE).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                if (isCancelled.get()) throw IOException("Extraction cancelled")
                                fos.write(buffer, 0, len)
                            }
                        }
                        count++
                        if (count % 25 == 0) {
                            onProgress(count, normalizedPath.substringAfterLast('/'))
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return count
    }
}
