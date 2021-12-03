package com.antest1.gotobrowser.Browser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.Activity.EntranceActivity;
import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Constants;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.antest1.gotobrowser.Browser.KcsInterface.GOTO_ANDROID;
import static com.antest1.gotobrowser.Constants.ADD_VIEWPORT_META;
import static com.antest1.gotobrowser.Constants.AUTOCOMPLETE_OOI;
import static com.antest1.gotobrowser.Constants.CAPTURE_SEND_DMM;
import static com.antest1.gotobrowser.Constants.CAPTURE_SEND_OOI;
import static com.antest1.gotobrowser.Constants.CONN_DMM;
import static com.antest1.gotobrowser.Constants.CONN_KANSU;
import static com.antest1.gotobrowser.Constants.CONN_NITRABBIT;
import static com.antest1.gotobrowser.Constants.CONN_OOI;
import static com.antest1.gotobrowser.Constants.DMM_COOKIE;
import static com.antest1.gotobrowser.Constants.DMM_REDIRECT_CODE;
import static com.antest1.gotobrowser.Constants.GADGET_URL;
import static com.antest1.gotobrowser.Constants.MUTE_SEND_DMM;
import static com.antest1.gotobrowser.Constants.MUTE_SEND_OOI;
import static com.antest1.gotobrowser.Constants.PREF_CONNECTOR;
import static com.antest1.gotobrowser.Constants.PREF_DMM_ID;
import static com.antest1.gotobrowser.Constants.PREF_DMM_PASS;
import static com.antest1.gotobrowser.Constants.PREF_LATEST_URL;
import static com.antest1.gotobrowser.Constants.PREF_LEGACY_RENDERER;
import static com.antest1.gotobrowser.Constants.URL_DMM;
import static com.antest1.gotobrowser.Constants.URL_DMM_FOREIGN;
import static com.antest1.gotobrowser.Constants.URL_DMM_LOGIN;
import static com.antest1.gotobrowser.Constants.URL_DMM_LOGIN_2;
import static com.antest1.gotobrowser.Constants.URL_DMM_LOGOUT;
import static com.antest1.gotobrowser.Constants.URL_KANSU;
import static com.antest1.gotobrowser.Constants.URL_KANSU_LOGOUT;
import static com.antest1.gotobrowser.Constants.URL_NITRABBIT;
import static com.antest1.gotobrowser.Constants.URL_OOI;
import static com.antest1.gotobrowser.Constants.URL_OOI_LOGOUT;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.getStringFromException;

public class WebViewManager {
    public static final String OPEN_KANCOLLE = "open_kancolle";
    public static final String OPEN_RES_DOWN = "open_res_down";

    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36";
    public static final String USER_AGENT_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1";

    private boolean logoutFlag;
    private BrowserActivity activity;
    private ResourceProcess resourceProcess;
    private SharedPreferences sharedPref;

    public WebViewManager (BrowserActivity ac) {
        activity = ac;
        logoutFlag = false;
        resourceProcess = new ResourceProcess(ac);
        sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
    }

