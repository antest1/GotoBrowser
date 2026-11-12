package com.antest1.gotobrowser.Helpers.chs

import android.app.Application
import org.junit.Assert.assertFalse
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
        // Cancel should be idempotent and callable anytime
        downloader.cancelDownload()

        // Calling downloadEntriesIncrementally after cancellation should exit immediately
        val entries = listOf(
            ChsManifest.FileEntry("text/sample.json", 100L, "dummyhash", "sha256")
        )
        downloader.downloadEntriesIncrementally(
            baseUrl = "https://example.com/",
            entries = entries,
            rootDir = rootDir,
            localMap = localMap,
            manifestFile = manifestFile,
            remoteVersion = "test-ver"
        )

        // No files should be written because it was cancelled
        assertFalse(File(rootDir, "text/sample.json").exists())
        assertFalse(File(rootDir, "text/sample.json.part").exists())
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

        val localMap = mutableMapOf(
            "text/corrupt.txt" to ChsManifest.gitBlobSha1Hex(validFile)
        )

        val downloader = ChsDownloader()
        downloader.reconcileExtractedFilesWithRemote(remote, rootDir, localMap)

        assertFalse(localMap.containsKey("text/corrupt.txt"))
        assertFalse(localMap["text/valid.txt"].isNullOrEmpty())
    }
}
