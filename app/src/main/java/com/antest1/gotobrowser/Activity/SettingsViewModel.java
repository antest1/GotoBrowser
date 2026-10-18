package com.antest1.gotobrowser.Activity;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Helpers.GotoVersionCheck;
import com.antest1.gotobrowser.Helpers.KcEnUtils;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils;

import java.io.IOException;
import java.util.Locale;

import static com.antest1.gotobrowser.Constants.DEFAULT_ALTER_GADGET_URL;
import static com.antest1.gotobrowser.Constants.DEFAULT_SUBTITLE_FONT_SIZE;
import static com.antest1.gotobrowser.Constants.GITHUBAPI_ROOT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD_PROXY;
import static com.antest1.gotobrowser.Constants.PREF_APP_VERSION;
import static com.antest1.gotobrowser.Constants.PREF_CHECK_UPDATE;
import static com.antest1.gotobrowser.Constants.PREF_LEGACY_RENDERER;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAI3D;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_DELETE;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_UPDATE;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH_EN;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH_GITHUB;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH_ID;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH_NAME;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_FONTSIZE;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_UPDATE;
import static com.antest1.gotobrowser.Constants.PREF_USE_EXTCACHE;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.getRetrofitAdapter;

/**
 * Holds the observable state of the Compose settings screen and performs the
 * same side effects the old {@code SettingsFragment} (PreferenceFragmentCompat)
 * used to trigger. Async helpers that used to mutate Preference objects now
 * push updates back through {@link SettingsStatusHost}, which is implemented by
 * {@link SettingsActivity} and delegates to the LiveData exposed here.
 */
public class SettingsViewModel extends AndroidViewModel {
    /** Host context is not available while the screen is off-screen. */
    private static final String SUBTITLE_UPDATE_LOADING = "checking updates...";

    private final SharedPreferences sharedPref;
    private final VersionDatabase versionTable;
    private final GotoVersionCheck appCheck;
    private final KcEnUtils enUtils = new KcEnUtils();

    private final MutableLiveData<String> patchUpdateSummary = new MutableLiveData<>("checking updates...");
    private final MutableLiveData<Boolean> patchUpdateEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<String> subtitleUpdateSummary = new MutableLiveData<>(SUBTITLE_UPDATE_LOADING);
    private final MutableLiveData<Boolean> subtitleUpdateEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> patchAboutTitleRes = new MutableLiveData<>(R.string.settings_mod_kantaien_about);
    private final MutableLiveData<String> patchAboutUrl = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        Context context = application.getApplicationContext();
        sharedPref = context.getSharedPreferences(context.getString(R.string.preference_key), Context.MODE_PRIVATE);
        versionTable = new VersionDatabase(context, null, VERSION_TABLE_VERSION);
        appCheck = getRetrofitAdapter(context, GITHUBAPI_ROOT).create(GotoVersionCheck.class);

