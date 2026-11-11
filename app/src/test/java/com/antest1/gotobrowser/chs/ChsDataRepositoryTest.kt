package com.antest1.gotobrowser.chs

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
    fun `loadFromDirectory loads text dictionaries, regex rules, 13 API tables, and pre-populates ja_maps`() {
        val root = tempFolder.newFolder("chs_patch")
        val textDir = File(root, "text/text").apply { mkdirs() }
        val regexDir = File(root, "text/text_regex").apply { mkdirs() }
        val apiDir = File(root, "text/api").apply { mkdirs() }

        // 1. 写入含注释与 dummy 的字典文件
        File(textDir, "01_ui.json").writeText(
            """
            {
              // 顶部注释
              "艦隊これくしょん": "舰队Collection",
              "dummy": "forNoComma",
            }
            """.trimIndent()
        )

        // 2. 写入正则文件
        File(regexDir, "01_regex.json").writeText(
            """
            {
              "^第(\\d+)艦隊$": "第 $1 舰队",
              "[broken": "skip"
            }
            """.trimIndent()
        )

        // 3. 写入 API 分表
        File(apiDir, "start2.api_mst_ship.json").writeText(
            """[{"api_id": 1, "api_name": "睦月", "api_getmes": "睦月です！<br>がんばります！"}]"""
        )
        File(apiDir, "start2.api_mst_mission.json").writeText(
            """[{"api_id": 1, "api_disp_no": "01", "api_name": "练习航海", "api_details": "在镇守府近海进行航海练习。<br>提升舰队熟练度吧！"}]"""
        )
        File(apiDir, "start2.api_mst_mapinfo.json").writeText(
            """[{"api_id": 11, "api_maparea_id": 1, "api_no": 1, "api_name": "镇守府正面海域", "api_opetext": "近海警备", "api_infotext": "出动进行镇守府正面近海的<br>警备工作！"}]"""
        )
        File(apiDir, "questlist.json").writeText(
            """[{"api_no": 201, "api_title": "击破敌方舰队！", "api_detail": "消灭敌方舰队！"}]"""
        )

        val sampleJaMaps = """
            {
              "expedition": {
                "01": {
                  "id": 1,
                  "name": "練習航海",
                  "details": "鎮守府近海を航海し、艦隊の練度<br>を高めよう！"
                }
              },
              "map": {
                "11": {
                  "name": "鎮守府正面海域",
                  "opetext": "近海警備",
                  "infotext": "鎮守府正面近海の<br>警備に出動せよ！"
                }
              }
            }
        """.trimIndent()

        val bundle = ChsDataRepository.loadFromDirectory(root, sampleJaMaps)

        // 验证静态字典与 ja_maps 预填充（包含 4 种 <br> 变体）
        assertEquals("舰队Collection", bundle.textDict["艦隊これくしょん"])
        assertEquals("练习航海", bundle.textDict["練習航海"])
        assertEquals(
            "在镇守府近海进行航海练习。\n提升舰队熟练度吧！",
            bundle.textDict["鎮守府近海を航海し、艦隊の練度\nを高めよう！"]
        )
        assertEquals("镇守府正面海域", bundle.textDict["鎮守府正面海域"])
        assertEquals("近海警备", bundle.textDict["近海警備"])
        assertEquals(
            "出动进行镇守府正面近海的警备工作！",
            bundle.textDict["鎮守府正面近海の警備に出動せよ！"]
        )

        // 验证正则加载
        assertEquals(1, bundle.regexRules.size)
        assertEquals("""^第(\d+)艦隊$""", bundle.regexRules[0].first)

        // 验证 API 表索引
        val shipEntry = bundle.apiTables["ship"]?.getAsJsonObject("1")
        assertNotNull(shipEntry)
        assertEquals("睦月", shipEntry!!.get("api_name").asString)

        val missionByDisp = bundle.apiTables["mission"]?.getAsJsonObject("01")
        val missionByInt = bundle.apiTables["mission"]?.getAsJsonObject("1")
        assertNotNull(missionByDisp)
        assertNotNull(missionByInt)
        assertEquals("练习航海", missionByInt!!.get("api_name").asString)

        val mapById = bundle.apiTables["mapinfo"]?.getAsJsonObject("11")
        val mapByKey = bundle.apiTables["mapinfo"]?.getAsJsonObject("1_1")
        assertNotNull(mapById)
        assertNotNull(mapByKey)
        assertEquals("镇守府正面海域", mapByKey!!.get("api_name").asString)

        val questEntry = bundle.apiTables["questlist"]?.getAsJsonObject("201")
        assertNotNull(questEntry)
        assertEquals("击破敌方舰队！", questEntry!!.get("api_title").asString)
        assertTrue(bundle.apiTables.keys.containsAll(listOf(
            "ship", "mission", "furniture", "mapinfo", "slotitem",
            "slotitemEquiptype", "stype", "useitem", "payitem",
            "pictureBookShip", "pictureBookSlot", "questlist", "flavorInfo"
        )))
    }
}
