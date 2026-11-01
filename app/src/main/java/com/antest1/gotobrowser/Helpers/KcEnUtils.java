package com.antest1.gotobrowser.Helpers;

import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_UPDATE;
import static com.antest1.gotobrowser.Helpers.KcUtils.getStringFromException;
import static com.antest1.gotobrowser.Helpers.KcUtils.parseJsonArray;
import static com.antest1.gotobrowser.Helpers.KcUtils.parseJsonObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.lingala.zip4j.ZipFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class KcEnUtils {
    private static String RAW_BASE() { return "https://raw.githubusercontent.com/" + getGitHubNameRootPath() + "master/"; }
    private static String GITHUB_BASE() { return "https://github.com/" + getGitHubNameRootPath(); }

    private static String ENPATCH_INFO_LOCAL_FILE() { return getLanguageSymbol() + "-patch.mod.json"; }
    private static String ENPATCH_INFO_URL() { return RAW_BASE() + ENPATCH_INFO_LOCAL_FILE(); }
    private static String ENPATCH_FILE_URL_ROOT() { return RAW_BASE(); }
    private static String ENPATCH_VERSION_URL() { return RAW_BASE() + "version.json"; }
    private static String ENPATCH_LOCAL_FOLDER() { return "/" + getRootPath(); }
    private static String ENPATCH_ZIP_FILE_SRC() { return GITHUB_BASE() + "archive/refs/heads/master.zip"; }
    private static String ENPATCH_COMMIT_URL() { return "https://api.github.com/repos/" + getGitHubNameRootPath() + "commits/master"; }

    private static int BUFFER_SIZE = 8192;

    private final OkHttpClient client = new OkHttpClient();
    private boolean newVersionFlag = false;
    static String currentVersion = null;

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

    public void setPatchLanguage(String preference) {
        KenPatcher.PatchLanguage language =
                KenPatcher.getCurrentPatchLanguage(preference);
        KenPatcher.setPatchLanguage(language);
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

    public static String getLanguageSymbol(){
        if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.EN)
            return "EN";
        else if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.ID) {
            return "ID";
        }
        // fallback
        return "EN";
    }

    public static String getGitHubNameRootPath() {
        if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.EN)
            return "Oradimi/KanColle-English-Patch-KCCP/";
        else if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.ID) {
            return "SLAVUSworks/KanColle-Indonesia-Patch-KCCP/";
        }
        // fallback
        return "Oradimi/KanColle-English-Patch-KCCP/";
    }

    public static String getRootPath() {
        if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.EN)
            return "KanColle-English-Patch-KCCP-master/";
        else if (KenPatcher.getPatchLanguage() == KenPatcher.PatchLanguage.ID) {
            return "KanColle-Indonesia-Patch-KCCP-master/";
        }
        // fallback
        return "KanColle-English-Patch-KCCP-master/";
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

    public static JsonObject getKantaiEnUpdateInfo(OkHttpClient client) {
        JsonObject enPatchInfo = new JsonObject();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<JsonObject> result = executor.submit(() -> {
                JsonObject resultData = new JsonObject();
                Request.Builder builder = new Request.Builder().url(ENPATCH_INFO_URL());
                Request request = builder.build();
                try {
                    Response response = client.newCall(request).execute();
                    if (response.code() == 200) {
                        ResponseBody body = response.body();
                        if (body != null) {
                            return parseJsonObject(body.string());
                        }
                    } else {
                        resultData.addProperty("error", String.valueOf(response.code()));
                    }

                } catch (IOException e) {
                    Log.e("GOTO", KcUtils.getStringFromException(e));
                    resultData.addProperty("error", getStringFromException(e));
                }
                return resultData;
            });
            enPatchInfo = result.get();
        } catch (Exception e) {
            Log.e("GOTO", KcUtils.getStringFromException(e));
            enPatchInfo.addProperty("error", getStringFromException(e));
        }
        Log.e("GOTO", "enPatchInfo: " + enPatchInfo.toString());
        return enPatchInfo;
    }

    public JsonArray getKantaiEnVersionData() {
        JsonArray enVersionInfo = new JsonArray();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<JsonArray> result = executor.submit(() -> {
                JsonArray resultData = new JsonArray();
                Request.Builder builder = new Request.Builder().url(ENPATCH_VERSION_URL());
                Request request = builder.build();
                try {
                    Response response = client.newCall(request).execute();
                    if (response.code() == 200) {
                        ResponseBody body = response.body();
                        if (body != null) {
                            return parseJsonArray(body.string());
                        }
                    }
                } catch (IOException e) {
                    Log.e("GOTO", KcUtils.getStringFromException(e));
                }
                return resultData;
            });
            enVersionInfo = result.get();
        } catch (Exception e) {
            Log.e("GOTO", KcUtils.getStringFromException(e));
        }
        Log.e("GOTO", "enPatchInfo: " + enVersionInfo.toString());
        return enVersionInfo;
    }

    public void checkKantaiEnUpdate(SettingsActivity.SettingsFragment fragment, Preference kantaiEnUpdate) {
        // To do: clean up this mess
        kantaiEnUpdate.setSummary("Checking updates...");
        kantaiEnUpdate.setEnabled(false);

        JsonObject enPatchLocalInfo;
        String enPatchLocalInfoPath = getEnPatchLocalFolder(fragment.requireContext()).concat(ENPATCH_INFO_LOCAL_FILE());

        JsonObject enPatchInfo = getKantaiEnUpdateInfo(client);
        String availableVersion = "";
        if (!enPatchInfo.has("error"))
            availableVersion = enPatchInfo.get("version").getAsString();

        File enPatchLocalInfoFile = new File(enPatchLocalInfoPath);
        if (!enPatchLocalInfoFile.exists()) {
            kantaiEnUpdate.setSummary(String.format(Locale.US,
                    "Data not installed yet. (%s)",
                    availableVersion));
            kantaiEnUpdate.setEnabled(true);
            newVersionFlag = false;
        } else {
            enPatchLocalInfo = KcUtils.readJsonObjectFromFile(enPatchLocalInfoFile.getPath());
            if (enPatchLocalInfo != null && enPatchLocalInfo.has("version")) {
                currentVersion = enPatchLocalInfo.get("version").getAsString();
                if (!currentVersion.equals(availableVersion)) {
                    kantaiEnUpdate.setSummary(String.format(Locale.US,
                            fragment.getString(R.string.setting_latest_download_subtitle),
                            availableVersion));
                    kantaiEnUpdate.setEnabled(true);
                    newVersionFlag = true;
                } else {
                    kantaiEnUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                    newVersionFlag = false;
                }
            } else {
                kantaiEnUpdate.setSummary("Error occurred while retrieving latest version");
                newVersionFlag = false;
            }
        }
    }

    public String checkKantaiEnUpdateEntrance(Context context) {
        String enPatchLocalInfoPath = getEnPatchLocalFolder(context).concat(ENPATCH_INFO_LOCAL_FILE());
        JsonObject enPatchInfo = getKantaiEnUpdateInfo(client);
        Log.e("GOTO-P", "enPatchInfo: " + enPatchInfo.toString());
        if (enPatchInfo.has("version")) {
            String availableVersion = enPatchInfo.get("version").getAsString();
            File enPatchLocalInfoFile = new File(enPatchLocalInfoPath);
            if (enPatchLocalInfoFile.exists()) {
                JsonObject enPatchLocalInfo = KcUtils.readJsonObjectFromFile(enPatchLocalInfoFile.getPath());
                Log.e("GOTO-P", "enPatchLocalInfo: " + enPatchLocalInfo.toString());
                currentVersion = enPatchLocalInfo.get("version").getAsString();
                if (!currentVersion.equals(availableVersion)) {
                    return availableVersion;
                }
            }
        }
        return null;
    }

    public boolean remoteFileExists(String URLName){
        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con =
                    (HttpURLConnection) new URL(URLName).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }
        catch (Exception e) {
            Log.e("GOTO", KcUtils.getStringFromException(e));
            return false;
        }
    }

    public void requestPatchUpdate(SettingsActivity.SettingsFragment fragment) throws IOException {
        // To do: clean up this mess
        Context context = fragment.requireContext();
        if (newVersionFlag) {
            // Updates the patch by downloading each new file individually, and deleting outdated ones
            new PatchIndividualDownloader(context, fragment).execute();
        } else {
            // Downloads and extracts the Patch zip
            JsonObject enPatchInfo = getKantaiEnUpdateInfo(client);
            new PatchZipDownloader(context, fragment, enPatchInfo).execute();
        }
    }

    public void requestPatchUpdateEntrance(Context context) {
        // To do: clean up this mess
        new PatchIndividualDownloader(context, null).execute();
    }

    public void requestPatchDelete(SettingsActivity.SettingsFragment fragment) {
        Context context = fragment.requireContext();
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(context);
        alertDialogBuilder.setTitle(R.string.settings_mod_kantaien_delete);
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(R.string.settings_mod_kantaien_delete_summary)
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            deleteEnglishPatch(context);
                            newVersionFlag = false;
                            KcUtils.showToastShort(context, "Patch deleted");
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> dialog.cancel());
        alertDialogBuilder.show();
    }

    private static void deleteEnglishPatch(Context fragment) {
        File zipFile = new File(KcUtils.getAppCacheFileDir(fragment, "/master.zip"));
        zipFile.delete();
        File patchFolder = new File(KcUtils.getAppCacheFileDir(fragment, ENPATCH_LOCAL_FOLDER()));
        KcUtils.deleteRecursive(patchFolder);
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
        File[] files = folder.listFiles();

        for (File file : Objects.requireNonNull(files)) {
            md5.append(getMD5OfFile(file.toString()));
        }
        md5 = new StringBuilder(GetMD5HashOfString(md5.toString()));
        return md5.toString();
    }


    public static String getMD5OfFile(String filePath) {
        StringBuilder returnVal = new StringBuilder();
        try {
            InputStream input = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
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

    private static String getLatestCommit(OkHttpClient client) throws IOException {
        Request request = new Request.Builder().url(ENPATCH_COMMIT_URL()).build();
        Response response = client.newCall(request).execute();

        if (response.isSuccessful() && response.body() != null) {
            JsonObject json = parseJsonObject(response.body().string());
            return json.get("sha").getAsString();
        }
        return null;
    }

    private static String getLocalCommit(Context context) {
        return context.getSharedPreferences("patch_meta", Context.MODE_PRIVATE)
                .getString("commit", null);
    }

    private static void saveLocalCommit(Context context, String sha) {
        context.getSharedPreferences("patch_meta", Context.MODE_PRIVATE)
                .edit()
                .putString("commit", sha)
                .apply();
    }

    private static class PatchIndividualDownloader extends AsyncTask<Void, String, Integer> {

        private final Context context;
        private final SettingsActivity.SettingsFragment fragment;
        private final OkHttpClient client = new OkHttpClient();
        private ProgressDialog dialog;

        public PatchIndividualDownloader(Context ctx, SettingsActivity.SettingsFragment frag) {
            this.context = ctx;
            this.fragment = frag;
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(context);
            dialog.setTitle("Update Patch Files");
            dialog.setMessage("Downloading...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            dialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            dialog.dismiss();

            if (result == 1) {
                KcUtils.showToast(context, "Patch updated");

                if (fragment != null) {
                    Preference pref = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                    if (pref != null) {
                        pref.setSummary(context.getString(R.string.setting_latest_version));
                        pref.setEnabled(false);
                    }
                }

            } else if (result == 2) {
                KcUtils.showToast(context, "Downloading Zip...");
                JsonObject enPatchInfo = getKantaiEnUpdateInfo(client);
                if (fragment != null)
                    new PatchZipDownloader(context, fragment, enPatchInfo).execute();
                else
                    new PatchZipDownloader(context, null, enPatchInfo).execute();
            } else {
                KcUtils.showToast(context, "Update failed");
            }
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                String remoteCommit = getLatestCommit(client);
                String localCommit = getLocalCommit(context);

                if (remoteCommit == null)
                    return -2;

                if (localCommit == null) {
                    return 2; // zip fallback
                }

                if (remoteCommit.equals(localCommit)) {
                    publishProgress("Already up to date");
                    return 1;
                }

                publishProgress("Fetching diff...");

                JsonObject compare = getCompare(localCommit, remoteCommit);
                if (compare == null || !compare.has("files"))
                    return -1;

                JsonArray files = compare.getAsJsonArray("files");

                if (files.size() >= 300)
                    return 2; // zip fallback

                int updated = 0;
                int totalFiles = files.size();
                int currentIndex = 0;
                long startTime = System.currentTimeMillis();
                for (JsonElement el : files) {
                    currentIndex++;

                    JsonObject file = el.getAsJsonObject();

                    String filename = file.get("filename").getAsString();
                    String status = file.get("status").getAsString();

                    File out = new File(
                            KcUtils.getAppCacheFileDir(context, ENPATCH_LOCAL_FOLDER()),
                            filename
                    );

                    long now = System.currentTimeMillis();
                    double elapsedSec = (now - startTime) / 1000.0;
                    double filesPerSec = elapsedSec > 0 ? currentIndex / elapsedSec : 0;
                    double eta = filesPerSec > 0 ? (totalFiles - currentIndex) / filesPerSec : 0;

                    String progressHeader = String.format(Locale.US,
                            "[%d / %d] (%.1f%%)\n%.2f files/s • ETA: %ds\n",
                            currentIndex, totalFiles, (currentIndex * 100.0 / totalFiles), filesPerSec, (int) eta
                    );

                    if ("removed".equals(status)) {
                        publishProgress(progressHeader + "Deleting:\n" + filename);
                        if (out.exists()) out.delete();
                        continue;
                    }

                    String rawUrl = file.get("raw_url").getAsString();

                    publishProgress(progressHeader + "Downloading:\n" + filename);

                    if (out.getParentFile() != null) {
                        out.getParentFile().mkdirs();
                    }

                    KcUtils.downloadResource(client, rawUrl, out);

                    updated++;
                }

                saveLocalCommit(context, remoteCommit);

                publishProgress("Updated files: " + updated);

                return 1;

            } catch (Exception e) {
                Log.e("GOTO", getStringFromException(e));
                return -1;
            }
        }

        // ---------- GitHub ----------

        private JsonObject getCompare(String oldSha, String newSha) throws IOException {
            String url = "https://api.github.com/repos/" + getGitHubNameRootPath() + "compare/"
                    + oldSha + "..." + newSha;

            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                return parseJsonObject(response.body().string());
            }

            return null;
        }
    }


    private static class PatchZipDownloader extends AsyncTask<Integer, String, Integer> {
        private SettingsActivity.SettingsFragment fragment;
        private final OkHttpClient client = new OkHttpClient();
        private Context context;
        private JsonObject patchInfo;
        private ProgressDialog mProgressDialog;
        long patchSize = 370000000;

        public PatchZipDownloader(Context ctx, SettingsActivity.SettingsFragment f, JsonObject patch_info) {
            this.context = ctx;
            this.fragment = f;
            this.patchInfo = patch_info;
        }

        @Override
        protected void onPreExecute() {
            if (patchInfo.has("size") && !patchInfo.get("size").isJsonNull()) {
                patchSize = patchInfo.get("size").getAsLong();
            }

            super.onPreExecute();
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setTitle("Update Patch Files");
            mProgressDialog.setMessage("Downloading...");
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(0);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mProgressDialog.setMessage(values[0]);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            try {
                URL url = new URL(ENPATCH_ZIP_FILE_SRC());
                String out = KcUtils.getAppCacheFileDir(context, "/master.zip");
                File zipOut = new File(out);

                byte[] data = new byte[BUFFER_SIZE];
                long transferred = 0;
                InputStream stream = url.openStream();
                BufferedInputStream bis = new BufferedInputStream(stream);
                FileOutputStream fos = new FileOutputStream(zipOut);
                int count;
                long lastUpdateTime = System.currentTimeMillis();
                long lastTransferred = 0;
                double mb = 1024.0 * 1024.0;
                double totalMB = patchSize / mb;
                while ((count = bis.read(data, 0, BUFFER_SIZE)) != -1) {
                    fos.write(data, 0, count);
                    transferred += count;

                    int percent = (int) (transferred * 100 / patchSize);

                    long now = System.currentTimeMillis();
                    long timeDiff = now - lastUpdateTime;

                    if (timeDiff > 500) {
                        long bytesDiff = transferred - lastTransferred;

                        double speed = (double) bytesDiff / timeDiff * 1000.0; // bytes/sec
                        double speedMB = speed / mb;

                        double downloadedMB = transferred / mb;

                        double eta = speed > 0 ? (patchSize - transferred) / speed : 0;

                        String message = String.format(Locale.US,
                                "Downloading...\n%.2f / %.2f MB (size estimate)\n%.2f MB/s • ETA: %ds",
                                downloadedMB, totalMB, speedMB, (int) eta
                        );

                        publishProgress(message);
                        mProgressDialog.setProgress(percent);

                        lastUpdateTime = now;
                        lastTransferred = transferred;
                    }
                }

                String existingFolderName = KcUtils.getAppCacheFileDir(context, ENPATCH_LOCAL_FOLDER());
                File existingFolder = new File(existingFolderName);
                if (existingFolder.exists())
                    existingFolder.delete();

                publishProgress("Extracting Zip File...");
                ZipFile zipFile = new ZipFile(out);
                zipFile.extractAll(KcUtils.getAppCacheFileDir(context, ""));
                Log.e("GOTO", "zip extracted to " + KcUtils.getAppCacheFileDir(context, ""));

                publishProgress("Create .nomedia File...");
                File file = new File(getEnPatchLocalFolder(context).concat(".nomedia"));
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    Log.e("GOTO", KcUtils.getStringFromException(e));
                    return -1;
                }
                Log.e("GOTO", "Created .nomedia file");

                String latestCommit = getLatestCommit(client);
                if (latestCommit != null) {
                    saveLocalCommit(context, latestCommit);
                }

                publishProgress("Removing Zip File...");
                boolean deleted = zipOut.delete();
                return deleted ? 1 : 0;
            } catch (Exception e) {
                Log.e("GOTO", KcUtils.getStringFromException(e));
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

            Preference kantaiEnUpdate = null;
            if (fragment != null) kantaiEnUpdate =
                    fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);

            if (kantaiEnUpdate != null) {
                if (integer == 1) {
                    Log.e("GOTO", "Zip was deleted");
                    KcUtils.showToast(context, R.string.en_install_done_notification);
                    kantaiEnUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                    kantaiEnUpdate.setEnabled(false);
                } else if (integer == 0) {
                    Log.e("GOTO", "Zip wasn't deleted");
                    KcUtils.showToast(context, "Download successful but zip was not deleted");
                    kantaiEnUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                    kantaiEnUpdate.setEnabled(false);
                } else if (integer == -1) {
                    Log.e("GOTO", "Error occurred while downloading");
                    KcUtils.showToast(context, "Error occurred while downloading");
                }
            }

            if (mProgressDialog != null) mProgressDialog.dismiss();
        }
    }
}