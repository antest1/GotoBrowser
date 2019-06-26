package com.antest1.gotobrowser.Browser;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class BrowserGestureListener extends GestureDetector.SimpleOnGestureListener {
    private View broswerPanel;
    public BrowserGestureListener(View panel) {
        broswerPanel = panel;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        Log.e("TAG", "onFling: ");
        Log.e("TAG", event1.toString());
        Log.e("TAG", event2.toString());
        Log.e("TAG", velocityX + " " + velocityY);
        if (event1.getX() < 200 && velocityX > 2000) {
            broswerPanel.setVisibility(View.VISIBLE);
        } else if (event1.getX() < 1500 && velocityX < -2000) {
            broswerPanel.setVisibility(View.GONE);
        }
        return super.onFling(event1, event2, velocityX, velocityY);
    }

}
