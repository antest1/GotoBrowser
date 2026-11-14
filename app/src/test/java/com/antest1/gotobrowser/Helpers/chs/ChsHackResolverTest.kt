package com.antest1.gotobrowser.Helpers.chs

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ChsHackResolverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `normalizeHackPath strips hack suffix and query parameters accurately`() {
        assertEquals(
            "/kcs2/img/port/port_main.png",
            ChsHackResolver.normalizeHackPath("/kcs2/img/port/port_main.hack.png?version=5.1.0")
        )
        assertEquals(
            "/kcs2/resources/font/A-OTF-UDShinGoPro-Regular.woff2",
            ChsHackResolver.normalizeHackPath("kcs2/resources/font/A-OTF-UDShinGoPro-Regular.hack.woff2")
        )
        assertEquals(
            "/kcs2/img/common/common_main.png",
            ChsHackResolver.normalizeHackPath("/kcs2/img/common/common_main.png?v=123")
        )
    }

    @Test
    fun `rebuildIndexFromDir indexes hack images and fonts and resolves requests without polluting original cache`() {
        val cacheDir = tempFolder.newFolder("cache")
        val portDir = File(cacheDir, "kcs2/img/port").apply { mkdirs() }
        val fontDir = File(cacheDir, "kcs2/resources/font").apply { mkdirs() }
        val maintDir = File(cacheDir, "kcs/html").apply { mkdirs() }

        val portPng = File(portDir, "port_main.hack.png").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val fontWoff2 = File(fontDir, "A-OTF-UDShinGoPro-Regular.hack.woff2").apply { writeBytes(byteArrayOf(5, 6, 7)) }
        val maintPng = File(maintDir, "maintenance.hack.png").apply { writeBytes(byteArrayOf(8, 9)) }

        ChsHackResolver.invalidateIndex()
        ChsHackResolver.rebuildIndexFromDir(cacheDir)

        // 1. Match standard game image path
        assertEquals(portPng.canonicalFile, ChsHackResolver.findHackFile("/kcs2/img/port/port_main.png?version=1.2")?.canonicalFile)

        // 2. Match font file (by full path or filename)
        assertEquals(
            fontWoff2.canonicalFile,
            ChsHackResolver.findHackFile("/kcs2/resources/font/A-OTF-UDShinGoPro-Regular.woff2")?.canonicalFile
        )
        assertEquals(
            fontWoff2.canonicalFile,
            ChsHackResolver.findHackFile("/any/custom/path/A-OTF-UDShinGoPro-Regular.woff2")?.canonicalFile
        )

        // 3. Match maintenance image path
        assertEquals(maintPng.canonicalFile, ChsHackResolver.findHackFile("/html/maintenance.png")?.canonicalFile)

        // 4. Unpatched asset returns null
        assertNull(ChsHackResolver.findHackFile("/kcs2/img/port/non_existent.png"))

        // 5. Invalidating index clears matches
        ChsHackResolver.invalidateIndex()
        assertNull(ChsHackResolver.findHackFile("/kcs2/img/port/port_main.png"))
    }
}
