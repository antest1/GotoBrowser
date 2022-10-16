package com.antest1.gotobrowser.Helpers;

import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_DELETE;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_UPDATE;
import static com.antest1.gotobrowser.Helpers.KcUtils.getStringFromException;
import static com.antest1.gotobrowser.Helpers.KcUtils.parseJsonArray;
import static com.antest1.gotobrowser.Helpers.KcUtils.parseJsonObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.R;
import com.google.gson.JsonArray;
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
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
    public static String ENPATCH_INFO_URL = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/EN-patch.mod.json";
    public static String ENPATCH_FILE_URL_ROOT = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/";
    public static String ENPATCH_VERSION_URL = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/version.json";
    public static String ENPATCH_INFO_LOCAL_FILE = "EN-patch.mod.json";
    public static String ENPATCH_LOCAL_FOLDER = "/KanColle-English-Patch-KCCP-master/";
    public static String ENPATCH_ZIP_FILE_SRC = "https://github.com/Oradimi/KanColle-English-Patch-KCCP/archive/refs/heads/master.zip";

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

    public static String getEnPatchLocalFolder(Context context) {
        return KcUtils.getAppCacheFileDir(context, ENPATCH_LOCAL_FOLDER);
    }

    public JsonObject getKantaiEnUpdateInfo() {
        JsonObject enPatchInfo = new JsonObject();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<JsonObject> result = executor.submit(() -> {
                JsonObject resultData = new JsonObject();
                Request.Builder builder = new Request.Builder().url(ENPATCH_INFO_URL);
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
                    e.printStackTrace();
                    resultData.addProperty("error", getStringFromException(e));
                }
                return resultData;
            });
            enPatchInfo = result.get();
        } catch (Exception e) {
            e.printStackTrace();
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
                Request.Builder builder = new Request.Builder().url(ENPATCH_VERSION_URL);
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
                    e.printStackTrace();
                }
                return resultData;
            });
            enVersionInfo = result.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("GOTO", "enPatchInfo: " + enVersionInfo.toString());
        return enVersionInfo;
    }

    public void checkKantaiEnUpdate(SettingsActivity.SettingsFragment fragment, Preference kantaiEnUpdate) {
        // To do: clean up this mess
        kantaiEnUpdate.setSummary("Checking updates...");
        kantaiEnUpdate.setEnabled(false);

        JsonObject enPatchLocalInfo;
        String enPatchLocalInfoPath = getEnPatchLocalFolder(fragment.requireContext()).concat(ENPATCH_INFO_LOCAL_FILE);

        JsonObject enPatchInfo = getKantaiEnUpdateInfo();
        String availableVersion = enPatchInfo.get("version").getAsString();

        File enPatchLocalInfoFile = new File(enPatchLocalInfoPath);
        if (!enPatchLocalInfoFile.exists()) {
            kantaiEnUpdate.setSummary(String.format(Locale.US,
                    "Data not installed yet. (%s)",
                    availableVersion));
            kantaiEnUpdate.setEnabled(true);
            newVersionFlag = false;
        } else {
            enPatchLocalInfo = KcUtils.readJsonObjectFromFile(enPatchLocalInfoFile.getPath());
            if (enPatchLocalInfo.has("version")) {
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
        String enPatchLocalInfoPath = getEnPatchLocalFolder(context).concat(ENPATCH_INFO_LOCAL_FILE);
        JsonObject enPatchInfo = getKantaiEnUpdateInfo();
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
            e.printStackTrace();
            return false;
        }
    }

    public void requestPatchUpdate(SettingsActivity.SettingsFragment fragment) throws IOException {
        // To do: clean up this mess
        Context context = fragment.requireContext();
        if (newVersionFlag) {
            // Updates the patch by downloading each new file individually, and deleting outdated ones
            JsonArray enPatchVersion = getKantaiEnVersionData();
            new PatchIndividualDownloader(context, enPatchVersion, fragment).execute();
        } else {
            // Downloads and extracts the English Patch zip
            JsonObject enPatchInfo = getKantaiEnUpdateInfo();
            new PatchZipDownloader(fragment, enPatchInfo).execute();
        }
    }

    public void requestPatchUpdateEntrance(Context context) {
        // To do: clean up this mess
        JsonArray enPatchVersion = getKantaiEnVersionData();
        new PatchIndividualDownloader(context, enPatchVersion, null).execute();
    }

    public void requestPatchDelete(SettingsActivity.SettingsFragment fragment) {
        Context context = fragment.requireContext();
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle(R.string.settings_mod_kantaien_delete);
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(R.string.settings_mod_kantaien_delete_summary)
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            deleteEnglishPatch(context);
                            newVersionFlag = false;
                            KcUtils.showToastShort(context, "English Patch deleted");
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private static void deleteEnglishPatch(Context fragment) {
        File zipFile = new File(KcUtils.getAppCacheFileDir(fragment, "/master.zip"));
        zipFile.delete();
        File patchFolder = new File(KcUtils.getAppCacheFileDir(fragment, ENPATCH_LOCAL_FOLDER));
        deleteRecursive(patchFolder);
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public static Set<String> listFiles(String dir) {
        Set<String> files = new HashSet<>();
        File[] fileList = new File(dir).listFiles();

        for (File f: fileList) {
            if (!f.isDirectory()) files.add(f.getName());
        }
        return files;
    }

    public static boolean bitmapEqual(Bitmap bitmap1, Bitmap bitmap2) {
        ByteBuffer buffer1 = ByteBuffer.allocate(bitmap1.getHeight() * bitmap1.getRowBytes());
        bitmap1.copyPixelsToBuffer(buffer1);

        ByteBuffer buffer2 = ByteBuffer.allocate(bitmap2.getHeight() * bitmap2.getRowBytes());
        bitmap2.copyPixelsToBuffer(buffer2);

        return Arrays.equals(buffer1.array(), buffer2.array());
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
        }
        catch(Throwable t) {t.printStackTrace();}
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

    private class PatchIndividualDownloader extends AsyncTask<Integer, String, Integer> {
        private SettingsActivity.SettingsFragment fragment;
        private Context context;
        private JsonArray enPatchVersions;
        private ProgressDialog mProgressDialog;
        private OkHttpClient client;

        public PatchIndividualDownloader(Context c, JsonArray patch_info, SettingsActivity.SettingsFragment f) {
            fragment = f;
            context = c;
            enPatchVersions = patch_info;
            client = new OkHttpClient();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setTitle("Update Patch Files");
            mProgressDialog.setMessage("Downloading...");
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(0);
            mProgressDialog.setIndeterminate(true);
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
        protected Integer doInBackground(Integer... integers) {
            Version installedVersion = new Version(currentVersion);
            try {
                ArrayList<List<String>> delUrl = new ArrayList<>();
                ArrayList<List<String>> addUrl = new ArrayList<>();

                if (enPatchVersions.size() == 0) {
                    return -2;
                } else {
                    if (fragment != null) {
                        Handler handler = new Handler(context.getMainLooper());
                        handler.post(() -> {
                            KcUtils.showToastShort(context, R.string.en_download_start);
                            Preference kantaiEn = fragment.findPreference(PREF_MOD_KANTAIEN);
                            Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                            Preference kantaiEnDelete = fragment.findPreference(PREF_MOD_KANTAIEN_DELETE);
                            kantaiEn.setEnabled(false);
                            kantaiEnUpdate.setSummary(R.string.settings_mod_kantaien_download_start);
                            kantaiEnUpdate.setEnabled(false);
                            kantaiEnDelete.setEnabled(false);
                        });
                    }

                    for (int i = 0; i < enPatchVersions.size(); i++) {
                        JsonObject ver = enPatchVersions.get(i).getAsJsonObject();
                        Log.e("GOTO", "ver[" +  String.valueOf(i) + "] " + ver.toString());
                        Version lookoverVersion = new Version(ver.get("version").getAsString());
                        Log.i("GOTO", "installedVersion: " + installedVersion);
                        Log.i("GOTO", "lookoverVersion: " + lookoverVersion);
                        Log.i("GOTO", "compare: " + installedVersion.compareTo(lookoverVersion));
                        if (installedVersion.compareTo(lookoverVersion) < 0) {
                            List<String> currDelUrl = new ArrayList<>();
                            List<String> currAddUrl = new ArrayList<>();
                            JsonArray del = ver.getAsJsonArray("del");
                            JsonArray add = ver.getAsJsonArray("add");
                            for (int j = 0; j < del.size(); j++) {
                                currDelUrl.add(del.get(j).getAsString());
                            }
                            for (int j = 0; j < add.size(); j++) {
                                currAddUrl.add(add.get(j).getAsString());
                            }
                            delUrl.add(currDelUrl);
                            addUrl.add(currAddUrl);
                        }
                    }
                    Log.e("GOTO", "delUrl: " + delUrl);
                    Log.e("GOTO", "addUrl: " + addUrl);
                    for (int i = 0; i < delUrl.size(); i++) {
                        for (int j = 0; j < delUrl.get(i).size(); j++) {
                            String delPath = KcUtils.getAppCacheFileDir(context, ENPATCH_LOCAL_FOLDER.concat(delUrl.get(i).get(j)));
                            File delFile = new File(delPath);
                            if (delFile.exists() && delFile.delete()) {
                                Log.e("GOTO", "patch file deleted: " + delPath);
                            }
                        }
                        for (int j = 0; j < addUrl.get(i).size(); j++) {
                            String addPath = KcUtils.getAppCacheFileDir(context, ENPATCH_LOCAL_FOLDER.concat(addUrl.get(i).get(j)));
                            File addFile = new File(addPath);
                            String masterPath = ENPATCH_FILE_URL_ROOT.concat(addUrl.get(i).get(j));
                            if (remoteFileExists(masterPath)) {
                                KcUtils.downloadResource(client, masterPath, addFile);
                                Log.e("GOTO", "patch file downloaded: " + addPath);

                                String version_progress = String.valueOf(i + 1);
                                String file_progress = String.valueOf(j);
                                publishProgress(String.format("[%s/%s] %s/%s %s",
                                        version_progress, delUrl.size(), file_progress, addUrl.get(i).size(), "files downloaded"));

                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("GOTO-E", KcUtils.getStringFromException(e));
                return -1;
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            mProgressDialog.dismiss();
            if (integer == 1) {
                KcUtils.showToast(context, R.string.en_update_done);
                if (fragment != null) {
                    Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                    kantaiEnUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                    kantaiEnUpdate.setEnabled(false);
                }
            } else if (integer == -1) {
                Log.e("GOTO", "Error occured while updating");
                KcUtils.showToast(context, "Error occured while updating");
            } else if (integer == -2) {
                Log.e("GOTO", "Error while processing version data");
                KcUtils.showToast(context, "Error while processing version data");
            }
        }
    }


    private class PatchZipDownloader extends AsyncTask<Integer, String, Integer> {
        private SettingsActivity.SettingsFragment fragment;
        private Context context;
        private JsonObject patchInfo;
        private ProgressDialog mProgressDialog;
        long patchSize = 310000000;

        public PatchZipDownloader(SettingsActivity.SettingsFragment f, JsonObject patch_info) {
            fragment = f;
            context = fragment.requireContext();
            patchInfo = patch_info;
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
                URL url = new URL(ENPATCH_ZIP_FILE_SRC);
                String out = KcUtils.getAppCacheFileDir(context, "/master.zip");
                File zipOut = new File(out);

                byte[] data = new byte[BUFFER_SIZE];
                long transferred = 0;
                InputStream stream = url.openStream();
                BufferedInputStream bis = new BufferedInputStream(stream);
                FileOutputStream fos = new FileOutputStream(zipOut);
                int count;
                while ((count = bis.read(data, 0, BUFFER_SIZE)) != -1) {
                    fos.write(data, 0, count);
                    transferred += count;
                    int finalTransferred = (int) (transferred * 100 / patchSize);
                    mProgressDialog.setProgress(finalTransferred);
                }

                publishProgress("Extracting Zip File...");
                ZipFile zipFile = new ZipFile(out);
                zipFile.extractAll(KcUtils.getAppCacheFileDir(context, ""));
                Log.e("GOTO", "zip extracted to " + KcUtils.getAppCacheFileDir(context, ""));

                publishProgress("Create .nomedia File...");
                File file = new File(getEnPatchLocalFolder(context).concat(".nomedia"));
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    return -1;
                }
                Log.e("GOTO", "Created .nomedia file");

                publishProgress("Removing Zip File...");
                boolean deleted = zipOut.delete();
                return deleted ? 1 : 0;
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if (integer == 1) {
                Log.e("GOTO", "Zip was deleted");
                KcUtils.showToast(context, R.string.en_install_done_notification);
                Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                kantaiEnUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                kantaiEnUpdate.setEnabled(false);
            } else if (integer == 0) {
                Log.e("GOTO", "Zip wasn't deleted");
                KcUtils.showToast(context, "Download successful but zip was not deleted");
            } else if (integer == -1) {
                Log.e("GOTO", "Error occurred while downloading");
                KcUtils.showToast(context, "Error occurred while downloading");
            }
            mProgressDialog.dismiss();
        }
    }
}