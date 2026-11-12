package com.antest1.gotobrowser.Helpers.chs

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * KCSAPI response localization merger.
 * Ported from poi-plugin-kancolle-chs (src/core/api-merger.es).
 *
 * Lazily fetches pre-serialized translation tables via the JS bridge and registers an Axios
 * response interceptor AFTER KcsInterface.AXIOS_INTERCEPT_SCRIPT so Kcanotify receives raw
 * Japanese server responses while the game UI renders Simplified Chinese payloads.
 */
object ChsApiMerger {
    private val gson = Gson()

    /**
     * Exposed to JS via KcsInterface.@JavascriptInterface to fetch pre-serialized tables on demand.
     */
    @JvmStatic
    fun getApiTableJson(context: Context, tableName: String?): String {
        if (tableName.isNullOrBlank() || !ChsConfig.isPatcherEnabled()) {
            return "{}"
        }
        return try {
            val bundle = ChsDataRepository.getOrLoad(context)
            bundle.apiTableJsonStrings[tableName] ?: "{}"
        } catch (_: Exception) {
            "{}"
        }
    }

    /**
     * Returns the runtime Axios response-merging script when CHS patching is active.
     */
    @JvmStatic
    fun patchChsApi(context: Context): String {
        if (!ChsConfig.isPatcherEnabled()) {
            return ""
        }
        return generateApiPatcherScript(inlineApiTables = null)
    }

