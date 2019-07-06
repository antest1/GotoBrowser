package com.antest1.gotobrowser.Proxy;

import android.util.Base64;
import android.util.Log;

import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.HttpHeaders;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.antest1.gotobrowser.Constants.BROWSER_USERAGENT;

class KcaRequest {
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(36000, TimeUnit.SECONDS)
            .readTimeout(36000, TimeUnit.SECONDS)
            .build();

    private static final MediaType FORM_DATA = MediaType.parse("application/x-www-form-urlencoded");

    KcaResponseObject post(String uri, String header_str, String data) throws IOException {
        Log.e("KCA", uri + " " + data);
        String url = "";
        String[] header = header_str.split("\r\n");
        for (int i = 0; i < header.length; i++) {
            String key = header[i].split(": ")[0];
            String value = header[i].split(": ")[1];
            if (key.startsWith(HttpHeaders.Names.HOST)) {
                url = "http://".concat(value).concat(uri);
                break;
            }
        }

        RequestBody body = RequestBody.create(FORM_DATA, data);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        for (int i = 0; i < header.length; i++) {
            String key = header[i].split(": ")[0];
            String value = header[i].split(": ")[1];
            if (key.startsWith(HttpHeaders.Names.USER_AGENT)) {
                builder.addHeader(key, value.concat(" ").concat(BROWSER_USERAGENT));
            } else if (!key.startsWith(HttpHeaders.Names.VIA)) {
                builder.addHeader(key, value);
            }
        }

        Request request = builder.build();
        Response response = client.newCall(request).execute();

        JsonObject responseData = new JsonObject();
        String responseHeaderString = "";

        Map<String, List<String>> map = response.headers().toMultimap();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().startsWith("X-Android") && !entry.getKey().startsWith("Via")) {
                responseHeaderString += String.format("%s: %s\r\n", entry.getKey(), KcUtils.joinStr(entry.getValue(), "; "));
            }
        }

        //String bytesData = Base64.encodeToString(response.body().bytes(), Base64.DEFAULT);
        Log.e("KCA", "Request URI: " + uri);
        //Log.e("GOTO", "Response Length: " + bytesData.length());

        return new KcaResponseObject(response);
    }

}

