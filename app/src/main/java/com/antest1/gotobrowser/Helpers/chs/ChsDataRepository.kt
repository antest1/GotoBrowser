package com.antest1.gotobrowser.Helpers.chs

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 简中汉化数据加载与内存缓存仓库
 * 完整移植自 poi-plugin-kancolle-chs 的 src/loaders/data-loader.es
 */
object ChsDataRepository {
    private const val TAG = "GOTO-CHS"
    private val DIGITS_ONLY_REGEX = Regex("""^\d+$""")
    private val gson = Gson()

    data class LoadedBundle(
        val textDict: Map<String, String>,
        val regexRules: List<Pair<String, String>>,
        val apiTables: Map<String, JsonObject>,
        val apiTableJsonStrings: Map<String, String> = emptyMap()
    )

    @Volatile
    private var cachedBundle: LoadedBundle? = null

    @Volatile
    private var cachedRootStamp: String = ""

    @JvmStatic
    fun invalidateCache() {
        cachedBundle = null
        cachedRootStamp = ""
    }

    /**
     * 获取或加载完整汉化数据包（带目录时间戳缓存，避免重复磁盘读取）
     */
    @JvmStatic
    fun getOrLoad(context: Context): LoadedBundle {
        val rootDir = ChsPaths.getRootDir(context)
        val stamp = computeDirStamp(rootDir)
        val current = cachedBundle
        if (current != null && stamp == cachedRootStamp) {
            return current
        }
        val loaded = loadFromDirectory(rootDir)
        cachedBundle = loaded
        cachedRootStamp = stamp
        return loaded
    }

    /**
     * 从指定根目录加载所有文本词典、正则规则与 API 补丁表（可供 JVM 单元测试直接调用）
     */
    @JvmStatic
    fun loadFromDirectory(sourceDir: File): LoadedBundle {
        val textDict = LinkedHashMap<String, String>()
        val regexRules = ArrayList<Pair<String, String>>()
        val apiTables = LinkedHashMap<String, JsonObject>()

        val tableNames = listOf(
            "ship", "mission", "furniture", "mapinfo",
            "slotitem", "slotitemEquiptype", "stype",
            "useitem", "payitem", "pictureBookShip",
            "pictureBookSlot", "questlist", "flavorInfo"
        )
        for (name in tableNames) {
            apiTables[name] = JsonObject()
        }

        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            val emptySerialized = apiTables.mapValues { "{}" }
            return LoadedBundle(textDict, regexRules, apiTables, emptySerialized)
        }

        // 1. 加载 text/text/*.json 精确字典
        val textDir = File(sourceDir, "text/text")
        if (textDir.isDirectory) {
            val files = textDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.sortedBy { it.name }
                ?: emptyList()
            for (file in files) {
                val parsed = readJsonFileSafe(file)
                if (parsed != null && parsed.isJsonObject) {
                    for ((k, v) in parsed.asJsonObject.entrySet()) {
                        if (k == "dummy" || v == null || !v.isJsonPrimitive) continue
                        val strVal = v.asString
                        if (strVal == "forNoComma") continue
                        textDict[k] = strVal
                    }
                }
            }
        }

        // 2. 加载 text/text_regex/*.json 正则替换规则
        val regexDir = File(sourceDir, "text/text_regex")
        if (regexDir.isDirectory) {
            val files = regexDir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                ?.sortedBy { it.name }
                ?: emptyList()
            for (file in files) {
                val parsed = readJsonFileSafe(file)
                val valid = ChsJsonCleaner.sanitizeRegexRules(parsed, file.name)
                regexRules.addAll(valid)
            }
        }

        // 3. 加载 text/api/*.json
        loadApiTables(sourceDir, apiTables, textDict)

        // 4. 在后台加载线程预先序列化各分表 JSON，避免在 JavaBridge 线程上发生阻塞与序列化开销
        val serializedTables = LinkedHashMap<String, String>(apiTables.size)
        for ((k, v) in apiTables) {
            serializedTables[k] = gson.toJson(v)
        }

