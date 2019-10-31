package com.antest1.gotobrowser.Browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.antest1.gotobrowser.ContentProvider.KcaPacketStore;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static com.antest1.gotobrowser.ContentProvider.KcaContentProvider.BROADCAST_ACTION;
import static com.antest1.gotobrowser.ContentProvider.KcaPacketStore.PACKETSTORE_VERSION;

public class KcsInterface {
    public static final String GOTO_ANDROID = "GotoBrowser";
    public static final String AXIOS_INTERCEPT_SCRIPT = "axios.interceptors.response.use(function(response){if(response.config.url.includes(\"kcsapi\")){var url=response.config.url;var request=response.config.data;var data=response.data;GotoBrowser.kcs_xhr_intercept(url,request,data);}return response;},function(error){return Promise.reject(error);});";
    private Activity activity;
    private KcaPacketStore packetTable;
    private final Handler handler = new Handler();

    public KcsInterface(Activity ac) {
        activity = ac;
        packetTable = new KcaPacketStore(ac.getApplicationContext(), null, PACKETSTORE_VERSION);
    }

    @JavascriptInterface
    public void kcs_xhr_intercept(String url, String requestHeader, String responseRaw) {
        if (url.contains("kcsapi") && responseRaw.contains("svdata=")) {
            Log.e("GOTO-XHR", url);
            handler.post(() -> sendBroadcast(url, requestHeader, responseRaw));
        };
    }

    public void sendBroadcast(String url, String request, String response) {
        url = url.replace("/kcsapi", "");
        packetTable.record(url, request, response);
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);
        Log.e("GOTO", "broadcast sent: " + url);
        activity.sendBroadcast(intent);
    }
}
