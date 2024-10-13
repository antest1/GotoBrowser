package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.Browser.ResourceProcess;
import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.antest1.gotobrowser.Constants.CACHE_SIZE_BYTES;
import static com.antest1.gotobrowser.Constants.PREF_USE_EXTCACHE;
import static android.webkit.WebViewClient.*;

public class KcUtils {
    private static FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showToast(Context context, int resource_id) {
        Toast.makeText(context, context.getString(resource_id), Toast.LENGTH_LONG).show();
    }

    public static void showToastShort(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showToastShort(Context context, int resource_id) {
        Toast.makeText(context, context.getString(resource_id), Toast.LENGTH_SHORT).show();
    }

    public static String getAppCacheFileDir(Context context, String folder) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_key), Context.MODE_PRIVATE);
        if (sharedPref.getBoolean(PREF_USE_EXTCACHE, false)) {
            return context.getExternalFilesDir(null).getAbsolutePath().concat(folder);
        } else {
            return context.getFilesDir().getAbsolutePath().concat(folder);
        }
    }

    public static String getStringFromException(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString().replaceAll("\n", " / ").replaceAll("\t", "");
    }

    public static void reportException(Exception e) {
        e.printStackTrace();
        crashlytics.recordException(e);
    }

    public static void reportException(ExceptionInInitializerError e) {
        e.printStackTrace();
        crashlytics.recordException(e);
    }

    public static Request getDownloadRequest(String url, String userAgent, String mimetype) {
        Request request = new Request.Builder().url(url)
                .addHeader("User-Agent", userAgent)
                .addHeader("Content-Type", mimetype)
                .addHeader("Cache-Control", "no-cache")
                .build();
        return request;
    }

    public static boolean checkIsPlaying(MediaPlayer player) {
        if (player == null) return false;
        try {
            return player.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static InputStream getEmptyStream() {
        return new InputStream() {
            @Override
            public int read() {
                return -1;
            }
        };
    }

    public static DisplayMetrics getActivityDimension(Activity activity) {
        DisplayMetrics dimension = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dimension);
        return dimension;
    }

    public static boolean unzipResource(Context context, InputStream is, String path, VersionDatabase db, String version) {
        String cache_path = getAppCacheFileDir(context, "/cache");
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
                    for (String item : content_list) {
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
                    Log.e("GOTO", "cache resource " + path + filename + ": " + version);
                    fout.close();
                }
                zis.closeEntry();
            }
            zis.close();
        } catch (IOException e) {
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

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public static void clearApplicationCache(Context context, File file) {
        File dir = null;
        if (file == null) {
            dir = context.getCacheDir();
        } else {
            dir = file;
        }
        if (dir != null) deleteRecursive(dir);
    }

    public static byte[] getBytesFromInputStream(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        in.close();
        return buffer.toByteArray();
    }

    public static String getStringFromInputStream(InputStream in) throws IOException {
        return new String(getBytesFromInputStream(in), StandardCharsets.UTF_8);
    }

    public static String getExpireInfoFromCacheControl(String cache_control) {
        Pattern p = Pattern.compile( "max-age=([0-9]+)(, public)?");
        Matcher m = p.matcher(cache_control);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

        long current_time = System.currentTimeMillis();
        long target_time = current_time;
        if (m.lookingAt() && m.group(1) != null) {
            long value = Integer.parseInt(m.group(1));
            target_time += value * 1000;
        }
        return formatter.format(new Date(target_time));
    }

    public static byte[] downloadDataFromURL(String url) throws IOException {
        InputStream in = new BufferedInputStream(new URL(url).openStream());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        in.close();
        byte[] byteArray = buffer.toByteArray();
        return byteArray;
    }

    public static String downloadResource(OkHttpClient client, String fullpath, File file) {
        Request.Builder builder = new Request.Builder()
                .header("User-Agent", ResourceProcess.getUserAgent())
                .url(fullpath);

        Log.e("GOTO", "download " + fullpath);
        Request request = builder.build();
        try {
            Response response = client.newCall(request).execute();
            if (response.code() == 200) {
                Log.e("GOTO", "200 OK " + fullpath);
                String cache_control = response.header("Cache-Control", "none");
                ResponseBody body = response.body();
                if (body != null) {
                    InputStream in = body.byteStream();
                    byte[] buffer = new byte[8 * 1024];
                    int bytes;
                    if (file != null) {
                        file.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(file);
                        while ((bytes = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytes);
                        }
                        fos.close();
                    }
                    body.close();
                }
                return cache_control;
            } else if (response.code() == 304) {
                Log.e("GOTO", "304 Not Modified " + fullpath);
                return "304";
            } else if (response.code() == 403) {
                Log.e("GOTO", "403 Forbidden" + fullpath);
                return "403";
            }
        } catch (Exception e) {
            Log.e("GOTO-E", getStringFromException(e));
            KcUtils.reportException(e);
            return null;
        }
        return null;
    }

    public static Retrofit getRetrofitAdapter(Context context, String baseUrl) {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        builder.cache(new Cache(context.getCacheDir(), CACHE_SIZE_BYTES));

        OkHttpClient client = builder.build();
        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .client(client).build();
    }

    public static String joinStr(List<String> list, String delim) {
        String resultStr = "";
        if (list.size() > 0) {
            int i;
            for (i = 0; i < list.size() - 1; i++) {
                resultStr = resultStr.concat(list.get(i));
                resultStr = resultStr.concat(delim);
            }
            resultStr = resultStr.concat(list.get(i));
        }
        return resultStr;
    }

    public static JsonObject parseJsonObject(String data) {
        final Gson gson = new Gson();
        return gson.fromJson(data, JsonObject.class);
    }

    public static JsonArray parseJsonArray(String data) {
        final Gson gson = new Gson();
        return gson.fromJson(data, JsonArray.class);
    }

    public static JsonObject readJsonObjectFromFile(String path) {
        try {
            final BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) sb.append(line);
            return parseJsonObject(sb.toString());
        } catch (IOException e) {
            KcUtils.reportException(e);
        }
        return null;
    }

    public static byte[] gzipcompress(String value) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutStream = new GZIPOutputStream(
                new BufferedOutputStream(byteArrayOutputStream));
        gzipOutStream.write(value.getBytes());
        gzipOutStream.finish();
        gzipOutStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] decompress(byte[] bytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            GZIPInputStream gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void requestLatestAppVersion(Activity ac, GotoVersionCheck appCheck, boolean show_toast) {
        Call<JsonObject> call = appCheck.version();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                Log.e("GOTO", response.headers().toString());
                if (response.code() == 200) {
                    checkAppUpdate(ac, response, show_toast);
                } else {
                    String message = "HTTP: " + response.code();
                    if (response.code() == 404) message = "No update found.";
                    Snackbar.make(ac.findViewById(R.id.main_container), message, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Snackbar.make(ac.findViewById(R.id.main_container), String.valueOf(t.getLocalizedMessage()), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private static void checkAppUpdate(Activity ac, retrofit2.Response<JsonObject> response, boolean show_toast) {
        JsonObject version_info = response.body();
        if (version_info != null && version_info.has("tag_name")) {
            Log.e("GOTO", version_info.toString());
            String tag = version_info.get("tag_name").getAsString().substring(1);
            String latest_file = String.format(Locale.US, "http://luckyjervis.com/GotoBrowser/apk_download.php?q=%s", tag);
            if (BuildConfig.VERSION_NAME.equals(tag)) {
                if (show_toast)
                    Snackbar.make(ac.findViewById(R.id.main_container), R.string.setting_latest_version, Snackbar.LENGTH_LONG).show();
            } else {
                showAppUpdateDownloadDialog(ac, tag, latest_file);
            }
        }
    }

    private static void showAppUpdateDownloadDialog(Activity ac, String tag, String latest_file) {
        if (!ac.isFinishing()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    ac);
            alertDialogBuilder.setTitle(ac.getString(R.string.app_name));
            alertDialogBuilder
                    .setCancelable(false)
                    .setMessage(String.format(Locale.US, ac.getString(R.string.setting_latest_download), tag))
                    .setPositiveButton(R.string.action_ok,
                            (dialog, id) -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(latest_file));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ac.startActivity(intent);
                            })
                    .setNegativeButton(R.string.action_cancel,
                            (dialog, id) -> dialog.cancel());
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    public static void processDataUriImage(ExecutorService executor, BrowserActivity activity, String data) {
        executor.submit(() -> {
            Context context = activity.getApplicationContext();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            String date = dateFormat.format(System.currentTimeMillis());
            String filename = "gtb-".concat(date);

            String image = data.substring(data.indexOf(",") + 1);
            byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
            Bitmap decodedImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fileUri = writeImageFile(context, filename, decodedImage);
            } else {
                fileUri = writeImageFileOld(context, filename, decodedImage);
            }

            Log.e("GOTO-DURL-P", "Path: " + fileUri.toString());
            Log.e("GOTO-DURL-P", "Image Size: " + decodedImage.getWidth() + "x" + decodedImage.getHeight());
            activity.runOnUiThread(() -> activity.showScreenshotNotification(decodedImage, fileUri));
        });
    }

    public static Uri writeImageFile(Context context, String filename, Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GotoBrowser");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename.concat(".png"));
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver contentResolver = context.getContentResolver();
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try {
            ParcelFileDescriptor pd = contentResolver.openFileDescriptor(item, "w", null);
            if (pd != null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                FileOutputStream fos = new FileOutputStream(pd.getFileDescriptor());
                fos.write(bytes.toByteArray());
                fos.close();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    contentResolver.update(item, values, null, null);
                }
                return item;
            } else {
                Log.e("GOTO", "writeImageFile pdf null");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Uri writeImageFileOld(Context context, String filename, Bitmap bitmap) {
        String root = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/GotoBrowser");
        myDir.mkdirs();
        File file = new File(myDir, filename.concat(".png"));
        Log.e("GOTO", file.getAbsolutePath());

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes.toByteArray());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename.concat(".png"));
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());

        ContentResolver contentResolver = context.getContentResolver();
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        return item;
    }

    public static String getDefaultTimeForCookie() {
        return getTimeForCookie(5);
    }

    public static String getTimeForCookie(int years) {
        Date targetTime = new Date();
        targetTime.setTime(targetTime.getTime() + (365L * years * 24 * 60 * 60 * 1000));
        DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(targetTime);
    }

    public static boolean checkIsLargeDisplay(Activity ac) {
        Display display = ac.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        float widthInches = metrics.widthPixels / metrics.xdpi;
        float heightInches = metrics.heightPixels / metrics.ydpi;
        double diagonalInches = Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));
        Log.e("GOTO-Utils", diagonalInches + " inch");
        return diagonalInches >= 7.0;
    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    public static String getWebkitErrorCodeText(int errorCode) {
        switch (errorCode) {
            case ERROR_AUTHENTICATION:
                return "ERROR_AUTHENTICATION";
            case ERROR_BAD_URL:
                return "ERROR_BAD_URL";
            case ERROR_CONNECT:
                return "ERROR_CONNECT";
            case ERROR_FAILED_SSL_HANDSHAKE:
                return "ERROR_FAILED_SSL_HANDSHAKE";
            case ERROR_FILE:
                return "ERROR_FILE";
            case ERROR_FILE_NOT_FOUND:
                return "ERROR_FILE_NOT_FOUND";
            case ERROR_HOST_LOOKUP:
                return "ERROR_HOST_LOOKUP";
            case ERROR_IO:
                return "ERROR_IO";
            case ERROR_PROXY_AUTHENTICATION:
                return "ERROR_PROXY_AUTHENTICATION";
            case ERROR_REDIRECT_LOOP:
                return "ERROR_REDIRECT_LOOP";
            case ERROR_TIMEOUT:
                return "ERROR_TIMEOUT";
            case ERROR_TOO_MANY_REQUESTS:
                return "ERROR_TOO_MANY_REQUESTS";
            case ERROR_UNSUPPORTED_AUTH_SCHEME:
                return "ERROR_UNSUPPORTED_AUTH_SCHEME";
            case ERROR_UNSUPPORTED_SCHEME:
                return "ERROR_UNSUPPORTED_SCHEME";
            case ERROR_UNSAFE_RESOURCE:
                return "ERROR_UNSAFE_RESOURCE";
            default:
                return String.format(Locale.US, "ERROR_UNKNOWN (%d)", errorCode);
        }
    }
}