    /**
     * Generates the KCSAPI response-merging script.
     * When [inlineApiTables] is null, tables are loaded lazily via GotoBrowser.chs_get_api_table.
     */
    fun generateApiPatcherScript(
        inlineApiTables: Map<String, JsonObject>? = null
    ): String {
        val inlineDataJson = if (inlineApiTables != null) {
            gson.toJson(inlineApiTables)
        } else {
            "{}"
        }

        return """
/* === Kancolle_to_CHS: KCSAPI response merger (runs after KcsInterface Axios hook) === */
(function() {
  if (typeof window === 'undefined') return;
  window.__kanchs_dynamic_dict = window.__kanchs_dynamic_dict || {};
  window.__kanchs_api_data = Object.assign($inlineDataJson, window.__kanchs_api_data || {});

  function getData(table) {
    var d = window.__kanchs_api_data;
    if (d && d[table] && typeof d[table] === 'object') {
      return d[table];
    }
    try {
      if (typeof GotoBrowser !== 'undefined' && typeof GotoBrowser.chs_get_api_table === 'function') {
        var raw = GotoBrowser.chs_get_api_table(table);
        if (raw && typeof raw === 'string') {
          var parsed = JSON.parse(raw);
          if (parsed && typeof parsed === 'object') {
            d[table] = parsed;
            return parsed;
          }
        }
      }
    } catch (e) {
      console.warn('[KanCHS] Failed to load API table:', table, e);
    }
    d[table] = {};
    return d[table];
  }

  function registerBrVariants(orig, patch) {
    if (!orig || !patch || !window.__kanchs_dynamic_dict) return;
    window.__kanchs_dynamic_dict[orig] = patch;
    var re = /<br\s*\/?>/gi;
    window.__kanchs_dynamic_dict[orig.replace(re, '\n')] = patch.replace(re, '\n');
    window.__kanchs_dynamic_dict[orig.replace(re, '\r\n')] = patch.replace(re, '\r\n');
    window.__kanchs_dynamic_dict[orig.replace(re, '')] = patch.replace(re, '');
  }

  function patchStart2(data) {
    if (!data || typeof data !== 'object') return false;
    var modified = false;

    // 1. Ship names and acquisition lines
    if (Array.isArray(data.api_mst_ship)) {
      var SHIP_DATA = getData('ship');
      for (var i = 0; i < data.api_mst_ship.length; i++) {
        var s = data.api_mst_ship[i];
        if (!s || s.api_id === undefined) continue;
        var patch = SHIP_DATA[s.api_id] || SHIP_DATA[String(s.api_id)];
        if (patch) {
          if (patch.api_name) s.api_name = patch.api_name;
          if (patch.api_getmes) {
            var origGetmes = s.api_getmes;
            s.api_getmes = patch.api_getmes;
            if (origGetmes) registerBrVariants(origGetmes, patch.api_getmes);
          }
          modified = true;
        }
      }
    }

    // 2. Expedition details
    if (Array.isArray(data.api_mst_mission)) {
      var MISSION_DATA = getData('mission');
      for (var mIdx = 0; mIdx < data.api_mst_mission.length; mIdx++) {
        var m = data.api_mst_mission[mIdx];
        if (!m) continue;
        var mPatch = (m.api_disp_no && MISSION_DATA[m.api_disp_no])
          || (m.api_id !== undefined && MISSION_DATA[m.api_id])
          || (m.api_id !== undefined && MISSION_DATA[String(m.api_id)])
          || (m.api_name && MISSION_DATA[m.api_name]);
        if (mPatch) {
          var cleanName = (mPatch.api_name || '').trim();
          if (cleanName && m.api_name) {
            var origName = m.api_name;
            m.api_name = cleanName;
            if (window.__kanchs_dynamic_dict && origName) {
              window.__kanchs_dynamic_dict[origName] = cleanName;
            }
          }
          if (mPatch.api_details) {
            var origDetails = m.api_details;
            m.api_details = mPatch.api_details;
            registerBrVariants(origDetails, mPatch.api_details);
          }
          modified = true;
        }
      }
    }

    // 3. Furniture titles and descriptions
    if (Array.isArray(data.api_mst_furniture)) {
      var FURNITURE_DATA = getData('furniture');
      for (var fIdx = 0; fIdx < data.api_mst_furniture.length; fIdx++) {
        var f = data.api_mst_furniture[fIdx];
        if (!f || f.api_id === undefined) continue;
        var fPatch = FURNITURE_DATA[f.api_id] || FURNITURE_DATA[String(f.api_id)];
        if (fPatch) {
          if (fPatch.api_title) f.api_title = fPatch.api_title;
          if (fPatch.api_description) f.api_description = fPatch.api_description;
          modified = true;
        }
      }
    }

    // 4. Map info
    if (Array.isArray(data.api_mst_mapinfo)) {
      var MAP_DATA = getData('mapinfo');
      for (var mapIdx = 0; mapIdx < data.api_mst_mapinfo.length; mapIdx++) {
        var map = data.api_mst_mapinfo[mapIdx];
        if (!map) continue;
        var mapId = map.api_id !== undefined ? map.api_id : (map.api_maparea_id !== undefined && map.api_no !== undefined ? (map.api_maparea_id * 10 + map.api_no) : null);
        var mapPatch = (mapId !== null ? (MAP_DATA[mapId] || MAP_DATA[String(mapId)]) : null)
          || (map.api_maparea_id && map.api_no ? MAP_DATA[map.api_maparea_id + '_' + map.api_no] : null);
        if (mapPatch) {
          if (mapPatch.api_name) map.api_name = mapPatch.api_name;
          if (mapPatch.api_opetext) {
            var origOpe = map.api_opetext;
            map.api_opetext = mapPatch.api_opetext;
            if (window.__kanchs_dynamic_dict && origOpe) {
              window.__kanchs_dynamic_dict[origOpe] = mapPatch.api_opetext;
            }
          }
          if (mapPatch.api_infotext) {
            var origInfo = map.api_infotext;
            map.api_infotext = mapPatch.api_infotext;
            registerBrVariants(origInfo, mapPatch.api_infotext);
          }
          modified = true;
        }
      }
    }

    // 5. Equipment names
    if (Array.isArray(data.api_mst_slotitem)) {
      var SLOT_DATA = getData('slotitem');
      for (var sIdx = 0; sIdx < data.api_mst_slotitem.length; sIdx++) {
        var slot = data.api_mst_slotitem[sIdx];
        if (!slot || slot.api_id === undefined) continue;
        var slotPatch = SLOT_DATA[slot.api_id] || SLOT_DATA[String(slot.api_id)];
        if (slotPatch && slotPatch.api_name) {
          slot.api_name = slotPatch.api_name;
          modified = true;
        }
      }
    }

    // 6. Equipment categories
    if (Array.isArray(data.api_mst_slotitem_equiptype)) {
      var EQUIPTYPE_DATA = getData('slotitemEquiptype');
      for (var eqIdx = 0; eqIdx < data.api_mst_slotitem_equiptype.length; eqIdx++) {
        var eq = data.api_mst_slotitem_equiptype[eqIdx];
        if (!eq || eq.api_id === undefined) continue;
        var eqPatch = EQUIPTYPE_DATA[eq.api_id] || EQUIPTYPE_DATA[String(eq.api_id)];
        if (eqPatch && eqPatch.api_name) {
          var origEqName = eq.api_name;
          eq.api_name = eqPatch.api_name;
          if (window.__kanchs_dynamic_dict && origEqName) {
            window.__kanchs_dynamic_dict[origEqName] = eqPatch.api_name;
          }
          modified = true;
        }
      }
    }

    // 7. Ship types
    if (Array.isArray(data.api_mst_stype)) {
      var STYPE_DATA = getData('stype');
      for (var stIdx = 0; stIdx < data.api_mst_stype.length; stIdx++) {
        var st = data.api_mst_stype[stIdx];
        if (!st || st.api_id === undefined) continue;
        var stPatch = STYPE_DATA[st.api_id] || STYPE_DATA[String(st.api_id)];
        if (stPatch && stPatch.api_name) {
          var origStName = st.api_name;
          st.api_name = stPatch.api_name;
          if (window.__kanchs_dynamic_dict && origStName) {
            window.__kanchs_dynamic_dict[origStName] = stPatch.api_name;
          }
          modified = true;
        }
      }
    }

    // 8. Consumable items
    if (Array.isArray(data.api_mst_useitem)) {
      var USEITEM_DATA = getData('useitem');
      for (var uIdx = 0; uIdx < data.api_mst_useitem.length; uIdx++) {
        var u = data.api_mst_useitem[uIdx];
        if (!u || u.api_id === undefined) continue;
        var uPatch = USEITEM_DATA[u.api_id] || USEITEM_DATA[String(u.api_id)];
        if (uPatch) {
          if (uPatch.api_name) {
            var origUName = u.api_name;
            u.api_name = uPatch.api_name;
            if (window.__kanchs_dynamic_dict && origUName) {
              window.__kanchs_dynamic_dict[origUName] = uPatch.api_name;
            }
          }
          if (uPatch.api_description) {
            u.api_description = uPatch.api_description;
            if (Array.isArray(uPatch.api_description)) {
              for (var dIdx = 0; dIdx < uPatch.api_description.length; dIdx++) {
                var line = uPatch.api_description[dIdx];
                if (typeof line === 'string' && line.trim()) {
                  registerBrVariants(line, line);
                }
              }
            } else if (typeof uPatch.api_description === 'string') {
              registerBrVariants(uPatch.api_description, uPatch.api_description);
            }
          }
          modified = true;
        }
      }
    }

    // 9. Cash shop items
    if (Array.isArray(data.api_mst_payitem)) {
      var PAYITEM_DATA = getData('payitem');
      for (var pIdx = 0; pIdx < data.api_mst_payitem.length; pIdx++) {
        var p = data.api_mst_payitem[pIdx];
        if (!p || p.api_id === undefined) continue;
        var pPatch = PAYITEM_DATA[p.api_id] || PAYITEM_DATA[String(p.api_id)];
        if (pPatch) {
          if (pPatch.api_name) {
            var origPName = p.api_name;
            p.api_name = pPatch.api_name;
            if (window.__kanchs_dynamic_dict && origPName) {
              window.__kanchs_dynamic_dict[origPName] = pPatch.api_name;
            }
          }
          if (pPatch.api_description) {
            var origPDesc = p.api_description;
            p.api_description = pPatch.api_description;
            registerBrVariants(origPDesc, pPatch.api_description);
          }
          if (pPatch.api_shop_description) {
            var origPShop = p.api_shop_description;
            p.api_shop_description = pPatch.api_shop_description;
            registerBrVariants(origPShop, pPatch.api_shop_description);
          }
          modified = true;
        }
      }
    }

    return modified;
  }

  function patchPictureBook(data, reqBody) {
    if (!data || !Array.isArray(data.api_list)) return false;
    var modified = false;
    var PB_SHIP_DATA = getData('pictureBookShip');
    var PB_SLOT_DATA = getData('pictureBookSlot');

    var isSlot = false;
    var isShip = false;

    if (typeof reqBody === 'string' && reqBody) {
      if (/(?:^|[&?])api(?:%5F|_)?type(?:%3D|=)2(?:&|$)/.test(reqBody)) {
        isSlot = true;
      } else if (/(?:^|[&?])api(?:%5F|_)?type(?:%3D|=)1(?:&|$)/.test(reqBody)) {
        isShip = true;
      }
    }

    if (!isSlot && !isShip) {
      for (var i = 0; i < data.api_list.length; i++) {
        var sample = data.api_list[i];
        if (!sample || typeof sample !== 'object') continue;
        if (sample.api_stype !== undefined || sample.api_sinfo !== undefined || sample.api_yomi !== undefined) {
          isShip = true;
          break;
        }
        if (Array.isArray(sample.api_type)) {
          isSlot = true;
          break;
        }
      }
    }

    for (var j = 0; j < data.api_list.length; j++) {
      var item = data.api_list[j];
      if (!item || typeof item !== 'object' || item.api_index_no === undefined) continue;
      var idx = item.api_index_no;

      if (isSlot || (!isShip && !item.api_stype && !item.api_sinfo)) {
        var slotPatch = PB_SLOT_DATA[idx] || PB_SLOT_DATA[String(idx)];
        if (slotPatch) {
          if (slotPatch.api_name) item.api_name = slotPatch.api_name;
          if (slotPatch.api_info) item.api_info = slotPatch.api_info;
          modified = true;
        }
      } else {
        var shipPatch = PB_SHIP_DATA[idx] || PB_SHIP_DATA[String(idx)];
        if (shipPatch) {
          if (shipPatch.api_name) item.api_name = shipPatch.api_name;
          if (shipPatch.api_info) item.api_info = shipPatch.api_info;
          if (shipPatch.api_sinfo) item.api_sinfo = shipPatch.api_sinfo;
          modified = true;
        }
      }
    }

    return modified;
  }

  function patchQuestlist(data) {
    if (!data) return false;
    var modified = false;
    var QUEST_DATA = getData('questlist');

    var list = Array.isArray(data.api_list) ? data.api_list : (Array.isArray(data.api_current_list) ? data.api_current_list : null);
    if (!list) return false;

    for (var i = 0; i < list.length; i++) {
      var q = list[i];
      if (!q || typeof q !== 'object' || q.api_no === undefined) continue;
      var patch = QUEST_DATA[q.api_no] || QUEST_DATA[String(q.api_no)];
      if (patch) {
        var origTitle = q.api_title;
        var origDetail = q.api_detail;
        if (patch.api_title) q.api_title = patch.api_title;
        if (patch.api_detail) q.api_detail = patch.api_detail;
        registerBrVariants(origTitle, patch.api_title);
        registerBrVariants(origDetail, patch.api_detail);
        modified = true;
      }
    }

    return modified;
  }

  function patchFlavor(data) {
    if (!data || typeof data !== 'object') return false;
    var modified = false;
    var FLAVOR_DATA = getData('flavorInfo');
    var SHIP_DATA = getData('ship');
    var dynamicDict = window.__kanchs_dynamic_dict || {};

    if (data.api_flavor_info) {
      var list = Array.isArray(data.api_flavor_info) ? data.api_flavor_info : [data.api_flavor_info];
      for (var i = 0; i < list.length; i++) {
        var f = list[i];
        if (!f || typeof f !== 'object') continue;

        var patch = null;
        if (f.api_voice_id !== undefined && f.api_voice_id !== null) {
          patch = FLAVOR_DATA[f.api_voice_id] || FLAVOR_DATA[String(f.api_voice_id)];
        }
        if (!patch && f.api_boss_ship_id !== undefined && f.api_boss_ship_id !== null) {
          patch = FLAVOR_DATA['ship_' + f.api_boss_ship_id] || FLAVOR_DATA[f.api_boss_ship_id];
        }

        var origMsg = f.api_message || '';
        var origShip = f.api_ship_name || '';
        var origClass = f.api_class_name || '';

        if (patch) {
          if (patch.api_message) {
            f.api_message = patch.api_message;
            registerBrVariants(origMsg, patch.api_message);
            modified = true;
          }
          if (patch.api_class_name) {
            f.api_class_name = patch.api_class_name;
            if (origClass) dynamicDict[origClass] = patch.api_class_name;
            modified = true;
          }
          if (patch.api_ship_name) {
            f.api_ship_name = patch.api_ship_name;
            if (origShip) dynamicDict[origShip] = patch.api_ship_name;
            modified = true;
          }
        }

        if (f.api_boss_ship_id && SHIP_DATA && SHIP_DATA[f.api_boss_ship_id] && SHIP_DATA[f.api_boss_ship_id].api_name) {
          if (!patch || !patch.api_ship_name) {
            f.api_ship_name = SHIP_DATA[f.api_boss_ship_id].api_name;
            if (origShip) dynamicDict[origShip] = f.api_ship_name;
            modified = true;
          }
        } else if (f.api_ship_name && dynamicDict[f.api_ship_name]) {
          f.api_ship_name = dynamicDict[f.api_ship_name];
          modified = true;
        }

        if (f.api_class_name && dynamicDict[f.api_class_name]) {
          f.api_class_name = dynamicDict[f.api_class_name];
          modified = true;
        }

        if ((!patch || !patch.api_message) && f.api_message) {
          if (dynamicDict[f.api_message]) {
            f.api_message = dynamicDict[f.api_message];
            registerBrVariants(origMsg, f.api_message);
            modified = true;
          } else {
            var clean = f.api_message.replace(/<br\s*\/?>/gi, '');
            if (dynamicDict[clean]) {
              f.api_message = dynamicDict[clean];
              registerBrVariants(origMsg, f.api_message);
              modified = true;
            }
          }
        }
      }
    }

    if (data.api_cell_flavor && typeof data.api_cell_flavor === 'object') {
      var cf = data.api_cell_flavor;
      if (cf.api_message) {
        var orig = cf.api_message;
        if (dynamicDict[orig]) {
          cf.api_message = dynamicDict[orig];
          registerBrVariants(orig, cf.api_message);
          modified = true;
        } else {
          var cleanCf = orig.replace(/<br\s*\/?>/gi, '');
          if (dynamicDict[cleanCf]) {
            cf.api_message = dynamicDict[cleanCf];
            registerBrVariants(orig, cf.api_message);
            modified = true;
          }
        }
      }
    }

    return modified;
  }

  function isTargetApi(url) {
    if (!url || typeof url !== 'string') return false;
    if (url.indexOf('/kcsapi/') === -1) return false;
    return url.indexOf('/api_start2/getData') !== -1 ||
           url.indexOf('/api_get_member/picture_book') !== -1 ||
           url.indexOf('/api_get_member/questlist') !== -1 ||
           url.indexOf('/api_req_map/') !== -1 ||
           url.indexOf('/api_req_sortie/') !== -1 ||
           url.indexOf('/api_req_combined_battle/') !== -1 ||
           url.indexOf('/api_req_battle_midnight/') !== -1 ||
           url.indexOf('/api_req_practice/') !== -1 ||
           url.indexOf('battle') !== -1;
  }

  function mergeSvdataString(url, raw, reqBody) {
    if (!raw || typeof raw !== 'string' || !raw.startsWith('svdata=')) return null;
    if (!isTargetApi(url)) return null;

    var json = JSON.parse(raw.slice(7));
    if (!json || json.api_result !== 1 || !json.api_data) return null;

    var modified = false;
    if (url.indexOf('/api_start2/getData') !== -1) {
      modified = patchStart2(json.api_data);
    } else if (url.indexOf('/api_get_member/picture_book') !== -1) {
      modified = patchPictureBook(json.api_data, reqBody);
    } else if (url.indexOf('/api_get_member/questlist') !== -1) {
      modified = patchQuestlist(json.api_data);
    }

    if (
      url.indexOf('/api_req_sortie/') !== -1 ||
      url.indexOf('/api_req_combined_battle/') !== -1 ||
      url.indexOf('/api_req_battle_midnight/') !== -1 ||
      url.indexOf('/api_req_practice/') !== -1 ||
      url.indexOf('/api_req_map/') !== -1 ||
      (json.api_data && (json.api_data.api_flavor_info || json.api_data.api_cell_flavor))
    ) {
      var flavorModified = patchFlavor(json.api_data);
      modified = modified || flavorModified;
    }

    if (modified) {
      return 'svdata=' + JSON.stringify(json);
    }
    return null;
  }

  // Export pure merge function for unit verification.
  window.__kanchs_merge_svdata = mergeSvdataString;

  // Register Axios response interceptor AFTER KcsInterface.AXIOS_INTERCEPT_SCRIPT so Kcanotify
  // receives the original Japanese response while the game UI renders localized text.
  if (typeof axios !== 'undefined' && axios && axios.interceptors && axios.interceptors.response) {
    if (!axios.__kanchs_axios_hooked) {
      axios.__kanchs_axios_hooked = true;
      axios.interceptors.response.use(function(response) {
        try {
          if (response && response.config && response.config.url && typeof response.data === 'string') {
            var url = response.config.url;
            var reqBody = response.config.data || '';
            var patched = mergeSvdataString(url, response.data, reqBody);
            if (patched) {
              response.data = patched;
            }
          }
        } catch (e) {
          console.warn('[KanCHS] Axios response merge error:', e);
        }
        return response;
      });
    }
  }
})();
""".trimIndent()
    }
}