        return LoadedBundle(textDict, regexRules, apiTables, serializedTables)
    }

    private fun loadApiTables(
        sourceDir: File,
        apiTables: MutableMap<String, JsonObject>,
        textDict: MutableMap<String, String>
    ) {
        val apiDir = File(sourceDir, "text/api")

        fun readApiArray(fileName: String): JsonArray? {
            val candidates = listOf(
                File(apiDir, fileName),
                File(sourceDir, "text/$fileName"),
                File(sourceDir, "text/text/$fileName"),
                File(sourceDir, "api/$fileName")
            )
            for (f in candidates) {
                if (f.isFile) {
                    val el = readJsonFileSafe(f)
                    if (el != null && el.isJsonArray) {
                        return el.asJsonArray
                    }
                }
            }
            return null
        }

        // 3.1 舰船与入手台词 (start2.api_mst_ship.json)
        readApiArray("start2.api_mst_ship.json")?.let { list ->
            val shipTable = apiTables["ship"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val id = obj.strOrId("api_id") ?: continue
                val entry = JsonObject().apply {
                    obj.get("api_name")?.let { add("api_name", it) }
                    addProperty("api_getmes", obj.optString("api_getmes"))
                }
                shipTable.add(id, entry)
            }
        }

        // 3.2 远征数据 (start2.api_mst_mission.json)
        readApiArray("start2.api_mst_mission.json")?.let { list ->
            val missionTable = apiTables["mission"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val dispNo = obj.optString("api_disp_no").trim().takeIf { it.isNotEmpty() }
                val cleanName = obj.optString("api_name").trim()
                val cleanDetails = obj.optString("api_details")

                val entry = JsonObject().apply {
                    if (dispNo != null) addProperty("api_disp_no", dispNo)
                    addProperty("api_name", cleanName)
                    addProperty("api_details", cleanDetails)
                    obj.get("api_id")?.let { add("api_id", it) }
                }

                if (dispNo != null) {
                    missionTable.add(dispNo, entry)
                    if (DIGITS_ONLY_REGEX.matches(dispNo)) {
                        val intNo = dispNo.toIntOrNull()?.toString()
                        if (intNo != null) {
                            missionTable.add(intNo, entry)
                        }
                    }
                }
                if (cleanName.isNotEmpty()) {
                    missionTable.add(cleanName, entry)
                }
                obj.strOrId("api_id")?.let { idStr ->
                    missionTable.add(idStr, entry)
                }
            }
        }

        // 3.3 家具数据 (start2.api_mst_furniture.json)
        readApiArray("start2.api_mst_furniture.json")?.let { list ->
            val furnitureTable = apiTables["furniture"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val id = obj.strOrId("api_id") ?: continue
                val title = obj.optString("api_title").ifEmpty { obj.optString("api_name") }
                val entry = JsonObject().apply {
                    if (title.isNotEmpty()) addProperty("api_title", title)
                    obj.get("api_description")?.let { add("api_description", it) }
                }
                furnitureTable.add(id, entry)
            }
        }

        // 3.4 海图数据 (start2.api_mst_mapinfo.json)
        readApiArray("start2.api_mst_mapinfo.json")?.let { list ->
            val mapTable = apiTables["mapinfo"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val areaId = obj.optIntOrNull("api_maparea_id")
                val mapNo = obj.optIntOrNull("api_no")
                val mapId = obj.optIntOrNull("api_id")
                    ?: if (areaId != null && mapNo != null) areaId * 10 + mapNo else null
                if (mapId != null) {
                    val entry = JsonObject().apply {
                        addProperty("api_id", mapId)
                        if (areaId != null) addProperty("api_maparea_id", areaId)
                        if (mapNo != null) addProperty("api_no", mapNo)
                        obj.get("api_name")?.let { add("api_name", it) }
                        obj.get("api_opetext")?.let { add("api_opetext", it) }
                        obj.get("api_infotext")?.let { add("api_infotext", it) }
                    }
                    mapTable.add(mapId.toString(), entry)
                    if (areaId != null && mapNo != null) {
                        mapTable.add("${areaId}_${mapNo}", entry)
                    }
                }
            }
        }

        // 3.5 装备静态数据 (start2.api_mst_slotitem.json)
        readApiArray("start2.api_mst_slotitem.json")?.let { list ->
            val slotTable = apiTables["slotitem"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val id = obj.strOrId("api_id") ?: continue
                val entry = JsonObject().apply {
                    obj.get("api_name")?.let { add("api_name", it) }
                }
                slotTable.add(id, entry)
            }
        }

        // 3.6 装备类型分类母表 (start2.api_mst_slotitem_equiptype.json)
        readApiArray("start2.api_mst_slotitem_equiptype.json")?.let { list ->
            val eqTable = apiTables["slotitemEquiptype"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val id = obj.strOrId("api_id") ?: continue
                val entry = JsonObject().apply {
                    obj.get("api_id")?.let { add("api_id", it) }
                    obj.get("api_name")?.let { add("api_name", it) }
                }
                eqTable.add(id, entry)
            }
        }

        // 3.7 基础舰种母表 (start2.api_mst_stype.json)
        readApiArray("start2.api_mst_stype.json")?.let { list ->
            val stypeTable = apiTables["stype"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val id = obj.strOrId("api_id") ?: continue
                val entry = JsonObject().apply {
                    obj.get("api_id")?.let { add("api_id", it) }
                    obj.get("api_name")?.let { add("api_name", it) }
                }
                stypeTable.add(id, entry)
            }
        }

        // 3.8 消耗道具母表 (start2.api_mst_useitem.json)
        readApiArray("start2.api_mst_useitem.json")?.let { list ->
            val useitemTable = apiTables["useitem"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val id = obj.strOrId("api_id") ?: continue
                val entry = JsonObject().apply {
                    obj.get("api_id")?.let { add("api_id", it) }
                    obj.get("api_name")?.let { add("api_name", it) }
                    obj.get("api_description")?.let { add("api_description", it) }
                }
                useitemTable.add(id, entry)
            }
        }

        // 3.9 商城与点券道具母表 (start2.api_mst_payitem.json)
        readApiArray("start2.api_mst_payitem.json")?.let { list ->
            val payitemTable = apiTables["payitem"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val id = obj.strOrId("api_id") ?: continue
                val entry = JsonObject().apply {
                    obj.get("api_id")?.let { add("api_id", it) }
                    obj.get("api_name")?.let { add("api_name", it) }
                    obj.get("api_description")?.let { add("api_description", it) }
                    obj.get("api_shop_description")?.let { add("api_shop_description", it) }
                }
                payitemTable.add(id, entry)
            }
        }

        // 3.10 任务数据 (questlist.json)
        readApiArray("questlist.json")?.let { list ->
            val questTable = apiTables["questlist"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val no = obj.strOrId("api_no") ?: continue
                val entry = JsonObject().apply {
                    obj.get("api_no")?.let { add("api_no", it) }
                    obj.get("api_title")?.let { add("api_title", it) }
                    obj.get("api_detail")?.let { add("api_detail", it) }
                }
                questTable.add(no, entry)
            }
        }

        // 3.11 舰船图鉴 (picture_book.ship.json)
        readApiArray("picture_book.ship.json")?.let { list ->
            val pbShipTable = apiTables["pictureBookShip"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val idx = obj.strOrId("api_index_no") ?: continue
                val entry = JsonObject().apply {
                    obj.get("api_name")?.let { add("api_name", it) }
                    obj.get("api_info")?.let { add("api_info", it) }
                    obj.get("api_sinfo")?.let { add("api_sinfo", it) }
                }
                pbShipTable.add(idx, entry)
            }
        }

        // 3.12 装备图鉴 (picture_book.slotitem.json)
        readApiArray("picture_book.slotitem.json")?.let { list ->
            val pbSlotTable = apiTables["pictureBookSlot"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val idx = obj.strOrId("api_index_no") ?: continue
                val entry = JsonObject().apply {
                    obj.get("api_name")?.let { add("api_name", it) }
                    obj.get("api_info")?.let { add("api_info", it) }
                }
                pbSlotTable.add(idx, entry)
            }
        }

        // 3.13 进点台词与海况 (battle.api_flavor_info.json)
        readApiArray("battle.api_flavor_info.json")?.let { list ->
            val flavorTable = apiTables["flavorInfo"]!!
            for (el in list) {
                val obj = el.asObjOrNull() ?: continue
                val voiceId = obj.strOrId("api_voice_id")
                val bossShipId = obj.strOrId("api_boss_ship_id")
                val entry = JsonObject().apply {
                    obj.get("api_voice_id")?.let { add("api_voice_id", it) }
                    obj.get("api_message")?.let { add("api_message", it) }
                    obj.get("api_class_name")?.let { add("api_class_name", it) }
                    obj.get("api_ship_name")?.let { add("api_ship_name", it) }
                    obj.get("api_boss_ship_id")?.let { add("api_boss_ship_id", it) }
                }
                if (voiceId != null) {
                    flavorTable.add(voiceId, entry)
                }
                if (bossShipId != null) {
                    flavorTable.add("ship_$bossShipId", entry)
                }
                val msgJa = obj.optString("api_message_ja")
                val msgChs = obj.optString("api_message")
                if (msgJa.isNotEmpty() && msgChs.isNotEmpty()) {
                    textDict[msgJa] = msgChs
                }
            }
        }
    }

    private fun readJsonFileSafe(file: File): JsonElement? {
        return try {
            val raw = file.readText(StandardCharsets.UTF_8)
            ChsJsonCleaner.safeParseJson(raw)
        } catch (e: Exception) {
            Log.w(TAG, "读取 JSON 文件失败 (${file.name}): ${e.message}")
            null
        }
    }

    private fun computeDirStamp(rootDir: File): String {
        if (!rootDir.isDirectory) return "empty"
        val sb = StringBuilder()
        for (sub in listOf("text/text", "text/text_regex", "text/api")) {
            val d = File(rootDir, sub)
            val files = d.listFiles()?.sortedBy { it.name }.orEmpty()
            sb.append(sub).append(':').append(files.size).append(';')
            for (f in files) {
                sb.append(f.name).append('=').append(f.length()).append('@').append(f.lastModified()).append(',')
            }
        }
        return sb.toString()
    }

    private fun JsonElement?.asObjOrNull(): JsonObject? =
        if (this != null && this.isJsonObject) this.asJsonObject else null

    private fun JsonObject.optString(key: String): String {
        val el = this.get(key) ?: return ""
        return if (el.isJsonPrimitive) el.asString else ""
    }

    private fun JsonObject.optIntOrNull(key: String): Int? {
        val el = this.get(key) ?: return null
        return if (el.isJsonPrimitive) {
            try { el.asInt } catch (_: Exception) { el.asString.toIntOrNull() }
        } else null
    }

    private fun JsonObject.strOrId(key: String): String? {
        val el = this.get(key) ?: return null
        if (!el.isJsonPrimitive) return null
        val s = el.asString.trim()
        return s.ifEmpty { null }
    }
}
