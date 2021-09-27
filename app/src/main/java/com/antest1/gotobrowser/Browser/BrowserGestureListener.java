package com.antest1.gotobrowser.Browser;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.R;

public class BrowserGestureListener extends GestureDetector.SimpleOnGestureListener {
    private final BrowserActivity activity;
    private final View browserPanel;

    public BrowserGestureListener(BrowserActivity activity, View.OnClickListener onClickListener) {
        this.activity = activity;
        this.onClickListener = onClickListener;
        browserPanel = activity.findViewById(R.id.browser_panel);
    }

    View.OnClickListener onClickListener;

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        onClickListener.onClick(browserPanel);
        return super.onSingleTapUp(e);
    }
}
