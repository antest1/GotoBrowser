package com.antest1.gotobrowser.Browser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.ContentProvider.KcaPacketStore;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.gotobrowser.Constants.PREF_BROADCAST;
import static com.antest1.gotobrowser.Constants.PREF_DEVTOOLS_DEBUG;
import static com.antest1.gotobrowser.ContentProvider.KcaContentProvider.BROADCAST_ACTION;
import static com.antest1.gotobrowser.ContentProvider.KcaPacketStore.PACKETSTORE_VERSION;

public class KcsInterface {
    // ooi.moe keys its app-only layout on this name (window.GotoBrowser), so it must not change.
    public static final String GOTO_ANDROID = "GotoBrowser";
    // Responses go to kcs_xhr_intercept; a kcsapi call that fails (no network, timeout, HTTP error)
    // goes to kcs_axios_error. The error branch hooks axios itself, so it does not depend on how
    // main.js is obfuscated.
    public static final String AXIOS_INTERCEPT_SCRIPT = "axios.interceptors.response.use(function(response){if(response.config.url.includes(\"kcsapi\")){var url=response.config.url;var request=response.config.data;var host=window.location.hostname;var data=response.data;GotoBrowser.kcs_xhr_intercept(host,url,request,data);}return response;},function(error){try{var c=error&&error.config;if(c&&c.url&&c.url.includes(\"kcsapi\")){var r=error.response;GotoBrowser.kcs_axios_error(window.location.hostname,c.url,r?r.status:0,String(error.code||\"\"),String(error.message||\"\"));}}catch(e){}return Promise.reject(error);});";
    // Holds kcsapi requests while the page is hidden and sends them once it is visible again.
    // A request started in the background can fail (cached app freezer, Android 15 background
    // network restriction) and the game shows a connection error. The request is only delayed,
    // never modified or re-sent; axios starts its timeout when the request is actually sent.
    public static final String AXIOS_HOLD_HIDDEN_SCRIPT = "axios.interceptors.request.use(function(config){if(!config.url||!config.url.includes(\"kcsapi\")||document.visibilityState!==\"hidden\")return config;console.log(\"[GOTO] kcsapi held while hidden: \"+config.url);return new Promise(function(resolve){function onVisible(){if(document.visibilityState===\"hidden\")return;document.removeEventListener(\"visibilitychange\",onVisible);console.log(\"[GOTO] kcsapi released: \"+config.url);resolve(config);}document.addEventListener(\"visibilitychange\",onVisible);});});";
    private static final String API_TOKEN_KEY = "api_token";
    private static final String API_TOKEN_DUMMY = "0000000000000000000000000000000000000000";
    private final BrowserActivity activity;
    private final KcaPacketStore packetTable;
    private final Handler handler = new Handler();
    ExecutorService executorService = Executors.newFixedThreadPool(30);
    private final SharedPreferences sharedPref;

    public KcsInterface(BrowserActivity ac) {
        activity = ac;
        packetTable = new KcaPacketStore(ac.getApplicationContext(), null, PACKETSTORE_VERSION);
        sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
    }

    // Read on every call so that toggling these settings applies immediately.
    private boolean isBroadcastMode() {
        return sharedPref.getBoolean(PREF_BROADCAST, true);
    }

    private boolean isDevtoolsEnabled() {
        return sharedPref.getBoolean(PREF_DEVTOOLS_DEBUG, false);
    }

    private static boolean isGameHost(String host) {
        return host != null && (host.endsWith(".kancolle-server.com") || host.equals("ooi.moe"));
    }

    @JavascriptInterface
    public void kcs_xhr_intercept(String host, String url, String requestHeader, String responseRaw) {
        if (isGameHost(host)) {
            if (url.contains("kcsapi") && responseRaw.contains("svdata=")) {
                Log.e("GOTO-XHR", host + " " + url + " - " + isDevtoolsEnabled());
                handler.post(() -> processXHR(url, requestHeader, responseRaw));
            }
        }
    }

    @JavascriptInterface
    public void kcs_process_canvas_dataurl(String dataurl) {
        if (dataurl != null && dataurl.length() > 100) {
            Log.e("GOTO-DURL", dataurl.substring(0, 100));
            KcUtils.processDataUriImage(executorService, activity, dataurl);
        }
    }

    /**
     * A kcsapi call failed without a usable response (see AXIOS_INTERCEPT_SCRIPT).
     * @param status HTTP status, or 0 when no response arrived (network error, timeout)
     */
    @JavascriptInterface
    public void kcs_axios_error(String host, String url, int status, String code, String message) {
        if (!isGameHost(host) || url == null) return;
        Log.e("GOTO-XHR", "kcsapi failed: " + url + " " + status + " " + code + " " + message);
        activity.showRequestFailure(url, status, code, message);
    }

    public void processXHR(String url, String request, String response) {
        try {
            response = response.replace("svdata=", "");
            Log.e("GOTO", "response: " + url);
            JsonObject response_obj = JsonParser.parseString(response).getAsJsonObject();
            if (url.contains("api_start2") && response_obj != null) {
                JsonObject api_data = response_obj.getAsJsonObject("api_data");
                if (api_data != null && api_data.has("api_mst_shipgraph")) {
                    SubtitleProviderUtils.loadKcApiData(api_data);
                }
            }

            // A rejected call is reported once, at the top of the game area; success leaves
            // other messages (e.g. a failed broadcast) untouched.
            if (response_obj != null && response_obj.has("api_result")) {
                int result = response_obj.get("api_result").getAsInt();
                if (result != 1) {
                    String message = response_obj.has("api_result_msg") ? response_obj.get("api_result_msg").getAsString() : "";
                    activity.showServerNotice(result, url, message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            KcUtils.reportException(e);
        }

        try {
            if (isBroadcastMode()) sendBroadcast(url, request, response);
        } catch (SQLiteFullException e) {
            activity.runOnUiThread(() -> {
                String text = "[error] broadcast failed: database or disk is full";
                activity.setErrorText(text);
            });
        }
    }

    // The broadcast reaches other apps on the device, so the session token is replaced
    // with a dummy value. Kcanotify reads only the other parameters.
    static String maskApiToken(String request) {
        if (request == null || request.isEmpty()) return request;
        String[] params = request.split("&", -1);
        for (int i = 0; i < params.length; i++) {
            int eq = params[i].indexOf('=');
            String key = eq < 0 ? params[i] : params[i].substring(0, eq);
            if (API_TOKEN_KEY.equals(Uri.decode(key))) {
                params[i] = key + "=" + API_TOKEN_DUMMY;
            }
        }
        return TextUtils.join("&", params);
    }

    public void sendBroadcast(String url, String request, String response) {
        url = url.replace("/kcsapi", "");
        request = maskApiToken(request);
        packetTable.record(url, request, response);
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);

        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        bundle.putString("request", request);
        byte[] responseBytes;
        responseBytes = response.getBytes();
        if (responseBytes.length > 150000) {
            try {
                responseBytes = KcUtils.gzipcompress(response);
                bundle.putBoolean("gzipped", true);
            } catch (Exception e) {
                // do nothing
                bundle.putBoolean("gzipped", false);
            }
        } else {
            bundle.putBoolean("gzipped", false);
        }
        bundle.putByteArray("response", responseBytes);
        bundle.putBoolean("use_devtools", isDevtoolsEnabled());

        intent.putExtras(bundle);
        Log.e("GOTO", "broadcast sent: " + url);
        activity.sendBroadcast(intent);
    }
}
