package com.antest1.gotobrowser.Helpers.chs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ChsManifestTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `parseManifestJson and parseGithubTreeJson parse files and skip subtitles directory`() {
        val manifestRaw = """
            {
              "version": "2026.11.11",
              "files": [
                {"path": "text/version.json", "size": 25, "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"},
                {"path": "text/text/subtitles/jp.json", "size": 100, "sha256": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"},
                {"path": "cache/kcs2/img/port/port_main.hack.png", "size": 1024, "sha256": "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"}
              ]
            }
        """.trimIndent()

        val manifest = ChsManifest.parseManifestJson(manifestRaw)
        assertNotNull(manifest)
        assertEquals("2026.11.11", manifest!!.version)
        assertEquals("应自动过滤 text/text/subtitles/ 下的文件", 2, manifest.files.size)
        assertTrue(manifest.files[0].isTextFile)
        assertTrue(manifest.files[1].isCacheFile)

        val treeRaw = """
            {
              "sha": "1234567890abcdef",
              "tree": [
                {"path": "text/api/questlist.json", "type": "blob", "size": 512, "sha": "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391"},
                {"path": "cache", "type": "tree", "sha": "000000"}
              ]
            }
        """.trimIndent()

        val treeData = ChsManifest.parseGithubTreeJson(treeRaw)
        assertNotNull(treeData)
        assertEquals("1234567890", treeData!!.version)
        assertEquals(1, treeData.files.size)
        assertEquals("git_sha", treeData.files[0].hashType)
    }

    @Test
    fun `computeDiff supports incremental comparison and text-only download mode`() {
        val rootDir = tempFolder.newFolder("chs_local")
        val existingTextFile = File(rootDir, "text/version.json").apply {
            parentFile?.mkdirs()
            writeText("12345") // 5 bytes
        }
        val sha = ChsManifest.sha256Hex(existingTextFile)

        val remote = ChsManifest.ManifestData(
            version = "v2.0",
            files = listOf(
                ChsManifest.FileEntry("text/version.json", 5L, sha, "sha256"),
                ChsManifest.FileEntry("text/api/questlist.json", 200L, "new_hash", "sha256"),
                ChsManifest.FileEntry("cache/kcs2/img/port/port_main.hack.png", 4096L, "img_hash", "sha256")
            )
        )

        val localMap = mapOf("text/version.json" to sha)

        // 1. 差分计算：text/version.json 已最新，其余 2 个文件待下载
        val diff = ChsManifest.computeDiff(remote, rootDir, localMap)
        assertTrue(diff.hasUpdates)
        assertEquals(1, diff.upToDateCount)
        assertEquals(2, diff.toDownload.size)
        assertEquals(4296L, diff.totalDownloadBytes)

        // 2. 保存并重新读取本地清单
        val manifestFile = File(rootDir, ".local_manifest.json")
        ChsManifest.saveLocalManifestMap(manifestFile, "v2.0", localMap)
        val reloaded = ChsManifest.loadLocalManifestMap(manifestFile)
        assertEquals(sha, reloaded["text/version.json"])
        assertFalse(reloaded.containsKey("non_existent"))
    }
}
