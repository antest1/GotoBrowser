package com.antest1.gotobrowser.Activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.webkit.WebViewFeature;

import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Helpers.GotoVersionCheck;
import com.antest1.gotobrowser.Helpers.KcEnUtils;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;

import static com.antest1.gotobrowser.Constants.DEFAULT_ALTER_GADGET_URL;
import static com.antest1.gotobrowser.Constants.GITHUBAPI_ROOT;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD_PROXY;
import static com.antest1.gotobrowser.Constants.PREF_APP_VERSION;
import static com.antest1.gotobrowser.Constants.PREF_BROADCAST;
import static com.antest1.gotobrowser.Constants.PREF_CHECK_UPDATE;
import static com.antest1.gotobrowser.Constants.PREF_CLICK_SETTINGS;
import static com.antest1.gotobrowser.Constants.PREF_DEVTOOLS_DEBUG;
import static com.antest1.gotobrowser.Constants.PREF_DOWNLOAD_RETRY;
import static com.antest1.gotobrowser.Constants.PREF_FONT_PREFETCH;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_LEGACY_RENDERER;
import static com.antest1.gotobrowser.Constants.PREF_MOD_FPS;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_DELETE;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN_UPDATE;
import static com.antest1.gotobrowser.Constants.PREF_MOD_CRIT;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAI3D;
import static com.antest1.gotobrowser.Constants.PREF_MULTIWIN_MARGIN;
import static com.antest1.gotobrowser.Constants.PREF_PIP_MODE;
import static com.antest1.gotobrowser.Constants.PREF_SETTINGS;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_UPDATE;
import static com.antest1.gotobrowser.Constants.PREF_TP_DISCLAIMED;
import static com.antest1.gotobrowser.Constants.PREF_USE_EXTCACHE;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.getRetrofitAdapter;

