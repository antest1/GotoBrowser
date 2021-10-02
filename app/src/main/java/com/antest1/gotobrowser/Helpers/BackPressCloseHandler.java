package com.antest1.gotobrowser.Helpers;

import android.app.Activity;

import com.antest1.gotobrowser.R;

public class BackPressCloseHandler {
    private static final int INTERVAL = 2000;
    long pressedTime = 0;

    private Activity activity;

    public BackPressCloseHandler(Activity context) {
        this.activity = context;
    }

    public void onBackPressed() {
        if (System.currentTimeMillis() > pressedTime + INTERVAL) {
            pressedTime = System.currentTimeMillis();
            showMessage();
            return;
        }
        if (System.currentTimeMillis() <= pressedTime + INTERVAL) {
            activity.finish();
        }
    }

    public void showMessage() {
        if (activity != null && !activity.isFinishing()) {
            KcUtils.showToastShort(activity.getApplicationContext(), R.string.backpress_msg);
        }
    }
}
