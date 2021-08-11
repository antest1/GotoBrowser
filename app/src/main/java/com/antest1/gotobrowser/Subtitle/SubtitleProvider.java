package com.antest1.gotobrowser.Subtitle;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;

public interface SubtitleProvider {
    int getFilenameByVoiceLine(int shipId, int lineNum);

    int getVoiceDiffByFilename(String shipId, String filename);

    String getVoiceLineByFilename(String shipId, String filename);

    void buildShipGraph(JsonArray data);

    void loadQuoteAnnotation(Context context);

    boolean loadQuoteData(Context context, String localeCode);

    int getDefaultTiming(String data);

    String findQuoteKeyByFileSize(String shipId, String voiceLine, String voiceSize);

    SubtitleData getSubtitleData(String id, String code, String size);

    void buildMapBgmGraph(JsonArray data);

    void checkUpdateFromPreference(SettingsActivity.SettingsFragment fragment, String subtitleLocale, Preference subtitleUpdate, VersionDatabase versionTable);
    void downloadUpdateFromPreference(SettingsActivity.SettingsFragment fragment, VersionDatabase versionTable);
}
