package com.antest1.gotobrowser.Helpers;

import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_UPDATE;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.Browser.ResourceProcess;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static void checkKantaiEnUpdate(SettingsActivity.SettingsFragment fragment, Preference kantaiEnUpdate) {
        // To do: clean up this mess
        String absolutePath = fragment.requireContext().getExternalFilesDir(null).getAbsolutePath();
        kantaiEnUpdate.setSummary("Checking updates...");
        kantaiEnUpdate.setEnabled(false);
        InputStream enPatchInfoFile;
        org.json.simple.JSONObject enPatchInfo;
        org.json.simple.JSONObject enPatchLocalInfo;
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void requestPatchUpdate(SettingsActivity.SettingsFragment fragment, Activity ac, Context context) throws IOException {
        // To do: clean up this mess
        CompletableFuture
                .runAsync(() -> {
                    if (newVersionFlag) {
                        try {
                            String absolutePath = context.getExternalFilesDir(null).getAbsolutePath();
                            Version installedVersion = new Version(currentVersion);
                            InputStream enPatchVersionsFile;
                            org.json.simple.JSONArray enPatchVersions;
                            String enPatchVersionsUrl = "https://raw.githubusercontent.com/Oradimi/KanColle-English-Patch-KCCP/master/version.json";
                            if (remoteFileExists(enPatchVersionsUrl)) {
                                URL verUrl = new URL(enPatchVersionsUrl);
                                ReadableByteChannel rbc = Channels.newChannel(verUrl.openStream());
                                String verPath = absolutePath + "/KanColle-English-Patch-KCCP-master/version.json";
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
                                KcUtils.showToastShort(ac, R.string.download_start);
                                Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                                assert kantaiEnUpdate != null;
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
                                    String delPath = absolutePath + "/KanColle-English-Patch-KCCP-master/" + delUrl.get(i).get(j);
                                    File delFile = new File(delPath);
                                    if (delFile.exists()) {
                                        boolean deleted = delFile.delete();
                                        if (deleted) {
                                            Log.e("GOTO", "Deleted: " + delPath);
                                        }
                                    }
                                }
                                for (int j = 0; j < addUrl.get(i).size(); j++) {
                                    String addPath = absolutePath + "/KanColle-English-Patch-KCCP-master/" + addUrl.get(i).get(j);
                                    File addFile = new File(addPath);
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
                            handler.post(() -> KcUtils.showToastShort(ac, R.string.installation_done));
                        } catch (IOException | ParseException | JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            Handler handler = new Handler(context.getMainLooper());
                            handler.post(() -> {
                                KcUtils.showToastShort(ac, R.string.download_start);
                                Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                                assert kantaiEnUpdate != null;
                                kantaiEnUpdate.setSummary("Downloading... Wait for 'Installation Complete' toast. This can take a while!");
                                kantaiEnUpdate.setEnabled(false);
                            });

                            Log.e("GOTO", "Download start");
                            URL url = new URL("https://github.com/Oradimi/KanColle-English-Patch-KCCP/archive/master.zip");
                            String absolutePath = context.getExternalFilesDir(null).getAbsolutePath();
                            String out = absolutePath + "/master.zip";
                            File zipOut = new File(out);

                            byte data[] = new byte[8192];
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
                                long finalTransferred = transferred / 3104000;
                                Log.e("GOTO", "Count: " + count);
                                Log.e("GOTO", "Transferred so far: " + transferred);
                                Log.e("GOTO", "Duration: " + Duration.between(start, end).compareTo(Duration.ofSeconds(5)));
                                if (Duration.between(start, end).compareTo(Duration.ofSeconds(5)) > 0) {
                                    start = Instant.now();
                                    handler.post(() -> {
                                        KcUtils.showToastShort(context, String.format("English Patch Download\nabout %s%s", finalTransferred, "% done"));
                                    });
                                }
                            }
                            Instant countEnd = Instant.now();
                            Log.e("GOTO", "English Patch download took " + Duration.between(countStart, countEnd) + " seconds");
                            handler.post(() -> KcUtils.showToastShort(ac, R.string.installation_start));
                            Log.e("GOTO", "Download complete");

                            ZipFile zipFile = new ZipFile(out);
                            Log.e("GOTO", "absolutePath: " + absolutePath);
                            zipFile.extractAll(absolutePath);
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
                                handler.post(() -> {
                                    Log.e("GOTO", "Zip successfully deleted");
                                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(context);
                                    mBuilder.setTitle("KanColle English Patch");
                                    mBuilder.setCancelable(false)
                                            .setMessage("The patch download and unzipping were successfully completed!")
                                            .setPositiveButton(R.string.action_ok,
                                                    (dialog, id) -> dialog.cancel());
                                    AlertDialog alertDialog = mBuilder.create();
                                    alertDialog.show();
                                });
                                //handler.post(() -> KcUtils.showToast(ac, R.string.installation_done));
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
                        KcUtils.showToastShort(ac, "Something went wrong with the patch download");
                    } else {
                        System.out.println("no exception, got result: " + input);
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Set<String> listFiles(String dir) {
        return Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean checkPatchValidity(String local_path, String destination, String origin) {
        Log.e("GOTO", "this is the destination boiiiiiii" + destination);
        if (ResourceProcess.isImage(ResourceProcess.getCurrentState(destination))) {
            Log.e("GOTO", "00000000000004");
            File metadata_file = new File(local_path.replace(".png", ".json"));
            File dest = new File(destination);
            if (!metadata_file.exists()) {
                File source = new File(origin.concat("/patched.png"));
                try {
                    Log.e("GOTO", "00000000000005");
                    FileUtils.copyFile(source, dest);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    JSONParser jsonParser = new JSONParser();
                    org.json.simple.JSONObject metadata = null;
                    metadata = (org.json.simple.JSONObject) jsonParser.parse(
                            new FileReader(metadata_file));
                    Set<String> original_files = listFiles(origin.concat("/original/"));
                    Set<String> patched_files = listFiles(origin.concat("/patched/"));
                    Log.e("GOTO", "welsh welsh" + original_files.toString());
                    Log.e("GOTO", "wash wash" + patched_files.toString());

                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }


}