package com.antest1.gotobrowser.Browser;

import static com.antest1.gotobrowser.Browser.KcsInterface.GOTO_ANDROID;
import static com.antest1.gotobrowser.Constants.*;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
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
import com.antest1.gotobrowser.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class WebViewManager {
    public static final String OPEN_KANCOLLE = "open_kancolle";

    public static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36";
    public static final String USER_AGENT_IOS = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15";

    private boolean logoutFlag;
    private boolean refreshFlag;
    private final BrowserActivity activity;
    private final ResourceProcess resourceProcess;
    private final SharedPreferences sharedPref;

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
        view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.getSettings().setOffscreenPreRaster(true);
        }
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public static void setWebViewDebugging(boolean enabled) {
        WebView.setWebContentsDebuggingEnabled(enabled);
    }

    public void setWebViewClient(BrowserActivity activity, WebViewL webview) {
        boolean is_kcbrowser_mode = activity.isKcMode();
        webview.addJavascriptInterface(new KcsInterface(activity), GOTO_ANDROID);
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (logoutFlag) closeWebView();
            }

            public void onPageFinished(WebView view, String url) {
                runLoginLogoutScript(webview, url);
                if (is_kcbrowser_mode) {
                    sharedPref.edit().putString(PREF_LATEST_URL, url).apply();
                    if (url.contains(Constants.URL_KANMOE_1) || url.contains(Constants.URL_OOI_1) || url.contains(URL_DMM)) {
                        activity.setStartedFlag();
                        webview.getSettings().setBuiltInZoomControls(true);
                        webview.getSettings().setDisplayZoomControls(false);
                        if (sharedPref.getBoolean(PREF_ADJUSTMENT, false)) {
                            webview.evaluateJavascript(ADJUST_SCRIPT, null);
                        }
                    }
                    if (url.contains("about:blank") && refreshFlag) {
                        refreshFlag = false;
                        String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
                        if (CONN_DMM.equals(pref_connector)) {
                            openPage(webview, getDefaultPage(activity, true), true);
                        } else {
                            if (webview.canGoBack()) webview.goBack();
                        }
                        webview.resumeTimers();
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                try {
                    view.stopLoading();
                } catch (Exception ignored) { }
                activity.showWebkitErrorDialog(errorCode, description, failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (is_kcbrowser_mode) {
                    Uri source = request.getUrl();
                    WebResourceResponse response = resourceProcess.processWebRequest(source);
                    Log.e("GOTO", "shouldInterceptRequest " + source + " " + (response == null));
                    if (response != null) return response;
                }
                return super.shouldInterceptRequest(view, request);
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (KcUtils.isValidCertError(error)) {
                    handler.proceed();
                } else {
                    activity.showSslErrorDialog(handler, error);
                }
            }
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

    public static void enableBrowserCookie(WebViewL webview) {
        webview.getSettings().setMixedContentMode(WebSettings
                .MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true);
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
                CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView, true);
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

    public void runLoginLogoutScript(WebViewL webview, String url) {
        String login_id = sharedPref.getString(PREF_DMM_ID, ""); // intent.getStringExtra("login_id");
        String login_password = sharedPref.getString(PREF_DMM_PASS, "");

        String cookie = getDmmCookie();
        // Login
        if (url.contains(URL_DMM_FOREIGN) || url.contains(URL_DMM_FOREIGN_2)) {
            webview.evaluateJavascript(cookie, null);
            webview.evaluateJavascript("location.href='".concat(URL_DMM).concat("';"), null);
        }

        if (url.contains(URL_DMM_LOGIN) || url.contains(URL_DMM_LOGIN_2)) {
            webview.evaluateJavascript(cookie + String.format(Locale.US, AUTOCOMPLETE_DMM, login_id, login_password), null);
        } else if (url.contains(URL_KANMOE) || url.contains(URL_OOI)) {
            webview.evaluateJavascript(
                    String.format(Locale.US, AUTOCOMPLETE_OOI, login_id, login_password), null);
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

        String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
        if (CONN_DMM.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, MUTE_SEND_DMM, is_mute ? 1 : 0), callback);
        } else if (CONN_KANMOE.equals(pref_connector) || CONN_OOI.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, MUTE_SEND_OOI, is_mute ? 1 : 0), callback);
        }
    }

    public void logoutGame(WebViewL webview) {
        logoutFlag = true;
        String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
        switch (pref_connector) {
            case CONN_DMM -> webview.loadUrl(URL_DMM_LOGOUT);
            case CONN_OOI -> webview.loadUrl(URL_OOI_LOGOUT);
            case CONN_KANMOE -> webview.loadUrl(URL_KANMOE_LOGOUT);
        }
    }

    public void refreshPage(WebViewL webview) {
        refreshFlag = true;
        webview.loadUrl("about:blank");
    }

    public void openPage(WebViewL webview, List<String> connector_info, boolean isKcBrowser) {
        String login_id = sharedPref.getString(PREF_DMM_ID, ""); // intent.getStringExtra("login_id");
        String login_password = sharedPref.getString(PREF_DMM_PASS, "");
        if (connector_info == null || connector_info.size() != 2) return;

        String connector_url_default = connector_info.get(0);
        // String connector_url = connector_info.get(1);

        webview.resumeTimers();
        webview.getSettings().setTextZoom(100);
        if (!isKcBrowser) {
            webview.loadUrl(connector_url_default);
        } else {
            String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
            if (CONN_KANMOE.equals(pref_connector) || CONN_OOI.equals(pref_connector)) {
                int connect_mode = 1;
                String post_data = String.format(Locale.US, "login_id=%s&password=%s&mode=%d",
                        encodeText(login_id), encodeText(login_password), connect_mode);
                webview.postUrl(connector_url_default, post_data.getBytes());
            } else {
                webview.loadUrl(connector_url_default);
            }
        }
    }

    public static String replaceEndpoint(String url, String endpoint) {
        if (!endpoint.endsWith("/")) endpoint += "/";
        if (url.startsWith(GADGET_HTTPS_URL)) {
            return url.replace(GADGET_HTTPS_URL, endpoint);
        } else {
            return url.replace(GADGET_HTTP_URL, endpoint);
        }
    }

    public static void setKcCacheProxy(String endpoint, Runnable onSuccessListener, Runnable onFailureListener) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            ProxyConfig proxyConfig = new ProxyConfig.Builder()
                    .addProxyRule(endpoint)
                    .addBypassRule("*com").addBypassRule("*jp")
                    .addDirect().build();
            Executor executor = Runnable::run;
            try {
                ProxyController.getInstance().setProxyOverride(proxyConfig, executor, onSuccessListener);
            } catch (IllegalArgumentException exception) {
                onFailureListener.run();
            }
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
        if (isKcBrowser) {
            String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
            switch (pref_connector) {
                case CONN_DMM -> {
                    url_list.add(URL_DMM);
                    url_list.add(URL_DMM);
                }
                case CONN_OOI -> {
                    url_list.add(URL_OOI);
                    url_list.add(URL_OOI);
                }
                case CONN_KANMOE -> {
                    url_list.add(URL_KANMOE);
                    url_list.add(URL_KANMOE);
                }
            }
            return url_list;
        } else {
            return null;
        }
    }

    public String getDmmCookie() {
        String targetTime = KcUtils.getDefaultTimeForCookie();
        return DMM_COOKIE.replace("{date}", targetTime);
    }

    public void captureGameScreen(WebViewL webview) {
        Log.e("GOTO", "captureGameScreen");
        ValueCallback<String> callback = s -> Log.e("GOTO", "capture " + s);

        String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
        if (CONN_DMM.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, CAPTURE_SEND_DMM), callback);
        } else if (CONN_KANMOE.equals(pref_connector) || CONN_OOI.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, CAPTURE_SEND_OOI), callback);
        }
    }

    private String encodeText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return URLEncoder.encode(text, StandardCharsets.UTF_8);
        } else {
            try {
                return URLEncoder.encode(text, "utf-8");
            } catch (UnsupportedEncodingException e) {
                return text;
            }
        }
    }

    private void setWebViewUserAgent(WebViewL view, boolean change_to_ios) {
        view.getSettings().setUserAgentString(change_to_ios ? USER_AGENT_IOS : USER_AGENT);
        ResourceProcess.setUserAgent(view.getSettings().getUserAgentString());
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
        setWebViewUserAgent(view, useCanvas);
    }
}
