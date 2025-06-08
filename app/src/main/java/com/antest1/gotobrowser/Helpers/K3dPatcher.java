package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.TextView;

import com.antest1.gotobrowser.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;
import static com.antest1.gotobrowser.Constants.PREF_LEGACY_RENDERER;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAI3D;
import static com.antest1.gotobrowser.Helpers.KcUtils.getRetrofitAdapter;
import static com.antest1.gotobrowser.Helpers.KcUtils.getStringFromException;

import retrofit2.Call;

public class K3dPatcher implements SensorEventListener {
    private Activity activity;
    private SensorManager mSensorManager;
    private Sensor mGyroscope;

    private float gyroX = 0f;
    private float gyroY = 0f;

    private static boolean isPatcherEnabled = false;
    private boolean isEffectEnabled = true; // for user to temporarily disable the effect in-game

    private long oldTime = 0;

    public boolean isPatcherEnabled() {
        return isPatcherEnabled;
    }

    public boolean isEffectEnabled() {
        return isEffectEnabled;
    }

    public void setEffectEnabled(boolean effectEnabled) {
        isEffectEnabled = effectEnabled;
    }

    @JavascriptInterface
    public float getX(){
        if (!isEffectEnabled) {
            return 0;
        }
        decayTiltAngle();
        float sign = Math.signum(gyroX);
        double num = Math.abs(gyroX) * 0.000002;
        double gotX = Math.sqrt(1.0 + num) - 1.0;
        return (float)gotX * sign ;
    }

    @JavascriptInterface
    public float getY(){
        if (!isEffectEnabled) {
            return 0;
        }
        float sign = Math.signum(gyroY);
        double num = Math.abs(gyroY) * 0.000002;
        double gotY = Math.sqrt(1.0 + num) - 1.0;
        return (float)gotY * sign ;
    }

    private String imageUrl = null;
    private boolean depthMapLoaded = false;

    @JavascriptInterface
    public void notifyError(String newImageUrl){
        imageUrl = newImageUrl;
        depthMapLoaded = false;
    }

    @JavascriptInterface
    public void notifyLoaded(String newImageUrl){
        imageUrl = newImageUrl;
        depthMapLoaded = true;
    }

    private void decayTiltAngle() {
        // Slowly rebound the tile angle until it becomes centre
        long newTime = System.currentTimeMillis();
        if (oldTime != 0) {
            // The angle becomes 95% after every 10ms
            double decay = Math.pow(0.994359f, (newTime - oldTime));
            gyroX *= decay;
            gyroY *= decay;
        }
        oldTime = newTime;
    }

    public void prepare(Activity activity) {
        // Only update the enable status when opening the browser view
        // Require reopening the browser after switching the MOD on or off
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);

        // Kantai3D is disabled if using a legacy renderer
        isPatcherEnabled = sharedPref.getBoolean(PREF_MOD_KANTAI3D, false) &&
                !sharedPref.getBoolean(PREF_LEGACY_RENDERER, false);

        if (isPatcherEnabled) {
            this.activity = activity;
            mSensorManager = (SensorManager)activity.getSystemService(SENSOR_SERVICE);
            if (mSensorManager != null) {
                mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);
            }
        }
    }

    public void pause() {
        if (isPatcherEnabled && mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    public void resume() {
        if (isPatcherEnabled && mSensorManager != null) {
            mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    long lastEventTimestamp = 0L;

    public void onSensorChanged(SensorEvent sensorEvent) {
        if (lastEventTimestamp != 0 && sensorEvent.timestamp != lastEventTimestamp) {
            int rotation = 0;
            if (activity != null) {
                rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            }
            switch (rotation) {
                default:
                case ROTATION_0:
                    gyroX -= sensorEvent.values[1] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    gyroY += sensorEvent.values[0] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    break;
                case ROTATION_90:
                    gyroX -= sensorEvent.values[0] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    gyroY -= sensorEvent.values[1] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    break;
                case ROTATION_180:
                    gyroX += sensorEvent.values[1] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    gyroY -= sensorEvent.values[0] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    break;
                case ROTATION_270:
                    gyroX += sensorEvent.values[0] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    gyroY += sensorEvent.values[1] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
            }
        }

        lastEventTimestamp = sensorEvent.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void showDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.k3d_form, null);

        if (imageUrl != null) {
            final TextView textView = dialogView.findViewById(R.id.kantai3d_msg_text);
            textView.setText(String.format(Locale.US, activity.getString(depthMapLoaded ? R.string.msg_kantai3d_loaded : R.string.msg_kantai3d_error), imageUrl));
        }

        MaterialSwitch switchCompat = dialogView.findViewById(R.id.switch_3d);
        switchCompat.setChecked(isEffectEnabled());

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.text_save, (dialog, which) -> {
            // Make the change effective
            setEffectEnabled(switchCompat.isChecked());
            dialog.dismiss();
        });

        builder.setNegativeButton(R.string.text_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public static String patchKantai3d(Context context, String main_js) {
        if (!isPatcherEnabled) {
            return main_js;
        }

        Map<String, String> stringsToReplace = new LinkedHashMap<>();

        K3dMetadataApi downloader = getRetrofitAdapter(context, "https://cdn.jsdelivr.net/").create(K3dMetadataApi.class);
        Call<JsonObject> response = downloader.getMetadata();
        JsonObject metadata;
        try {
            metadata = response.execute().body();
        } catch (IOException e) {
            Log.e("GOTO", getStringFromException(e));
            return main_js;
        }
        JsonArray patches = metadata.getAsJsonArray("goto_patches");

        for (JsonElement patch : patches) {
            stringsToReplace.put(patch.getAsJsonObject().get("pattern").getAsString(), patch.getAsJsonObject().get("replacement").getAsString());
        }

        String replaced = main_js;
        StringBuffer sb = new StringBuffer();
        Matcher matcher = null;
        for (Map.Entry<String, String> stringToReplace : stringsToReplace.entrySet()) {
            Pattern pattern = Pattern.compile(stringToReplace.getKey());
            // Match the next pattern using the same matcher
            if (matcher == null) {
                matcher = pattern.matcher(replaced);
            } else {
                matcher.usePattern(pattern);
            }
            if (matcher.find()) {
                // Find one and only one match
                matcher.appendReplacement(sb, stringToReplace.getValue());
            } else {
                // The main.js is probably updated and no longer support the 3D patch currently
                // Immediately return the unpatched main.js
                return main_js;
            }
        }
        if (matcher != null) {
            matcher.appendTail(sb);
            return sb.toString();
        } else {
            return main_js;
        }
    }
}
