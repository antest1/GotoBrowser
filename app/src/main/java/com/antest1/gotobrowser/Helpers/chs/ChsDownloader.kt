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

        // Present in rootDir while commitStagingDirectory swaps text/ and cache/.
        private const val SWAP_MARKER = ".swap_in_progress"

        // Serializes archive commits and recovery across downloader instances.
        private val installLock = Any()

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

    private val nextTaskToken = AtomicLong(0L)
    @Volatile
    private var activeDownloadToken: Long = 0L
    @Volatile
    private var cancelledUpToToken: Long = 0L
    @Volatile
    private var activeDownloadPool: ExecutorService? = null
    @Volatile
    private var currentDownloadHost: SettingsStatusHost? = null
    @Volatile
    private var currentOnComplete: ((summary: String) -> Unit)? = null

    internal fun beginDownloadTask(): Long {
        val token = nextTaskToken.incrementAndGet()
        activeDownloadToken = token
        return token
    }

    internal fun isTaskCancelled(token: Long): Boolean {
        return token != 0L && cancelledUpToToken >= token
    }

    /** Shuts down background executors and cancels any active download. */
    fun shutdown() {
        cancelDownload()
        workerExecutor.shutdownNow()
    }

    /** Cancels an in-flight download or archive extraction. */
    fun cancelDownload() {
        val token = activeDownloadToken
        if (token == 0L || cancelledUpToToken >= token) {
            return
        }
        cancelledUpToToken = token
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
                    val changeCount = diff.toDownload.size + diff.toDelete.size
                    appContext.getString(
                        R.string.chs_status_update_available_format,
                        remote.version,
                        changeCount,
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
        val token = beginDownloadTask()
        currentDownloadHost = host
        currentOnComplete = onCompleteSummary

        val modTitle = appContext.getString(R.string.settings_mod_chs_download)

        mainHandler.post {
            if (!isTaskCancelled(token)) {
                host.setPatchDownloadState(
                    PatchDownloadState.Running(
                        message = appContext.getString(R.string.patch_download_checking),
                        progress = null,
                        title = modTitle,
                        onCancel = { cancelDownload() }
                    )
                )
            }
        }

        workerExecutor.execute {
            try {
                ChsConfig.init(appContext)
                val rootDir = ChsPaths.getRootDir(appContext)
                recoverInterruptedArchiveInstall(rootDir)
                val manifestFile = ChsPaths.getLocalManifestFile(appContext)
                val localMap = ChsManifest.loadLocalManifestMap(manifestFile)

                if (isTaskCancelled(token)) return@execute

                val remote = fetchRemoteManifest { isTaskCancelled(token) }
                    ?: throw IOException(appContext.getString(R.string.chs_status_check_failed))

                if (isTaskCancelled(token)) return@execute

                val diff = ChsManifest.computeDiff(remote, rootDir, localMap)

                // Delete orphaned/stale files removed from upstream repo
                for (relPath in diff.toDelete) {
                    if (isTaskCancelled(token)) return@execute
                    if (deleteStaleFile(rootDir, relPath)) {
                        localMap.remove(relPath)
                    }
                }

                if (diff.toDownload.isEmpty()) {
                    if (isTaskCancelled(token)) return@execute
                    ChsConfig.setLocalVersion(remote.version)
                    ChsManifest.saveLocalManifestMap(manifestFile, remote.version, localMap)
                    ChsDataRepository.invalidateCache()
                    ChsHackResolver.invalidateIndex()
                    ChsConfig.prepare(appContext)

                    mainHandler.post {
                        if (!isTaskCancelled(token)) {
                            host.setPatchDownloadState(PatchDownloadState.Idle)
                            host.showPatchMessage(appContext.getString(R.string.patch_download_updated))
                            onCompleteSummary(
                                appContext.getString(R.string.chs_status_up_to_date_format, remote.version)
                            )
                        }
                    }
                    return@execute
                }

                if (isTaskCancelled(token)) return@execute

                val baseUrl = ChsConfig.resolveRawBaseUrl()
                downloadEntriesIncrementally(
                    baseUrl = baseUrl,
                    entries = diff.toDownload,
                    rootDir = rootDir,
                    localMap = localMap,
                    manifestFile = manifestFile,
                    remoteVersion = remote.version,
                    isCancelledCheck = { isTaskCancelled(token) },
                    onProgress = { current, total, fileName, downloadedBytes ->
                        if (isTaskCancelled(token)) return@downloadEntriesIncrementally
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
                            if (!isTaskCancelled(token)) {
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

                if (isTaskCancelled(token)) return@execute

                ChsConfig.setLocalVersion(remote.version)
                ChsManifest.saveLocalManifestMap(manifestFile, remote.version, localMap)
                ChsDataRepository.invalidateCache()
                ChsHackResolver.invalidateIndex()
                ChsConfig.prepare(appContext)

                mainHandler.post {
                    if (!isTaskCancelled(token)) {
                        host.setPatchDownloadState(PatchDownloadState.Idle)
                        host.showPatchMessage(appContext.getString(R.string.patch_download_updated))
                        onCompleteSummary(
                            appContext.getString(R.string.chs_status_up_to_date_format, remote.version)
                        )
                    }
                }
            } catch (e: Exception) {
                if (isTaskCancelled(token)) {
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
                if (activeDownloadToken == token) {
                    activeDownloadToken = 0L
                    currentDownloadHost = null
                    currentOnComplete = null
                }
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
        val token = beginDownloadTask()
        currentDownloadHost = host
        currentOnComplete = onCompleteSummary

        val title = appContext.getString(R.string.settings_mod_chs_download_archive_option)

        mainHandler.post {
            if (!isTaskCancelled(token)) {
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

        workerExecutor.execute {
            var archivePartFile: File? = null
            var archiveZipFile: File? = null
            var stagingDir: File? = null
            try {
                ChsConfig.init(appContext)
                val rootDir = ChsPaths.getRootDir(appContext)
                rootDir.mkdirs()
                recoverInterruptedArchiveInstall(rootDir)

                if (isTaskCancelled(token)) return@execute

                // 1. Fetch latest release tag
                val tag = fetchLatestReleaseTag()
                    ?: throw IOException(appContext.getString(R.string.chs_status_check_failed))

                if (isTaskCancelled(token)) return@execute

                val candidateUrls = resolveReleaseArchiveUrls(tag)
                archiveZipFile = File(rootDir, "release_$tag.zip")
                archivePartFile = File(rootDir, "release_$tag.zip.part")
                stagingDir = File(rootDir, ".staging_$token")
                if (stagingDir.exists()) {
                    stagingDir.deleteRecursively()
                }
                stagingDir.mkdirs()

                // 2. Stream-download release archive with progress updates
                downloadArchiveFile(
                    candidateUrls = candidateUrls,
                    destFile = archivePartFile,
                    isCancelledCheck = { isTaskCancelled(token) },
                    onProgress = { downloaded, total ->
                        if (isTaskCancelled(token)) return@downloadArchiveFile
                        val fraction = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null
                        val msg = if (total > 0L) {
                            String.format(Locale.US, "%s / %s", formatSize(downloaded), formatSize(total))
                        } else {
                            formatSize(downloaded)
                        }
                        mainHandler.post {
                            if (!isTaskCancelled(token)) {
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

                if (isTaskCancelled(token)) {
                    archivePartFile.delete()
                    return@execute
                }

                if (!archivePartFile.renameTo(archiveZipFile)) {
                    archivePartFile.copyTo(archiveZipFile, overwrite = true)
                    archivePartFile.delete()
                }

                // 3. Extract zip archive into isolated staging directory before touching rootDir
                mainHandler.post {
                    if (!isTaskCancelled(token)) {
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

                val extractedCount = extractZipArchive(
                    zipFile = archiveZipFile,
                    targetDir = stagingDir,
                    isCancelledCheck = { isTaskCancelled(token) }
                ) { count, fileName ->
                    if (isTaskCancelled(token)) return@extractZipArchive
                    val msg = String.format(
                        Locale.US,
                        "%s (%d)\n%s",
                        appContext.getString(R.string.patch_download_extracting),
                        count,
                        fileName
                    )
                    mainHandler.post {
                        if (!isTaskCancelled(token)) {
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

                if (isTaskCancelled(token)) return@execute
                if (extractedCount <= 0) {
                    throw IOException("Archive contained no supported CHS patch files")
                }

                archiveZipFile.delete()

                // Verify extracted files inside stagingDir against remote hashes before replacing rootDir.
                mainHandler.post {
                    if (!isTaskCancelled(token)) {
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

                val remote = fetchRemoteManifest { isTaskCancelled(token) }
                if (isTaskCancelled(token)) return@execute

                if (remote != null) {
                    reconcileExtractedFilesWithRemote(remote, stagingDir, localMap) { isTaskCancelled(token) }
                    if (isTaskCancelled(token)) return@execute

                    val matchedRemoteCount = remote.files.count { localMap.containsKey(it.path) }
                    if (remote.files.isNotEmpty() && matchedRemoteCount == 0) {
                        throw IOException("Release archive verification failed: no files matched remote manifest")
                    }

                    commitStagingDirectory(stagingDir, rootDir)
                    if (isTaskCancelled(token)) return@execute

                    val diff = ChsManifest.computeDiff(remote, rootDir, localMap)
                    if (isTaskCancelled(token)) return@execute

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
                        if (!isTaskCancelled(token)) {
                            host.setPatchDownloadState(PatchDownloadState.Idle)
                            if (diff.hasUpdates) {
                                val changeCount = diff.toDownload.size + diff.toDelete.size
                                host.showPatchMessage(appContext.getString(R.string.chs_archive_extracted_with_updates))
                                onCompleteSummary(
                                    appContext.getString(
                                        R.string.chs_status_update_available_format,
                                        remote.version,
                                        changeCount,
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
                    }
                } else {
                    val baseVersion = "$tag (release)"
                    // When remote manifest is unreachable, compute SHA-256 hashes (matching manifest.json)
                    // inside stagingDir before committing to rootDir.
                    val stagedHashes = mutableMapOf<String, String>()
                    stagingDir.walkTopDown().filter { it.isFile }.forEach { f ->
                        if (isTaskCancelled(token)) return@execute
                        val relPath = f.relativeTo(stagingDir).path.replace('\\', '/')
                        if (ChsManifest.isSupportedRepoPath(relPath)) {
                            stagedHashes[relPath] = ChsManifest.sha256Hex(f)
                        }
                    }
                    if (stagedHashes.isEmpty()) {
                        throw IOException("Archive contained no supported CHS patch files")
                    }
                    if (isTaskCancelled(token)) return@execute

                    commitStagingDirectory(stagingDir, rootDir)
                    if (isTaskCancelled(token)) return@execute

                    localMap.clear()
                    localMap.putAll(stagedHashes)
                    ChsConfig.setLocalVersion(baseVersion)
                    ChsManifest.saveLocalManifestMap(manifestFile, baseVersion, localMap)
                    ChsDataRepository.invalidateCache()
                    ChsHackResolver.invalidateIndex()
                    ChsConfig.prepare(appContext)

                    mainHandler.post {
                        if (!isTaskCancelled(token)) {
                            host.setPatchDownloadState(PatchDownloadState.Idle)
                            host.showPatchMessage(appContext.getString(R.string.patch_download_updated))
                            onCompleteSummary(
                                appContext.getString(R.string.chs_status_offline_installed, baseVersion)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                archivePartFile?.delete()
                archiveZipFile?.delete()
                if (isTaskCancelled(token)) {
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
                try {
                    stagingDir?.deleteRecursively()
                } catch (_: Exception) {}
                if (activeDownloadToken == token) {
                    activeDownloadToken = 0L
                    currentDownloadHost = null
                    currentOnComplete = null
                }
            }
        }
    }

    /**
     * Cleans up after an archive install that did not finish (e.g. the process was killed).
     *
     * A `.backup_<name>_*` directory is always a complete copy (see [commitStagingDirectory]).
     * If the swap marker is present, the swap was cut off and `text/` or `cache/` may be partial,
     * so each backed-up directory is restored. Without the marker, a backup is restored only when
     * its target is missing. Leftover backups, staging directories and archive files are then
     * removed. If a restore fails, the backups and the marker are kept for the next attempt.
     */
    internal fun recoverInterruptedArchiveInstall(rootDir: File) {
        synchronized(installLock) {
            val entries = rootDir.listFiles() ?: return
            val marker = File(rootDir, SWAP_MARKER)
            val swapInterrupted = marker.exists()
            for (name in listOf("text", "cache")) {
                val backups = entries
                    .filter { it.isDirectory && it.name.startsWith(".backup_${name}_") }
                    .sortedByDescending { it.lastModified() }
                val target = File(rootDir, name)
                val restoreFrom = backups.firstOrNull()?.takeIf { swapInterrupted || !target.exists() }
                if (restoreFrom != null) {
                    restoreBackup(restoreFrom, target, rootDir)
                }
                backups.filter { it.exists() }.forEach { it.deleteRecursively() }
            }
            marker.delete()
            entries
                .filter {
                    it.name.startsWith(".staging_") ||
                        it.name.startsWith(".backup_tmp_") ||
                        it.name.startsWith(".restore_") ||
                        (it.isFile && it.name.startsWith("release_") &&
                            (it.name.endsWith(".zip") || it.name.endsWith(".zip.part")))
                }
                .forEach { it.deleteRecursively() }
        }
    }

    /** Replaces [target] with [backup]; a copy goes through a temporary name so it is never seen half-done. */
    private fun restoreBackup(backup: File, target: File, rootDir: File) {
        if (target.exists() && !target.deleteRecursively()) {
            throw IOException("Failed to remove ${target.name} before restoring its backup")
        }
        if (backup.renameTo(target)) return
        val temp = File(rootDir, ".restore_${target.name}")
        temp.deleteRecursively()
        backup.copyRecursively(temp, overwrite = true)
        if (!temp.renameTo(target)) {
            throw IOException("Failed to restore ${target.name} from its backup")
        }
    }

    /**
     * Replaces `text/` and `cache/` inside [rootDir] from [stagingDir], restoring backups if
     * swapping fails. A swap marker stays in [rootDir] until the swap is complete, so
     * [recoverInterruptedArchiveInstall] can roll back a swap cut off by a process kill.
     */
    internal fun commitStagingDirectory(stagingDir: File, rootDir: File) {
        synchronized(installLock) {
            val stagedDirs = listOf("text", "cache").filter { File(stagingDir, it).isDirectory }
            if (stagedDirs.isEmpty()) {
                throw IOException("Staging directory contains neither text/ nor cache/")
            }
            val marker = File(rootDir, SWAP_MARKER)
            marker.writeText("")
            val backups = mutableListOf<Pair<File, File>>()
            val createdTargets = mutableListOf<File>()
            try {
                for (name in stagedDirs) {
                    val stagedSub = File(stagingDir, name)
                    val targetSub = File(rootDir, name)
                    if (targetSub.exists()) {
                        val backupSub = File(rootDir, ".backup_${name}_${System.nanoTime()}")
                        if (targetSub.renameTo(backupSub)) {
                            backups.add(targetSub to backupSub)
                        } else {
                            // Copy under a temporary name so a .backup_<name>_* directory is always complete.
                            val tempBackup = File(rootDir, ".backup_tmp_$name")
                            tempBackup.deleteRecursively()
                            targetSub.copyRecursively(tempBackup, overwrite = true)
                            if (!tempBackup.renameTo(backupSub)) {
                                throw IOException("Failed to prepare backup for $name")
                            }
                            backups.add(targetSub to backupSub)
                            if (!targetSub.deleteRecursively()) {
                                throw IOException("Failed to replace $name")
                            }
                        }
                    } else {
                        createdTargets.add(targetSub)
                    }
                    if (!stagedSub.renameTo(targetSub)) {
                        stagedSub.copyRecursively(targetSub, overwrite = true)
                        stagedSub.deleteRecursively()
                    }
                }
            } catch (e: Exception) {
                var rolledBack = true
                for (created in createdTargets) {
                    try {
                        if (created.exists()) {
                            created.deleteRecursively()
                        }
                    } catch (_: Exception) {}
                }
                for ((targetSub, backupSub) in backups) {
                    try {
                        restoreBackup(backupSub, targetSub, rootDir)
                    } catch (_: Exception) {
                        rolledBack = false
                    }
                }
                // Keep the marker when a backup could not be restored, so recovery retries it.
                if (rolledBack) {
                    marker.delete()
                }
                throw e
            }
            // The swap is complete: drop the marker before the backups, so cleanup cut off
            // midway can never restore old data over the new install.
            marker.delete()
            for ((_, backupSub) in backups) {
                backupSub.deleteRecursively()
            }
        }
    }

    /**
     * Verifies extracted archive files against [remote] hashes before updating [localMap].
     * Any missing, size-mismatched, or hash-mismatched file is excluded so it will be repaired
     * by incremental download. Files deleted upstream or untracked in [rootDir] are removed.
     */
    fun reconcileExtractedFilesWithRemote(
        remote: ChsManifest.ManifestData,
        rootDir: File,
        localMap: MutableMap<String, String>,
        isCancelledCheck: () -> Boolean = { false }
    ) {
        val remotePaths = remote.files.map { it.path }.toSet()
        val iterator = localMap.entries.iterator()
        while (iterator.hasNext()) {
            if (isCancelledCheck()) throw IOException("Download cancelled")
            val entry = iterator.next()
            if (!remotePaths.contains(entry.key) && deleteStaleFile(rootDir, entry.key)) {
                iterator.remove()
            }
        }

        // Purge any untracked supported files extracted from the archive that are absent from remote manifest.
        for (subName in listOf("text", "cache")) {
            val subDir = File(rootDir, subName)
            if (subDir.isDirectory) {
                subDir.walkTopDown().filter { it.isFile }.forEach { f ->
                    if (isCancelledCheck()) throw IOException("Download cancelled")
                    val relPath = f.relativeTo(rootDir).path.replace('\\', '/')
                    if (ChsManifest.isSupportedRepoPath(relPath) && !remotePaths.contains(relPath)) {
                        f.delete()
                        localMap.remove(relPath)
                    }
                }
            }
        }

        for (entry in remote.files) {
            if (isCancelledCheck()) throw IOException("Download cancelled")
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
            Log.w(TAG, "Skipped unsafe stale path: $relPath")
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
        isCancelledCheck: () -> Boolean = { false },
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
                    if (isCancelledCheck() || firstError.get() != null) return@submit
                    val targetFile = File(rootDir, entry.path)
                    val tempFile = File(rootDir, "${entry.path}.part")
                    try {
                        tempFile.parentFile?.mkdirs()

                        val fileUrl = normalizedBase + entry.path
                        downloadSingleFile(fileUrl, tempFile, isCancelledCheck)

                        if (isCancelledCheck()) {
                            tempFile.delete()
                            return@submit
                        }

                        if (!ChsManifest.verifyFileHash(tempFile, entry.hash, entry.hashType)) {
                            tempFile.delete()
                            throw IOException("Hash mismatch (${entry.hashType}): ${entry.path}")
                        }

                        if (isCancelledCheck()) {
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
                        if (!isCancelledCheck()) {
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
                if (isCancelledCheck()) break
            }
        } finally {
            activeDownloadPool = null
            pool.shutdownNow()
        }

        if (isCancelledCheck()) {
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
    fun fetchRemoteManifest(isCancelledCheck: () -> Boolean = { false }): ChsManifest.ManifestData? {
        val baseUrl = ChsConfig.resolveRawBaseUrl()
        val manifestUrl = "${baseUrl}manifest.json"
        try {
            val raw = fetchUrlString(manifestUrl, isCancelledCheck)
            if (!raw.isNullOrBlank()) {
                ChsManifest.parseManifestJson(raw)?.takeIf { it.files.isNotEmpty() }?.let {
                    return it
                }
            }
        } catch (_: Exception) {
        }

        if (isCancelledCheck()) return null

        // Fall back to GitHub Git Trees API (?recursive=1) which provides git blob SHA-1 hashes.
        return try {
            val treeRaw = fetchUrlString(GITHUB_TREES_API, isCancelledCheck)
            ChsManifest.parseGithubTreeJson(treeRaw)?.takeIf { it.files.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Trees API fallback failed: ${e.message}")
            null
        }
    }

    private fun fetchUrlString(url: String, isCancelledCheck: () -> Boolean = { false }): String? {
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
                    if (isCancelledCheck()) {
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

    private fun downloadSingleFile(
        url: String,
        destFile: File,
        isCancelledCheck: () -> Boolean = { false }
    ) {
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
                        if (isCancelledCheck()) {
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
        isCancelledCheck: () -> Boolean = { false },
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) {
        var lastException: Exception? = null
        for (url in candidateUrls) {
            if (isCancelledCheck()) return
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
                                if (isCancelledCheck()) {
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
                if (isCancelledCheck()) throw e
                lastException = e
            }
        }
        throw lastException ?: IOException("Failed to download release archive from all candidate URLs")
    }

    fun extractZipArchive(
        zipFile: File,
        targetDir: File,
        isCancelledCheck: () -> Boolean = { false },
        onProgress: (extractedCount: Int, fileName: String) -> Unit
    ): Int {
        var count = 0
        val buffer = ByteArray(BUFFER_SIZE)
        val canonicalDest = targetDir.canonicalPath + File.separator

        java.util.zip.ZipInputStream(java.io.BufferedInputStream(java.io.FileInputStream(zipFile), BUFFER_SIZE)).use { zis ->
            var entry: java.util.zip.ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (isCancelledCheck()) {
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
                                if (isCancelledCheck()) throw IOException("Extraction cancelled")
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
