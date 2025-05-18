package com.antest1.gotobrowser.Helpers;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GotoVersionCheck {
    @GET("repos/antest1/gotobrowser/releases/latest")
    Call<JsonObject> version();
}


