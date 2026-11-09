package com.antest1.gotobrowser;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.antest1.gotobrowser.Browser.WebViewManager;
import com.antest1.gotobrowser.Helpers.CredentialStore;

public class GotoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences pref = getSharedPreferences(getString(R.string.preference_key), Context.MODE_PRIVATE);
        CredentialStore.migrateLegacyPassword(pref);
        WebViewManager.migrateRetiredConnector(pref);
    }
}
