package com.antest1.gotobrowser.Helpers

import android.app.Application
import com.antest1.gotobrowser.Helpers.KccpPatchDownloader.ChangedFile
import com.antest1.gotobrowser.Helpers.KccpPatchDownloader.CompareResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class KccpPatchDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val enRepo = KccpPatchDownloader.repoFor(KenPatcher.PatchLanguage.EN)

    private fun compareJson(status: String, vararg files: JsonObject) = JsonObject().apply {
        addProperty("status", status)
        add("files", JsonArray().apply { files.forEach { add(it) } })
    }

    private fun fileJson(name: String, status: String, sha: String? = "a".repeat(40), previous: String? = null) =
        JsonObject().apply {
            addProperty("filename", name)
            addProperty("status", status)
            sha?.let { addProperty("sha", it) }
            previous?.let { addProperty("previous_filename", it) }
        }

    @Test
    fun parseCompareReadsChangesWhenAhead() {
        val result = KccpPatchDownloader.parseCompare(
            compareJson(
                "ahead",
                fileJson("EN-patch/a.png", "modified"),
                fileJson("EN-patch/b.png", "renamed", previous = "EN-patch/old_b.png"),
                fileJson("EN-patch/c.png", "removed", sha = null)
            )
        )
        result as CompareResult.Changes
        assertEquals(3, result.files.size)
        assertEquals("EN-patch/old_b.png", result.files[1].previousPath)
        assertTrue(result.files[2].isRemoved)
    }

    @Test
    fun parseCompareFallsBackToArchiveWhenFilesCannotBeApplied() {
        assertEquals(CompareResult.UpToDate, KccpPatchDownloader.parseCompare(compareJson("identical")))
        assertTrue(KccpPatchDownloader.parseCompare(compareJson("diverged")) is CompareResult.NeedsArchive)
        assertTrue(KccpPatchDownloader.parseCompare(compareJson("behind")) is CompareResult.NeedsArchive)

        val capped = (0 until KccpPatchDownloader.COMPARE_FILE_LIMIT)
            .map { fileJson("EN-patch/$it.png", "modified") }
            .toTypedArray()
        assertTrue(KccpPatchDownloader.parseCompare(compareJson("ahead", *capped)) is CompareResult.NeedsArchive)
    }

    @Test
    fun parseCompareFallsBackToArchiveForFilesItCannotUpdate() {
        assertTrue(KccpPatchDownloader.parseCompare(compareJson("ahead", fileJson("../evil.js", "added")))
            is CompareResult.NeedsArchive)
        assertTrue(KccpPatchDownloader.parseCompare(compareJson("ahead", fileJson("a\\b.png", "added")))
            is CompareResult.NeedsArchive)
        assertTrue(KccpPatchDownloader.parseCompare(compareJson("ahead", fileJson("a.png", "modified", sha = null)))
            is CompareResult.NeedsArchive)
        // A removed file needs no hash.
        assertTrue(KccpPatchDownloader.parseCompare(compareJson("ahead", fileJson("a.png", "removed", sha = null)))
            is CompareResult.Changes)
    }

    @Test
    fun safeRelativePathRejectsTraversalAndAbsolutePaths() {
        assertTrue(KccpPatchDownloader.isSafeRelativePath("EN-patch/kcs2/img/a.png"))
        assertFalse(KccpPatchDownloader.isSafeRelativePath(""))
        assertFalse(KccpPatchDownloader.isSafeRelativePath("/etc/passwd"))
        assertFalse(KccpPatchDownloader.isSafeRelativePath("a/../../b"))
        assertFalse(KccpPatchDownloader.isSafeRelativePath("a//b"))
        assertFalse(KccpPatchDownloader.isSafeRelativePath("a\\b"))
    }

    private fun zipOf(file: File, entries: Map<String, String>) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

    @Test
    fun extractArchiveDropsTheTopFolder() {
        val zip = tempFolder.newFile("patch.zip")
        zipOf(zip, mapOf(
            "Repo-0123/EN-patch.mod.json" to "{}",
            "Repo-0123/EN-patch/kcs2/a.txt" to "a"
        ))
        val target = File(tempFolder.root, "staging")
        var lastProgress = 0 to 0
        val count = KccpPatchDownloader.extractArchive(zip, target) { done, total -> lastProgress = done to total }

        assertEquals(2, count)
        assertEquals(2 to 2, lastProgress)
        assertEquals("{}", File(target, "EN-patch.mod.json").readText())
        assertEquals("a", File(target, "EN-patch/kcs2/a.txt").readText())
        // Two files of a block each, plus the top folder and EN-patch/ and EN-patch/kcs2/.
        assertEquals(5 * 4096L, KccpPatchDownloader.extractedDiskSize(zip))
    }

    @Test
    fun extractArchiveRejectsZipSlip() {
        val zip = tempFolder.newFile("evil.zip")
        zipOf(zip, mapOf("Repo-0123/../../evil.txt" to "x"))
        try {
            KccpPatchDownloader.extractArchive(zip, File(tempFolder.root, "staging"))
            fail("Expected an IOException")
        } catch (_: IOException) {
        }
        assertFalse(File(tempFolder.root.parentFile, "evil.txt").exists())
    }

    @Test
    fun swapInReplacesTheInstallAndLeavesTheBackupForRecovery() {
        val layout = KccpPatchDownloader.Layout(tempFolder.root, enRepo)
        File(layout.root, "old.txt").apply { parentFile?.mkdirs(); writeText("old") }
        File(layout.staging, "new.txt").apply { parentFile?.mkdirs(); writeText("new") }
        File(layout.staging, enRepo.infoFileName).writeText("{}")

        KccpPatchDownloader.swapIn(layout)

        assertTrue(File(layout.root, "new.txt").isFile)
        assertFalse(File(layout.root, "old.txt").exists())
        assertFalse(layout.staging.exists())
        assertTrue(File(layout.backup, "old.txt").isFile)

        KccpPatchDownloader.recoverInterruptedInstall(layout)

        assertTrue(File(layout.root, "new.txt").isFile)
        assertFalse(layout.backup.exists())
    }

    @Test
    fun recoveryRestoresAnInstallMovedAsideByAnInterruptedSwap() {
        val layout = KccpPatchDownloader.Layout(tempFolder.root, enRepo)
        File(layout.backup, "old.txt").apply { parentFile?.mkdirs(); writeText("old") }
        File(layout.backup, enRepo.infoFileName).writeText("{}")
        File(layout.staging, "partial.txt").apply { parentFile?.mkdirs(); writeText("partial") }
        layout.archive.writeText("zip")
        layout.legacyArchive.writeText("zip")

        KccpPatchDownloader.recoverInterruptedInstall(layout)

        assertEquals("old", File(layout.root, "old.txt").readText())
        assertFalse(layout.backup.exists())
        assertFalse(layout.staging.exists())
        assertFalse(layout.archive.exists())
        assertFalse(layout.legacyArchive.exists())
    }

    @Test
    fun recoveryDropsABackupLeftAfterACompletedSwap() {
        val layout = KccpPatchDownloader.Layout(tempFolder.root, enRepo)
        File(layout.root, "new.txt").apply { parentFile?.mkdirs(); writeText("new") }
        File(layout.root, enRepo.infoFileName).writeText("{}")
        File(layout.backup, "old.txt").apply { parentFile?.mkdirs(); writeText("old") }

        KccpPatchDownloader.recoverInterruptedInstall(layout)

        assertEquals("new", File(layout.root, "new.txt").readText())
        assertFalse(layout.backup.exists())
    }

    @Test
    fun applyChangesMovesNewFilesAndDeletesStaleOnes() {
        val root = tempFolder.newFolder("root")
        val staging = tempFolder.newFolder("staging")
        File(root, "keep.txt").writeText("keep")
        File(root, "removed.txt").writeText("removed")
        File(root, "renamed_from.txt").writeText("old name")
        File(root, "modified.txt").writeText("v1")
        // a.txt is renamed to b.txt while a new a.txt takes its place.
        File(root, "a.txt").writeText("old a")
        File(staging, "modified.txt").writeText("v2")
        File(staging, "renamed_to.txt").writeText("new name")
        File(staging, "dir/added.txt").apply { parentFile?.mkdirs(); writeText("added") }
        File(staging, "a.txt").writeText("new a")
        File(staging, "b.txt").writeText("old a")

        KccpPatchDownloader.applyChanges(
            listOf(
                ChangedFile("removed.txt", "removed", null, null),
                ChangedFile("renamed_to.txt", "renamed", "x", "renamed_from.txt"),
                ChangedFile("modified.txt", "modified", "x", null),
                ChangedFile("dir/added.txt", "added", "x", null),
                ChangedFile("a.txt", "added", "x", null),
                ChangedFile("b.txt", "renamed", "x", "a.txt")
            ),
            staging, root
        )

        assertEquals("keep", File(root, "keep.txt").readText())
        assertFalse(File(root, "removed.txt").exists())
        assertFalse(File(root, "renamed_from.txt").exists())
        assertEquals("new name", File(root, "renamed_to.txt").readText())
        assertEquals("v2", File(root, "modified.txt").readText())
        assertEquals("added", File(root, "dir/added.txt").readText())
        assertEquals("new a", File(root, "a.txt").readText())
        assertEquals("old a", File(root, "b.txt").readText())
    }

    @Test
    fun applyChangesMovesTheVersionFileLast() {
        val root = tempFolder.newFolder("root")
        val staging = tempFolder.newFolder("staging")
        File(root, "EN-patch.mod.json").writeText("old")
        File(staging, "EN-patch.mod.json").writeText("new")
        // No staged copy, so moving this file fails.
        val changes = listOf(
            ChangedFile("EN-patch.mod.json", "modified", "x", null),
            ChangedFile("EN-patch/a.png", "modified", "x", null)
        )

        try {
            KccpPatchDownloader.applyChanges(changes, staging, root, "EN-patch.mod.json")
            fail("Expected the missing staged file to fail")
        } catch (_: Exception) {
        }

        assertEquals("old", File(root, "EN-patch.mod.json").readText())
    }

    @Test
    fun applyChangesKeepsTheSourceOfACopiedFile() {
        val root = tempFolder.newFolder("root")
        val staging = tempFolder.newFolder("staging")
        File(root, "source.txt").writeText("source")
        File(staging, "copy.txt").writeText("source")

        KccpPatchDownloader.applyChanges(
            listOf(ChangedFile("copy.txt", "copied", "x", "source.txt")),
            staging, root
        )

        assertEquals("source", File(root, "source.txt").readText())
        assertEquals("source", File(root, "copy.txt").readText())
    }

    @Test
    fun outOfSpaceCoversTheChecksAndFailedWrites() {
        assertTrue(KccpPatchDownloader.isOutOfSpace(KccpPatchDownloader.NoSpaceException("check")))
        assertTrue(KccpPatchDownloader.isOutOfSpace(
            IOException("write failed", IOException("write failed: ENOSPC (No space left on device)"))))
        assertFalse(KccpPatchDownloader.isOutOfSpace(IOException("HTTP 500")))
    }

    @Test
    fun installedCommitLivesInTheInstallFolder() {
        val repo = KccpPatchDownloader.repoFor(KenPatcher.PatchLanguage.EN)
        val layout = KccpPatchDownloader.Layout(tempFolder.root, repo)
        val sha = "0123456789abcdef0123456789abcdef01234567"
        layout.root.mkdirs()
        KccpPatchDownloader.writeCommit(layout.root, sha)
        // Without the version file the folder is not an install.
        assertNull(KccpPatchDownloader.readInstalledCommit(layout))

        File(layout.root, repo.infoFileName).writeText("{}")
        assertEquals(sha, KccpPatchDownloader.readInstalledCommit(layout))

        layout.commitFile.writeText("not a commit")
        assertNull(KccpPatchDownloader.readInstalledCommit(layout))
    }

    @Test
    fun recoveryRemovesAFolderLeftByAnInterruptedDelete() {
        val layout = KccpPatchDownloader.Layout(tempFolder.root, enRepo)
        File(layout.root, "EN-patch/a.png").apply { parentFile?.mkdirs(); writeText("a") }

        KccpPatchDownloader.recoverInterruptedInstall(layout)

        assertFalse(layout.root.exists())
    }

    @Test
    fun deleteInstallRemovesTheWholeFolder() {
        val repo = KccpPatchDownloader.repoFor(KenPatcher.PatchLanguage.EN)
        val layout = KccpPatchDownloader.Layout(tempFolder.root, repo)
        File(layout.root, "EN-patch/a.png").apply { parentFile?.mkdirs(); writeText("a") }
        File(layout.root, repo.infoFileName).writeText("{}")
        KccpPatchDownloader.writeCommit(layout.root, "0123456789abcdef0123456789abcdef01234567")

        KccpPatchDownloader.deleteInstall(layout)

        assertFalse(layout.root.exists())
        assertFalse(KccpPatchDownloader.isInstalled(layout))
    }

    @Test
    fun onlyANewerPublishedVersionIsAnUpdate() {
        assertTrue(KcEnUtils.isNewer("4.1.7", "4.1.6"))
        assertTrue(KcEnUtils.isNewer("4.2", "4.1.10"))
        assertFalse(KcEnUtils.isNewer("4.1.6", "4.1.6"))
        // A cached older file right after an update.
        assertFalse(KcEnUtils.isNewer("4.1.5", "4.1.6"))
        assertTrue(KcEnUtils.isNewer("4.1.6-beta", "4.1.6"))
    }

    @Test
    fun resolveUnderKeepsPathsInsideTheRoot() {
        val root = tempFolder.newFolder("root")
        assertEquals(File(root, "a/b.txt").canonicalPath, KccpPatchDownloader.resolveUnder(root, "a/b.txt")?.canonicalPath)
        assertNull(KccpPatchDownloader.resolveUnder(root, "../b.txt"))
    }
}
