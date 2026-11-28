package com.antest1.gotobrowser.Helpers;

import static com.antest1.gotobrowser.Helpers.KcUtils.getStringFromException;
import static com.antest1.gotobrowser.Helpers.KcUtils.parseJsonObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.antest1.gotobrowser.Activity.SettingsStatusHost;
import com.antest1.gotobrowser.R;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class KcEnUtils {
    private static String ENPATCH_INFO_LOCAL_FILE() { return getInfoFileName(targetLanguage); }

    private static String getInfoUrl(KenPatcher.PatchLanguage language) {
        return "https://raw.githubusercontent.com/" + getGitHubNameRootPath(language)
                + "master/" + getInfoFileName(language);
    }
    private static String ENPATCH_LOCAL_FOLDER() { return "/" + getRootPath(); }

    public static class Version implements Comparable<Version> {

        private final String version;

        public final String get() {
            return this.version;
        }

        public Version(String version) {
            if(version == null)
                throw new IllegalArgumentException("Version can not be null");
            if(!version.matches("[0-9]+(\\.[0-9]+)*"))
                throw new IllegalArgumentException("Invalid version format");
            this.version = version;
        }

        @Override public int compareTo(Version that) {
            if(that == null)
                return 1;
            String[] thisParts = this.get().split("\\.");
            String[] thatParts = that.get().split("\\.");
            int length = Math.max(thisParts.length, thatParts.length);
            for(int i = 0; i < length; i++) {
                int thisPart = i < thisParts.length ?
                        Integer.parseInt(thisParts[i]) : 0;
                int thatPart = i < thatParts.length ?
                        Integer.parseInt(thatParts[i]) : 0;
                if(thisPart < thatPart)
                    return -1;
                if(thisPart > thatPart)
                    return 1;
            }
            return 0;
        }

        @Override public boolean equals(Object that) {
            if(this == that)
                return true;
            if(that == null)
                return false;
            if(this.getClass() != that.getClass())
                return false;
            return this.compareTo((Version) that) == 0;
        }

    }

    // Language targeted by patch update / download / delete from the settings sheet.
    // Kept apart from KenPatcher's active language, which only changes on browser refresh.
    private static KenPatcher.PatchLanguage targetLanguage = KenPatcher.PatchLanguage.EN;

    public void setTargetLanguage(String preference) {
        targetLanguage = KenPatcher.getCurrentPatchLanguage(preference);
    }

    public static String getPatchedCachePath() {
        if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.EN)
            return "/patched_cache_en";
        else if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.ID) {
            return "/patched_cache_id";
        }
        // fallback
        return "/patched_cache_en";
    }

    /** Language the settings sheet's patch rows (download / delete) currently point at. */
    public static KenPatcher.PatchLanguage getTargetLanguage() {
        return targetLanguage;
    }

    public static String getLanguageSymbol(KenPatcher.PatchLanguage language) {
        if (language == KenPatcher.PatchLanguage.ID) return "ID";
        return "EN"; // EN and fallback
    }

    public static String getGitHubNameRootPath(KenPatcher.PatchLanguage language) {
        if (language == KenPatcher.PatchLanguage.ID) return "SLAVUSworks/KanColle-Indonesia-Patch-KCCP/";
        return "Oradimi/KanColle-English-Patch-KCCP/"; // EN and fallback
    }

    /** Product name of the patch, as in the patch list of the settings sheet. */
    public static String getPatchTitle(KenPatcher.PatchLanguage language) {
        if (language == KenPatcher.PatchLanguage.ID) return "KanColle Indonesia Patch";
        return "KanColle English Patch"; // EN and fallback
    }

    public static String getRootPath() {
        return getRootPath(targetLanguage);
    }

    public static String getRootPath(KenPatcher.PatchLanguage language) {
        if (language == KenPatcher.PatchLanguage.ID) return "KanColle-Indonesia-Patch-KCCP-master/";
        return "KanColle-English-Patch-KCCP-master/"; // EN and fallback
    }

    /** Version file at the patch repository root, also used to tell whether the patch is installed. */
    public static String getInfoFileName(KenPatcher.PatchLanguage language) {
        return getLanguageSymbol(language) + "-patch.mod.json";
    }

    public static String getAssetPath() {
        if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.EN)
            return "KanColle-English-Patch-KCCP-master/EN-patch";
        else if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.ID) {
            return "KanColle-Indonesia-Patch-KCCP-master/ID-patch";
        }
        // fallback
        return "KanColle-English-Patch-KCCP-master/EN-patch";
    }

    public static String getEnPatchLocalFolder(Context context) {
        return KcUtils.getAppCacheFileDir(context, ENPATCH_LOCAL_FOLDER());
    }

    /**
     * Fetches the patch info file. Blocks on the network, so the update check runs it on
     * its own thread.
     * On failure the result holds an "error" property.
     */
    public static JsonObject getKantaiEnUpdateInfo(OkHttpClient client) {
        return getKantaiEnUpdateInfo(client, targetLanguage);
    }

    private static JsonObject getKantaiEnUpdateInfo(OkHttpClient client, KenPatcher.PatchLanguage language) {
        JsonObject enPatchInfo = new JsonObject();
        Request request = new Request.Builder().url(getInfoUrl(language)).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (response.code() != 200) {
                enPatchInfo.addProperty("error", String.valueOf(response.code()));
            } else if (body != null) {
                try (Reader reader = body.charStream()) {
                    JsonObject parsed = parseJsonObject(reader);
                    // An empty body parses to null.
                    if (parsed != null) enPatchInfo = parsed;
                }
            }
        } catch (IOException | RuntimeException e) {
            Log.e("GOTO", KcUtils.getStringFromException(e));
            enPatchInfo = new JsonObject();
            enPatchInfo.addProperty("error", getStringFromException(e));
        }
        Log.e("GOTO", "enPatchInfo: " + enPatchInfo);
        return enPatchInfo;
    }

    /**
     * The published version of the [language] patch when it is installed and the published
     * one is newer; null when not installed, up to date, or the check fails. Blocks on the
     * network.
     */
    public static String findAvailableUpdate(Context context, KenPatcher.PatchLanguage language) {
        File localInfo = new File(KcUtils.getAppCacheFileDir(context, "/" + getRootPath(language))
                + getInfoFileName(language));
        if (!localInfo.isFile()) return null;
        String installed = versionOf(KcUtils.readJsonObjectFromFile(localInfo.getPath()));
        if (installed == null) return null;

        JsonObject remote = getKantaiEnUpdateInfo(KcUtils.getSharedHttpClient(), language);
        String available = remote.has("error") ? null : versionOf(remote);
        return available != null && isNewer(available, installed) ? available : null;
    }

    /**
     * Whether [available] is a newer version than [installed]. The raw file can be served
     * from a cache for a few minutes after an update, so an older published version is not
     * an update. Versions that are not dotted numbers count as newer when they differ.
     */
    static boolean isNewer(String available, String installed) {
        try {
            return new Version(available).compareTo(new Version(installed)) > 0;
        } catch (IllegalArgumentException e) {
            return !available.equals(installed);
        }
    }

    private static String versionOf(JsonObject info) {
        if (info == null || !info.has("version") || !info.get("version").isJsonPrimitive()) return null;
        return info.get("version").getAsString();
    }

    // Bumped per check so a slow, superseded check cannot overwrite a newer result.
    private final AtomicInteger updateCheckGeneration = new AtomicInteger();

    public void checkKantaiEnUpdate(SettingsStatusHost host) {
        host.setPatchUpdateStatus("Checking updates...", false);
        int generation = updateCheckGeneration.incrementAndGet();
        String enPatchLocalInfoPath = getEnPatchLocalFolder(host.getContext()).concat(ENPATCH_INFO_LOCAL_FILE());
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // The version lookup is a network call; running it on the main thread caused ANRs.
        new Thread(() -> {
            JsonObject enPatchInfo = getKantaiEnUpdateInfo(KcUtils.getSharedHttpClient());
            String availableVersion = "";
            if (!enPatchInfo.has("error") && enPatchInfo.has("version"))
                availableVersion = enPatchInfo.get("version").getAsString();

            File enPatchLocalInfoFile = new File(enPatchLocalInfoPath);
            boolean installed = enPatchLocalInfoFile.exists();
            JsonObject enPatchLocalInfo = installed
                    ? KcUtils.readJsonObjectFromFile(enPatchLocalInfoFile.getPath()) : null;

            String finalAvailableVersion = availableVersion;
            mainHandler.post(() -> {
                if (generation != updateCheckGeneration.get()) return;
                applyUpdateCheckResult(host, finalAvailableVersion, installed, enPatchLocalInfo);
            });
        }).start();
    }

    private void applyUpdateCheckResult(SettingsStatusHost host, String availableVersion,
                                        boolean installed, JsonObject enPatchLocalInfo) {
        if (!installed) {
            host.setPatchUpdateStatus(String.format(Locale.US,
                    "Data not installed yet. (%s)",
                    availableVersion), true);
        } else if (enPatchLocalInfo != null && enPatchLocalInfo.has("version")) {
            String currentVersion = enPatchLocalInfo.get("version").getAsString();
            if (!currentVersion.equals(availableVersion)) {
                host.setPatchUpdateStatus(String.format(Locale.US,
                        host.getString(R.string.setting_latest_download_subtitle),
                        availableVersion), true);
            } else {
                host.setPatchUpdateStatus(host.getString(R.string.setting_latest_version), false);
            }
        } else {
            host.setPatchUpdateStatus("Error occurred while retrieving latest version", false);
        }
    }

    public static Set<String> listFiles(String dir) {
        Set<String> files = new HashSet<>();
        File[] fileList = new File(dir).listFiles();
        if (fileList != null) {
            for (File f: fileList) {
                if (!f.isDirectory()) files.add(f.getName());
            }
        }
        return files;
    }

    public static boolean bitmapEqual(Bitmap b1, Bitmap b2, float tolerance) {
        // match tolerance to match KCCP patching
        if (b1 == b2)
            return true;
        if (b1 == null || b2 == null)
            return false;

        if (b1.getWidth() != b2.getWidth() || b1.getHeight() != b2.getHeight()) {
            return false;
        }

        int width = b1.getWidth();
        int height = b1.getHeight();

        int[] pixels1 = new int[width * height];
        int[] pixels2 = new int[width * height];

        b1.getPixels(pixels1, 0, width, 0, 0, width, height);
        b2.getPixels(pixels2, 0, width, 0, 0, width, height);

        int maxDiff = (int)(255 * tolerance);

        for (int i = 0; i < pixels1.length; i++) {
            int p1 = pixels1[i];
            int p2 = pixels2[i];

            int a1 = (p1 >> 24) & 0xFF;
            int r1 = (p1 >> 16) & 0xFF;
            int g1 = (p1 >> 8) & 0xFF;
            int b1c = p1 & 0xFF;

            int a2 = (p2 >> 24) & 0xFF;
            int r2 = (p2 >> 16) & 0xFF;
            int g2 = (p2 >> 8) & 0xFF;
            int b2c = p2 & 0xFF;

            if (Math.abs(a1 - a2) > maxDiff ||
                    Math.abs(r1 - r2) > maxDiff ||
                    Math.abs(g1 - g2) > maxDiff ||
                    Math.abs(b1c - b2c) > maxDiff) {
                return false;
            }
        }

        return true;
    }

    public static String dirMD5(String dir) {
        StringBuilder md5 = new StringBuilder();
        File folder = new File(dir);
        // null when the folder is missing (e.g. original/ without patched/) or can't be read.
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                md5.append(getMD5OfFile(file.toString()));
            }
        }
        md5 = new StringBuilder(GetMD5HashOfString(md5.toString()));
        return md5.toString();
    }


    public static String getMD5OfFile(String filePath) {
        StringBuilder returnVal = new StringBuilder();
        try {
            InputStream input = new FileInputStream(filePath);
            byte[] buffer = new byte[16384];
            MessageDigest md5Hash = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = input.read(buffer);
                if (numRead > 0) {
                    md5Hash.update(buffer, 0, numRead);
                }
            }
            input.close();

            byte[] md5Bytes = md5Hash.digest();
            for (byte md5Byte : md5Bytes) {
                returnVal.append(Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1));
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.e("GOTO", KcUtils.getStringFromException(e));
        }
        return returnVal.toString().toUpperCase();
    }

    public static String GetMD5HashOfString(String str) {
        MessageDigest md5;
        StringBuilder hexString = new StringBuilder();
        try {
            md5 = MessageDigest.getInstance("md5");
            md5.reset();
            md5.update(str.getBytes());
            byte[] messageDigest = md5.digest();
            for (byte b : messageDigest) {
                hexString.append(Integer.toHexString((0xF0 & b) >> 4));
                hexString.append(Integer.toHexString(0x0F & b));
            }
        }
        catch (Throwable ignored) {}
        return hexString.toString();
    }

}
