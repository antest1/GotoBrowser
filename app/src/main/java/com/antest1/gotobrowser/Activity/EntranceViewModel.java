package com.antest1.gotobrowser.Activity;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;

import com.antest1.gotobrowser.Browser.WebViewManager;
import com.antest1.gotobrowser.Helpers.KenPatcher;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.R;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static com.antest1.gotobrowser.Constants.CACHE_DIR;
import static com.antest1.gotobrowser.Constants.CONN_DMM;
import static com.antest1.gotobrowser.Constants.PREF_CONNECTOR;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.PREF_BROADCAST;
import static com.antest1.gotobrowser.Constants.PREF_PANELSTART;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.clearApplicationCache;

public class EntranceViewModel extends AndroidViewModel {
    private final SharedPreferences sharedPref;
    private final VersionDatabase versionTable;
    private final boolean kcanotifyInstalledFlag;
    private final KenPatcher kenPatcher = new KenPatcher();

    private final MutableLiveData<String> connector = new MutableLiveData<>();
    private final MutableLiveData<Boolean> silentMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> broadcastMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> panelStart = new MutableLiveData<>();

    public EntranceViewModel(@NonNull Application application) {
        super(application);
        sharedPref = application.getSharedPreferences(application.getString(R.string.preference_key), Context.MODE_PRIVATE);
        versionTable = new VersionDatabase(application, null, VERSION_TABLE_VERSION);
        kcanotifyInstalledFlag = KcUtils.isKcanotifyInstalled(application);
        kenPatcher.prepare(application);

        connector.setValue(sharedPref.getString(PREF_CONNECTOR, CONN_DMM));
        silentMode.setValue(sharedPref.getBoolean(PREF_SILENT, false));
        broadcastMode.setValue(sharedPref.getBoolean(PREF_BROADCAST, false));
        panelStart.setValue(sharedPref.getBoolean(PREF_PANELSTART, false));
    }

    public LiveData<String> getConnector() { return connector; }
    public LiveData<Boolean> getSilentMode() { return silentMode; }
    public LiveData<Boolean> getBroadcastMode() { return broadcastMode; }
    public LiveData<Boolean> getPanelStart() { return panelStart; }

    public boolean isKcanotifyInstalled() {
        return kcanotifyInstalledFlag;
    }

    public void setSilentMode(boolean enabled) {
        sharedPref.edit().putBoolean(PREF_SILENT, enabled).apply();
        silentMode.setValue(enabled);
    }

    public void setBroadcastMode(boolean enabled) {
        sharedPref.edit().putBoolean(PREF_BROADCAST, enabled).apply();
        broadcastMode.setValue(enabled);
    }

    public void setPanelStart(boolean enabled) {
        sharedPref.edit().putBoolean(PREF_PANELSTART, enabled).apply();
        panelStart.setValue(enabled);
    }

    public void setConnector(String value) {
        sharedPref.edit().putString(PREF_CONNECTOR, value).apply();
        connector.setValue(value);
    }

    public void clearBrowserCache() {
        Context context = getApplication().getApplicationContext();
        // clear webview cache
        WebView webview = new WebView(context);
        webview.clearCache(true);

        // clear version table
        versionTable.clearVersionDatabase();

        // clear internal cache dir
        clearApplicationCache(context, context.getCacheDir());

        // clear resource cache dir
        File cache_dir = new File(KcUtils.getAppCacheFileDir(context, CACHE_DIR));
        clearApplicationCache(context, cache_dir);

        // clear legacy cache dir
        File cache_old = new File(KcUtils.getAppCacheFileDir(context, "/cache/"));
        if (cache_old.exists()) {
            clearApplicationCache(context, cache_old);
            cache_old.delete();
        }

        // clear patched cache dir
        for (KenPatcher.PatchLanguage language : KenPatcher.PatchLanguage.values()) {
            String folderName = "/_patched_cache_" + language.name().toLowerCase();
            String patched_cache_dir = KcUtils.getAppCacheFileDir(context, folderName);
            clearApplicationCache(context, new File(patched_cache_dir));
        }
    }

    public SharedPreferences getSharedPref() {
        return sharedPref;
    }

    public KenPatcher getKenPatcher() {
        return kenPatcher;
    }
}
