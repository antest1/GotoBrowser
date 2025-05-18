package com.antest1.gotobrowser.Helpers;

import android.app.Activity;

import androidx.activity.OnBackPressedCallback;

import com.antest1.gotobrowser.R;

public class BackPressCloseHandler extends OnBackPressedCallback {
    private static final int INTERVAL = 2000;
    long pressedTime = 0;

    private final Activity activity;

    public BackPressCloseHandler(Activity context, boolean enabled) {
        super(enabled);
        this.activity = context;
    }

    public void showMessage() {
        if (activity != null && !activity.isFinishing()) {
            KcUtils.showToastShort(activity.getApplicationContext(), R.string.backpress_msg);
        }
    }

    @Override
    public void handleOnBackPressed() {
        if (System.currentTimeMillis() > pressedTime + INTERVAL) {
            pressedTime = System.currentTimeMillis();
            showMessage();
            return;
        }
        if (System.currentTimeMillis() <= pressedTime + INTERVAL) {
            activity.finish();
        }
    }
}
