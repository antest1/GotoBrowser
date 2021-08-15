package com.antest1.gotobrowser.Subtitle;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface KcwikiSubtitleApi {
    @GET("/subtitles/version")
    Call<JsonObject> getSubTitleVersion();

    @GET("/subtitles/{locale}")
    Call<JsonObject> getSubtitle(@Path("locale") String locale);
}
