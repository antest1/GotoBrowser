package com.antest1.gotobrowser.Proxy;

import com.antest1.gotobrowser.Helpers.KcUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

public class KcaResponseObject {
    int status;
    String header;
    byte[] data;

    public KcaResponseObject(Response response) {
        status = response.code();
        header = "";
        Map<String, List<String>> map = response.headers().toMultimap();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().startsWith("X-Android") && !entry.getKey().startsWith("Via")) {
                header += String.format("%s: %s\r\n", entry.getKey(), KcUtils.joinStr(entry.getValue(), "; "));
            }
        }
        try {
            data = response.body().bytes();
        } catch (Exception e) {
            KcUtils.reportException(e);
            data = null;
        }
    }
}
