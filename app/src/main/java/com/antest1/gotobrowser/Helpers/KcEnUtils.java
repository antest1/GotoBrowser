package com.antest1.gotobrowser.Helpers;

import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_UPDATE;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_DELETE;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.R;

import net.lingala.zip4j.ZipFile;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KcEnUtils {

    static boolean newVersionFlag = false;
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

    public static void checkKantaiEnUpdate(SettingsActivity.SettingsFragment fragment, Preference kantaiEnUpdate) {
        // To do: clean up this mess
        kantaiEnUpdate.setSummary("Checking updates...");
        kantaiEnUpdate.setEnabled(false);
        InputStream enPatchInfoFile;
        org.json.simple.JSONObject enPatchInfo;
        org.json.simple.JSONObject enPatchLocalInfo;
        String enPatchLocalInfoFileName = "EN-patch.mod.json";
        String enPatchLocalFolder = KcUtils.getAppCacheFileDir(fragment.requireContext(), "/KanColle-English-Patch-KCCP-master/");
        String enPatchLocalInfoPath = enPatchLocalFolder.concat(enPatchLocalInfoFileName);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        try {
            String enPatchInfoUrl = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/EN-patch.mod.json";
            enPatchInfoFile = new URL(enPatchInfoUrl).openConnection().getInputStream();
            JSONParser jsonParser = new JSONParser();
            enPatchInfo = (org.json.simple.JSONObject) jsonParser.parse(
                    new InputStreamReader(enPatchInfoFile, StandardCharsets.UTF_8));
            String availableVersion = String.valueOf(enPatchInfo.get("version"));
            File enPatchLocalInfoFile = new File(enPatchLocalInfoPath);
            if (!enPatchLocalInfoFile.exists()) {
                kantaiEnUpdate.setSummary(String.format(Locale.US,
                        "Data not installed yet. (%s)",
                        availableVersion));
                kantaiEnUpdate.setEnabled(true);
                newVersionFlag = false;
            } else {
                enPatchLocalInfo = (org.json.simple.JSONObject) jsonParser.parse(
                        new FileReader(enPatchLocalInfoFile));
                currentVersion = String.valueOf(enPatchLocalInfo.get("version"));
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
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public static String checkKantaiEnUpdateEntrance(Context context) {
        // To do: clean up this mess
        InputStream enPatchInfoFile;
        org.json.simple.JSONObject enPatchInfo;
        org.json.simple.JSONObject enPatchLocalInfo;
        String enPatchLocalInfoFileName = "EN-patch.mod.json";
        String enPatchLocalFolder = KcUtils.getAppCacheFileDir(context, "/KanColle-English-Patch-KCCP-master/");
        String enPatchLocalInfoPath = enPatchLocalFolder.concat(enPatchLocalInfoFileName);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        try {
            String enPatchInfoUrl = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/EN-patch.mod.json";
            enPatchInfoFile = new URL(enPatchInfoUrl).openConnection().getInputStream();
            JSONParser jsonParser = new JSONParser();
            enPatchInfo = (org.json.simple.JSONObject) jsonParser.parse(
                    new InputStreamReader(enPatchInfoFile, StandardCharsets.UTF_8));
            String availableVersion = String.valueOf(enPatchInfo.get("version"));
            enPatchInfoFile.close();
            File enPatchLocalInfoFile = new File(enPatchLocalInfoPath);
            if (enPatchLocalInfoFile.exists()) {
                enPatchLocalInfo = (org.json.simple.JSONObject) jsonParser.parse(
                        new FileReader(enPatchLocalInfoFile));
                currentVersion = String.valueOf(enPatchLocalInfo.get("version"));
                if (!currentVersion.equals(availableVersion)) {
                    return availableVersion;
                }
            }
            return null;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean remoteFileExists(String URLName){
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void requestPatchUpdate(SettingsActivity.SettingsFragment fragment) throws IOException {
        // To do: clean up this mess
        Context context = fragment.requireContext();
        CompletableFuture
                .runAsync(() -> {
                    if (newVersionFlag) {

                        // Updates the patch by downloading each new file individually, and deleting outdated ones
                        try {
                            Version installedVersion = new Version(currentVersion);
                            InputStream enPatchVersionsFile;
                            org.json.simple.JSONArray enPatchVersions;
                            String enPatchVersionsUrl = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/version.json";
                            if (remoteFileExists(enPatchVersionsUrl)) {
                                URL verUrl = new URL(enPatchVersionsUrl);
                                ReadableByteChannel rbc = Channels.newChannel(verUrl.openStream());
                                String verPath = KcUtils.getAppCacheFileDir(context, "/KanColle-English-Patch-KCCP-master/version.json");
                                File verFile = new File(verPath);
                                FileOutputStream fos = FileUtils.openOutputStream(verFile);
                                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                            }
                            enPatchVersionsFile = new URL(enPatchVersionsUrl).openConnection().getInputStream();
                            JSONParser jsonParser = new JSONParser();
                            enPatchVersions = (org.json.simple.JSONArray) jsonParser.parse(
                                    new InputStreamReader(enPatchVersionsFile, StandardCharsets.UTF_8));

                            ArrayList<List<String>> delUrl = new ArrayList<>();
                            ArrayList<List<String>> addUrl = new ArrayList<>();

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

                            for (int i = 0; i < enPatchVersions.size(); i++) {
                                JSONObject ver = new JSONObject(enPatchVersions.get(i).toString());
                                Version lookoverVersion = new Version(String.valueOf(ver.get("version")));
                                Log.i("GOTO", "installedVersion: " + installedVersion);
                                Log.i("GOTO", "lookoverVersion: " + lookoverVersion);
                                Log.i("GOTO", "compare: " + installedVersion.compareTo(lookoverVersion));
                                if (installedVersion.compareTo(lookoverVersion) == -1) {
                                    List<String> currDelUrl = new ArrayList<>();
                                    List<String> currAddUrl = new ArrayList<>();
                                    JSONArray del = ver.getJSONArray("del");
                                    JSONArray add = ver.getJSONArray("add");
                                    for (int j = 0; j < del.length(); j++) {
                                        currDelUrl.add(del.getString(j));
                                    }
                                    for (int j = 0; j < add.length(); j++) {
                                        currAddUrl.add(add.getString(j));
                                    }
                                    delUrl.add(currDelUrl);
                                    addUrl.add(currAddUrl);
                                }
                            }
                            Log.e("GOTO", "delUrl: " + delUrl);
                            Log.e("GOTO", "addUrl: " + addUrl);
                            for (int i = 0; i < delUrl.size(); i++) {
                                for (int j = 0; j < delUrl.get(i).size(); j++) {
                                    String delPath = KcUtils.getAppCacheFileDir(context, "/KanColle-English-Patch-KCCP-master/".concat(delUrl.get(i).get(j)));
                                    File delFile = new File(delPath);
                                    if (delFile.exists() && delFile.delete()) {
                                        Log.e("GOTO", "patch file deleted: " + delPath);
                                    }
                                }
                                Instant start = Instant.now();
                                Instant end;
                                for (int j = 0; j < addUrl.get(i).size(); j++) {
                                    String addPath = KcUtils.getAppCacheFileDir(context, "/KanColle-English-Patch-KCCP-master/".concat(addUrl.get(i).get(j)));
                                    File addFile = new File(addPath);
                                    String masterPath = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/".concat(addUrl.get(i).get(j));
                                    if (remoteFileExists(masterPath)) {
                                        URL master = new URL(masterPath);
                                        ReadableByteChannel rbc = Channels.newChannel(master.openStream());
                                        FileOutputStream fos = FileUtils.openOutputStream(addFile);
                                        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                                        fos.close();
                                        Log.e("GOTO", "patch file downloaded: " + addPath);
                                        end = Instant.now();
                                        if (Duration.between(start, end).compareTo(Duration.ofSeconds(5)) > 0) {
                                            start = Instant.now();
                                            String version_progress = String.valueOf(i + 1);
                                            String file_progress = String.valueOf(j);
                                            int finalI = i;
                                            handler.post(() -> KcUtils.showToastShort(context, String.format("English Patch Update %s/%s\n%s/%s %s",
                                                    version_progress, delUrl.size(), file_progress, addUrl.get(finalI).size(), "files downloaded")));
                                        }
                                    }
                                }
                            }
                            handler.post(() -> {
                                KcUtils.showToastShort(context, R.string.en_update_done);
                            });
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "en_patch")
                                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                    .setContentTitle("KanColle English Patch")
                                    .setContentText(context.getString(R.string.en_update_done_notification))
                                    .setPriority(NotificationCompat.PRIORITY_MAX);

                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                            notificationManager.notify(0, builder.build());
                        } catch (IOException | ParseException | JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Downloads and extracts the English Patch zip
                        try {
                            org.json.simple.JSONObject enPatchInfo;
                            InputStream enPatchInfoFile;
                            String enPatchInfoUrl = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/EN-patch.mod.json";
                            enPatchInfoFile = new URL(enPatchInfoUrl).openConnection().getInputStream();
                            JSONParser jsonParserInfo = new JSONParser();
                            enPatchInfo = (org.json.simple.JSONObject) jsonParserInfo.parse(
                                    new InputStreamReader(enPatchInfoFile, StandardCharsets.UTF_8));
                            String patchSize = String.valueOf(enPatchInfo.get("size"));
                            if (patchSize.equals("null")) {
                                patchSize = "310000000";
                            }
                            Log.e("GOTO", "patchSize: " + patchSize);

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

                            Log.e("GOTO", "english patch download start");
                            URL url = new URL("https://github.com/Oradimi/KanColle-English-Patch-KCCP/archive/refs/heads/master.zip");
                            String out = KcUtils.getAppCacheFileDir(context, "/master.zip");
                            File zipOut = new File(out);

                            byte[] data = new byte[8192];
                            long transferred = 0;
                            Instant start = Instant.now();
                            Instant end;
                            InputStream stream = url.openStream();
                            BufferedInputStream bis = new BufferedInputStream(stream);
                            FileOutputStream fos = new FileOutputStream(zipOut);
                            int count;
                            Instant countStart = Instant.now();
                            while ((count = bis.read(data, 0, 8192)) != -1) {
                                fos.write(data, 0, count);
                                transferred += count;
                                end = Instant.now();
                                long finalTransferred = transferred * 100 / Integer.parseInt(patchSize);
                                if (Duration.between(start, end).compareTo(Duration.ofSeconds(5)) > 0) {
                                    start = Instant.now();
                                    handler.post(() -> KcUtils.showToastShort(context, String.format("English Patch Download\nabout %s%s", finalTransferred, "% done")));
                                }
                            }
                            Instant countEnd = Instant.now();
                            Log.e("GOTO", "english patch download complete: took " + Duration.between(countStart, countEnd));
                            handler.post(() -> KcUtils.showToastShort(context, R.string.en_unzip_start));

                            ZipFile zipFile = new ZipFile(out);
                            zipFile.extractAll(KcUtils.getAppCacheFileDir(context, ""));
                            Log.e("GOTO", "zip extracted to " + KcUtils.getAppCacheFileDir(context, ""));

                            File file = new File(KcUtils.getAppCacheFileDir(context, "/KanColle-English-Patch-KCCP-master/.nomedia"));
                            try {
                                file.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Log.e("GOTO", "Created .nomedia file");

                            boolean deleted = zipOut.delete();
                            if (deleted) {
                                Log.e("GOTO", "Zip was deleted");
                                handler.post(() -> {
                                    KcUtils.showToastShort(context, R.string.en_unzip_done);
                                });
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "en_patch")
                                        .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                        .setContentTitle("KanColle English Patch")
                                        .setContentText(context.getString(R.string.en_install_done_notification))
                                        .setPriority(NotificationCompat.PRIORITY_MAX);

                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                                notificationManager.notify(0, builder.build());
                            } else {
                                Log.e("GOTO", "Zip wasn't deleted");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .whenComplete((input, exception) -> {
                    if (exception != null) {
                        System.out.println("exception occurs");
                        System.err.println(exception);
                        Log.e("GOTO", "Something went wrong with the patch download");
                    } else {
                        System.out.println("no exception, got result: " + input);
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void requestPatchUpdateEntrance(Context context) {
        // To do: clean up this mess
        CompletableFuture
                .runAsync(() -> {
                    // Updates the patch by downloading each new file individually, and deleting outdated ones
                    try {
                        Version installedVersion = new Version(currentVersion);
                        InputStream enPatchVersionsFile;
                        org.json.simple.JSONArray enPatchVersions;
                        String enPatchVersionsUrl = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/version.json";
                        if (remoteFileExists(enPatchVersionsUrl)) {
                            URL verUrl = new URL(enPatchVersionsUrl);
                            ReadableByteChannel rbc = Channels.newChannel(verUrl.openStream());
                            String verPath = KcUtils.getAppCacheFileDir(context, "/KanColle-English-Patch-KCCP-master/version.json");
                            File verFile = new File(verPath);
                            FileOutputStream fos = FileUtils.openOutputStream(verFile);
                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                        }
                        enPatchVersionsFile = new URL(enPatchVersionsUrl).openConnection().getInputStream();
                        JSONParser jsonParser = new JSONParser();
                        enPatchVersions = (org.json.simple.JSONArray) jsonParser.parse(
                                new InputStreamReader(enPatchVersionsFile, StandardCharsets.UTF_8));

                        ArrayList<List<String>> delUrl = new ArrayList<>();
                        ArrayList<List<String>> addUrl = new ArrayList<>();

                        Handler handler = new Handler(context.getMainLooper());
                        handler.post(() -> {
                            KcUtils.showToastShort(context, R.string.en_download_start);
                        });

                        for (int i = 0; i < enPatchVersions.size(); i++) {
                            JSONObject ver = new JSONObject(enPatchVersions.get(i).toString());
                            Version lookoverVersion = new Version(String.valueOf(ver.get("version")));
                            Log.i("GOTO", "installedVersion: " + installedVersion);
                            Log.i("GOTO", "lookoverVersion: " + lookoverVersion);
                            Log.i("GOTO", "compare: " + installedVersion.compareTo(lookoverVersion));
                            if (installedVersion.compareTo(lookoverVersion) == -1) {
                                List<String> currDelUrl = new ArrayList<>();
                                List<String> currAddUrl = new ArrayList<>();
                                JSONArray del = ver.getJSONArray("del");
                                JSONArray add = ver.getJSONArray("add");
                                for (int j = 0; j < del.length(); j++) {
                                    currDelUrl.add(del.getString(j));
                                }
                                for (int j = 0; j < add.length(); j++) {
                                    currAddUrl.add(add.getString(j));
                                }
                                delUrl.add(currDelUrl);
                                addUrl.add(currAddUrl);
                            }
                        }
                        Log.e("GOTO", "delUrl: " + delUrl);
                        Log.e("GOTO", "addUrl: " + addUrl);
                        for (int i = 0; i < delUrl.size(); i++) {
                            for (int j = 0; j < delUrl.get(i).size(); j++) {
                                String delPath = KcUtils.getAppCacheFileDir(context, "/KanColle-English-Patch-KCCP-master/".concat(delUrl.get(i).get(j)));
                                File delFile = new File(delPath);
                                if (delFile.exists() && delFile.delete()) {
                                    Log.e("GOTO", "patch file deleted: " + delPath);
                                }
                            }
                            Instant start = Instant.now();
                            Instant end;
                            for (int j = 0; j < addUrl.get(i).size(); j++) {
                                String addPath = KcUtils.getAppCacheFileDir(context, "/KanColle-English-Patch-KCCP-master/".concat(addUrl.get(i).get(j)));
                                File addFile = new File(addPath);
                                String masterPath = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/".concat(addUrl.get(i).get(j));
                                if (remoteFileExists(masterPath)) {
                                    URL master = new URL(masterPath);
                                    ReadableByteChannel rbc = Channels.newChannel(master.openStream());
                                    FileOutputStream fos = FileUtils.openOutputStream(addFile);
                                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                                    fos.close();
                                    Log.e("GOTO", "patch file downloaded: " + addPath);
                                    end = Instant.now();
                                    if (Duration.between(start, end).compareTo(Duration.ofSeconds(5)) > 0) {
                                        start = Instant.now();
                                        String version_progress = String.valueOf(i + 1);
                                        String file_progress = String.valueOf(j);
                                        int finalI = i;
                                        handler.post(() -> KcUtils.showToastShort(context, String.format("English Patch Update %s/%s\n%s/%s %s",
                                                version_progress, delUrl.size(), file_progress, addUrl.get(finalI).size(), "files downloaded")));
                                    }
                                }
                            }
                        }
                        handler.post(() -> {
                            KcUtils.showToastShort(context, R.string.en_update_done);
                        });
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "en_patch")
                                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                .setContentTitle("KanColle English Patch")
                                .setContentText(context.getString(R.string.en_update_done_notification))
                                .setPriority(NotificationCompat.PRIORITY_MAX);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                        notificationManager.notify(0, builder.build());
                    } catch (IOException | ParseException | JSONException e) {
                        e.printStackTrace();
                    }
                })
                .whenComplete((input, exception) -> {
                    if (exception != null) {
                        System.out.println("exception occurs");
                        System.err.println(exception);
                        Log.e("GOTO", "Something went wrong with the patch download");
                    } else {
                        System.out.println("no exception, got result: " + input);
                    }
                });
    }

    public static void requestPatchDelete(SettingsActivity.SettingsFragment fragment) {
        Context context = fragment.requireContext();
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle(R.string.settings_mod_kantaien_delete);
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(R.string.settings_mod_kantaien_delete_summary)
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            deleteEnglishPatch(context);
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
        File patchFolder = new File(KcUtils.getAppCacheFileDir(fragment, "/KanColle-English-Patch-KCCP-master/"));
        deleteRecursive(patchFolder);
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Set<String> listFiles(String dir) {
        return Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
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
}