package com.antest1.gotobrowser.Browser;

import android.view.ScaleGestureDetector;
import android.view.View;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.R;

public class BrowserScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    private final View browserPanel;

    public BrowserScaleGestureListener(BrowserActivity activity, View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        browserPanel = activity.findViewById(R.id.browser_panel);
    }

    View.OnClickListener onClickListener;

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (detector.getScaleFactor() < 0.5) {
            // Zoom In
            onClickListener.onClick(browserPanel);
            return true;
        }
        return false;
    }
}