        patchAboutUrl.setValue(context.getString(R.string.settings_mod_kccp_patch_en_link));
    }

    // ---------------------------------------------------------------------
    // Raw preference accessors
    // ---------------------------------------------------------------------

    public SharedPreferences getSharedPref() {
        return sharedPref;
    }

    public boolean getBoolean(String key, boolean defValue) {
        return sharedPref.getBoolean(key, defValue);
    }

    public String getString(String key, String defValue) {
        return sharedPref.getString(key, defValue);
    }

    public int getInt(String key, int defValue) {
        return sharedPref.getInt(key, defValue);
    }

    public int getSubtitleFontSize() {
        return sharedPref.getInt(PREF_SUBTITLE_FONTSIZE, DEFAULT_SUBTITLE_FONT_SIZE);
    }

    public String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public void setBoolean(String key, boolean value) {
        sharedPref.edit().putBoolean(key, value).apply();
    }

    public void setString(String key, String value) {
        sharedPref.edit().putString(key, value).apply();
    }

    public void setInt(String key, int value) {
        sharedPref.edit().putInt(key, value).apply();
    }

    // ---------------------------------------------------------------------
    // Observable status, driven by SettingsStatusHost
    // ---------------------------------------------------------------------

    public LiveData<String> getPatchUpdateSummary() {
        return patchUpdateSummary;
    }

    public LiveData<Boolean> getPatchUpdateEnabled() {
        return patchUpdateEnabled;
    }

    public LiveData<String> getSubtitleUpdateSummary() {
        return subtitleUpdateSummary;
    }

    public LiveData<Boolean> getSubtitleUpdateEnabled() {
        return subtitleUpdateEnabled;
    }

    public LiveData<Integer> getPatchAboutTitleRes() {
        return patchAboutTitleRes;
    }

    public LiveData<String> getPatchAboutUrl() {
        return patchAboutUrl;
    }

    public void setPatchUpdateStatus(String summary, boolean enabled) {
        patchUpdateSummary.setValue(summary);
        patchUpdateEnabled.setValue(enabled);
    }

    public void setSubtitleUpdateStatus(String summary, boolean enabled) {
        subtitleUpdateSummary.setValue(summary);
        subtitleUpdateEnabled.setValue(enabled);
    }

    public void setPatchGithubInfo(int titleResId, String url) {
        patchAboutTitleRes.setValue(titleResId);
        patchAboutUrl.setValue(url);
    }

    // ---------------------------------------------------------------------
    // Derived / dependent rows
    // ---------------------------------------------------------------------

    public boolean isKantai3dEnabled() {
        // Kantai3D only works with the WebGL renderer.
        return !sharedPref.getBoolean(PREF_LEGACY_RENDERER, false);
    }

    public boolean isKccpPatchEnabled() {
        return sharedPref.getBoolean(PREF_MOD_KCCP_LANG_PATCH, false);
    }

    public boolean isAlterGadgetEnabled() {
        return sharedPref.getBoolean(com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET, false);
    }

    public String getKccpPatchSummary(String value) {
        if (PREF_MOD_KCCP_LANG_PATCH_EN.equals(value)) {
            return getApplication().getString(R.string.settings_mod_kccp_patch_en_summary);
        } else if (PREF_MOD_KCCP_LANG_PATCH_ID.equals(value)) {
            return getApplication().getString(R.string.settings_mod_kccp_patch_id_summary);
        }
        return value;
    }

    /**
     * Refresh the patch dropdown dependent rows (About link + Download row).
     * Mirrors the old SettingsFragment#updateKCCPLangPatchInfo and
     * #updateKCCPLangPatchDescriptionText.
     */
    public void refreshKccpDependentRows(String lang) {
        Context context = getApplication().getApplicationContext();
        if (lang == null) {
            lang = sharedPref.getString(PREF_MOD_KCCP_LANG_PATCH_NAME, PREF_MOD_KCCP_LANG_PATCH_EN);
        }

        if (PREF_MOD_KCCP_LANG_PATCH_ID.equals(lang)) {
            setPatchGithubInfo(R.string.settings_mod_kantaiid_about,
                    context.getString(R.string.settings_mod_kccp_patch_id_link));
        } else {
            setPatchGithubInfo(R.string.settings_mod_kantaien_about,
                    context.getString(R.string.settings_mod_kccp_patch_en_link));
        }

        if (sharedPref.getBoolean(PREF_MOD_KCCP_LANG_PATCH, false)) {
            enUtils.setPatchLanguage(lang);
            enUtils.checkKantaiEnUpdate(host);
        } else {
            setPatchUpdateStatus("Mod disabled.", false);
        }
    }

    public void refreshSubtitleDescription() {
        Context context = getApplication().getApplicationContext();
        String subtitleLocale = sharedPref.getString(PREF_SUBTITLE_LOCALE, "");
        if (subtitleLocale != null && !subtitleLocale.isEmpty()) {
            SubtitleProviderUtils.getSubtitleProvider(subtitleLocale)
                    .checkUpdateFromPreference(host, subtitleLocale, versionTable);
        } else {
            setSubtitleUpdateStatus(context.getString(R.string.subtitle_select_language), false);
        }
    }

    // ---------------------------------------------------------------------
    // Row actions
    // ---------------------------------------------------------------------

    public void checkAppUpdate(android.app.Activity activity) {
        KcUtils.requestLatestAppVersion(activity, appCheck, true);
    }

    public void downloadSubtitleUpdate() {
        SubtitleProviderUtils.getCurrentSubtitleProvider()
                .downloadUpdateFromPreference(host, versionTable);
    }

    public void requestPatchUpdate() {
        try {
            enUtils.requestPatchUpdate(host);
        } catch (IOException e) {
            Log.e("GOTO", KcUtils.getStringFromException(e));
        }
    }

    public void requestPatchDelete() {
        enUtils.requestPatchDelete(host);
    }

    public void onSubtitleLocaleChanged(String localeCode) {
        SubtitleProviderUtils.getSubtitleProvider(localeCode)
                .checkUpdateFromPreference(host, localeCode, versionTable);
    }

    public void onPatchLanguageChanged(String lang) {
        sharedPref.edit().putString(PREF_MOD_KCCP_LANG_PATCH_NAME, lang).apply();
        refreshKccpDependentRows(lang);
    }

    public void onAlterGadgetChanged() {
        setBoolean(com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET, isAlterGadgetEnabled());
    }

    public void onExternalCacheChanged() {
        refreshSubtitleDescription();
    }

    public void onLegacyRendererChanged() {
        // Dependent rows (PREF_MOD_KANTAI3D) recompute from shared prefs.
    }

    public void onKccpPatchChanged() {
        refreshKccpDependentRows(null);
    }

    /**
     * @return true if the change is accepted, false if it should be rejected
     *         (e.g. PROXY_OVERRIDE is not supported on this device).
     */
    public boolean onAlterMethodSelected(String value) {
        if (PREF_ALTER_METHOD_PROXY.equals(value)
                && !androidx.webkit.WebViewFeature.isFeatureSupported(
                        androidx.webkit.WebViewFeature.PROXY_OVERRIDE)) {
            return false;
        }
        setString(PREF_ALTER_METHOD, value);
        return true;
    }

    public void onAlterEndpointChanged(String value) {
        if (value == null || value.isEmpty()) {
            value = DEFAULT_ALTER_GADGET_URL;
        }
        setString(PREF_ALTER_ENDPOINT, value);
    }

    public String getAlterEndpoint() {
        return sharedPref.getString(PREF_ALTER_ENDPOINT, DEFAULT_ALTER_GADGET_URL);
    }

    public Intent getPatchAboutIntent() {
        String url = patchAboutUrl.getValue();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (url != null) intent.setData(Uri.parse(url));
        return intent;
    }

    // ---------------------------------------------------------------------
    // SettingsStatusHost backed by this ViewModel's LiveData
    // ---------------------------------------------------------------------

    private final SettingsStatusHost host = new SettingsStatusHost() {
        @Override
        public Context getContext() {
            return getApplication().getApplicationContext();
        }

        @Override
        public Context requireContext() {
            return getApplication().getApplicationContext();
        }

        @Override
        public String getString(int resId) {
            return getApplication().getString(resId);
        }

        @Override
        public void setPatchUpdateStatus(String summary, boolean enabled) {
            SettingsViewModel.this.setPatchUpdateStatus(summary, enabled);
        }

        @Override
        public void setSubtitleUpdateStatus(String summary, boolean enabled) {
            SettingsViewModel.this.setSubtitleUpdateStatus(summary, enabled);
        }

        @Override
        public void setPatchGithubInfo(int titleResId, String url) {
            SettingsViewModel.this.setPatchGithubInfo(titleResId, url);
        }
    };

    public static String formatSubtitleExample(Context context, int size) {
        return String.format(Locale.US, context.getString(R.string.settings_subtitle_example), size);
    }

    public void setSubtitleLoaded(boolean value) {
        // Retained for interface parity with the previous fragment implementation.
    }
}
