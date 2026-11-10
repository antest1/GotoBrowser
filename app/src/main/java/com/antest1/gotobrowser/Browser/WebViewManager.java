package com.antest1.gotobrowser.Browser;

import static com.antest1.gotobrowser.Browser.KcsInterface.GOTO_ANDROID;
import static com.antest1.gotobrowser.Constants.*;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.util.Log;
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

import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Constants;
import com.antest1.gotobrowser.Helpers.CredentialStore;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.R;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class WebViewManager {
    public static final int FLAG_UA_DEFAULT = 0;
    public static final int FLAG_UA_IOS = 1;
    public static final int FLAG_UA_MOBILE = 1 << 1;

    public static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36";
    public static final String USER_AGENT_IOS = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15";
    public static final String USER_AGENT_MOBILE = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36";

    private boolean logoutFlag;
    private boolean refreshFlag;
    private final BrowserActivity activity;
    private final ResourceProcess resourceProcess;
    private final SharedPreferences sharedPref;
    private String ooiStyleScript;

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
        applyRendererSetting(view);
        view.setScrollbarFadingEnabled(true);
        view.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        // Leave Chromium's default rendering path; the WebView is already hardware accelerated.
    }

    public static void setWebViewDebugging(boolean enabled) {
        WebView.setWebContentsDebuggingEnabled(enabled);
    }

    public void setWebViewClient(BrowserActivity activity, WebViewL webview) {
        webview.addJavascriptInterface(new KcsInterface(activity), GOTO_ANDROID);
        addOoiStyleScript(webview);
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                activity.getLoadingTracker().onPageStarted(url, isAccountPage(url));
                super.onPageStarted(view, url, favicon);
            }

            // Also runs for the final URL of a redirect (e.g. game page -> login form).
            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                activity.getLoadingTracker().onPageShown(url, isGameUrl(url));
                super.doUpdateVisitedHistory(view, url, isReload);
            }

            public void onPageFinished(WebView view, String url) {
                activity.getLoadingTracker().onPageShown(url, isGameUrl(url));
                if (logoutFlag && !url.contains("about:blank")) {
                    logoutFlag = false;
                    refreshPage(webview);
                    return;
                }
                runLoginLogoutScript(webview, url);
                if (url.startsWith(URL_OOI)) webview.evaluateJavascript(getOoiStyleScript(), null);
                sharedPref.edit().putString(PREF_LATEST_URL, url).apply();
                if (url.contains(Constants.URL_OOI_1) || url.contains(URL_DMM)) {
                    activity.setStartedFlag();
                    webview.getSettings().setBuiltInZoomControls(true);
                    webview.getSettings().setDisplayZoomControls(false);
                    if (sharedPref.getBoolean(PREF_ADJUSTMENT, true)) {
                        webview.evaluateJavascript(ADJUST_SCRIPT, null);
                    }
                }
                if (url.contains("about:blank") && refreshFlag) {
                    refreshFlag = false;
                    openPage(webview, getDefaultPage(activity));
                    webview.resumeTimers();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith(URL_GOOGLE_ACCOUNTS)) {
                    setWebViewUserAgent(view, FLAG_UA_MOBILE);
                    view.loadUrl(url);
                    return true;
                } else if (url.startsWith(URL_DMM)) {
                    setWebViewUserAgent(view, getDefaultUserAgentFlag());
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            // Main frame only (the default onReceivedError(WebResourceRequest, ...) forwards just those).
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                try {
                    view.stopLoading();
                } catch (Exception ignored) { }
                if (!activity.onMainFrameLoadError(errorCode, description, failingUrl)) {
                    activity.showWebkitErrorDialog(errorCode, description, failingUrl);
                }
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400) {
                    activity.onMainFrameHttpError(errorResponse.getStatusCode(), request.getUrl().toString());
                }
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri source = request.getUrl();
                if (request.isForMainFrame()) activity.getLoadingTracker().onMainFrameRequest(source.toString());
                activity.getLoadingTracker().onRequest(source.toString());
                WebResourceResponse response = resourceProcess.processWebRequest(source);
                Log.e("GOTO", "shouldInterceptRequest " + source + " " + (response == null));
                if (response != null) return response;
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

    /**
     * ooi.moe keeps its own stylesheet and theme; only the game layout rules (assets/ooi.css)
     * go on top. They are added at document start where the WebView supports it, so the page
     * never lays out without them; onPageFinished() adds them otherwise.
     */
    private void addOoiStyleScript(WebViewL webview) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webview, getOoiStyleScript(),
                    Collections.singleton(URL_OOI_ORIGIN));
        }
    }

    private String getOoiStyleScript() {
        if (ooiStyleScript == null) {
            String css = "";
            try (InputStream in = activity.getAssets().open("ooi.css")) {
                css = KcUtils.getStringFromInputStream(in);
            } catch (IOException e) {
                KcUtils.reportException(e);
            }
            ooiStyleScript = String.format(Locale.US, OOI_STYLE_SCRIPT, JSONObject.quote(css));
        }
        return ooiStyleScript;
    }

    /**
     * The game page itself, matched on host and path: a login URL can carry the game
     * URL as its return parameter, so a substring match is not enough here.
     */
    private static boolean isGameUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null || path == null) return false;
        return (host.equals("play.games.dmm.com") && path.startsWith("/game/kancolle"))
                || (host.equals("ooi.moe") && path.startsWith("/kancolle"));
    }

    /** Login, logout and region pages: ordinary pages the user reads and fills in. */
    private static boolean isAccountPage(String url) {
        if (url == null) return false;
        return url.contains(URL_DMM_LOGIN) || url.contains(URL_DMM_LOGIN_2)
                || url.contains(URL_DMM_FOREIGN) || url.contains(URL_DMM_FOREIGN_2)
                || url.startsWith(URL_OOI_LOGOUT);
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
            public void onProgressChanged(WebView view, int newProgress) {
                activity.getLoadingTracker().onProgressChanged(newProgress);
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                KcUtils.showToast(activity.getApplicationContext(), message);
                return super.onJsAlert(view, url, message, result);
            }

            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                final WebViewL popupWebView = new WebViewL(activity);
                activity.setCloseButtonVisible(true);
                CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView, true);

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

                // Logic handled in BrowserActivity via closeButtonVisible state
                // closeButton.setOnClickListener(v -> {
                //    view.removeView(popupWebView);
                //    v.setVisibility(View.GONE);
                // });
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
        String login_password = CredentialStore.getPassword(sharedPref);

        String cookie = getDmmCookie();
        // Login
        if (url.contains(URL_DMM_FOREIGN) || url.contains(URL_DMM_FOREIGN_2)) {
            webview.evaluateJavascript(cookie, null);
            webview.evaluateJavascript("location.href='".concat(URL_DMM).concat("';"), null);
        }

        if (url.contains(URL_DMM_LOGIN) || url.contains(URL_DMM_LOGIN_2)) {
            webview.evaluateJavascript(cookie + String.format(Locale.US, AUTOCOMPLETE_DMM, login_id, login_password), null);
        } else if (url.contains(URL_OOI)) {
            webview.evaluateJavascript(ooiAutofillScript(login_id, login_password), null);
        }
    }

    /** The values are quoted as JS strings, so quotes or backslashes in a password survive. */
    static String ooiAutofillScript(String loginId, String password) {
        return String.format(Locale.US, AUTOCOMPLETE_OOI,
                JSONObject.quote(loginId == null ? "" : loginId),
                JSONObject.quote(password == null ? "" : password));
    }

    public void closeWebView() {
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
        } else if (CONN_OOI.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, MUTE_SEND_OOI, is_mute ? 1 : 0), callback);
        }
    }

    /**
     * Loads the connector's logout URL, invalidating the session. onPageFinished()
     * then takes the browser back to the connector's start page, which now shows
     * the login form.
     */
    public void logoutGame(WebViewL webview) {
        // The logout dialog pauses timers; while paused the logout redirect never
        // finishes loading, so onPageFinished() (and the login autocomplete) never runs.
        webview.resumeTimers();
        String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
        switch (pref_connector) {
            case CONN_DMM -> webview.loadUrl(URL_DMM_LOGOUT);
            case CONN_OOI -> webview.loadUrl(URL_OOI_LOGOUT);
            // Nothing to load, so leave logoutFlag untouched rather than arming
            // a flow that waits for a page that will never arrive.
            default -> {
                return;
            }
        }
        logoutFlag = true;
    }

    public void refreshPage(WebViewL webview) {
        refreshFlag = true;
        webview.loadUrl("about:blank");
    }

    /**
     * Reloads only the game frame (iframe) instead of the whole page.
     * Lighter and faster than {@link #refreshPage(WebViewL)}, but it may not
     * always recover the game when the surrounding page is in a broken state.
     */
    public void refreshGameFrame(WebViewL webview) {
        ValueCallback<String> callback = s -> Log.e("GOTO", "refreshGameFrame " + s);

        String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
        if (CONN_DMM.equals(pref_connector)) {
            webview.evaluateJavascript(REFRESH_GAME_FRAME_DMM, callback);
        } else if (CONN_OOI.equals(pref_connector)) {
            webview.evaluateJavascript(REFRESH_GAME_FRAME_OOI, callback);
        }
        webview.resumeTimers();
    }

    /**
     * Opens the connector's start page. Every connector shows its own login form there
     * (unless a session is still active), and runLoginLogoutScript() fills it in; the user
     * submits it. ooi.moe used to be logged into by posting the stored credentials, which
     * left no way to stay logged out or to pick the form's other options.
     */
    public void openPage(WebViewL webview, List<String> connector_info) {
        if (connector_info == null || connector_info.size() != 2) return;

        String connector_url_default = connector_info.get(0);
        // String connector_url = connector_info.get(1);

        webview.resumeTimers();
        webview.getSettings().setTextZoom(100);
        webview.loadUrl(connector_url_default);
    }

    public static String replaceEndpoint(String url, String endpoint) {
        if (!endpoint.endsWith("/")) endpoint += "/";
        if (url.startsWith(GADGET_HTTPS_URL)) {
            return url.replace(GADGET_HTTPS_URL, endpoint);
        } else {
            return url.replace(GADGET_HTTP_URL, endpoint);
        }
    }

    // Last proxy state applied to the process-wide ProxyController (null endpoint = cleared).
    // Lets refreshes skip the async round trip when nothing has changed.
    private static volatile boolean isProxyStateKnown = false;
    private static volatile String appliedProxyEndpoint = null;

    // Proxy changes are asynchronous: load pages only after a listener has run.
    public static void setKcCacheProxy(String endpoint, Runnable onSuccessListener, Runnable onFailureListener) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            if (isProxyStateKnown && endpoint.equals(appliedProxyEndpoint)) {
                onSuccessListener.run();
                return;
            }
            ProxyConfig proxyConfig = new ProxyConfig.Builder()
                    .addProxyRule(endpoint)
                    .addBypassRule("*com").addBypassRule("*jp")
                    .addDirect().build();
            Executor executor = Runnable::run;
            isProxyStateKnown = false;
            try {
                ProxyController.getInstance().setProxyOverride(proxyConfig, executor, () -> {
                    appliedProxyEndpoint = endpoint;
                    isProxyStateKnown = true;
                    onSuccessListener.run();
                });
            } catch (IllegalArgumentException exception) {
                onFailureListener.run();
            }
        } else {
            onFailureListener.run();
        }
    }

    public static void clearKcCacheProxy(Runnable onCompleteListener) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            if (isProxyStateKnown && appliedProxyEndpoint == null) {
                onCompleteListener.run();
                return;
            }
            Executor executor = Runnable::run;
            isProxyStateKnown = false;
            ProxyController.getInstance().clearProxyOverride(executor, () -> {
                appliedProxyEndpoint = null;
                isProxyStateKnown = true;
                onCompleteListener.run();
            });
        } else {
            onCompleteListener.run();
        }
    }

    public static List<String> getDefaultPage(BrowserActivity activity) {
        List<String> url_list = new ArrayList<>();
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
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
        }
        return url_list;
    }

    /** kancolle.moe has shut down: a session saved with it moves to ooi.moe. */
    public static void migrateRetiredConnector(SharedPreferences sharedPref) {
        if ("kancolle.moe".equals(sharedPref.getString(PREF_CONNECTOR, CONN_DMM))) {
            sharedPref.edit().putString(PREF_CONNECTOR, CONN_OOI).apply();
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
        } else if (CONN_OOI.equals(pref_connector)) {
            webview.evaluateJavascript(String.format(Locale.US, CAPTURE_SEND_OOI), callback);
        }
    }

    private void setWebViewUserAgent(WebView view, int flags) {
        String userAgentString = USER_AGENT;
        if ((flags & FLAG_UA_IOS) != 0) {
            userAgentString = USER_AGENT_IOS;
        } else if ((flags & FLAG_UA_MOBILE) != 0) {
            userAgentString = USER_AGENT_MOBILE;
        }
        // Changing the UA while a page is loading restarts the load, so skip no-op updates.
        if (!userAgentString.equals(view.getSettings().getUserAgentString())) {
            view.getSettings().setUserAgentString(userAgentString);
        }
        ResourceProcess.setUserAgent(view.getSettings().getUserAgentString());
    }

    public void applyRendererSetting(WebView view) {
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

        setWebViewUserAgent(view, getDefaultUserAgentFlag());
    }

    // The legacy (Canvas) renderer relies on the iOS user agent, so every default UA goes through here.
    private int getDefaultUserAgentFlag() {
        boolean useCanvas = sharedPref.getBoolean(PREF_LEGACY_RENDERER, false);
        return useCanvas ? FLAG_UA_IOS : FLAG_UA_DEFAULT;
    }
}
