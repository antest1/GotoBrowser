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
}
