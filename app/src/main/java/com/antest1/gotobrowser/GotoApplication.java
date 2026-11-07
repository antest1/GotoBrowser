package com.antest1.gotobrowser;

import android.app.Application;
import android.content.Context;

import com.antest1.gotobrowser.Helpers.CredentialStore;

public class GotoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CredentialStore.migrateLegacyPassword(
                getSharedPreferences(getString(R.string.preference_key), Context.MODE_PRIVATE));
    }
}
