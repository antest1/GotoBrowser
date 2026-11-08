package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.Browser.ResourceProcess;
import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.ui.component.DialogButton;
import com.antest1.gotobrowser.ui.component.MessageDialog;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLException;

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
import static com.antest1.gotobrowser.Constants.KCANOTIFY_PACKAGE_NAME;
import static com.antest1.gotobrowser.Constants.PREF_USE_EXTCACHE;
import static android.net.http.SslError.SSL_DATE_INVALID;
import static android.net.http.SslError.SSL_EXPIRED;
import static android.net.http.SslError.SSL_IDMISMATCH;
import static android.net.http.SslError.SSL_INVALID;
import static android.net.http.SslError.SSL_NOTYETVALID;
import static android.net.http.SslError.SSL_UNTRUSTED;
import static android.webkit.WebViewClient.*;

public class KcUtils {
    private static final FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

    private static final Gson gson = new Gson();

    public static Gson getGson() {
        return gson;
    }

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
            File externalFilesDir = context.getExternalFilesDir(null);
            if (externalFilesDir != null) {
                return externalFilesDir.getAbsolutePath().concat(folder);
            }
        }
        File filesDir = context.getFilesDir();
        return filesDir.getAbsolutePath().concat(folder);
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

    // For download paths: connectivity failures (timeouts, DNS, dropped connections) are
    // expected on mobile networks and say nothing about the app, so they only go to the
    // Crashlytics log as context instead of opening an issue.
    public static void reportDownloadException(Exception e) {
        if (isNetworkException(e) || e instanceof HttpStatusException) {
            e.printStackTrace();
            crashlytics.log("download failed: " + e);
        } else {
            reportException(e);
        }
    }

    public static boolean isNetworkException(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketException            // incl. ConnectException, connection abort/reset
                    || t instanceof UnknownHostException
                    || t instanceof InterruptedIOException  // incl. SocketTimeoutException
                    || t instanceof SSLException
                    || t instanceof ProtocolException       // unexpected end of stream
                    || t instanceof EOFException) {         // truncated response
                return true;
            }
            if (t.getCause() == t) break;
        }
        return false;
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

    public static String getProcessName(Context context) {
        if (context != null) {
            ActivityManager manager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo>
                    processList = manager.getRunningAppProcesses();
            if (processList != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : processList) {
                    if (processInfo.pid == android.os.Process.myPid()) {
                        return processInfo.processName;
                    }
                }
            }
        }
        return BuildConfig.APPLICATION_ID;
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
        byte[] data = new byte[16384];
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

        long target_time = System.currentTimeMillis();
        if (m.lookingAt()) {
            String match = m.group(1);
            if (match != null) {
                long value = Integer.parseInt(match);
                target_time += value * 1000;
            }
        }
        return formatter.format(new Date(target_time));
    }

    // The server answered with an error status (e.g. 404); not a fault in the app.
    public static class HttpStatusException extends IOException {
        public final int statusCode;

        public HttpStatusException(String url, int statusCode) {
            super("HTTP " + statusCode + " " + url);
            this.statusCode = statusCode;
        }
    }

    public static byte[] downloadDataFromURL(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            int statusCode = connection.getResponseCode();
            if (statusCode >= 400) throw new HttpStatusException(url, statusCode);
            try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                return buffer.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    public static class DownloadResult {
        public int responseCode = -1;
        public String lastModified;
        public String cacheControl;
        public String error;
        // Set with responseCode == -1: true for connectivity failures, false for local file errors.
        public boolean networkError;
    }

    public static DownloadResult downloadResource(OkHttpClient client, String fullpath, File file) {
        return downloadResource(client, fullpath, file, null);
    }

    public static DownloadResult downloadResource(OkHttpClient client, String fullpath, File file, String if_modified_since) {
        Request.Builder builder = new Request.Builder()
                .header("User-Agent", ResourceProcess.getUserAgent())
                .url(fullpath);
        if (if_modified_since != null) {
            builder = builder.addHeader("If-Modified-Since", if_modified_since);
        }
        DownloadResult downloadResult = new DownloadResult();

        Log.e("GOTO", "download " + fullpath + " " + if_modified_since);
        Request request = builder.build();
        File temp = null;
        try (Response response = client.newCall(request).execute()) {
            downloadResult.responseCode = response.code();
            downloadResult.lastModified = response.header("Last-Modified", null);
            downloadResult.cacheControl = response.header("Cache-Control", null);

            if (response.code() == 200) {
                Log.e("GOTO", "200 OK " + fullpath);
                ResponseBody body = response.body();
                if (body != null) {
                    InputStream in = body.byteStream();
                    byte[] buffer = new byte[16384];
                    int bytes;
                    if (file != null) {
                        temp = newTempSibling(file);
                        try (FileOutputStream fos = new FileOutputStream(temp)) {
                            while ((bytes = in.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytes);
                            }
                        }
                        if (!temp.renameTo(file)) {
                            throw new IOException("failed to replace " + file.getPath());
                        }
                        temp = null;
                    }
                }
            } else if (response.code() == 304) {
                Log.e("GOTO", "304 Not Modified " + fullpath);
            } else if (response.code() == 403) {
                Log.e("GOTO", "403 Forbidden " + fullpath);
            }
        } catch (Exception e) {
            Log.e("GOTO-E", getStringFromException(e));
            KcUtils.reportDownloadException(e);
            // A failure while saving the body must not look like a successful 200.
            downloadResult.responseCode = -1;
            downloadResult.networkError = isNetworkException(e);
            downloadResult.error = getStringFromException(e);
        } finally {
            if (temp != null) temp.delete();
        }
        return downloadResult;
    }

    public static Long extractMaxAge(String header) {
        if (header == null || header.isEmpty()) return null;

        // Regex looks for "max-age=" followed by one or more digits
        Pattern pattern = Pattern.compile("max-age=(\\d+)");
        Matcher matcher = pattern.matcher(header);

        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    public static Retrofit getRetrofitAdapter(Context context, String baseUrl) {
        return getRetrofitAdapter(context, baseUrl, true);
    }

    private static OkHttpClient sharedHttpClient;
    private static OkHttpClient retrofitBaseClient;

    /**
     * One client for the app's own HTTP calls, without a response cache. Building a client
     * sets up TLS and its own connection pool, so each new one was slow to create (often on
     * the main thread) and kept its own threads. Derive variants with {@code newBuilder()},
     * which shares all of that.
     */
    public static synchronized OkHttpClient getSharedHttpClient() {
        if (sharedHttpClient == null) {
            sharedHttpClient = new OkHttpClient();
        }
        return sharedHttpClient;
    }

    /**
     * The shared client plus the response cache, for every Retrofit adapter. One Cache
     * instance only: separate instances must not share one directory.
     */
    private static synchronized OkHttpClient getRetrofitBaseClient(Context context) {
        if (retrofitBaseClient == null) {
            File cacheDir = context.getApplicationContext().getCacheDir();
            retrofitBaseClient = getSharedHttpClient().newBuilder()
                    .cache(new Cache(cacheDir, CACHE_SIZE_BYTES))
                    .build();
        }
        return retrofitBaseClient;
    }

    public static Retrofit getRetrofitAdapter(Context context, String baseUrl, boolean followRedirects) {
        // newBuilder() shares the base client's TLS setup, connection pool and cache.
        OkHttpClient client = getRetrofitBaseClient(context).newBuilder()
                .followRedirects(followRedirects)
                .followSslRedirects(followRedirects)
                .build();
        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(getGson()))
                .baseUrl(baseUrl)
                .client(client).build();
    }

    public static String joinStr(List<String> list, String delim) {
        String resultStr = "";
        if (!list.isEmpty()) {
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
        return getGson().fromJson(data, JsonObject.class);
    }

    public static JsonArray parseJsonArray(String data) {
        return getGson().fromJson(data, JsonArray.class);
    }

    public static JsonObject parseJsonObject(java.io.Reader reader) {
        return getGson().fromJson(reader, JsonObject.class);
    }

    public static JsonArray parseJsonArray(java.io.Reader reader) {
        return getGson().fromJson(reader, JsonArray.class);
    }

    public static JsonObject readJsonObjectFromFile(String path) {
        File file = new File(path);
        // Not downloaded yet is an expected state, not an error worth reporting.
        if (!file.exists()) return null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return getGson().fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            KcUtils.reportException(e);
        } catch (JsonParseException e) {
            // Truncated or corrupted file: drop it so it can be downloaded again.
            KcUtils.reportException(e);
            file.delete();
        }
        return null;
    }

    // Writes through a temp sibling and renames it over the target, so a reader
    // never sees a partially written file. Throws if the target was not replaced.
    public static void writeStringToFileAtomically(File dest, String content) throws IOException {
        File temp = newTempSibling(dest);
        try (OutputStream os = new FileOutputStream(temp)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            temp.delete();
            throw e;
        }
        if (!temp.renameTo(dest)) {
            temp.delete();
            throw new IOException("failed to replace " + dest.getPath());
        }
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
        requestLatestAppVersion(ac, appCheck, show_toast, null);
    }

    /**
     * Checks the latest release.
     *
     * @param onMessage when non-null, the outcome text is delivered here instead
     *        of being shown in the activity's window. This lets a host that owns
     *        its own surface (e.g. a modal bottom sheet in its own window) display
     *        the message where the user can actually see it.
     */
    public static void requestLatestAppVersion(Activity ac, GotoVersionCheck appCheck,
                                               boolean show_toast, MessageCallback onMessage) {
        Call<Void> call = appCheck.latestRelease();
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull retrofit2.Response<Void> response) {
                int code = response.code();
                if (code >= 300 && code < 400) {
                    String tag = parseReleaseTag(response.headers().get("Location"));
                    if (tag != null) {
                        checkAppUpdate(ac, tag, show_toast, onMessage);
                    } else {
                        // Without any release, GitHub redirects to the release list.
                        report(ac, "No update found.", onMessage);
                    }
                } else {
                    String message = "HTTP: " + code;
                    if (code == 404) message = "No update found.";
                    report(ac, message, onMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                report(ac, String.valueOf(t.getLocalizedMessage()), onMessage);
            }
        });
    }

    /** Delivers the message to the callback when present, else to the window. */
    private static void report(Activity ac, String message, MessageCallback onMessage) {
        if (onMessage != null) {
            onMessage.onMessage(message);
        } else {
            showMessage(ac, message);
        }
    }

    /**
     * Shows a message to the user, preferring the activity's window but falling
     * back to a plain toast when the content view is not available yet (e.g. the
     * check fires before the activity has set its content view).
     */
    private static void showMessage(Activity ac, String message) {
        if (ac.isFinishing() || ac.isDestroyed()) return;
        showToast(ac.getApplicationContext(), message);
    }

    /** Extracts "3.0-rev9" from a ".../releases/tag/v3.0-rev9" redirect target. */
    private static String parseReleaseTag(String location) {
        if (location == null) return null;
        String marker = "/releases/tag/";
        int idx = location.indexOf(marker);
        if (idx < 0) return null;
        String tag = Uri.decode(location.substring(idx + marker.length()));
        if (tag.startsWith("v")) tag = tag.substring(1);
        return tag.isEmpty() ? null : tag;
    }

    private static void checkAppUpdate(Activity ac, String tag, boolean show_toast,
                                       MessageCallback onMessage) {
        Log.e("GOTO", "latest release: " + tag);
        String latest_file = String.format(Locale.US,
                "https://luckyjervis.com/GotoBrowser/apk_download.php?q=%s", tag);
        if (BuildConfig.VERSION_NAME.equals(tag)) {
            if (show_toast)
                report(ac, ac.getString(R.string.setting_latest_version), onMessage);
        } else {
            // A newer release exists: the dialog must be shown, so it stays
            // on the activity's window even when a message callback is set.
            showAppUpdateDownloadDialog(ac, tag, latest_file, onMessage);
        }
    }

    private static void showAppUpdateDownloadDialog(Activity ac, String tag, String latest_file,
                                                    MessageCallback onMessage) {
        // Drawn by the browser's Compose layer so it follows the settings theme.
        // BrowserActivity is the only activity, so nothing else can host it.
        if (!(ac instanceof BrowserActivity)) return;
        ((BrowserActivity) ac).showMessageDialog(new MessageDialog(
                ac.getString(R.string.app_name),
                String.format(Locale.US, ac.getString(R.string.setting_latest_download), tag),
                new DialogButton(ac.getString(R.string.action_ok), () -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(latest_file));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ac.startActivity(intent);
                    // The sheet stays open behind the dialog, so tell it
                    // to close now that the user is leaving to download.
                    if (onMessage != null) onMessage.onMessage(null);
                }),
                new DialogButton(ac.getString(R.string.action_cancel))));
    }

    public static void processDataUriImage(ExecutorService executor,
                                           BrowserActivity activity, String data) {
        executor.submit(() -> {
            Context context = activity.getApplicationContext();
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "yyyy-MM-dd HH-mm-ss", Locale.US);
            String date = dateFormat.format(System.currentTimeMillis());
            String filename = "gtb-".concat(date);

            String image = data.substring(data.indexOf(",") + 1);
            byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
            Bitmap decodedImage = BitmapFactory.decodeByteArray(
                    decodedString, 0, decodedString.length);
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fileUri = writeImageFile(context, filename, decodedImage);
            } else {
                fileUri = writeImageFileOld(context, filename, decodedImage);
            }

            if (fileUri != null) {
                Log.e("GOTO-DURL-P", "Path: " + fileUri);
                Log.e("GOTO-DURL-P", "Image Size: " +
                        decodedImage.getWidth() + "x" + decodedImage.getHeight());
            }
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
            if (item != null) {
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
            } else {
                Log.e("GOTO", "writeImageFile resolve result null");
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
        int years = 5;
        Date targetTime = new Date();
        targetTime.setTime(targetTime.getTime() + (365L * years * 24 * 60 * 60 * 1000));
        return getTimeForCookie(targetTime);
    }

    public static String getTimeForCookie(Date datetime) {
        DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(datetime);
    }

    public static boolean isKcanotifyInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(KCANOTIFY_PACKAGE_NAME, PackageManager.PackageInfoFlags.of(0));
            } else {
                pm.getPackageInfo(KCANOTIFY_PACKAGE_NAME, 0);
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        File temp = newTempSibling(dest);
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(temp)) {
            byte[] buffer = new byte[16384];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            temp.delete();
            throw e;
        }
        replaceFile(temp, dest);
    }

    // Write through a unique temp sibling, then rename(2) over the target so a concurrent reader
    // on the previous inode never sees partially written content.
    private static File newTempSibling(File dest) {
        File parent = dest.getParentFile();
        if (parent != null) parent.mkdirs();
        return new File(parent, dest.getName() + "-" + System.nanoTime() + ".tmp");
    }

    private static void replaceFile(File temp, File dest) {
        if (!temp.renameTo(dest)) temp.delete();
    }

    public static String getWebkitErrorCodeText(int errorCode) {
        return switch (errorCode) {
            case ERROR_AUTHENTICATION -> "ERROR_AUTHENTICATION";
            case ERROR_BAD_URL -> "ERROR_BAD_URL";
            case ERROR_CONNECT -> "ERROR_CONNECT";
            case ERROR_FAILED_SSL_HANDSHAKE -> "ERROR_FAILED_SSL_HANDSHAKE";
            case ERROR_FILE -> "ERROR_FILE";
            case ERROR_FILE_NOT_FOUND -> "ERROR_FILE_NOT_FOUND";
            case ERROR_HOST_LOOKUP -> "ERROR_HOST_LOOKUP";
            case ERROR_IO -> "ERROR_IO";
            case ERROR_PROXY_AUTHENTICATION -> "ERROR_PROXY_AUTHENTICATION";
            case ERROR_REDIRECT_LOOP -> "ERROR_REDIRECT_LOOP";
            case ERROR_TIMEOUT -> "ERROR_TIMEOUT";
            case ERROR_TOO_MANY_REQUESTS -> "ERROR_TOO_MANY_REQUESTS";
            case ERROR_UNSUPPORTED_AUTH_SCHEME -> "ERROR_UNSUPPORTED_AUTH_SCHEME";
            case ERROR_UNSUPPORTED_SCHEME -> "ERROR_UNSUPPORTED_SCHEME";
            case ERROR_UNSAFE_RESOURCE -> "ERROR_UNSAFE_RESOURCE";
            default -> String.format(Locale.US, "ERROR_UNKNOWN (%d)", errorCode);
        };
    }

    public static String getSslErrorCodeTitle(int errorCode) {
        return switch (errorCode) {
            case SSL_DATE_INVALID -> "SSL_DATE_INVALID";
            case SSL_EXPIRED -> "SSL_EXPIRED";
            case SSL_IDMISMATCH -> "SSL_IDMISMATCH";
            case SSL_INVALID -> "SSL_INVALID";
            case SSL_NOTYETVALID -> "SSL_NOTYETVALID";
            case SSL_UNTRUSTED -> "SSL_UNTRUSTED";
            default -> String.format(Locale.US, "ERROR_UNKNOWN (%d)", errorCode);
        };
    }

    public static String getSslErrorCodeDescription(int errorCode) {
        return switch (errorCode) {
            case SSL_DATE_INVALID -> "The date of the certificate is invalid";
            case SSL_EXPIRED -> "The certificate has expired";
            case SSL_IDMISMATCH -> "Hostname mismatch";
            case SSL_INVALID -> "A generic error occurred";
            case SSL_NOTYETVALID -> "The certificate is not yet valid";
            case SSL_UNTRUSTED -> "The certificate authority is not trusted";
            default -> String.format(Locale.US, "unknown ssl error [%d]", errorCode);
        };
    }

    public static boolean isKcanotifyCert(SslCertificate cert) {
        String ca_name = "Kcanotify CA";
        try {
            return ca_name.equals(cert.getIssuedBy().getCName());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidCertError(SslError error) {
        boolean is_untrusted = KcUtils.getSslErrorCodeTitle(error.getPrimaryError()).equals("SSL_UNTRUSTED");
        boolean is_valid_cert = KcUtils.isKcanotifyCert(error.getCertificate());
        return is_untrusted && is_valid_cert;
    }
}