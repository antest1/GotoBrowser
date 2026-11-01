package com.antest1.gotobrowser.Browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import com.antest1.gotobrowser.Activity.BrowserActivity
import com.antest1.gotobrowser.Helpers.*
import com.antest1.gotobrowser.Constants.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.*
import java.util.*
import java.util.regex.Pattern

object ResourcePatcher {
    private const val TAG_P = "GOTO-P"

    private val INIT_VOLUME_PATTERN = Pattern.compile(
        String.format(
            Locale.US, "(%s,%s,%s,%s,%s);",
            "this\\[\\w+\\(\\w+\\)\\]=(\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\))",
            "this\\[\\w+\\(\\w+\\)\\]=(\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\))",
            "this\\[\\w+\\(\\w+\\)\\]=(\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\))",
            "this\\[\\w+\\(\\w+\\)\\]=0x1===\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\)",
            "this\\[\\w+\\(\\w+\\)\\]=0x1===\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\)"
        )
    )
    private val HOWL_PATTERN = Pattern.compile("(new \\w+\\[\\(\\w+\\(\\w+\\)\\)])(\\(\\w+\\)),this(?:\\[\\w+\\(\\w+\\)]){2}\\(\\w+,\\w+,\\w+\\)\\):")
    private val TOUCH_EVENT_PATTERN = Pattern.compile("('(out|over|down|move|up)'?:[^,;=}]{20,150},?){5,}")

    fun patchMainScript(
        context: Context,
        activity: BrowserActivity,
        mainJs: String,
        silentMode: Boolean,
        isCursorTouchMode: Boolean,
        touchEventPatchJs: String?
    ): String {
        var patchedJs = K3dPatcher.patchKantai3d(context, mainJs)
        patchedJs = KenPatcher.patchKantaiEn(patchedJs, activity)
        patchedJs = FpsPatcher.patchFps(patchedJs)
        patchedJs = CritPatcher.patchCrit(patchedJs)

        if (silentMode) {
            val invPatternMatcher = INIT_VOLUME_PATTERN.matcher(patchedJs)
            if (invPatternMatcher.find()) {
                val statement = invPatternMatcher.group(0)
                val varBgm = invPatternMatcher.group(2)
                val varSe = invPatternMatcher.group(3)
                val varVoice = invPatternMatcher.group(4)

                if (statement != null && varBgm != null && varSe != null && varVoice != null) {
                    val newStatement = statement.replace(varBgm, "0")
                        .replace(varSe, "0").replace(varVoice, "0")
                    patchedJs = patchedJs.replace(statement, newStatement)
                }
            }
        }

        val howlPatternMatcher = HOWL_PATTERN.matcher(patchedJs)
        if (howlPatternMatcher.find()) {
            val howlFn = escapeMatchedGroup(howlPatternMatcher.group(1))
            if (howlFn != null) patchedJs = patchedJs.replaceFirst(howlFn.toRegex(), "add_bgm")
        }

        if (isCursorTouchMode) {
            patchedJs = TOUCH_EVENT_PATTERN.matcher(patchedJs).replaceFirst(
                "down:void 0!==document.ontouchstart?'touchstart':'mousedown',\n" +
                        "move:void 0!==document.ontouchstart?'touchmove':'mousemove',\n" +
                        "up:void 0!==document.ontouchstart?'touchend':'mouseup',\n" +
                        "over:'touchover',\n" +
                        "out:'touchout'"
            )
        }

        val muteVar = if (activity.isMuteMode()) "var global_mute=1;Howler.mute(true);\n"
        else "var global_mute=0;Howler.mute(false);\n"

        patchedJs = """
            var gb_h=null;
            function add_bgm(b){b.onend=function(){(global_mute||gb_h.volume()==0)&&(gb_h.unload(),console.log('unload'))};global_mute&&(b.autoplay=false);gb_h=new Howl(b);return gb_h;}
            $muteVar
        """.trimIndent() + patchedJs

        if (isCursorTouchMode && touchEventPatchJs != null) {
            patchedJs += touchEventPatchJs
        }

        patchedJs = patchedJs + MUTE_LISTEN + CAPTURE_LISTEN + "\n" + KcsInterface.AXIOS_INTERCEPT_SCRIPT

        return patchedJs
    }

