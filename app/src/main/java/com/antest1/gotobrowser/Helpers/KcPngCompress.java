package com.antest1.gotobrowser.Helpers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.wrmndfzzy.pngquant.LibPngQuant;

import java.io.File;

public class KcPngCompress {
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
        File outputFile = new File(input.replace(".png", FILE_POSTFIX));
        if (outputFile.exists()) {
            Thread.currentThread().interrupt();
        }
        boolean result = new LibPngQuant().pngQuantFile(inputFile, outputFile);
        Log.e("GOTO-Q", "result: " + result);
    }

    public static String getCompressedFilePath(String path) {
        File check = new File(path.replace(".png", FILE_POSTFIX));
        if (check.exists()) return check.getAbsolutePath();
        else return path;
    }

    public static boolean isCompressed(String path) {
        File file = new File(path);
        if (!file.exists()) return false;
        File outfile = new File(path.replace(".png", FILE_POSTFIX));
        return file.length() < 1048576 || outfile.exists();
    }

    public static boolean shouldBeCompressed(String path) {
        if (path.contains(FILE_POSTFIX)) return false;
        File file = new File(path);
        File outfile = new File(path.replace(".png", FILE_POSTFIX));
        return file.exists() && file.length() >= 1048576 && !outfile.exists();
    }

    public static boolean removeCompressedFile(String path) {
        if (!path.contains(".png")) return true;
        File target = new File(path.replace(".png", FILE_POSTFIX));
        if (target.exists()) return target.delete();
        else return true;
    }
}
