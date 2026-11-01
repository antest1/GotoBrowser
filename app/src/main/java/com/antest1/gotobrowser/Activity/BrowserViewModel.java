package com.antest1.gotobrowser.Activity;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.antest1.gotobrowser.Helpers.CritPatcher;
import com.antest1.gotobrowser.Helpers.FpsPatcher;
import com.antest1.gotobrowser.Helpers.K3dPatcher;
import com.antest1.gotobrowser.Helpers.KenPatcher;
import com.antest1.gotobrowser.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static com.antest1.gotobrowser.Constants.PREF_MUTEMODE;
import static com.antest1.gotobrowser.Constants.PREF_CAPTURE;
import static com.antest1.gotobrowser.Constants.PREF_SHOWCC;
import static com.antest1.gotobrowser.Constants.PREF_LOCKMODE;
import static com.antest1.gotobrowser.Constants.PREF_KEEPMODE;
import static com.antest1.gotobrowser.Constants.PREF_DISABLE_REFRESH_DIALOG;

public class BrowserViewModel extends AndroidViewModel {
    private final SharedPreferences sharedPref;
    private final K3dPatcher k3dPatcher = new K3dPatcher();
    private final KenPatcher kenPatcher = new KenPatcher();
    private final CritPatcher critPatcher = new CritPatcher();
    private final FpsPatcher fpsPatcher = new FpsPatcher();

    private final MutableLiveData<Boolean> isMuteMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isCaptureMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLockMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isKeepMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isCaptionMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isNoRefreshPopupMode = new MutableLiveData<>();
    
    private final MutableLiveData<Boolean> isKcBrowserMode = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isStartedFlag = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isAdjustChangedByUser = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSubtitleLoaded = new MutableLiveData<>(false);

    private List<String> connectorInfo = new ArrayList<>();

    public BrowserViewModel(@NonNull Application application) {
        super(application);
        sharedPref = application.getSharedPreferences(application.getString(R.string.preference_key), Context.MODE_PRIVATE);
        
        isMuteMode.setValue(sharedPref.getBoolean(PREF_MUTEMODE, false));
        isCaptureMode.setValue(sharedPref.getBoolean(PREF_CAPTURE, false));
        isCaptionMode.setValue(sharedPref.getBoolean(PREF_SHOWCC, false));
        isLockMode.setValue(sharedPref.getBoolean(PREF_LOCKMODE, false));
        isKeepMode.setValue(sharedPref.getBoolean(PREF_KEEPMODE, false));
        isNoRefreshPopupMode.setValue(sharedPref.getBoolean(PREF_DISABLE_REFRESH_DIALOG, false));
        
        k3dPatcher.prepare(application);
        kenPatcher.prepare(application);
        critPatcher.prepare(application);
        fpsPatcher.prepare(application);
    }

    public LiveData<Boolean> getIsMuteMode() { return isMuteMode; }
    public LiveData<Boolean> getIsCaptureMode() { return isCaptureMode; }
    public LiveData<Boolean> getIsLockMode() { return isLockMode; }
    public LiveData<Boolean> getIsKeepMode() { return isKeepMode; }
    public LiveData<Boolean> getIsCaptionMode() { return isCaptionMode; }
    public LiveData<Boolean> getIsNoRefreshPopupMode() { return isNoRefreshPopupMode; }

    public void toggleMuteMode() {
        boolean newValue = !Boolean.TRUE.equals(isMuteMode.getValue());
        isMuteMode.setValue(newValue);
        sharedPref.edit().putBoolean(PREF_MUTEMODE, newValue).apply();
    }

    public void toggleCaptureMode() {
        boolean newValue = !Boolean.TRUE.equals(isCaptureMode.getValue());
        isCaptureMode.setValue(newValue);
        sharedPref.edit().putBoolean(PREF_CAPTURE, newValue).apply();
    }

    public void toggleCaptionMode() {
        boolean newValue = !Boolean.TRUE.equals(isCaptionMode.getValue());
        isCaptionMode.setValue(newValue);
        sharedPref.edit().putBoolean(PREF_SHOWCC, newValue).apply();
    }
    
    public void toggleLockMode() {
        boolean newValue = !Boolean.TRUE.equals(isLockMode.getValue());
        isLockMode.setValue(newValue);
        sharedPref.edit().putBoolean(PREF_LOCKMODE, newValue).apply();
    }

    public void toggleKeepMode() {
        boolean newValue = !Boolean.TRUE.equals(isKeepMode.getValue());
        isKeepMode.setValue(newValue);
        sharedPref.edit().putBoolean(PREF_KEEPMODE, newValue).apply();
    }

    public void setNoRefreshPopupMode(boolean value) {
        isNoRefreshPopupMode.setValue(value);
        sharedPref.edit().putBoolean(PREF_DISABLE_REFRESH_DIALOG, value).apply();
    }

    public K3dPatcher getK3dPatcher() { return k3dPatcher; }
    public KenPatcher getKenPatcher() { return kenPatcher; }
    public CritPatcher getCritPatcher() { return critPatcher; }
    public FpsPatcher getFpsPatcher() { return fpsPatcher; }

    public SharedPreferences getSharedPref() { return sharedPref; }

    public List<String> getConnectorInfo() { return connectorInfo; }
    public void setConnectorInfo(List<String> info) { this.connectorInfo = info; }

    public boolean isKcBrowserMode() { return Boolean.TRUE.equals(isKcBrowserMode.getValue()); }
    public void setKcBrowserMode(boolean value) { isKcBrowserMode.setValue(value); }

    public boolean isStartedFlag() { return Boolean.TRUE.equals(isStartedFlag.getValue()); }
    public void setStartedFlag(boolean value) { isStartedFlag.setValue(value); }

    public boolean isSubtitleLoaded() { return Boolean.TRUE.equals(isSubtitleLoaded.getValue()); }
    public void setSubtitleLoaded(boolean value) { isSubtitleLoaded.setValue(value); }
}
