package com.antest1.gotobrowser.Activity;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.antest1.gotobrowser.R;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class SettingsViewModel extends AndroidViewModel {
    private final SharedPreferences sharedPref;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        sharedPref = application.getSharedPreferences(application.getString(R.string.preference_key), Context.MODE_PRIVATE);
    }

    public SharedPreferences getSharedPref() {
        return sharedPref;
    }
}