import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        createNotificationChannel();
    }

    public static void setInitialSettings(SharedPreferences sharedPref) {
        SharedPreferences.Editor editor = sharedPref.edit();
        for (String key: PREF_SETTINGS) {
            if (!sharedPref.contains(key)) switch (key) {
                case PREF_LANDSCAPE:
                case PREF_ADJUSTMENT:
                case PREF_FONT_PREFETCH:
                case PREF_USE_EXTCACHE:
                case PREF_DOWNLOAD_RETRY:
                case PREF_BROADCAST:
                    editor.putBoolean(key, true);
                    break;
                case PREF_PIP_MODE:
                case PREF_MULTIWIN_MARGIN:
                case PREF_DEVTOOLS_DEBUG:
                case PREF_TP_DISCLAIMED:
                case PREF_MOD_KANTAI3D:
                case PREF_MOD_KANTAIEN:
                case PREF_MOD_FPS:
                case PREF_MOD_CRIT:
                case PREF_LEGACY_RENDERER:
                    editor.putBoolean(key, false);
                    break;
                case PREF_ALTER_GADGET:
                    editor.putBoolean(key, !Locale.getDefault().getCountry().equals("JP"));
                    break;
                case PREF_ALTER_METHOD:
                    editor.putString(key, "1");
                    break;
                case PREF_ALTER_ENDPOINT:
                    editor.putString(key, DEFAULT_ALTER_GADGET_URL);
                    break;
                default:
                    editor.putString(key, "");
                    break;
            }
        }
        editor.apply();
    }

    public static Locale getCurrentLocale(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return context.getResources().getConfiguration().getLocales().get(0);
        } else{
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener,
            SharedPreferences.OnSharedPreferenceChangeListener,
            Preference.OnPreferenceClickListener {
        private VersionDatabase versionTable;
        private SharedPreferences sharedPref;
        private GotoVersionCheck appCheck;
        private KcEnUtils enUtils = new KcEnUtils();

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            sharedPref = getContext().getSharedPreferences(
                    getString(R.string.preference_key), Context.MODE_PRIVATE);
            sharedPref.registerOnSharedPreferenceChangeListener(this);
            versionTable = new VersionDatabase(getContext(), null, VERSION_TABLE_VERSION);
            appCheck = getRetrofitAdapter(getContext(), GITHUBAPI_ROOT).create(GotoVersionCheck.class);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Map<String, ?> allEntries = sharedPref.getAll();
            for (String key : allEntries.keySet()) {
                Log.e("GOTO", key);
                Preference preference = findPreference(key);
                if (preference == null) continue;
                if (preference instanceof ListPreference) {
                    Log.e("GOTO", key + ": " + sharedPref.getString(key, ""));
                } else if (preference instanceof EditTextPreference) {
                    Log.e("GOTO", key + ": " + sharedPref.getString(key, ""));
                    EditTextPreference ep = (EditTextPreference) preference;
                    ep.setSummary(sharedPref.getString(key, ""));
                } else if (preference instanceof SwitchPreferenceCompat) {
                    Log.e("GOTO", key + ": " + sharedPref.getBoolean(key, false));
                    SwitchPreferenceCompat sp = (SwitchPreferenceCompat) preference;
                    sp.setChecked(sharedPref.getBoolean(key, false));
                }
                preference.setOnPreferenceChangeListener(this);
            }
            for (String key: PREF_CLICK_SETTINGS) {
                Preference preference = findPreference(key);
                if (preference != null) {
                    preference.setOnPreferenceClickListener(this);
                }
            }
            updateSubtitleDescriptionText();
            updateKantaiEnDescriptionText();
            updateKantai3dDisable();
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            Preference version_pref = findPreference(PREF_APP_VERSION);
            version_pref.setSummary(BuildConfig.VERSION_NAME);
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            String key = preference.getKey();
            switch (key) {
                case PREF_CHECK_UPDATE:
                    KcUtils.requestLatestAppVersion(getActivity(), appCheck, true);
                    break;
                case PREF_SUBTITLE_UPDATE:
                    SubtitleProviderUtils.getCurrentSubtitleProvider().downloadUpdateFromPreference(this, versionTable);
                    break;
                case PREF_MOD_KANTAIEN_UPDATE:
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            enUtils.requestPatchUpdate(this);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case PREF_MOD_KANTAIEN_DELETE:
                    enUtils.requestPatchDelete(this);
                    break;
            }
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            if (preference instanceof ListPreference) {
                String stringValue = (String) newValue;
                if (key.equals(PREF_ALTER_METHOD)) {
                    if (stringValue.equals(PREF_ALTER_METHOD_PROXY)
                            && !WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                        if (getActivity() != null) {
                            Snackbar.make(getActivity().findViewById(R.id.main_container),
                                    "PROXY_OVERRIDE not supported, use other option",
                                    Snackbar.LENGTH_LONG).show();
                        }
                        return false;
                    }
                }
                sharedPref.edit().putString(key, stringValue).apply();
                if (key.equals(PREF_SUBTITLE_LOCALE)) {
                    setSubtitlePreference(stringValue);
                }
            }
            if (preference instanceof EditTextPreference) {
                String stringValue = (String) newValue;
                if (stringValue.length() == 0) stringValue = DEFAULT_ALTER_GADGET_URL;
                sharedPref.edit().putString(key, stringValue).apply();
                preference.setSummary(stringValue);
            }
            if (preference instanceof SwitchPreferenceCompat) {
                sharedPref.edit().putBoolean(key, (boolean) newValue).apply();
                if (key.equals(PREF_USE_EXTCACHE)) {
                    updateSubtitleDescriptionText();
                }
                if (key.equals(PREF_MOD_KANTAIEN)) {
                    updateKantaiEnDescriptionText();
                }
                if (key.equals(PREF_LEGACY_RENDERER)) {
                    updateKantai3dDisable();
                }
            }
            return true;
        }

        private void updateSubtitleDescriptionText() {
            String subtitleLocale = sharedPref.getString(PREF_SUBTITLE_LOCALE, "");
            if (subtitleLocale.length() > 0) {
                setSubtitlePreference(subtitleLocale);
            } else {
                findPreference(PREF_SUBTITLE_UPDATE).setEnabled(false);
                findPreference(PREF_SUBTITLE_UPDATE).setSummary(getString(R.string.subtitle_select_language));
            }
        }


        private void updateKantai3dDisable() {
            // Kantai3D only works with WebGL renderer
            // Gray out the option when legacy renderer is chosen
            boolean isWebglEnabled = !sharedPref.getBoolean(PREF_LEGACY_RENDERER, false);
            findPreference(PREF_MOD_KANTAI3D).setEnabled(isWebglEnabled);
        }

        private void setSubtitlePreference(String subtitleLocaleCode) {
            Preference subtitleUpdate = findPreference(PREF_SUBTITLE_UPDATE);
            SubtitleProviderUtils.getSubtitleProvider(subtitleLocaleCode).checkUpdateFromPreference(this, subtitleLocaleCode, subtitleUpdate, versionTable);
        }

        private void updateKantaiEnDescriptionText() {
            Preference kantaiEn = findPreference(PREF_MOD_KANTAIEN);
            Preference kantaiEnUpdate = findPreference(PREF_MOD_KANTAIEN_UPDATE);

            if (sharedPref.getBoolean(PREF_MOD_KANTAIEN, false)) {
                kantaiEnUpdate.setSummary("Checking updates...");
                kantaiEnUpdate.setEnabled(false);
                enUtils.checkKantaiEnUpdate(this, kantaiEnUpdate);
            } else {
                kantaiEnUpdate.setEnabled(false);
                kantaiEnUpdate.setSummary("Mod disabled.");
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("en_patch", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
