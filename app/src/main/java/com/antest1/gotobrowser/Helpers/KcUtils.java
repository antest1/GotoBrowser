package com.antest1.gotobrowser.Helpers;

import android.app.ActivityManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import com.antest1.gotobrowser.VersionDatabase;
import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class KcUtils {
    public static String getStringFromException(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString().replaceAll("\n", " / ").replaceAll("\t", "");
    }

    public static boolean checkIsPlaying (MediaPlayer player) {
        if (player == null) return false;
        try {
            return player.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static boolean unzipResource(Context context, InputStream is, String path, VersionDatabase db, String version) {
        String cache_path = context.getFilesDir().getAbsolutePath().concat("/cache");
        File dir = new File(cache_path + path);
        if (!dir.exists()) dir.mkdirs();

        JsonObject prefixInfo = null;
        ZipInputStream zis;
        try {
            String filename;
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;
            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                Log.e("GOTO", "zip - " + filename);
                if (path == null) continue;
                if (ze.isDirectory()) {
                    File fmd = new File(cache_path + path + filename);
                    if (!fmd.exists()) fmd.mkdirs();
                    continue;
                } else if (filename.contains("version.txt")) {
                    prefixInfo = new JsonObject();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }
                    String content = baos.toString();
                    Log.e("GOTO", content);
                    String[] content_list = content.split("\\n", -1);
                    for (String item: content_list) {
                        if (item.trim().length() > 0) {
                            String[] item_v = item.split("\\t");
                            String key = item_v[0];
                            String value = item_v[1].trim().replace("_", "");
                            if (value.length() > 0) prefixInfo.addProperty(key, value);
                        }
                    }
                    baos.close();
                    Log.e("GOTO", prefixInfo.toString());
                    db.overrideByPrefix(prefixInfo);
                } else {
                    FileOutputStream fout = new FileOutputStream(cache_path + path + filename);
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                    if (version != null) db.putValue(path + filename, version);
                    Log.e("GOTO", "cache resource " + path + filename +  ": " + version);
                    fout.close();
                }
                zis.closeEntry();
            }
            zis.close();
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getProcessName(Context context) {
        if (context == null) return null;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.processName;
            }
        }
        return null;
    }

    public static void clearApplicationCache(Context context, File file) {
        File dir = null;
        if (file == null) {
            dir = context.getCacheDir();
        } else {
            dir = file;
        }
        if (dir == null) return;
        File[] children = dir.listFiles();
        try {
            for (File child : children)
                if (child.isDirectory()) clearApplicationCache(context, child);
                else child.delete();
        } catch (Exception e) {
            Crashlytics.logException(e);
        }
    }

    public static String downloadResourceWithLastModified(OkHttpClient client, String fullpath, File file) {
        Request request = new Request.Builder().url(fullpath).build();
        try {
            Response response = client.newCall(request).execute();
            String last_modified = response.header("Last-Modified", "none");
            ResponseBody body = response.body();
            if (body != null) {
                InputStream in = body.byteStream();
                byte[] buffer = new byte[8 * 1024];
                int bytes;
                file.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                while ((bytes = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytes);
                }
                fos.close();
                body.close();
                return last_modified;
            } else {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

