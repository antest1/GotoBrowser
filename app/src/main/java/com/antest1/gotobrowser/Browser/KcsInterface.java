package com.antest1.gotobrowser.Browser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteFullException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.TextView;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.ContentProvider.KcaPacketStore;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.gotobrowser.Constants.PREF_BROADCAST;
import static com.antest1.gotobrowser.Constants.PREF_DEVTOOLS_DEBUG;
import static com.antest1.gotobrowser.ContentProvider.KcaContentProvider.BROADCAST_ACTION;
import static com.antest1.gotobrowser.ContentProvider.KcaPacketStore.PACKETSTORE_VERSION;

public class KcsInterface {
    public static final String GOTO_ANDROID = "GotoBrowser";
    public static final String AXIOS_INTERCEPT_SCRIPT = "axios.interceptors.response.use(function(response){if(response.config.url.includes(\"kcsapi\")){var url=response.config.url;var request=response.config.data;var host=window.location.hostname;var data=response.data;GotoBrowser.kcs_xhr_intercept(host,url,request,data);}return response;},function(error){return Promise.reject(error);});";
    private final BrowserActivity activity;
    private final KcaPacketStore packetTable;
    private final Handler handler = new Handler();
    ExecutorService executorService = Executors.newFixedThreadPool(30);
    private final boolean broadcast_mode;
    private final boolean use_devtools;

    public KcsInterface(BrowserActivity ac) {
        activity = ac;
        packetTable = new KcaPacketStore(ac.getApplicationContext(), null, PACKETSTORE_VERSION);
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
        broadcast_mode = sharedPref.getBoolean(PREF_BROADCAST, false);
        use_devtools = sharedPref.getBoolean(PREF_DEVTOOLS_DEBUG, false);
    }

    @JavascriptInterface
    public void kcs_xhr_intercept(String host, String url, String requestHeader, String responseRaw) {
        if (host != null && (host.endsWith(".kancolle-server.com") || host.equals("ooi.moe"))) {
            if (url.contains("kcsapi") && responseRaw.contains("svdata=")) {
                Log.e("GOTO-XHR", host + " " + url + " - " + use_devtools);
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

    @JavascriptInterface
    public void kcs_axios_error(String error) {
        activity.runOnUiThread(() -> {
            String text = String.format(Locale.US, "[Error] %s", error);
            ((TextView) activity.findViewById(R.id.kc_error_text)).setText(text);
        });
    }

    public void processXHR(String url, String request, String response) {
        try {
            response = response.replace("svdata=", "");
            Log.e("GOTO", "response: " + url);
            JsonObject response_obj = JsonParser.parseString(response).getAsJsonObject();
            if (url.contains("api_start2") && response_obj != null) {
                JsonObject api_data = response_obj.getAsJsonObject("api_data");
                if (api_data != null && api_data.has("api_mst_shipgraph")) {
                    SubtitleProviderUtils.getCurrentSubtitleProvider().loadKcApiData(api_data);
                }
            }

            String finalResponse = response;
            activity.runOnUiThread(() -> {
                if (response_obj != null) {
                    int result = response_obj.get("api_result").getAsInt();
                    String message = response_obj.get("api_result_msg").getAsString();
                    String text = (result == 1) ? "" : String.format(Locale.US, "[%d] %s\n%s (%d)", result, message, url, finalResponse.length());
                    ((TextView) activity.findViewById(R.id.kc_error_text)).setText(text);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            KcUtils.reportException(e);
        }

        try {
            if (broadcast_mode) sendBroadcast(url, request, response);
        } catch (SQLiteFullException e) {
            activity.runOnUiThread(() -> {
                String text = "[error] broadcast failed: database or disk is full";
                ((TextView) activity.findViewById(R.id.kc_error_text)).setText(text);
            });
        }
    }

    public void sendBroadcast(String url, String request, String response) {
        url = url.replace("/kcsapi", "");
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
        bundle.putBoolean("use_devtools", use_devtools);

        intent.putExtras(bundle);
        Log.e("GOTO", "broadcast sent: " + url);
        activity.sendBroadcast(intent);
    }
}
