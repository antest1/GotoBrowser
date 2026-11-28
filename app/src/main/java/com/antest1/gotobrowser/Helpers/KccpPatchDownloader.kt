package com.antest1.gotobrowser.Helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.antest1.gotobrowser.Activity.PatchDownloadState
import com.antest1.gotobrowser.Activity.SettingsStatusHost
import com.antest1.gotobrowser.Helpers.chs.ChsManifest
import com.antest1.gotobrowser.R
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipFile

/**
 * Installs and updates the KCCP language patches (EN / ID) from their GitHub repositories.
 *
 * - Install, or whenever an incremental update is not possible: the archive of the exact
 *   latest commit is downloaded, extracted into a staging folder and swapped in.
 * - Update: the files changed since the installed commit are downloaded into the staging
 *   folder and checked against their git blob hashes before they are moved into place.
 *
 * A run that fails or is cancelled before the new files are moved into place leaves the
 * current install as it was, unless the install had to be removed to make room for the
 * extraction; from that point the run can no longer be cancelled.
 *
 * The commit an install came from is kept in a file inside the install, so it always
 * describes the files next to it, whichever storage the app uses. It is written only once
 * the new files are in place, and an update moves the version file last: when an update
 * changes the version, one cut off while moving files still shows as outdated and is
 * applied again on the next run. Otherwise it is applied again with the next update,
 * which starts from the commit still recorded.
 *
 * Tasks run one at a time on a single worker thread shared by the process. From Android 14
 * a download runs as a user-initiated data transfer job ([KccpPatchJobService]), so it goes
 * on, install included, with its own notification when the app is left.
 */
