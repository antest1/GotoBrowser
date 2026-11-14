package com.antest1.gotobrowser.Helpers.chs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ChsDataRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `loadFromDirectory loads text dictionaries, regex rules, and 13 API tables`() {
        val root = tempFolder.newFolder("chs_patch")
        val textDir = File(root, "text/text").apply { mkdirs() }
        val regexDir = File(root, "text/text_regex").apply { mkdirs() }
        val apiDir = File(root, "text/api").apply { mkdirs() }

        // 1. Write dictionary file with comments and dummy entry (synthetic non-copyright strings)
        File(textDir, "01_ui.json").writeText(
            """
            {
              // top-level comment
              "raw_ui_title": "translated_ui_title",
              "dummy": "forNoComma",
            }
            """.trimIndent()
        )

        // 2. Write regex file
        File(regexDir, "01_regex.json").writeText(
            """
            {
              "^Unit_(\\d+)$": "Unit #$1",
              "[broken": "skip"
            }
            """.trimIndent()
        )

        // 3. Write API tables
        File(apiDir, "start2.api_mst_ship.json").writeText(
            """[{"api_id": 1, "api_name": "Ship_01_CHS", "api_getmes": "Line_A_CHS<br>Line_B_CHS"}]"""
        )
        File(apiDir, "start2.api_mst_mission.json").writeText(
            """[{"api_id": 1, "api_disp_no": "01", "api_name": "Mission_01_CHS", "api_details": "Mission_Detail_1_CHS<br>Mission_Detail_2_CHS"}]"""
        )
        File(apiDir, "start2.api_mst_mapinfo.json").writeText(
            """[{"api_id": 11, "api_maparea_id": 1, "api_no": 1, "api_name": "Map_11_CHS", "api_opetext": "Ope_11_CHS", "api_infotext": "Map_Info_1_CHS<br>Map_Info_2_CHS"}]"""
        )
        File(apiDir, "questlist.json").writeText(
            """[{"api_no": 201, "api_title": "Quest_201_Title_CHS", "api_detail": "Quest_201_Detail_CHS"}]"""
        )

        val bundle = ChsDataRepository.loadFromDirectory(root)

        // Verify static dictionary
        assertEquals("translated_ui_title", bundle.textDict["raw_ui_title"])

        // Verify regex loading
        assertEquals(1, bundle.regexRules.size)
        assertEquals("""^Unit_(\d+)$""", bundle.regexRules[0].first)

        // Verify API table indexing
        val shipEntry = bundle.apiTables["ship"]?.getAsJsonObject("1")
        assertNotNull(shipEntry)
        assertEquals("Ship_01_CHS", shipEntry!!.get("api_name").asString)

        val missionByDisp = bundle.apiTables["mission"]?.getAsJsonObject("01")
        val missionByInt = bundle.apiTables["mission"]?.getAsJsonObject("1")
        assertNotNull(missionByDisp)
        assertNotNull(missionByInt)
        assertEquals("Mission_01_CHS", missionByInt!!.get("api_name").asString)

        val mapById = bundle.apiTables["mapinfo"]?.getAsJsonObject("11")
        val mapByKey = bundle.apiTables["mapinfo"]?.getAsJsonObject("1_1")
        assertNotNull(mapById)
        assertNotNull(mapByKey)
        assertEquals("Map_11_CHS", mapByKey!!.get("api_name").asString)

        val questEntry = bundle.apiTables["questlist"]?.getAsJsonObject("201")
        assertNotNull(questEntry)
        assertEquals("Quest_201_Title_CHS", questEntry!!.get("api_title").asString)
        assertTrue(bundle.apiTables.keys.containsAll(listOf(
            "ship", "mission", "furniture", "mapinfo", "slotitem",
            "slotitemEquiptype", "stype", "useitem", "payitem",
            "pictureBookShip", "pictureBookSlot", "questlist", "flavorInfo"
        )))
    }
}