    public void setHardwareAcceleratedFlag() {
        activity.getWindow().setFlags( WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
    }

    public void setDataDirectorySuffix() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = KcUtils.getProcessName(activity.getApplicationContext());
            if (!BuildConfig.APPLICATION_ID.equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setWebViewSettings(WebViewL view) {
        view.setInitialScale(1);
        view.getSettings().setLoadWithOverviewMode(true);
        view.getSettings().setSaveFormData(true);
        view.getSettings().setDatabaseEnabled(true);
        view.getSettings().setDomStorageEnabled(true);
        view.getSettings().setUseWideViewPort(true);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setTextZoom(100);
        view.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        view.getSettings().setSupportMultipleWindows(true);
        view.getSettings().setSupportZoom(false);
        setWebViewRendererSetting(view);
        view.setScrollbarFadingEnabled(true);
        view.getSettings().setAppCacheEnabled(false);
        view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.getSettings().setOffscreenPreRaster(true);
        }
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public static void setWebViewDebugging(boolean enabled) {
        WebView.setWebContentsDebuggingEnabled(enabled);
    }

    public void setWebViewClient(BrowserActivity activity, WebViewL webview, List<String> connector_info) {
        Context context = activity.getApplicationContext();
        boolean is_kcbrowser_mode = activity.isKcMode();
        webview.addJavascriptInterface(new KcsInterface(activity), GOTO_ANDROID);
        webview.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                runLoginLogoutScript(webview, url);
                if (is_kcbrowser_mode) {
                    sharedPref.edit().putString(PREF_LATEST_URL, url).apply();
                    if (url.contains(Constants.URL_KANSU_1) || url.contains(Constants.URL_OOI_1) || url.contains(URL_DMM)) {
                        activity.setStartedFlag();
                        if (!KcUtils.checkIsLargeDisplay(activity)) {
                            webview.evaluateJavascript(ADD_VIEWPORT_META, null);
                        }
                    }
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                Uri source = Uri.parse(url);
                if (is_kcbrowser_mode) {
                    WebResourceResponse response = resourceProcess.processWebRequest(source);
                    if (response != null) return response;
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Uri source = request.getUrl();
                    if (is_kcbrowser_mode) {
                        WebResourceResponse response = resourceProcess.processWebRequest(source);
                        if (response != null) return response;
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
    }

    public void setWebViewDownloader(WebViewL webview) {
        ProgressDialog downloadDialog = activity.getDownloadDialog();
        webview.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Uri uri = Uri.parse(url);
            String filename = uri.getLastPathSegment();
            Log.e("GOTO", url);
            WebViewManager.setDownloadProgressDialog(downloadDialog);
            WebViewManager.setProgressDialogMessage(downloadDialog,
                    String.format(Locale.US, "Downloading %s...", filename));
            downloadDialog.show();
            getResourceDownloadThread(activity, url, userAgent, mimetype).start();
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void setPopupWebViewSetting(WebViewL webview) {
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setSaveFormData(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.getSettings().setSupportZoom(true);
    }

    public static void setDownloadProgressDialog(ProgressDialog progress) {
        progress.setCancelable(false);
        progress.setProgressNumberFormat(null);
        progress.setProgressPercentFormat(null);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setIndeterminate(true);
    }

    public static void setProgressDialogMessage(ProgressDialog progress, String message) {
        progress.setMessage(message);
    }

    public static void enableBrowserCookie(WebViewL webview) {
        if (Build.VERSION.SDK_INT >= 21) {
            webview.getSettings().setMixedContentMode(WebSettings
                    .MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true);
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
        }
    }


    public void setPopupView(WebViewL webview) {
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                KcUtils.showToast(activity.getApplicationContext(), message);
                return super.onJsAlert(view, url, message, result);
            }

            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                final WebViewL popupWebView = new WebViewL(activity);
                ImageView closeButton = activity.findViewById(R.id.dmm_browser_close);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView, true);
                }
                closeButton.setVisibility(View.VISIBLE);

                setPopupWebViewSetting(popupWebView);
                popupWebView.setWebViewClient(new WebViewClient() {
                    @TargetApi(Build.VERSION_CODES.N)
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        view.loadUrl(url);
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }
                });

                closeButton.setOnClickListener(v -> {
                    view.removeView(popupWebView);
                    v.setVisibility(View.GONE);
                });
                view.addView(popupWebView);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popupWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                super.onCloseWindow(window);
            }
        });
    }

    public static String getResourceDownloadOutputPath(Context context, Uri uri) {
        String outputpath = "";
        if (uri.getPath().contains(context.getString(R.string.resource_download_prefix))) {
            outputpath = uri.getPath().replace(context.getString(R.string.resource_download_prefix), "")
                    .replace(".zip", "/");
        } else if (uri.getPath().contains("kcs2-all") || uri.getPath().contains("kcs2-event")) {
            outputpath = "/";
        }
        return outputpath;
    }

    public static void setSoundMuteCookie(WebViewL webview) {
        CookieSyncManager syncManager = CookieSyncManager.createInstance(webview.getContext());
        syncManager.sync();
        CookieManager cookieManager = CookieManager.getInstance();

        String osapi_url = "http://osapi.dmm.com/";
        String osapi_value = cookieManager.getCookie(osapi_url);
        if (osapi_value != null) {
            String[] cookie_list = osapi_value.split(";");
            for (String s: cookie_list) {
                if (s.contains("kcs_options=")) {
                    String match_result_group = s.replace("kcs_options=", "").trim();
                    match_result_group = match_result_group.replaceAll("vol_bgm%3D\\d+?%3B", "vol_bgm%3D0%3B");
                    match_result_group = match_result_group.replaceAll("vol_se%3D\\d+?%3B", "vol_se%3D0%3B");
                    match_result_group = match_result_group.replaceAll("vol_voice%3D\\d+?%3B", "vol_voice%3D0%3B");
                    cookieManager.setCookie(osapi_url, String.format("kcs_options=%s;expires=Thu, 16-Jan-2023 00:00:00 GMT;path=/;domain=dmm.com", match_result_group));
                }
            }
        } else {
            cookieManager.setCookie(osapi_url, String.format("kcs_options=%s;expires=Thu, 16-Jan-2023 00:00:00 GMT;path=/;domain=dmm.com", "vol_bgm%3D0%3Bvol_se%3D0%3Bvol_voice%3D0%3Bv_be_left%3D1%3Bv_duty%3D1"));
        }
    }

