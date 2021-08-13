package com.antest1.gotobrowser.Subtitle;

import android.content.Context;

import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.google.gson.JsonObject;

import java.text.ParseException;

public interface SubtitleProvider {
    void loadKcApiData(JsonObject api_data);

    boolean loadQuoteData(Context context, String localeCode);

    SubtitleData getSubtitleData(String url, String path, String voiceSize) throws ParseException;

    void checkUpdateFromPreference(SettingsActivity.SettingsFragment fragment, String localeCode, Preference subtitleUpdate, VersionDatabase versionTable);

    void downloadUpdateFromPreference(SettingsActivity.SettingsFragment fragment, VersionDatabase versionTable);
}
