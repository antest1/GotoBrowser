package com.antest1.gotobrowser;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.KeyEvent;

import androidx.appcompat.app.AlertDialog;

public class BackPressCloseHandler {
    private static final int INTERVAL = 1500;
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
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                activity);
        alertDialogBuilder.setTitle(activity.getString(R.string.app_name));
        alertDialogBuilder
                .setOnKeyListener((dialog, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        activity.finish();
                        dialog.dismiss();
                        return true;
                    }
                    return false;
                });
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(activity.getString(R.string.backpress_msg))
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            activity.finish();
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> dialog.cancel());
        if (activity != null && !activity.isFinishing()) {
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }
}
