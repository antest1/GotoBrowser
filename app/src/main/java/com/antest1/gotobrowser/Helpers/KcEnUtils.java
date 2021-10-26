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
import com.antest1.gotobrowser.R;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class KcEnUtils {

    public static void checkKantaiEnUpdate(SettingsActivity.SettingsFragment fragment, Preference kantaiEnUpdate) {
        kantaiEnUpdate.setSummary("checking updates...");
        kantaiEnUpdate.setEnabled(false);
        InputStream enPatchInfoFile;
        org.json.simple.JSONObject enPatchInfo = null;
        org.json.simple.JSONObject enPatchLocalInfo = null;
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
            //The English Patch is 291401713 bytes in size.
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
            } else {
                enPatchLocalInfo = (org.json.simple.JSONObject) jsonParser.parse(
                        new FileReader(enPatchLocalInfoFile));
                String currentVersion = String.valueOf(enPatchLocalInfo.get("version"));
                if (!currentVersion.equals(availableVersion)) {
                    kantaiEnUpdate.setSummary(String.format(Locale.US,
                            fragment.getString(R.string.setting_latest_download_subtitle),
                            availableVersion));
                    kantaiEnUpdate.setEnabled(true);
                } else {
                    kantaiEnUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void requestPatchUpdate(SettingsActivity.SettingsFragment fragment, Activity ac, Context context) throws IOException {
        CompletableFuture
                .runAsync(() -> {
                    try {
                        Handler handler =  new Handler(context.getMainLooper());
                        handler.post(() -> {
                            KcUtils.showToastShort(ac, R.string.download_start);
                            Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                            kantaiEnUpdate.setSummary("Downloading...");
                            kantaiEnUpdate.setEnabled(false);
                        });
                        Log.e("GOTO", "Download start");

                        URL master = new URL("https://github.com/Oradimi/KanColle-English-Patch-KCCP/archive/master.zip");
                        ReadableByteChannel rbc = Channels.newChannel(master.openStream());
                        Path absolutePath = Paths.get(context.getExternalFilesDir(null).getAbsolutePath());
                        Path out = Paths.get(absolutePath + "/master.zip");
                        FileOutputStream fos = FileUtils.openOutputStream(new File(String.valueOf(out)));
                        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                        handler.post(() -> {
                            KcUtils.showToastShort(ac, R.string.installation_start);
                            Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                            kantaiEnUpdate.setSummary("Installing...");
                            kantaiEnUpdate.setEnabled(false);
                        });
                        Log.e("GOTO", "Download complete");

                        File zipOut = new File(String.valueOf(out));
                        ZipFile zipFile = new ZipFile(String.valueOf(out));
                        zipFile.extractAll(String.valueOf(absolutePath));
                        Log.e("GOTO", "Zip extracted");

                        boolean deleted = zipOut.delete();
                        if (deleted) {
                            Log.e("GOTO", "Zip successfully deleted");
                            handler.post(() -> {
                                KcUtils.showToastShort(ac, R.string.installation_done);
                                Preference kantaiEnUpdate = fragment.findPreference(PREF_MOD_KANTAIEN_UPDATE);
                                kantaiEnUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                                kantaiEnUpdate.setEnabled(false);
                            });
                        } else {
                            Log.e("GOTO", "Zip wasn't deleted");
                        }
                    } catch (IOException | ZipException e) {
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
}
