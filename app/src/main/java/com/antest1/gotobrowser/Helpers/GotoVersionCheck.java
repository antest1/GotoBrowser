package com.antest1.gotobrowser.Helpers;

import retrofit2.Call;
import retrofit2.http.HEAD;

public interface GotoVersionCheck {
    // The web page redirects to /releases/tag/<tag>. Unlike the REST API's
    // releases/latest, it is not subject to the 60 requests/hour unauthenticated
    // limit, which answers 403 once exhausted (shared by everyone behind one IP).
    @HEAD("antest1/GotoBrowser/releases/latest")
    Call<Void> latestRelease();
}
