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

        // 2. Save and reload local manifest map.
        val manifestFile = File(rootDir, ".local_manifest.json")
        ChsManifest.saveLocalManifestMap(manifestFile, "v2.0", localMap)
        val reloaded = ChsManifest.loadLocalManifestMap(manifestFile)
        assertEquals(sha, reloaded["text/version.json"])
        assertFalse(reloaded.containsKey("non_existent"))

        // 3. Stale/deleted file detection (must reject traversal paths)
        val staleLocalMap = mapOf(
            "text/version.json" to sha,
            "text/old_removed.json" to "old_hash",
            "cache/kcs2/old_img.hack.png" to "old_img_hash",
            "text/../../escape.txt" to "bad_hash"
        )
        val diffWithDeletions = ChsManifest.computeDiff(remote, rootDir, staleLocalMap)
        assertTrue(diffWithDeletions.hasUpdates)
        assertEquals(2, diffWithDeletions.toDelete.size)
        assertTrue(diffWithDeletions.toDelete.contains("text/old_removed.json"))
        assertTrue(diffWithDeletions.toDelete.contains("cache/kcs2/old_img.hack.png"))
        assertFalse(diffWithDeletions.toDelete.contains("text/../../escape.txt"))
        assertEquals(null, ChsManifest.safeResolveUnderRoot(rootDir, "text/../../escape.txt"))
    }

    @Test
    fun `verifyFileHash supports both sha256 and git_sha blob hashes`() {
        val emptyFile = tempFolder.newFile("empty.txt").apply { writeBytes(ByteArray(0)) }
        assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", ChsManifest.gitBlobSha1Hex(emptyFile))
        assertTrue(ChsManifest.verifyFileHash(emptyFile, "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", "git_sha"))

        val helloFile = tempFolder.newFile("hello.txt").apply { writeText("hello\n") }
        assertEquals("ce013625030ba8dba906f756967f9e9ca394464a", ChsManifest.gitBlobSha1Hex(helloFile))
        assertTrue(ChsManifest.verifyFileHash(helloFile, "ce013625030ba8dba906f756967f9e9ca394464a", "git_sha"))
        assertFalse(ChsManifest.verifyFileHash(helloFile, "0000000000000000000000000000000000000000", "git_sha"))

        val sha256 = ChsManifest.sha256Hex(helloFile)
        assertTrue(ChsManifest.verifyFileHash(helloFile, sha256, "sha256"))
        assertFalse(ChsManifest.verifyFileHash(helloFile, sha256.replaceFirstChar { if (it == 'a') 'b' else 'a' }, "sha256"))
    }

    @Test
    fun `computeDiff avoids redundant download when migrating from git_sha to sha256`() {
        val rootDir = tempFolder.newFolder("chs_migrate")
        val textFile = File(rootDir, "text/sample.json").apply {
            parentFile?.mkdirs()
            writeText("{\"k\":\"v\"}")
        }
        val gitSha = ChsManifest.gitBlobSha1Hex(textFile)
        val sha256 = ChsManifest.sha256Hex(textFile)

        val remote = ChsManifest.ManifestData(
            version = "v2.1",
            files = listOf(ChsManifest.FileEntry("text/sample.json", textFile.length(), sha256, "sha256"))
        )
        val localMap = mapOf("text/sample.json" to gitSha)

        val diff = ChsManifest.computeDiff(remote, rootDir, localMap)
        assertFalse(diff.hasUpdates)
        assertEquals(1, diff.upToDateCount)
        assertTrue(diff.toDownload.isEmpty())
    }
}