    fun applyKenPatcherIfAvailable(
        context: Context,
        versionTable: VersionDatabase,
        path: String,
        originalFile: File,
        updateFlag: Boolean
    ): File {
        if (!KenPatcher.isPatcherEnabled()) return originalFile

        val patchedFilePath = KcUtils.getAppCacheFileDir(context, KcEnUtils.getPatchedCachePath() + path)
        val patchFilePath = KcUtils.getAppCacheFileDir(context, "/" + KcEnUtils.getAssetPath() + path)

        val patchedFile = File(patchedFilePath)
        val patchFile = File(patchFilePath)

        if (!patchFile.exists()) return originalFile

        var usePatchedCache = false
        val patchStrings = if (patchFile.isDirectory) {
            if (File(patchFilePath + "/original").isDirectory) {
                KcEnUtils.dirMD5(patchFilePath + "/original") + KcEnUtils.dirMD5(patchFilePath + "/patched")
            } else {
                KcEnUtils.dirMD5(patchFilePath)
            }
        } else {
            patchFilePath + "_" + patchFile.length() + "_" + patchFile.lastModified()
        }

        val hash = KcEnUtils.GetMD5HashOfString(patchStrings)
        val patchVersion = versionTable.getVersionValue(patchFilePath)

        if (!patchedFile.exists() || updateFlag || patchVersion == null || patchVersion != hash) {
            versionTable.putVersionValue(patchFilePath, hash)
            Log.e(TAG_P, "needs repatch: $patchedFilePath $hash")

            try {
                patchedFile.parentFile?.mkdirs()

                if (patchFile.isFile) {
                    KcUtils.copyFileUsingStream(patchFile, patchedFile)
                    usePatchedCache = true
                } else {
                    val name = originalFile.name
                    val dot = name.lastIndexOf('.')
                    val ext = if (dot != -1) name.substring(dot) else ""

                    val originalPatch = File(patchFilePath + "/original" + ext)
                    val patchedPatch = File(patchFilePath + "/patched" + ext)

                    if (originalPatch.exists() && patchedPatch.exists()) {
                        val originalBytes = KcUtils.getBytesFromInputStream(FileInputStream(originalFile))
                        val patchOriginalBytes = KcUtils.getBytesFromInputStream(FileInputStream(originalPatch))

                        if (originalBytes.contentEquals(patchOriginalBytes)) {
                            KcUtils.copyFileUsingStream(patchedPatch, patchedFile)
                            Log.e(TAG_P, "patched via original/patched match: $path")
                            usePatchedCache = true
                        } else {
                            Log.e(TAG_P, "original mismatch, skipping patch: $path")
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG_P, KcUtils.getStringFromException(e))
                return originalFile
            }
        } else {
            Log.e(TAG_P, "using cached patched file: $patchedFilePath $hash")
            usePatchedCache = true
        }

        return if (usePatchedCache) patchedFile else originalFile
    }

    fun patchImageIfAvailable(
        context: Context,
        versionTable: VersionDatabase,
        path: String,
        originalFile: File,
        updateFlag: Boolean
    ): File {
        if (!KenPatcher.isPatcherEnabled()) return originalFile

        val patchedFilePath = KcUtils.getAppCacheFileDir(context, KcEnUtils.getPatchedCachePath() + path)
        val patchFilePath = KcUtils.getAppCacheFileDir(context, "/" + KcEnUtils.getAssetPath() + path)
        val patchedFile = File(patchedFilePath)
        val patchFile = File(patchFilePath)

        if (!patchFile.exists() || !patchFile.isDirectory) return originalFile

        var usePatchedCache = false
        val patchStrings = if (File(patchFilePath + "/original").isDirectory) {
            KcEnUtils.dirMD5(patchFilePath + "/original") + KcEnUtils.dirMD5(patchFilePath + "/patched")
        } else {
            KcEnUtils.dirMD5(patchFilePath)
        }

        val hash = KcEnUtils.GetMD5HashOfString(patchStrings)
        val patchVersion = versionTable.getVersionValue(patchFilePath)

        if (!patchedFile.exists() || updateFlag || patchVersion == null || patchVersion != hash) {
            versionTable.putVersionValue(patchFilePath, hash)
            Log.e(TAG_P, "needs repatch (image): $patchedFilePath $hash")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                usePatchedCache = patchImage(originalFile.absolutePath, patchedFilePath, patchFilePath)
            }
        } else {
            Log.e(TAG_P, "using cached patched image: $patchedFilePath $hash")
            usePatchedCache = true
        }

        return if (usePatchedCache) patchedFile else originalFile
    }

    fun patchImage(ogDestination: String, ptDestination: String, patchFile: String): Boolean {
        try {
            val sourceUri = Uri.fromFile(File(ptDestination))
            if (ResourceProcess.isImage(ResourceProcess.getCurrentState(sourceUri))) {
                val ogSpritesheet = BitmapFactory.decodeFile(ogDestination)
                val metadataFile = File(getSpriteMetadataPath(ogDestination))
                val dest = File(ptDestination)
                
                if (!metadataFile.exists()) {
                    val ogImage = BitmapFactory.decodeFile("$patchFile/original.png")
                    if (ogSpritesheet != null && ogImage != null && KcEnUtils.bitmapEqual(ogSpritesheet, ogImage, 0.01f)) {
                        val source = File("$patchFile/patched.png")
                        dest.parentFile?.mkdirs()
                        dest.createNewFile()
                        KcUtils.copyFileUsingStream(source, dest)
                        Log.e(TAG_P, "image patched: $ptDestination")
                        return true
                    }
                } else {
                    val ogFolder = "/original/"
                    val ptFolder = "/patched/"
                    var patchFound = false

                    val metadata = FileReader(metadataFile).use { reader ->
                        JsonParser.parseReader(reader).asJsonObject
                    }

                    val frames = metadata.getAsJsonObject("frames")
                    val originalFiles = KcEnUtils.listFiles("$patchFile$ogFolder")
                    val patchedFiles = KcEnUtils.listFiles("$patchFile$ptFolder")

                    val ptSpritesheet = ogSpritesheet.copy(ogSpritesheet.config, true)
                    val ptWidth = ptSpritesheet.width
                    val ptHeight = ptSpritesheet.height
                    val ptPixels = IntArray(ptWidth * ptHeight)
                    ptSpritesheet.getPixels(ptPixels, 0, ptWidth, 0, 0, ptWidth, ptHeight)

                    for (originalName in originalFiles) {
                        if (patchedFiles.contains(originalName)) {
                            val ogSprite = getPatchFolderSprite(patchFile, ogFolder, originalName)
                            val ptSprite = getPatchFolderSprite(patchFile, ptFolder, originalName)
                            if (ogSprite != null && ptSprite != null &&
                                ogSprite.width == ptSprite.width && ogSprite.height == ptSprite.height
                            ) {
                                patchFound = isSpritePatched(
                                    frames, ogSpritesheet, ptSpritesheet,
                                    ogSprite, ptSprite, ogSprite.width, ogSprite.height
                                )
                            }
                        }
                    }
                    return patchExported(dest, patchFound, ptSpritesheet)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG_P, KcUtils.getStringFromException(e))
        }
        return false
    }

    private fun getSpriteMetadataPath(path: String): String {
        return when {
            path.endsWith(".jpg") -> path.replace(".jpg", ".json")
            path.endsWith(".png") -> path.replace(".png", ".json")
            else -> "$path.json"
        }
    }

    private fun getPatchFolderSprite(patchFile: String, folder: String, fileName: String): Bitmap? {
        return BitmapFactory.decodeFile("$patchFile$folder$fileName")
    }

    private fun patchExported(dest: File, patchFound: Boolean, ptSpritesheet: Bitmap): Boolean {
        if (patchFound) {
            dest.parentFile?.mkdirs()
            FileOutputStream(dest).use { fos ->
                ptSpritesheet.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            return true
        }
        return false
    }

    private fun isSpritePatched(
        frames: JsonObject,
        ogSpritesheet: Bitmap,
        ptSpritesheet: Bitmap,
        ogSprite: Bitmap,
        ptSprite: Bitmap,
        ogSpriteWidth: Int,
        ogSpriteHeight: Int
    ): Boolean {
        for (key in frames.keySet()) {
            val sprite = frames.getAsJsonObject(key)
            val frame = sprite.getAsJsonObject("frame")
            val fx = frame.get("x").asInt
            val fy = frame.get("y").asInt

            val sourceSize = sprite.getAsJsonObject("sourceSize")
            val w = sourceSize.get("w").asInt
            val h = sourceSize.get("h").asInt

            if (w == ogSpriteWidth && h == ogSpriteHeight) {
                val pixels = IntArray(w * h)
                ogSpritesheet.getPixels(pixels, 0, w, fx, fy, w, h)
                val spritesheetSprite = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                spritesheetSprite.setPixels(pixels, 0, w, 0, 0, w, h)

                if (KcEnUtils.bitmapEqual(ogSprite, spritesheetSprite, 0.01f)) {
                    val ptPixels = IntArray(w * h)
                    ptSprite.getPixels(ptPixels, 0, w, 0, 0, w, h)
                    ptSpritesheet.setPixels(ptPixels, 0, w, fx, fy, w, h)
                    return true
                }
            }
        }
        return false
    }

    private fun escapeMatchedGroup(group: String?): String? {
        return group?.replace("(", "\\(")?.replace(")", "\\)")
            ?.replace("[", "\\[")?.replace("]", "\\]")
    }
}
