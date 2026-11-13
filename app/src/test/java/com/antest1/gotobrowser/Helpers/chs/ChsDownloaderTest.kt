package com.antest1.gotobrowser.Helpers.chs

import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ChsDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCancelDownloadStopsExecution() {
        val rootDir = tempFolder.newFolder("chs_root")
        val manifestFile = File(rootDir, "manifest.local.json")
        val localMap = mutableMapOf<String, String>()

        val downloader = ChsDownloader()
        val token1 = downloader.beginDownloadTask()
        assertFalse(downloader.isTaskCancelled(token1))
        downloader.cancelDownload()
        assertTrue("Active task token should be marked cancelled", downloader.isTaskCancelled(token1))

        // Calling downloadEntriesIncrementally with cancelled token check should exit immediately
        val entries = listOf(
            ChsManifest.FileEntry("text/sample.json", 100L, "dummyhash", "sha256")
        )
        downloader.downloadEntriesIncrementally(
            baseUrl = "https://example.com/",
            entries = entries,
            rootDir = rootDir,
            localMap = localMap,
            manifestFile = manifestFile,
            remoteVersion = "test-ver",
            isCancelledCheck = { downloader.isTaskCancelled(token1) }
        )

        // No files should be written because it was cancelled
        assertFalse(File(rootDir, "text/sample.json").exists())
        assertFalse(File(rootDir, "text/sample.json.part").exists())

        // A subsequent task token must not be poisoned by the earlier cancelDownload()
        val token2 = downloader.beginDownloadTask()
        assertFalse("Subsequent task token must not be poisoned by prior cancel", downloader.isTaskCancelled(token2))
    }

    @Test
    fun testCommitStagingDirectoryReplacesTargetsAndRejectsEmptyStaging() {
        val rootDir = tempFolder.newFolder("chs_staging_root")
        val oldText = File(rootDir, "text/old.json").apply {
            parentFile?.mkdirs()
            writeText("old")
        }
        val oldHack = File(rootDir, "cache/old.hack").apply {
            parentFile?.mkdirs()
            writeText("old_hack")
        }

        // Empty staging directory must fail without touching existing rootDir contents
        val emptyStaging = tempFolder.newFolder("empty_staging")
        val downloader = ChsDownloader()
        var threw = false
        try {
            downloader.commitStagingDirectory(emptyStaging, rootDir)
        } catch (_: java.io.IOException) {
            threw = true
        }
        assertTrue("Empty staging directory should throw IOException", threw)
        assertTrue("Existing text/ must be preserved on empty staging failure", oldText.exists())
        assertTrue("Existing cache/ must be preserved on empty staging failure", oldHack.exists())

        // Valid staging directory atomically swaps text/ and cache/, removing untracked old files
        val validStaging = tempFolder.newFolder("valid_staging")
        File(validStaging, "text/new.json").apply {
            parentFile?.mkdirs()
            writeText("new")
        }
        File(validStaging, "cache/new.hack").apply {
            parentFile?.mkdirs()
            writeText("new_hack")
        }
        downloader.commitStagingDirectory(validStaging, rootDir)

        assertFalse("Old untracked text file should be gone after staging swap", oldText.exists())
        assertFalse("Old untracked cache file should be gone after staging swap", oldHack.exists())
        assertTrue(File(rootDir, "text/new.json").exists())
        assertTrue(File(rootDir, "cache/new.hack").exists())
    }

    @Test
    fun testReconcileExtractedFilesRejectsSameLengthCorruptedFiles() {
        val rootDir = tempFolder.newFolder("chs_extract")
        val validFile = File(rootDir, "text/valid.txt").apply {
            parentFile?.mkdirs()
            writeText("hello\n")
        }
        val corruptedFile = File(rootDir, "text/corrupt.txt").apply {
            parentFile?.mkdirs()
            writeText("world\n") // same 6-byte length as "hello\n", different git_sha
        }

        val remote = ChsManifest.ManifestData(
            version = "test",
            files = listOf(
                ChsManifest.FileEntry(
                    path = "text/valid.txt",
                    size = validFile.length(),
                    hash = ChsManifest.gitBlobSha1Hex(validFile),
                    hashType = "git_sha"
                ),
                ChsManifest.FileEntry(
                    path = "text/corrupt.txt",
                    size = corruptedFile.length(),
                    hash = ChsManifest.gitBlobSha1Hex(validFile),
                    hashType = "git_sha"
                )
            )
        )

        val outsideFile = File(rootDir.parentFile, "outside_do_not_delete.txt").apply { writeText("keep") }
        val localMap = mutableMapOf(
            "text/corrupt.txt" to ChsManifest.gitBlobSha1Hex(validFile),
            "text/old_stale.txt" to "stale_hash",
            "text/../../outside_do_not_delete.txt" to "evil_hash"
        )
        val staleFile = File(rootDir, "text/old_stale.txt").apply { writeText("stale") }

        val downloader = ChsDownloader()
        downloader.reconcileExtractedFilesWithRemote(remote, rootDir, localMap)

        assertFalse(localMap.containsKey("text/corrupt.txt"))
        assertFalse(localMap.containsKey("text/old_stale.txt"))
        assertFalse(localMap.containsKey("text/../../outside_do_not_delete.txt"))
        assertFalse("Stale file should be deleted from disk", staleFile.exists())
        assertTrue("Traversal target outside rootDir must NOT be deleted", outsideFile.exists())
        assertFalse(localMap["text/valid.txt"].isNullOrEmpty())
    }
}
