package com.antest1.gotobrowser.Helpers;

import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_UPDATE;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.Browser.ResourceProcess;
import com.antest1.gotobrowser.R;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class KcEnUtils {

    static boolean newVersionFlag = false;
    static String currentVersion = null;

    public static class Version implements Comparable<Version> {

        private String version;

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void checkKantaiEnUpdate(SettingsActivity.SettingsFragment fragment, Preference kantaiEnUpdate) {
        // To do: clean up this mess
        Path absolutePath = Paths.get(fragment.requireContext().getExternalFilesDir(null).getAbsolutePath());
        kantaiEnUpdate.setSummary("Checking updates...");
        kantaiEnUpdate.setEnabled(false);
        InputStream enPatchInfoFile;
        org.json.simple.JSONObject enPatchInfo = null;
        org.json.simple.JSONObject enPatchLocalInfo = null;
        String enPatchLocalInfoFileName = "EN-patch.mod.json";
        String enPatchLocalFolder = absolutePath + "/KanColle-English-Patch-KCCP-master/";
        String enPatchLocalInfoPath = enPatchLocalFolder.concat(enPatchLocalInfoFileName);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        try {
            //The English Patch is about 291401713 bytes in size.
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
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void requestPatchUpdate(SettingsActivity.SettingsFragment fragment, Activity ac, Context context) throws IOException {
        // To do: clean up this mess
        CompletableFuture
                .runAsync(() -> {
                    if (newVersionFlag) {
                        try {
                            Path absolutePath = Paths.get(context.getExternalFilesDir(null).getAbsolutePath());
                            Version installedVersion = new Version(currentVersion);
                            InputStream enPatchVersionsFile;
                            org.json.simple.JSONArray enPatchVersions = null;
                            String enPatchVersionsUrl = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/version.json";
                            if (remoteFileExists(enPatchVersionsUrl)) {
                                URL verUrl = new URL(enPatchVersionsUrl);
                                ReadableByteChannel rbc = Channels.newChannel(verUrl.openStream());
                                Path verPath = Paths.get(absolutePath + "/KanColle-English-Patch-KCCP-master/version.json");
                                File verFile = new File(String.valueOf(verPath));
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
                                KcUtils.showToastShort(ac, R.string.download_start);
                                Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                                kantaiEnUpdate.setSummary("Downloading... Wait for 'Installation Complete' toast");
                                kantaiEnUpdate.setEnabled(false);
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
                                //String progress = String.valueOf(i);
                                //handler.post(() -> {
                                //    KcUtils.showToastShort(ac, "Progress: " + progress + "/" + delUrl.size());
                                //});
                                for (int j = 0; j < delUrl.get(i).size(); j++) {
                                    Path delPath = Paths.get(absolutePath + "/KanColle-English-Patch-KCCP-master/" + delUrl.get(i).get(j));
                                    File delFile = new File(String.valueOf(delPath));
                                    if (delFile.exists()) {
                                        boolean deleted = delFile.delete();
                                        if (deleted) {
                                            Log.e("GOTO", "Deleted: " + delPath );
                                        }
                                    }
                                }
                                for (int j = 0; j < addUrl.get(i).size(); j++) {
                                    Path addPath = Paths.get(absolutePath + "/KanColle-English-Patch-KCCP-master/" + addUrl.get(i).get(j));
                                    File addFile = new File(String.valueOf(addPath));
                                    Log.e("GOTO", "path dude add" + addFile);
                                    String masterPath = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/" + addUrl.get(i).get(j);
                                    if (remoteFileExists(masterPath)) {
                                        URL master = new URL(masterPath);
                                        ReadableByteChannel rbc = Channels.newChannel(master.openStream());
                                        FileOutputStream fos = FileUtils.openOutputStream(addFile);
                                        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                                    }
                                }
                            }
                            handler.post(() -> {
                                KcUtils.showToast(ac, R.string.installation_done);
                            });
                        } catch (IOException | ParseException | JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Handler handler = new Handler(context.getMainLooper());
                            handler.post(() -> {
                                KcUtils.showToastShort(ac, R.string.download_start);
                                Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                                kantaiEnUpdate.setSummary("Downloading... Wait for 'Installation Complete' toast. This can take a while!");
                                kantaiEnUpdate.setEnabled(false);
                            });
                            Log.e("GOTO", "Download start");

                            URL master = new URL("https://github.com/Oradimi/KanColle-English-Patch-KCCP/archive/master.zip");
                            ReadableByteChannel rbc = Channels.newChannel(master.openStream());
                            Path absolutePath = Paths.get(context.getExternalFilesDir(null).getAbsolutePath());
                            Path out = Paths.get(absolutePath + "/master.zip");
                            File zipOut = new File(String.valueOf(out));
                            FileOutputStream fos = FileUtils.openOutputStream(zipOut);
                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                            handler.post(() -> {
                                KcUtils.showToastShort(ac, R.string.installation_start);
                            });
                            Log.e("GOTO", "Download complete");

                            ZipFile zipFile = new ZipFile(String.valueOf(out));
                            zipFile.extractAll(String.valueOf(absolutePath));
                            Log.e("GOTO", "Zip extracted");

                            File file = new File(absolutePath + "/KanColle-English-Patch-KCCP-master/.nomedia");
                            try {
                                file.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Log.e("GOTO", "Created .nomedia file");

                            boolean deleted = zipOut.delete();
                            if (deleted) {
                                Log.e("GOTO", "Zip successfully deleted");
                                handler.post(() -> {
                                    KcUtils.showToast(ac, R.string.installation_done);
                                });
                            } else {
                                Log.e("GOTO", "Zip wasn't deleted");
                            }
                        } catch (IOException | ZipException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .whenComplete((input, exception) -> {
                    if (exception != null) {
                        System.out.println("exception occurs");
                        System.err.println(exception);
                        Log.e("GOTO", "Something went wrong with the patch download");
                        KcUtils.showToast(ac, "Something went wrong with the patch download");
                    } else {
                        System.out.println("no exception, got result: " + input);
                    }
                });
    }

    public static boolean checkPatchValidity(String local_path, String destination, String origin) {
        Log.e("GOTO", "this is the destination boiiiiiii" + destination);
        if (ResourceProcess.isImage(ResourceProcess.getCurrentState(destination))) {
            Log.e("GOTO", "00000000000004");
            File metadata_file = new File(local_path.replace(".png", ".json"));
            if (!metadata_file.exists()) {
                File source = new File(origin.concat("/patched.png"));
                File dest = new File(destination);
                try {
                    Log.e("GOTO", "00000000000005");
                    FileUtils.copyFile(source, dest);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
