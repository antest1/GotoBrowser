package com.antest1.gotobrowser.Helpers;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;

public interface K3dMetadataApi {
    @GET("gh/laplamgor/kantai3d-depth-maps@latest/metadata.json")
    Call<JsonObject> getMetadata();
}
