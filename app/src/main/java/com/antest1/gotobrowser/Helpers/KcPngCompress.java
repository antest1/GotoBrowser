package com.antest1.gotobrowser.Helpers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.wrmndfzzy.pngquant.LibPngQuant;

import java.io.File;

public class KcPngCompress {
    private final static String TEMP_POSTFIX = ".compressed.png";
    private final static String FILE_POSTFIX = ".compressed.png";
    public static void execQuantTask(String path) {
        new AsyncTask<String, Object, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                Log.e("GOTO-Q", "quantize " + path);
                quantize(path);
                return null;
            }
            @Override
            protected void onPostExecute(Void v){
                Log.e("GOTO-Q", "quantize done");
            }
        }.execute();
    }

    public static void quantize(String input) {
        String output = input.replace(".png", FILE_POSTFIX);
        File inputFile = new File(input);
        File tempFile = new File(input.replace(".png", TEMP_POSTFIX));
        if (tempFile.exists()) {
            Thread.currentThread().interrupt();
        }
        new LibPngQuant().pngQuantFile(inputFile, tempFile);
        File outputFile = new File(input.replace(".png", FILE_POSTFIX));
        boolean result = tempFile.renameTo(outputFile);
        Log.e("GOTO-Q", "result: " + result);
    }

    public static String getCompressedFilePath(String path) {
        File check = new File(path.replace(".png", FILE_POSTFIX));
        if (check.exists()) return check.getAbsolutePath();
        else return path;
    }

    public static boolean isCompressed(String path) {
        if (path.contains(FILE_POSTFIX)) return true;
        File file = new File(path);
        File outfile = new File(path.replace(".png", FILE_POSTFIX));
        return file.length() < 1048576 || outfile.exists();
    }
}
