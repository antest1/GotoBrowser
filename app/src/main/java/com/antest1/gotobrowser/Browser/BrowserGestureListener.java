package com.antest1.gotobrowser.Browser;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.R;

public class BrowserGestureListener extends GestureDetector.SimpleOnGestureListener {
    private BrowserActivity activity;
    private View browserPanel;
    public BrowserGestureListener(BrowserActivity activity) {
        this.activity = activity;
        browserPanel = activity.findViewById(R.id.browser_panel);
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        Log.e("GOTO", "onFling: ");
        Log.e("GOTO", event1.toString());
        Log.e("GOTO", event2.toString());
        Log.e("GOTO", velocityX + " " + velocityY);
        if (event1.getX() < 200 && velocityX > 2000) {
            browserPanel.setVisibility(View.VISIBLE);
            activity.setPanelVisibleValue(true);
        } else if (event1.getX() < 1500 && velocityX < -2000) {
            browserPanel.setVisibility(View.GONE);
            activity.setPanelVisibleValue(false);
        }
        return super.onFling(event1, event2, velocityX, velocityY);
    }

}
