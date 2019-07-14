package com.antest1.gotobrowser.Proxy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;

import com.antest1.gotobrowser.Browser.ResourceProcess;
import com.antest1.gotobrowser.ContentProvider.KcaPacketStore;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import static com.antest1.gotobrowser.Constants.BROWSER_USERAGENT;
import static com.antest1.gotobrowser.Constants.KANCOLLE_SERVER_LIST;
import static com.antest1.gotobrowser.Constants.OOI_SERVER_LIST;
import static com.antest1.gotobrowser.ContentProvider.KcaContentProvider.BROADCAST_ACTION;
import static com.antest1.gotobrowser.ContentProvider.KcaPacketStore.PACKETSTORE_VERSION;

public class LocalProxyServer {
    private HttpProxyServer proxyServer;
    private static final int PORT = 33914;
    private static final int TIMEOUT = 20000;
    private static final int MAX_BUFFER_SIZE = 256 * 1024 * 1024;

    private Activity activity;
    private Context context;
    private KcaRequest kcaRequest = new KcaRequest();
    private KcaPacketStore packetTable = new KcaPacketStore(context, null, PACKETSTORE_VERSION);

    public LocalProxyServer(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        packetTable = new KcaPacketStore(context, null, PACKETSTORE_VERSION);
        proxyServer = null;
    }

    public static String getServerAddress() {
        return "127.0.0.1:" + PORT;
    }

    private boolean is_on() {
        return proxyServer != null;
    }

    public void start() {
        if (proxyServer == null) {
            HttpFiltersSource filtersSource = getFiltersSource();
            proxyServer = DefaultHttpProxyServer.bootstrap().withPort(PORT).withAllowLocalOnly(false)
                    .withConnectTimeout(TIMEOUT)
                    .withFiltersSource(filtersSource)
                    .withName("FilterProxy")
                    .start();
            Log.e("KCA", "Proxy Start");
            KcUtils.showToast(context, "Proxy Start");
        }
    }

    public void stop() {
        if (is_on()) {
            proxyServer.abort();
            proxyServer = null;
        }
        KcUtils.showToast(context, "Proxy Stop");
        Log.e("KCA", "Stop");
    }

    public void sendBroadcast(String url, String request, String response) {
        url = url.replace("/kcsapi", "");
        packetTable.record(url, request, response);
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);
        Log.e("GOTO", "broadcast sent: " + url);
        activity.sendBroadcast(intent);
    }

    public boolean isKancolleServer(String url) {
        for (String s: KANCOLLE_SERVER_LIST) {
            if (url.startsWith(s)) return true;
        }
        for (String s: OOI_SERVER_LIST) {
            if (url.startsWith(s)) return true;
        }
        return false;
    }

    private HttpFiltersSource getFiltersSource() {
        return new HttpFiltersSourceAdapter() {
            @Override
            public int getMaximumRequestBufferSizeInBytes() {
                return MAX_BUFFER_SIZE;
            }

            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
                        if (httpObject instanceof FullHttpRequest) {
                            FullHttpRequest request = (FullHttpRequest) httpObject;
                            String requestUri = request.getUri();

                            if (request.headers().contains(HttpHeaders.Names.ACCEPT_ENCODING)) {
                                String acceptEncodingData = request.headers().get(HttpHeaders.Names.ACCEPT_ENCODING).trim();
                                if (acceptEncodingData.endsWith(",")) {
                                    acceptEncodingData = acceptEncodingData.concat(" sdch");
                                    request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, acceptEncodingData);
                                }
                            }

                            int resource_type = ResourceProcess.getCurrentState(requestUri);
                            boolean isKcsApi = ResourceProcess.isKcsApi(resource_type);

                            String[] requestData = request.toString().split("\n");
                            String requestHeaderString = "";
                            boolean isKcRequest = false;

                            boolean isInternalRequest = false;

                            Iterator<Map.Entry<String, String>> requestHeader = request.headers().iterator();
                            Log.e("GOTO", "Header:" );
                            while (requestHeader.hasNext()) {
                                Map.Entry<String, String> data = requestHeader.next();
                                String key = data.getKey();
                                String value = data.getValue();
                                Log.e("GOTO", key + " => " + value);
                                requestHeaderString += key + ": " + value + "\r\n";
                                if (key.equals(HttpHeaders.Names.HOST)) {
                                    isKcRequest = isKancolleServer(value);
                                }
                                if (key.equals(HttpHeaders.Names.USER_AGENT)) {
                                    if (value.contains(BROWSER_USERAGENT)) {
                                        isInternalRequest = true;
                                    }
                                }
                            }

                            if (isInternalRequest) {
                                Log.e("KCA", "Request(I) " + request.getUri());
                                String useragent = HttpHeaders.getHeader(request, HttpHeaders.Names.USER_AGENT).replaceAll(BROWSER_USERAGENT, "").trim();
                                request.headers().set(HttpHeaders.Names.USER_AGENT, useragent);
                                return null;
                            } else if (isKcRequest && isKcsApi) {
                                Log.e("KCA", "Request " + request.getUri());
                                ByteBuf contentBuf = request.content();
                                boolean set_flag = false;
                                boolean gzipped = false;
                                byte[] requestBody = new byte[contentBuf.readableBytes()];
                                int readerIndex = contentBuf.readerIndex();
                                contentBuf.getBytes(readerIndex, requestBody);
                                String requestBodyStr = new String(requestBody);

                                try {
                                    KcaResponseObject responseObject = kcaRequest.post(request.getUri(), requestHeaderString, requestBodyStr);
                                    String responseHeader = responseObject.header;
                                    int statusCode = responseObject.status;
                                    byte[] responseBody = responseObject.data;
                                    if (responseBody != null) {
                                        ByteBuf buffer = Unpooled.wrappedBuffer(responseBody);
                                        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode), buffer);
                                        for (String line : responseHeader.split("\r\n")) {
                                            String[] entry = line.trim().split(": ");
                                            HttpHeaders.setHeader(response, entry[0], entry[1]);
                                        }
                                        if (response.headers().contains(HttpHeaders.Names.CONTENT_ENCODING)) {
                                            if (response.headers().get(HttpHeaders.Names.CONTENT_ENCODING).startsWith("gzip")) {
                                                gzipped = true;
                                                responseBody = KcUtils.gzipdecompress(responseBody);
                                            }
                                        }
                                        String resp_str = new String(responseBody);
                                        Log.e("GOTO", "response: " + resp_str.length());
                                        set_flag = true;
                                        try {
                                            sendBroadcast(requestUri, requestBodyStr, resp_str);
                                        } catch (Exception e) {
                                            KcUtils.reportException(e);
                                        }
                                        return response;
                                    }
                                } catch (IOException e) {
                                    KcUtils.reportException(e);
                                    return null;
                                }
                            }
                        }
                        return null;
                    }
                };
            }
        };
    }
}
