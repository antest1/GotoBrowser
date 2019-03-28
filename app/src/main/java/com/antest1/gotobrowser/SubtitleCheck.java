package com.antest1.gotobrowser;

import com.google.gson.JsonArray;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SubtitleCheck {
    @GET("repos/KC3Kai/kc3-translations/commits")
    Call<JsonArray> check(@Query("path") String path);
}


