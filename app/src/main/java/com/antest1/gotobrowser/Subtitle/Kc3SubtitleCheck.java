package com.antest1.gotobrowser.Subtitle;

import com.google.gson.JsonArray;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface Kc3SubtitleCheck {
    @GET("repos/KC3Kai/KC3Kai/commits")
    Call<JsonArray> checkMeta(@Query("path") String path);

    @GET("repos/KC3Kai/kc3-translations/commits")
    Call<JsonArray> check(@Query("path") String path);
}