    public void runLoginLogoutScript(WebViewL webview, String url) {
        String login_id = sharedPref.getString(PREF_DMM_ID, ""); // intent.getStringExtra("login_id");
        String login_password = sharedPref.getString(PREF_DMM_PASS, "");

        // Login
        if (url.contains(URL_DMM_FOREIGN)) {
            webview.evaluateJavascript(DMM_COOKIE, null);
            webview.evaluateJavascript("location.href='".concat(URL_DMM).concat("';"), null);
        }
        if (url.contains(URL_DMM_LOGIN) || url.contains(URL_DMM_LOGIN_2) || url.equals(URL_KANSU) || url.equals(URL_OOI)) {
            if (url.contains(URL_DMM_LOGIN) || url.contains(URL_DMM_LOGIN_2)) {
                webview.evaluateJavascript(DMM_COOKIE, null);
            }
            webview.evaluateJavascript(
                    String.format(Locale.US, AUTOCOMPLETE_OOI, login_id, login_password), null);
        }

        // Logout
        if (logoutFlag && url.contains("rurl") && url.contains(DMM_REDIRECT_CODE)) {
            closeWebView();
        }

        if (logoutFlag && (url.contains(CONN_OOI) || url.contains(CONN_KANSU))) {
            closeWebView();
        }
    }

    public void closeWebView() {
        Intent intent = new Intent(activity, EntranceActivity.class);
        activity.startActivity( intent);
        activity.finish();
    }

    public void runMuteScript(WebViewL webview, boolean is_mute) {
        runMuteScript(webview, is_mute, false);
    }

