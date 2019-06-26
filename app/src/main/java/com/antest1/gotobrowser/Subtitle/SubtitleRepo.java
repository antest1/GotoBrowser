package com.antest1.gotobrowser.Subtitle;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface SubtitleRepo {
    @GET("KC3Kai/kc3-translations/{commit}/{path}")
    Call<JsonObject> download(@Path("commit") String commit, @Path("path") String path);
}
