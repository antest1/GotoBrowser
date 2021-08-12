package com.antest1.gotobrowser.Subtitle;

import android.content.Context;

import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.google.gson.JsonArray;

import java.text.ParseException;

public interface SubtitleProvider {
    int getFilenameByVoiceLine(int shipId, int lineNum);

    int getVoiceDiffByFilename(String shipId, String filename);

    String getVoiceLineByFilename(String shipId, String filename);



    void buildShipGraph(JsonArray data);

    void buildMapBgmGraph(JsonArray data);



    void loadQuoteAnnotation(Context context);

    boolean loadQuoteData(Context context, String localeCode);

    int getDefaultTiming(String data);

    SubtitleData getSubtitleData(String shipId, String voiceLine, String voiceSize) throws ParseException;

    void checkUpdateFromPreference(SettingsActivity.SettingsFragment fragment, String subtitleLocale, Preference subtitleUpdate, VersionDatabase versionTable);
    void downloadUpdateFromPreference(SettingsActivity.SettingsFragment fragment, VersionDatabase versionTable);
}
