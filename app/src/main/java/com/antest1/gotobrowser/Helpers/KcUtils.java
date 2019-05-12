package com.antest1.gotobrowser.Helpers;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import com.antest1.gotobrowser.VersionDatabase;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    public static boolean unZip(Context context, InputStream is, String path, VersionDatabase db, String version) {
        String cache_path = context.getFilesDir().getAbsolutePath().concat("/cache");
        File dir = new File(cache_path + path);
        if (!dir.exists()) dir.mkdirs();

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
                }

                FileOutputStream fout = new FileOutputStream(cache_path + path + filename);
                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }
                db.putValue(path + filename, version);
                Log.e("GOTO", "cache resource " + path + filename +  ": " + version);
                fout.close();
                zis.closeEntry();
            }
            zis.close();
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}