    public void runMuteScript(WebViewL webview, boolean is_mute, boolean force_pause) {
        ValueCallback<String> callback = s -> {
            /// Do Nothing
        };

        String pref_connector = sharedPref.getString(PREF_CONNECTOR, null);
        if (CONN_DMM.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, MUTE_SEND_DMM, is_mute ? 1 : 0), callback);
        } else if (CONN_KANSU.equals(pref_connector) || CONN_OOI.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, MUTE_SEND_OOI, is_mute ? 1 : 0), callback);
        }
    }

    public void logoutGame(WebViewL webview) {
        logoutFlag = true;
        String pref_connector = sharedPref.getString(PREF_CONNECTOR, null);
        if (CONN_DMM.equals(pref_connector)) {
            webview.loadUrl(URL_DMM_LOGOUT);
        } else if (CONN_OOI.equals(pref_connector)) {
            webview.loadUrl(URL_OOI_LOGOUT);
        } else if (CONN_KANSU.equals(pref_connector)) {
            webview.loadUrl(URL_KANSU_LOGOUT);
        }
    }

    public void openPage(WebViewL webview, List<String> connector_info, boolean isKcBrowser) {
        String login_id = sharedPref.getString(PREF_DMM_ID, ""); // intent.getStringExtra("login_id");
        String login_password = sharedPref.getString(PREF_DMM_PASS, "");
        if (connector_info == null || connector_info.size() != 2) return;

        String connector_url_default = connector_info.get(0);
        String connector_url = connector_info.get(1);

        webview.resumeTimers();
        webview.getSettings().setTextZoom(100);
        if (!isKcBrowser) {
            webview.loadUrl(connector_url);
        } else {
            String pref_connector = sharedPref.getString(PREF_CONNECTOR, null);
            if (CONN_KANSU.equals(pref_connector) || CONN_OOI.equals(pref_connector)) {
                String postdata = "";
                try {
                    int connect_mode = connector_url_default.equals(URL_OOI) ? 1 : 1;
                    postdata = String.format(Locale.US, "login_id=%s&password=%s&mode=%d",
                            URLEncoder.encode(login_id, "utf-8"),
                            URLEncoder.encode(login_password, "utf-8"),
                            connect_mode);
                    webview.postUrl(connector_url, postdata.getBytes());
                } catch (UnsupportedEncodingException e) {
                    KcUtils.reportException(e);
                }
            } else {
                webview.loadUrl(connector_url_default);
            }
        }
    }

    public static String replaceEndpoint(String url, String endpoint) {
        return url.replace(GADGET_URL, endpoint);
    }

    public static void setKcCacheProxy(String endpoint, Runnable listener) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            ProxyConfig proxyConfig = new ProxyConfig.Builder()
                    .addProxyRule(endpoint)
                    .addBypassRule("*com").addBypassRule("*jp")
                    .addDirect().build();
            Executor executor = command -> {
                command.run();
            };
            ProxyController.getInstance().setProxyOverride(proxyConfig, executor, listener);
        }
    }

    public static void clearKcCacheProxy() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Executor executor = command -> { };
            Runnable listener = () -> { };
            ProxyController.getInstance().clearProxyOverride(executor, listener);
        }
    }

    public static List<String> getDefaultPage(BrowserActivity activity, boolean isKcBrowser) {
        List<String> url_list = new ArrayList<>();
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
        if (!isKcBrowser) {
            String download_url = activity.getString(R.string.resource_download_link);
            url_list.add(download_url);
            url_list.add(download_url);
        } else {
            String pref_connector = sharedPref.getString(PREF_CONNECTOR, null);
            if (CONN_DMM.equals(pref_connector)) {
                url_list.add(URL_DMM);
                url_list.add(URL_DMM);
            } else if (CONN_KANSU.equals(pref_connector) || CONN_OOI.equals(pref_connector)) {
                String connector_url = "";
                String connector_url_default = "";
                if (CONN_KANSU.equals(pref_connector)) {
                    connector_url_default = URL_KANSU;
                    connector_url = URL_KANSU;
                } else {
                    connector_url_default = URL_OOI;
                    connector_url = URL_OOI;
                }
                url_list.add(connector_url_default);
                url_list.add(connector_url);
            } else if (CONN_NITRABBIT.equals(pref_connector)) {
                url_list.add(URL_NITRABBIT);
                url_list.add(sharedPref.getString(PREF_LATEST_URL, URL_NITRABBIT));
            }
        }
        return url_list;
    }

    public static Thread getResourceDownloadThread(BrowserActivity activity, String url, String userAgent, String mimetype) {
        Context context = activity.getApplicationContext();
        ProgressDialog downloadDialog = activity.getDownloadDialog();
        
        Uri uri = Uri.parse(url);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        VersionDatabase versionTable = new VersionDatabase(context, null, VERSION_TABLE_VERSION);

        return new Thread() {
            public void run() {
                OkHttpClient client = new OkHttpClient();
                Request request = KcUtils.getDownloadRequest(url, userAgent, mimetype);

                String message = "";
                String filename = uri.getLastPathSegment();
                String outputpath = WebViewManager.getResourceDownloadOutputPath(context, uri);
                String version = uri.getQueryParameter("v");
                if (version == null) version = "";

                Log.e("GOTO", outputpath);
                Log.e("GOTO", "version: " + version);

                try {
                    Response response = client.newCall(request).execute();
                    InputStream in = response.body().byteStream();
                    activity.runOnUiThread(() -> WebViewManager.setProgressDialogMessage(downloadDialog,
                            String.format(Locale.US, "Extracting %s...", filename)));
                    KcUtils.unzipResource(context, in, outputpath, versionTable, version);
                    message = String.format(Locale.US, "Process finished: %s", filename);
                } catch (NullPointerException | IOException e) {
                    KcUtils.reportException(e);
                    message = getStringFromException(e);
                } finally {
                    String finish_message = message;
                    activity.runOnUiThread(() -> {
                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        if (!activity.isFinishing() && downloadDialog != null && downloadDialog.isShowing()) {
                            downloadDialog.dismiss();
                        }
                        KcUtils.showToast(context, finish_message);
                    });
                    versionTable.close();
                }
            }
        };
    }

    public void captureGameScreen(WebViewL webview) {
        Log.e("GOTO", "captureGameScreen");
        ValueCallback<String> callback = s -> {
            Log.e("GOTO", "capture " + s);
        };

        String pref_connector = sharedPref.getString(PREF_CONNECTOR, null);
        if (CONN_DMM.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, CAPTURE_SEND_DMM), callback);
        } else if (CONN_KANSU.equals(pref_connector) || CONN_OOI.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, CAPTURE_SEND_OOI), callback);
        }
    }

    private void setWebViewRendererSetting(WebViewL view) {
        // Setting the user agent to change PIXI renderer type:
        // It is the easiest way to improve compatibility,
        // without extra patching to the game client or PixiJS library

        // In modern chromium/webview implementation,
        // disabling HW acceleration does not necessarily fully disable WebGL functionality
        // WebView will still use software/CPU to achieve partial WebGL compatibility
        // In this case, the game does not run.
        // So, disabling HW acceleration in App-level is pointless

        // In order to support old devices that don't run OpenGL,
        // PixiJS provides an option to use HTML5 Canvas renderer instead of WebGL renderer
        // WebView actually has some GPU HW acceleration on Canvas applications
        // But this PixiJS Canvas renderer is much more consistency across different HW

        // Kancolle can run on Canvas renderer.
        // Indeed, Kancolle game client has a hardcoded logic for iOS devices
        // if user agent is iOS device, KC will set forceCanvas:true when init the PIXI Application

        boolean useCanvas = sharedPref.getBoolean(PREF_LEGACY_RENDERER, false);
        view.getSettings().setUserAgentString(useCanvas ? USER_AGENT_IOS : USER_AGENT);
    }
}
