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
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            activity.finish();
                            dialog.dismiss();
                            return true;
                        }
                        return false;
                    }
                });
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(activity.getString(R.string.backpress_msg))
                .setPositiveButton("Close",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                activity.finish();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