class KccpPatchDownloader(
    private val httpClient: OkHttpClient = createDefaultHttpClient()
) {
    /** Where one language's patch lives, captured when a task starts. */
    internal data class Repo(
        val symbol: String,
        /** `owner/name` on GitHub. */
        val githubPath: String,
        /** Product name, shown on the download notification. */
        val title: String,
        /** Install folder name under the app files folder. */
        val rootName: String,
        val infoFileName: String
    )

    internal class Layout(parent: File, repo: Repo) {
        val root = File(parent, repo.rootName)
        val staging = File(parent, ".${repo.rootName}.staging")
        val backup = File(parent, ".${repo.rootName}.backup")
        val archive = File(parent, ".${repo.rootName}.zip.part")
        val commitFile = File(root, COMMIT_FILE_NAME)

        /** The version file; without it the folder is not an install. */
        val infoFile = File(root, repo.infoFileName)

        /** Left behind by the previous downloader, which kept the archive under this name. */
        val legacyArchive = File(parent, "master.zip")
    }

    internal data class ChangedFile(
        val path: String,
        val status: String,
        /** Git blob SHA-1 of the new content, when the file still exists. */
        val sha: String?,
        val previousPath: String?
    ) {
        val isRemoved get() = status == "removed"

        /** Moved from [previousPath]. A "copied" file also has one, but its source stays. */
        val isRenamed get() = status == "renamed"
    }

    internal sealed interface CompareResult {
        data object UpToDate : CompareResult
        data class Changes(val files: List<ChangedFile>) : CompareResult

        /** The changes cannot be applied file by file (unknown base, history rewritten, too many files). */
        data class NeedsArchive(val reason: String) : CompareResult
    }

    private class CancelledException : IOException("Download cancelled")

    internal class NoSpaceException(message: String) : IOException(message)

    private enum class Outcome { INSTALLED, UPDATED, UP_TO_DATE }

    companion object {
        private const val TAG = "GOTO-KCCP"
        private const val BUFFER_SIZE = 32768
        private const val CONCURRENCY = 4
        private const val PROGRESS_INTERVAL_MS = 100L
        private const val SPEED_INTERVAL_MS = 500L

        // GitHub's compare API lists at most 300 files.
        internal const val COMPARE_FILE_LIMIT = 300

        private const val MAX_API_BYTES = 32L * 1024 * 1024
        private const val MAX_SINGLE_FILE_BYTES = 64L * 1024 * 1024
        private const val MAX_ARCHIVE_BYTES = 4L * 1024 * 1024 * 1024
        private const val SPACE_MARGIN_BYTES = 64L * 1024 * 1024
        private const val DISK_BLOCK = 4096L

        internal const val COMMIT_FILE_NAME = ".kccp-commit"

        // Where earlier versions kept the installed commit.
        private const val META_PREFS = "patch_meta"
        private const val LEGACY_COMMIT_KEY = "commit"

        private val LANGUAGES = listOf(KenPatcher.PatchLanguage.EN, KenPatcher.PatchLanguage.ID)
        private val COMMIT_SHA = Regex("[0-9a-f]{40}")

        // Every task already runs on the one worker below, which is what keeps installs,
        // swaps and recovery apart; the lock only guards these helpers when called directly,
        // as the tests do.
        private val installLock = Any()

        // One worker for the process, which keeps installs, swaps, deletes and recovery apart.
        private val workerExecutor = Executors.newSingleThreadExecutor()

        /**
         * The downloader of the process: a download can outlive the settings screen that
         * started it (it may run as a job), and a new screen picks up its progress.
         */
        @JvmStatic
        val shared: KccpPatchDownloader by lazy { KccpPatchDownloader() }

        private fun createDefaultHttpClient(): OkHttpClient {
            // Shares the app client's connection pool; the timeouts keep a stalled
            // transfer from hanging the progress dialog forever.
            return KcUtils.getSharedHttpClient().newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        internal fun repoFor(language: KenPatcher.PatchLanguage) = Repo(
            symbol = KcEnUtils.getLanguageSymbol(language),
            githubPath = KcEnUtils.getGitHubNameRootPath(language).trimEnd('/'),
            title = KcEnUtils.getPatchTitle(language),
            rootName = KcEnUtils.getRootPath(language).trimEnd('/'),
            infoFileName = KcEnUtils.getInfoFileName(language)
        )

        /** The commit the install in [layout] came from, or null when unknown or not installed. */
        internal fun readInstalledCommit(layout: Layout): String? {
            if (!isInstalled(layout)) return null
            return try {
                layout.commitFile.takeIf { it.isFile }?.readText()?.trim()?.takeIf { COMMIT_SHA.matches(it) }
            } catch (_: IOException) {
                null
            }
        }

        /** Records [sha] as the commit of the install in [dir], replacing the file in one step. */
        internal fun writeCommit(dir: File, sha: String) {
            val temp = File(dir, "$COMMIT_FILE_NAME.tmp")
            temp.writeText(sha)
            if (!temp.renameTo(File(dir, COMMIT_FILE_NAME))) {
                temp.delete()
                throw IOException("Failed to record the installed commit")
            }
        }

        /**
         * Whether [e] comes from a full disk: the checks before a large write, or the disk
         * filling up during one (a write failing with ENOSPC).
         */
        internal fun isOutOfSpace(e: Throwable): Boolean =
            generateSequence(e) { it.cause }.take(8).any {
                it is NoSpaceException || it.message?.contains("ENOSPC") == true
            }

        internal fun isInstalled(layout: Layout) = layout.infoFile.isFile

        /** A repository path that stays inside the folder it is resolved against. */
        internal fun isSafeRelativePath(path: String): Boolean {
            if (path.isEmpty() || path.startsWith("/") || path.contains('\\') || path.contains('\u0000')) {
                return false
            }
            return path.split('/').none { it.isEmpty() || it == "." || it == ".." }
        }

        internal fun resolveUnder(root: File, relPath: String): File? {
            if (!isSafeRelativePath(relPath)) return null
            return try {
                val prefix = root.canonicalPath.trimEnd(File.separatorChar) + File.separator
                File(root, relPath).takeIf { it.canonicalPath.startsWith(prefix) }
            } catch (_: IOException) {
                null
            }
        }

        /** Reads a GitHub compare response between the installed and the latest commit. */
        internal fun parseCompare(json: JsonObject): CompareResult {
            val status = json.get("status")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
            if (status == "identical") return CompareResult.UpToDate
            // "diverged" or "behind": the installed commit is not an ancestor any more, so
            // the listed files would be relative to some other commit.
            if (status != "ahead") return CompareResult.NeedsArchive("compare status '$status'")
            val files = json.get("files")?.takeIf { it.isJsonArray }?.asJsonArray
                ?: return CompareResult.NeedsArchive("no file list")
            if (files.size() >= COMPARE_FILE_LIMIT) {
                return CompareResult.NeedsArchive("${files.size()} changed files")
            }
            val changes = files.map { element ->
                val file = element.asJsonObject
                fun str(key: String) = file.get(key)?.takeIf { it.isJsonPrimitive }?.asString
                val path = str("filename").orEmpty()
                val previous = str("previous_filename")
                val change = ChangedFile(path, str("status").orEmpty(), str("sha"), previous)
                // Files that cannot be updated one by one would fail every run; the archive
                // install has its own checks.
                if (!isSafeRelativePath(path) || (previous != null && !isSafeRelativePath(previous))) {
                    return CompareResult.NeedsArchive("unusable path '$path'")
                }
                if (!change.isRemoved && change.sha?.let { COMMIT_SHA.matches(it) } != true) {
                    return CompareResult.NeedsArchive("no blob hash for '$path'")
                }
                change
            }
            return CompareResult.Changes(changes)
        }

        /**
         * Room the extracted archive takes on disk. With tens of thousands of entries, the
         * block each folder takes and the partly used last block of each file add up to
         * about 100 MB, so every file is rounded up to whole blocks and every folder counts
         * as one.
         */
        internal fun extractedDiskSize(zip: File): Long = ZipFile(zip).use { zipFile ->
            val folders = HashSet<String>()
            var bytes = 0L
            for (entry in zipFile.entries()) {
                val name = entry.name.replace('\\', '/').trimEnd('/')
                var parent = if (entry.isDirectory) name else name.substringBeforeLast('/', "")
                while (parent.isNotEmpty() && folders.add(parent)) {
                    parent = parent.substringBeforeLast('/', "")
                }
                if (!entry.isDirectory) {
                    bytes += (entry.size.coerceAtLeast(0L) + DISK_BLOCK - 1) / DISK_BLOCK * DISK_BLOCK
                }
            }
            bytes + folders.size * DISK_BLOCK
        }

        /**
         * Extracts a GitHub archive into [targetDir], dropping the archive's top folder
         * (`<repo>-<commit>/`). Returns the number of files written.
         */
        internal fun extractArchive(
            zip: File,
            targetDir: File,
            isCancelled: () -> Boolean = { false },
            onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
        ): Int {
            targetDir.mkdirs()
            ZipFile(zip).use { zipFile ->
                val entries = zipFile.entries().toList().filter { !it.isDirectory }
                val buffer = ByteArray(BUFFER_SIZE)
                var done = 0
                for (entry in entries) {
                    if (isCancelled()) throw CancelledException()
                    val name = entry.name.replace('\\', '/')
                    val relPath = name.substringAfter('/', "")
                    if (relPath.isEmpty()) continue
                    val out = resolveUnder(targetDir, relPath)
                        ?: throw IOException("Unsafe path in archive: $name")
                    out.parentFile?.mkdirs()
                    zipFile.getInputStream(entry).use { input ->
                        BufferedOutputStream(FileOutputStream(out), BUFFER_SIZE).use { output ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    done++
                    onProgress(done, entries.size)
                }
                return done
            }
        }

        /**
         * Replaces the install folder with the staging folder. The old install is moved aside
         * first and put back if the swap fails; [recoverInterruptedInstall] finishes or rolls
         * back a swap cut off by a process kill. The old install is left as the backup:
         * deleting about 1 GB takes a while, so the task removes it after reporting the
         * result, through [recoverInterruptedInstall].
         */
        internal fun swapIn(layout: Layout) {
            synchronized(installLock) {
                if (layout.backup.exists() && !layout.backup.deleteRecursively()) {
                    throw IOException("Failed to clear ${layout.backup.name}")
                }
                if (layout.root.exists() && !layout.root.renameTo(layout.backup)) {
                    throw IOException("Failed to move the current install aside")
                }
                if (!layout.staging.renameTo(layout.root)) {
                    if (layout.backup.exists()) layout.backup.renameTo(layout.root)
                    throw IOException("Failed to move the new install into place")
                }
            }
        }

        /**
         * Deletes the install in [layout]. The commit and version files go first, so a delete
         * cut off by a process kill leaves a folder that no longer counts as installed.
         */
        internal fun deleteInstall(layout: Layout) {
            synchronized(installLock) {
                layout.commitFile.delete()
                layout.infoFile.delete()
                layout.root.deleteRecursively()
            }
        }

        /**
         * Cleans up after a task that did not finish (cancelled, failed or killed), and a
         * delete cut off partway: a folder without its version file is no install any more.
         */
        internal fun recoverInterruptedInstall(layout: Layout) {
            synchronized(installLock) {
                if (layout.backup.exists()) {
                    // Without an install the swap was cut off between its two renames.
                    if (!layout.root.exists()) {
                        layout.backup.renameTo(layout.root)
                    } else {
                        layout.backup.deleteRecursively()
                    }
                }
                if (layout.root.isDirectory && !layout.infoFile.isFile) {
                    layout.root.deleteRecursively()
                }
                layout.staging.deleteRecursively()
                layout.archive.delete()
                layout.legacyArchive.delete()
            }
        }

        /**
         * Moves downloaded files from [staging] into [root] and deletes removed ones.
         * Deletions go first, so a file renamed onto another's old path is not lost.
         * [lastPath] (the version file) is moved after every other file.
         */
        internal fun applyChanges(
            changes: List<ChangedFile>,
            staging: File,
            root: File,
            lastPath: String? = null
        ) {
            synchronized(installLock) {
                for (change in changes) {
                    val stale = when {
                        change.isRemoved -> change.path
                        change.isRenamed && change.previousPath != change.path -> change.previousPath
                        else -> null
                    } ?: continue
                    resolveUnder(root, stale)?.let { if (it.isFile) it.delete() }
                }
                for (change in changes.sortedBy { it.path == lastPath }) {
                    if (change.isRemoved) continue
                    val source = resolveUnder(staging, change.path)
                        ?: throw IOException("Unsafe path: ${change.path}")
                    val target = resolveUnder(root, change.path)
                        ?: throw IOException("Unsafe path: ${change.path}")
                    target.parentFile?.mkdirs()
                    // A rename replaces an existing file in one step, so the version file is
                    // never missing; only a folder in the way is removed first.
                    if (target.isDirectory && !target.deleteRecursively()) {
                        throw IOException("Failed to replace ${change.path}")
                    }
                    if (!source.renameTo(target)) {
                        source.copyTo(target, overwrite = true)
                        source.delete()
                    }
                }
            }
        }
    }

    /**
     * Receives the progress and the end of KCCP patch downloads, on the main thread. An
     * observer added while a download runs gets its current progress right away.
     */
    interface Observer {
        fun onDownloadStateChanged(state: PatchDownloadState)

        /**
         * The download ended, with [message] to show. Unless it succeeded, the install may be
         * gone (removed to make room), so it is worth checking again.
         */
        fun onDownloadFinished(message: String, result: Result)
    }

    enum class Result {
        /** The patch is installed and up to date. */
        SUCCEEDED,
        FAILED,
        CANCELLED
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // Main thread only.
    private val observers = mutableListOf<Observer>()
    private var currentState: PatchDownloadState = PatchDownloadState.Idle

    private val nextTaskToken = AtomicLong(0L)
    @Volatile
    private var activeToken = 0L
    @Volatile
    private var cancelledUpToToken = 0L
    @Volatile
    private var activePool: ExecutorService? = null
    // What the active download installs, and whether it runs as a user-initiated job.
    @Volatile
    private var activeRepo: Repo? = null
    @Volatile
    private var activeAsJob = false
    @Volatile
    private var appContext: Context? = null
    private val activeCalls: MutableSet<Call> = Collections.newSetFromMap(ConcurrentHashMap())

    // Guards the hand-off between cancelling and moving new files into place, and the
    // switch of the active download's fields between tasks.
    private val cancelLock = Any()
    @Volatile
    private var committingToken = 0L

    // GitHub builds the archive on the fly and can take a while to send the first bytes.
    private val archiveClient: OkHttpClient by lazy {
        httpClient.newBuilder().readTimeout(60, TimeUnit.SECONDS).build()
    }

    private fun isCancelled(token: Long) = cancelledUpToToken >= token

    private fun checkCancelled(token: Long) {
        if (isCancelled(token)) throw CancelledException()
    }

    /**
     * Ends the cancellable part of the task: from here on the new files are moved into
     * place, or the current install is removed to make room, which must not stop halfway.
     * The dialog drops its Cancel action to match, also for the progress that follows.
     */
    private fun beginCommit(token: Long, message: String) {
        synchronized(cancelLock) {
            checkCancelled(token)
            committingToken = token
        }
        showProgress(token, message, null, cancellable = false)
    }

    /** Whether a download runs and has not been cancelled. */
    fun isDownloadActive(): Boolean {
        val token = activeToken
        return token != 0L && !isCancelled(token)
    }

    /** Main thread. */
    fun addObserver(observer: Observer) {
        observers.add(observer)
        if (isDownloadActive()) observer.onDownloadStateChanged(currentState)
    }

    /**
     * Main thread. A download that runs in the app, not as a job, is cancelled once its last
     * observer is gone: nothing shows its progress any more, and the app may be frozen soon.
     */
    fun removeObserver(observer: Observer) {
        observers.remove(observer)
        if (observers.isEmpty() && !activeAsJob) cancelDownload()
    }

    /**
     * Installs the patch of the language the settings sheet points at, or updates it, and
     * reports to the observers. Ignored while another download runs (e.g. a start-up notice
     * answered late): its progress is already shown.
     *
     * From Android 14 the download runs as a user-initiated data transfer job, which goes on
     * with its own notification when the app is left; the app must be visible when this is
     * called. Before that it runs in the app. Main thread.
     */
    fun startUpdate(context: Context) {
        if (isDownloadActive()) return
        val appContext = context.applicationContext
        val repo = repoFor(KcEnUtils.getTargetLanguage())
        val token = nextTaskToken.incrementAndGet()
        // The job starts on the main thread too, so not before the fields below are set.
        val asJob = KccpPatchJobService.schedule(appContext)
        synchronized(cancelLock) {
            activeToken = token
            activeRepo = repo
            activeAsJob = asJob
            this.appContext = appContext
        }
        showProgress(token, appContext.getString(R.string.patch_download_checking), null)
        if (!asJob) runTask(appContext, token, repo) {}
    }

    /**
     * Runs the download a job was scheduled for, calling [onEnd] once it has ended. False when
     * there is none to run, e.g. it was cancelled before the job started, or the process was
     * restarted in between.
     */
    internal fun runJob(onEnd: () -> Unit): Boolean {
        val token: Long
        val repo: Repo
        val context: Context
        synchronized(cancelLock) {
            token = activeToken
            repo = activeRepo ?: return false
            context = appContext ?: return false
            if (token == 0L || !activeAsJob || isCancelled(token)) return false
        }
        runTask(context, token, repo, onEnd)
        return true
    }

    private fun runTask(appContext: Context, token: Long, repo: Repo, onEnd: () -> Unit) {
        workerExecutor.execute {
            val layout = Layout(File(KcUtils.getAppCacheFileDir(appContext, "")), repo)
            try {
                checkCancelled(token)
                recoverInterruptedInstall(layout)
                migrateLegacyCommit(appContext)
                val outcome = runUpdate(token, appContext, repo, layout)
                val message = when (outcome) {
                    Outcome.INSTALLED -> R.string.en_install_done_notification
                    Outcome.UPDATED -> R.string.patch_download_updated
                    Outcome.UP_TO_DATE -> R.string.setting_latest_version
                }
                finish(token, appContext.getString(message), Result.SUCCEEDED)
            } catch (e: Exception) {
                if (isCancelled(token)) {
                    Log.i(TAG, "KCCP patch download cancelled")
                } else {
                    Log.e(TAG, "KCCP patch download failed: ${KcUtils.getStringFromException(e)}")
                    val message = if (isOutOfSpace(e)) R.string.patch_download_no_space else R.string.patch_download_failed
                    finish(token, appContext.getString(message), Result.FAILED)
                }
            } finally {
                // Done for the user once the result is out, so a new request is taken right
                // away; it runs on this worker after the cleanup below. A newer task may have
                // started meanwhile; its fields stay.
                synchronized(cancelLock) {
                    if (activeToken == token) {
                        activeToken = 0L
                        activeRepo = null
                        activeAsJob = false
                    }
                }
                // Drops the old install left by a swap, and the staging folder and archive
                // of a failed or cancelled run. Before onEnd, so a job keeps the app running
                // while it deletes about 1 GB.
                recoverInterruptedInstall(layout)
                onEnd()
            }
        }
    }

    /** Cancels the running download, unless its new files are already being moved into place. */
    fun cancelDownload() {
        stop(Result.CANCELLED, cancelJob = true)
    }

    /**
     * Stops the download of a job the system stopped: by the user from the task manager
     * ([byUser]), or for a reason of its own such as a lost connection.
     */
    internal fun stopForJob(byUser: Boolean) {
        stop(if (byUser) Result.CANCELLED else Result.FAILED, cancelJob = false)
    }

    private fun stop(result: Result, cancelJob: Boolean) {
        val asJob: Boolean
        val context: Context?
        synchronized(cancelLock) {
            val token = activeToken
            if (token == 0L || isCancelled(token) || committingToken == token) return
            cancelledUpToToken = token
            asJob = activeAsJob
            context = appContext
        }
        activeCalls.forEach { it.cancel() }
        // shutdownNow() only drains the queued downloads; cancel them as well, or the worker
        // waiting on one of them in updateFiles would wait forever.
        activePool?.shutdownNow()?.forEach { (it as? Future<*>)?.cancel(false) }
        if (context == null) return
        // Also when the job has not started yet. A running one ends with the task.
        if (cancelJob && asJob) KccpPatchJobService.cancel(context)
        val message = context.getString(
            if (result == Result.CANCELLED) R.string.patch_download_cancelled else R.string.patch_download_failed
        )
        mainHandler.post { report(message, result) }
    }

    /** Reports the end of the task [token], unless it was stopped, which reports by itself. */
    private fun finish(token: Long, message: String, result: Result) {
        post(token) { report(message, result) }
    }

    private fun report(message: String, result: Result) {
        currentState = PatchDownloadState.Idle
        observers.toList().forEach {
            it.onDownloadStateChanged(PatchDownloadState.Idle)
            it.onDownloadFinished(message, result)
        }
    }

    /** Patch name of the active download, for its notification. */
    internal fun activeTitle(): String? = activeRepo?.title

    /** Deletes the patch of the language the settings sheet points at, then runs [onDeleted] on the main thread. */
    fun deletePatch(host: SettingsStatusHost, onDeleted: Runnable) {
        val appContext = host.getContext()?.applicationContext ?: return
        val repo = repoFor(KcEnUtils.getTargetLanguage())
        // On the worker, so it never runs in the middle of a download of the same patch.
        workerExecutor.execute {
            val layout = Layout(File(KcUtils.getAppCacheFileDir(appContext, "")), repo)
            recoverInterruptedInstall(layout)
            deleteInstall(layout)
            mainHandler.post {
                host.showPatchMessage(appContext.getString(R.string.patch_deleted))
                onDeleted.run()
            }
        }
    }

    /**
     * Cleans up what an interrupted install of any language left behind (staging folder,
     * partial archive, an install moved aside by a cut-off swap). Runs on the worker, so it
     * never meets a running download; the returned future completes once it is done.
     */
    fun recoverInterruptedInstalls(context: Context): Future<*> {
        val appContext = context.applicationContext
        return workerExecutor.submit {
            try {
                val parent = File(KcUtils.getAppCacheFileDir(appContext, ""))
                LANGUAGES.map(::repoFor).forEach { recoverInterruptedInstall(Layout(parent, it)) }
            } catch (e: Exception) {
                Log.w(TAG, "Patch install recovery failed: ${e.message}")
            }
        }
    }

    private fun runUpdate(
        token: Long,
        context: Context,
        repo: Repo,
        layout: Layout
    ): Outcome {
        val head = fetchLatestCommit(token, repo)
        val base = readInstalledCommit(layout)

        return if (base == null) {
            // An install without a recorded commit (e.g. from an earlier version) is refreshed.
            val existed = isInstalled(layout)
            installArchive(token, context, repo, head, layout)
            if (existed) Outcome.UPDATED else Outcome.INSTALLED
        } else if (!versionMatchesCommit(token, repo, base, layout)) {
            // The changes since [base] would not bring these files to [head].
            Log.i(TAG, "Reinstalling ${repo.symbol} patch: its files are not at the recorded commit")
            installArchive(token, context, repo, head, layout)
            // The patch was there before, so for the user it was updated.
            Outcome.UPDATED
        } else if (base == head) {
            // Nothing to compare; saves an API call.
            Outcome.UP_TO_DATE
        } else {
            when (val compare = fetchCompare(token, repo, base, head)) {
                CompareResult.UpToDate -> Outcome.UP_TO_DATE
                is CompareResult.Changes -> {
                    updateFiles(token, context, repo, head, compare.files, layout)
                    Outcome.UPDATED
                }
                is CompareResult.NeedsArchive -> {
                    Log.i(TAG, "Reinstalling ${repo.symbol} patch: ${compare.reason}")
                    installArchive(token, context, repo, head, layout)
                    Outcome.UPDATED
                }
            }
        }
    }

    /**
     * Whether the installed version file matches the one at [commit], the recorded commit.
     * A recorded commit can run ahead of the files: a commit migrated from an earlier version
     * was read after its download. When the version at [commit] cannot be fetched, the
     * commit is trusted.
     */
    private fun versionMatchesCommit(token: Long, repo: Repo, commit: String, layout: Layout): Boolean {
        val installed = KcUtils.readJsonObjectFromFile(File(layout.root, repo.infoFileName).path)
            ?.get("version")?.takeIf { it.isJsonPrimitive }?.asString ?: return false
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/${repo.githubPath}/$commit/${repo.infoFileName}")
            .build()
        val published = try {
            execute(token, request) { response ->
                if (!response.isSuccessful) return@execute null
                val json = JsonParser.parseString(readText(token, response))
                if (!json.isJsonObject) return@execute null
                json.asJsonObject.get("version")?.takeIf { it.isJsonPrimitive }?.asString
            }
        } catch (e: CancelledException) {
            throw e
        } catch (e: Exception) {
            checkCancelled(token)
            null
        }
        return published == null || published == installed
    }

    private fun installArchive(
        token: Long,
        context: Context,
        repo: Repo,
        head: String,
        layout: Layout
    ) {
        val downloadingText = context.getString(R.string.patch_download_archive)
        showProgress(token, downloadingText, null)
        val sizeEstimate = fetchSizeEstimate(token, repo)
        val parent = layout.root.parentFile ?: throw IOException("No parent folder")
        // The published size runs a little short of the actual archive.
        if (sizeEstimate > 0 && parent.usableSpace < sizeEstimate * 11 / 10 + SPACE_MARGIN_BYTES) {
            throw NoSpaceException("Archive needs about $sizeEstimate bytes, ${parent.usableSpace} free")
        }

        // Pinned to [head], so the recorded commit always matches the installed files.
        val url = "https://github.com/${repo.githubPath}/archive/$head.zip"
        val speed = SpeedMeter()
        downloadToFile(token, url, layout.archive, MAX_ARCHIVE_BYTES, archiveClient) { transferred, contentLength ->
            val total = if (contentLength > 0) contentLength else sizeEstimate
            speed.update(transferred)?.let { bytesPerSec ->
                showProgress(token, archiveProgressText(transferred, total, contentLength > 0, bytesPerSec),
                    if (total > 0) (transferred.toFloat() / total).coerceAtMost(1f) else null)
            }
        }

        val extractingText = context.getString(R.string.patch_download_extracting)
        showProgress(token, extractingText, 0f)
        layout.staging.deleteRecursively()
        ensureSpaceForExtraction(token, repo, layout, extractingText)
        var lastReport = 0L
        extractArchive(layout.archive, layout.staging, { isCancelled(token) }) { done, total ->
            val now = System.currentTimeMillis()
            if (now - lastReport >= PROGRESS_INTERVAL_MS || done == total) {
                lastReport = now
                showProgress(token, "$extractingText\n($done / $total)", done.toFloat() / total)
            }
        }
        layout.archive.delete()

        if (!File(layout.staging, repo.infoFileName).isFile) {
            throw IOException("Archive has no ${repo.infoFileName}")
        }
        File(layout.staging, ".nomedia").createNewFile()
        writeCommit(layout.staging, head)

        beginCommit(token, context.getString(R.string.patch_download_installing))
        swapIn(layout)
    }

    /**
     * The extracted files live next to the current install until the swap. When there is no
     * room for both, the current install goes first: it is being replaced anyway, it just
     * cannot be kept for a failed install. The run can no longer be cancelled from then on,
     * since a cancel would leave the user without the patch. When even its room would not
     * be enough, the install stays and the run fails for lack of space.
     */
    private fun ensureSpaceForExtraction(
        token: Long,
        repo: Repo,
        layout: Layout,
        message: String
    ) {
        val needed = extractedDiskSize(layout.archive) + SPACE_MARGIN_BYTES
        val parent = layout.root.parentFile ?: throw IOException("No parent folder")
        if (parent.usableSpace >= needed) return
        if (layout.root.exists()) {
            val installSize = layout.root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            if (parent.usableSpace + installSize < needed) {
                throw NoSpaceException("Extraction needs $needed bytes, ${parent.usableSpace} free besides the install")
            }
            beginCommit(token, message)
            Log.i(TAG, "Removing the current ${repo.symbol} patch to make room for the new one")
            deleteInstall(layout)
        }
        if (parent.usableSpace < needed) {
            throw NoSpaceException("Extraction needs $needed bytes, ${parent.usableSpace} free")
        }
    }

    private fun updateFiles(
        token: Long,
        context: Context,
        repo: Repo,
        head: String,
        changes: List<ChangedFile>,
        layout: Layout
    ) {
        layout.staging.deleteRecursively()
        layout.staging.mkdirs()
        val downloads = changes.filter { !it.isRemoved }
        val total = downloads.size
        val done = AtomicInteger(0)
        val lastReport = AtomicLong(0L)
        val firstError = AtomicReference<Exception?>(null)
        val rawBase = "https://raw.githubusercontent.com/${repo.githubPath}/$head/".toHttpUrl()

        val pool = Executors.newFixedThreadPool(CONCURRENCY)
        activePool = pool
        try {
            val futures = downloads.map { change ->
                pool.submit {
                    if (isCancelled(token) || firstError.get() != null) return@submit
                    try {
                        val target = resolveUnder(layout.staging, change.path)
                            ?: throw IOException("Unsafe path: ${change.path}")
                        val url = rawBase.newBuilder().addPathSegments(change.path).build().toString()
                        downloadToFile(token, url, target, MAX_SINGLE_FILE_BYTES)
                        val sha = change.sha
                        if (sha == null || !ChsManifest.verifyFileHash(target, sha, "git_sha")) {
                            throw IOException("Hash mismatch: ${change.path}")
                        }
                        val count = done.incrementAndGet()
                        val now = System.currentTimeMillis()
                        val last = lastReport.get()
                        if (count == total || (now - last >= PROGRESS_INTERVAL_MS && lastReport.compareAndSet(last, now))) {
                            val name = change.path.substringAfterLast('/')
                            showProgress(token, String.format(Locale.US, "%s (%d/%d)", name, count, total),
                                count.toFloat() / total)
                        }
                    } catch (e: Exception) {
                        if (!isCancelled(token)) firstError.compareAndSet(null, e)
                    }
                }
            }
            for (future in futures) {
                try {
                    future.get()
                } catch (_: Exception) {
                }
                if (isCancelled(token)) break
            }
        } finally {
            activePool = null
            pool.shutdownNow()
            // Downloads still running after a cancel or a failure would keep writing into
            // the staging folder after it is cleaned up. Cancel their calls (only this
            // task's are open now) and wait for them to end.
            activeCalls.forEach { it.cancel() }
            try {
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Patch file downloads did not stop in time")
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        checkCancelled(token)
        firstError.get()?.let { throw it }

        beginCommit(token, context.getString(R.string.patch_download_installing))
        applyChanges(changes, layout.staging, layout.root, repo.infoFileName)
        writeCommit(layout.root, head)
    }

    private fun fetchLatestCommit(token: Long, repo: Repo): String {
        val request = Request.Builder()
            .url("https://api.github.com/repos/${repo.githubPath}/commits/master")
            .header("Accept", "application/vnd.github.sha")
            .build()
        return execute(token, request) { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for the latest commit")
            val sha = readText(token, response).trim()
            if (!COMMIT_SHA.matches(sha)) throw IOException("Unexpected commit response")
            sha
        }
    }

    private fun fetchCompare(token: Long, repo: Repo, base: String, head: String): CompareResult {
        val request = Request.Builder()
            // The file list comes with the first page; one commit per page keeps the rest small.
            .url("https://api.github.com/repos/${repo.githubPath}/compare/$base...$head?per_page=1")
            .header("Accept", "application/vnd.github+json")
            .build()
        return execute(token, request) { response ->
            when {
                // The installed commit is unknown to the repository.
                response.code == 404 || response.code == 422 ->
                    CompareResult.NeedsArchive("compare returned HTTP ${response.code}")
                !response.isSuccessful -> throw IOException("HTTP ${response.code} for the change list")
                else -> {
                    val json = JsonParser.parseString(readText(token, response))
                    if (!json.isJsonObject) throw IOException("Unexpected compare response")
                    parseCompare(json.asJsonObject)
                }
            }
        }
    }

    /** Size of the archive as published in the patch info file, or 0 when unknown. */
    private fun fetchSizeEstimate(token: Long, repo: Repo): Long {
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/${repo.githubPath}/master/${repo.infoFileName}")
            .build()
        return try {
            execute(token, request) { response ->
                if (!response.isSuccessful) return@execute 0L
                val json = JsonParser.parseString(readText(token, response))
                val size = if (json.isJsonObject) json.asJsonObject.get("size") else null
                if (size != null && size.isJsonPrimitive) size.asLong else 0L
            }
        } catch (e: CancelledException) {
            throw e
        } catch (e: Exception) {
            checkCancelled(token)
            Log.w(TAG, "Patch size unavailable: ${e.message}")
            0L
        }
    }

    private fun downloadToFile(
        token: Long,
        url: String,
        dest: File,
        maxBytes: Long,
        client: OkHttpClient = httpClient,
        onProgress: (transferred: Long, contentLength: Long) -> Unit = { _, _ -> }
    ) {
        execute(token, Request.Builder().url(url).build(), client) { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $url")
            val body = response.body ?: throw IOException("Empty body: $url")
            val contentLength = body.contentLength()
            if (contentLength > maxBytes) throw IOException("Too large ($contentLength bytes): $url")
            dest.parentFile?.mkdirs()
            BufferedOutputStream(FileOutputStream(dest), BUFFER_SIZE).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var transferred = 0L
                    while (true) {
                        checkCancelled(token)
                        val read = input.read(buffer)
                        if (read == -1) break
                        transferred += read
                        if (transferred > maxBytes) throw IOException("Too large: $url")
                        output.write(buffer, 0, read)
                        onProgress(transferred, contentLength)
                    }
                }
            }
        }
    }

    private fun readText(token: Long, response: Response): String {
        val body = response.body ?: throw IOException("Empty body")
        if (body.contentLength() > MAX_API_BYTES) throw IOException("Response too large")
        val bytes = body.byteStream().use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                checkCancelled(token)
                val read = input.read(buffer)
                if (read == -1) break
                out.write(buffer, 0, read)
                if (out.size() > MAX_API_BYTES) throw IOException("Response too large")
            }
            out.toByteArray()
        }
        return String(bytes, Charsets.UTF_8)
    }

    /** Runs [request] so that [cancelDownload] can abort it. */
    private fun <T> execute(
        token: Long,
        request: Request,
        client: OkHttpClient = httpClient,
        block: (Response) -> T
    ): T {
        checkCancelled(token)
        val call = client.newCall(request)
        activeCalls.add(call)
        try {
            // A cancel between the check above and add() would miss this call.
            checkCancelled(token)
            return call.execute().use(block)
        } catch (e: IOException) {
            checkCancelled(token)
            throw e
        } finally {
            activeCalls.remove(call)
        }
    }

    private fun showProgress(
        token: Long,
        message: String,
        progress: Float?,
        cancellable: Boolean = true
    ) {
        val canCancel = cancellable && committingToken != token
        val onCancel: (() -> Unit)? = if (canCancel) ({ cancelDownload() }) else null
        val state = PatchDownloadState.Running(message, progress, null, onCancel)
        post(token) {
            currentState = state
            observers.toList().forEach { it.onDownloadStateChanged(state) }
        }
    }

    /** Runs [action] on the main thread unless the task has been cancelled by then. */
    private fun post(token: Long, action: () -> Unit) {
        mainHandler.post { if (!isCancelled(token)) action() }
    }

    private fun archiveProgressText(transferred: Long, total: Long, exact: Boolean, bytesPerSec: Double): String {
        val mb = 1024.0 * 1024.0
        val speedLine = if (total > 0 && bytesPerSec > 0) {
            String.format(Locale.US, "%.2f MB/s • ETA: %ds",
                bytesPerSec / mb, ((total - transferred).coerceAtLeast(0) / bytesPerSec).toInt())
        } else {
            String.format(Locale.US, "%.2f MB/s", bytesPerSec / mb)
        }
        val sizeLine = when {
            total <= 0 -> String.format(Locale.US, "%.2f MB", transferred / mb)
            exact -> String.format(Locale.US, "%.2f / %.2f MB", transferred / mb, total / mb)
            else -> String.format(Locale.US, "%.2f / ~%.2f MB", transferred / mb, total / mb)
        }
        return "$sizeLine\n$speedLine"
    }

    /**
     * Throughput over the last [SPEED_INTERVAL_MS]; [update] returns a new value once per
     * interval. Timing starts with the first bytes, so the wait for the server is not
     * counted as a slow transfer.
     */
    private class SpeedMeter {
        private var lastTime = 0L
        private var lastBytes = 0L

        fun update(transferred: Long): Double? {
            val now = System.currentTimeMillis()
            if (lastTime == 0L) {
                lastTime = now
                lastBytes = transferred
                return null
            }
            val elapsed = now - lastTime
            if (elapsed < SPEED_INTERVAL_MS) return null
            val speed = (transferred - lastBytes) * 1000.0 / elapsed
            lastTime = now
            lastBytes = transferred
            return speed
        }
    }

    /**
     * The previous downloader kept a single commit for every language in preferences. Move it
     * into the one installed language it can belong to; with zero or several installs it is
     * ambiguous and dropped, so the next update reinstalls from the archive. A commit that
     * runs ahead of the files is caught by [versionMatchesCommit].
     */
    private fun migrateLegacyCommit(context: Context) {
        val prefs = context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        val legacy = prefs.getString(LEGACY_COMMIT_KEY, null) ?: return
        val parent = File(KcUtils.getAppCacheFileDir(context, ""))
        val owner = LANGUAGES.map(::repoFor)
            .map { Layout(parent, it) }
            .filter { isInstalled(it) }
            .singleOrNull()
        if (owner != null && !owner.commitFile.exists() && COMMIT_SHA.matches(legacy)) {
            try {
                writeCommit(owner.root, legacy)
            } catch (e: IOException) {
                Log.w(TAG, "Could not move the legacy commit: ${e.message}")
            }
        }
        prefs.edit().remove(LEGACY_COMMIT_KEY).apply()
    }
}
