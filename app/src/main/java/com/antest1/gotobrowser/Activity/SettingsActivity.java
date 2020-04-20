package com.antest1.gotobrowser.Activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleCheck;
import com.antest1.gotobrowser.Subtitle.SubtitleRepo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.antest1.gotobrowser.Constants.GITHUBAPI_ROOT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET;
import static com.antest1.gotobrowser.Constants.PREF_APP_VERSION;
import static com.antest1.gotobrowser.Constants.PREF_CHECK_UPDATE;
import static com.antest1.gotobrowser.Constants.PREF_FONT_PREFETCH;
import static com.antest1.gotobrowser.Constants.PREF_MULTIWIN_MARGIN;
import static com.antest1.gotobrowser.Constants.PREF_PANEL_METHOD;
import static com.antest1.gotobrowser.Constants.PREF_PIP_MODE;
import static com.antest1.gotobrowser.Constants.PREF_SETTINGS;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_UPDATE;
import static com.antest1.gotobrowser.Constants.SUBTITLE_PATH_FORMAT;
import static com.antest1.gotobrowser.Constants.SUBTITLE_ROOT;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.getRetrofitAdapter;

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
    }

    public static void setInitialSettings(SharedPreferences sharedPref) {
        SharedPreferences.Editor editor = sharedPref.edit();
        for (String key: PREF_SETTINGS) {
            if (!sharedPref.contains(key)) switch (key) {
                case PREF_FONT_PREFETCH:
                    editor.putBoolean(key, true);
                    break;
                case PREF_PIP_MODE:
                case PREF_ALTER_GADGET:
                case PREF_MULTIWIN_MARGIN:
                    editor.putBoolean(key, false);
                    break;
                case PREF_PANEL_METHOD:
                    editor.putString(key, "1");
                    break;
                default:
                    editor.putString(key, "");
                    break;
            }
        }
        editor.apply();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {
        private VersionDatabase versionTable;
        private SubtitleCheck updateCheck;
        private SubtitleRepo subtitleRepo;
        private SharedPreferences sharedPref;
        private JsonObject subtitleData = null;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            sharedPref = getContext().getSharedPreferences(
                    getString(R.string.preference_key), Context.MODE_PRIVATE);
            sharedPref.registerOnSharedPreferenceChangeListener(this);
            versionTable = new VersionDatabase(getContext(), null, VERSION_TABLE_VERSION);
            updateCheck = getRetrofitAdapter(getContext(), GITHUBAPI_ROOT).create(SubtitleCheck.class);
            subtitleRepo = getRetrofitAdapter(getContext(), SUBTITLE_ROOT).create(SubtitleRepo.class);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            findPreference(PREF_APP_VERSION).setSummary(BuildConfig.VERSION_NAME);
            Map<String, ?> allEntries = sharedPref.getAll();
            for (String key : allEntries.keySet()) {
                Log.e("GOTO", key);
                Preference preference = findPreference(key);
                if (preference == null) continue;
                if (preference instanceof ListPreference) {
                    Log.e("GOTO", key + ": " + sharedPref.getString(key, ""));
                } else if (preference instanceof SwitchPreferenceCompat) {
                    Log.e("GOTO", key + ": " + sharedPref.getBoolean(key, false));
                    SwitchPreferenceCompat sp = (SwitchPreferenceCompat) preference;
                    sp.setChecked(sharedPref.getBoolean(key, false));
                }
                preference.setOnPreferenceChangeListener(this);
            }

            String subtitleLocale = sharedPref.getString(PREF_SUBTITLE_LOCALE, "");
            if (subtitleLocale.length() > 0) {
                setSubtitlePreference(subtitleLocale);
            } else {
                findPreference(PREF_SUBTITLE_UPDATE).setEnabled(false);
                findPreference(PREF_SUBTITLE_UPDATE).setSummary(getString(R.string.subtitle_select_language));
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            String key = preference.getKey();
            if (key.equals(PREF_CHECK_UPDATE)) {
                KcUtils.requestLatestAppVersion(getActivity(), updateCheck, true);
            } else if (key.equals(PREF_SUBTITLE_UPDATE) && subtitleData != null) {
                onLocaleItemDownload(subtitleData);
            }
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            if (preference instanceof ListPreference) {
                String stringValue = (String) newValue;
                sharedPref.edit().putString(key, stringValue).apply();
                if (key.equals(PREF_SUBTITLE_LOCALE)) {
                    setSubtitlePreference(stringValue);
                }
            }
            if (preference instanceof SwitchPreferenceCompat) {
                sharedPref.edit().putBoolean(key, (boolean) newValue).apply();
            }
            return true;
        }

        private void setSubtitlePreference(String subtitleLocale) {
            subtitleData = null;
            Preference subtitleUpdate = findPreference(PREF_SUBTITLE_UPDATE);
            subtitleUpdate.setSummary("checking updates...");
            subtitleUpdate.setEnabled(false);
            String subtitlePath = String.format(Locale.US, SUBTITLE_PATH_FORMAT, subtitleLocale);
            Call<JsonArray> call = updateCheck.check(subtitlePath);
            call.enqueue(new Callback<JsonArray>() {
                @Override
                public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                    if (getActivity() == null) return;
                    JsonArray commit_log = response.body();
                    if (commit_log != null && !commit_log.isJsonNull()) {
                        String filename = String.format(Locale.US, "quotes_%s.json", subtitleLocale);
                        String currentCommit = versionTable.getValue(filename);
                        if (commit_log.size() > 0) {
                            JsonObject latestData = commit_log.get(0).getAsJsonObject();
                            String latestCommit = latestData.get("sha").getAsString();
                            if (!currentCommit.equals(latestCommit)) {
                                subtitleData = new JsonObject();
                                subtitleData.addProperty("locale_code", subtitleLocale);
                                subtitleData.addProperty("latest_commit", latestCommit);
                                subtitleData.addProperty("download_url", subtitlePath);
                                String summary = String.format(Locale.US,
                                        getString(R.string.setting_latest_download_subtitle),
                                        latestCommit.substring(0, 6));
                                subtitleUpdate.setSummary(summary);
                                subtitleUpdate.setEnabled(true);
                            } else {
                                subtitleUpdate.setSummary(getString(R.string.setting_latest_version));
                            }
                        } else {
                            subtitleUpdate.setSummary("no data");
                        }
                    }
                }
                @Override
                public void onFailure(Call<JsonArray> call, Throwable t) {
                    if (getActivity() == null) return;
                    findPreference(PREF_SUBTITLE_UPDATE).setSummary("failed loading subtitle data");
                }
            });
        }

        private void onLocaleItemDownload(JsonObject item) {
            String commit = item.get("latest_commit").getAsString();
            String path = item.get("download_url").getAsString();
            Call<JsonObject> call = subtitleRepo.download(commit, path);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    saveQuotesFile(item, response);
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    KcUtils.showToast(getContext(), t.getLocalizedMessage());
                }
            });
        }

        private void saveQuotesFile(JsonObject item, Response<JsonObject> response) {
            String message = "";
            String locale_code = item.get("locale_code").getAsString();
            String commit = item.get("latest_commit").getAsString();
            String filename = String.format(Locale.US, "quotes_%s.json", locale_code);

            JsonObject data = response.body();
            String subtitle_folder = getContext().getFilesDir().getAbsolutePath().concat("/subtitle/");
            String subtitle_path = subtitle_folder.concat(filename);
            File file = new File(subtitle_folder);
            try {
                if (!file.exists()) file.mkdirs();
                if (data != null) {
                    File subtitle_file = new File(subtitle_path);
                    FileOutputStream fos = new FileOutputStream(subtitle_file);
                    fos.write(data.toString().getBytes());
                    fos.close();
                    versionTable.putValue(filename, commit);
                    Preference subtitleUpdate = findPreference(PREF_SUBTITLE_UPDATE);
                    subtitleUpdate.setSummary(getString(R.string.setting_latest_version));
                    subtitleUpdate.setEnabled(false);
                } else {
                    message = "No data to write: quotes_".concat(locale_code).concat(".json");
                    KcUtils.showToast(getContext(), message);
                }
            } catch (IOException e) {
                KcUtils.reportException(e);
                message = "IOException while saving quotes_".concat(locale_code).concat(".json");
                KcUtils.showToast(getContext(), message);
            }
        }
    }
}