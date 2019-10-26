// Based on https://stackoverflow.com/questions/41025200/android-view-inflateexception-error-inflating-class-android-webkit-webview/41721789#41721789

package com.antest1.gotobrowser.Browser;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

public class WebViewL extends WebView {
    private boolean alwaysVisible = false;

    public WebViewL(Context context) {
        super(getFixedContext(context));
    }

    public WebViewL(Context context, AttributeSet attrs) {
        super(getFixedContext(context), attrs);
    }

    public WebViewL(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getFixedContext(context), attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebViewL(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(getFixedContext(context), attrs, defStyleAttr, defStyleRes);
    }

    public WebViewL(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(getFixedContext(context), attrs, defStyleAttr, privateBrowsing);
    }

    public static Context getFixedContext(Context context) {
        return context.createConfigurationContext(new Configuration());
    }

    /**
     * Set whether the WebView should ignore invisible state.
     * WebView will draw new frames even in background if it is in visible state.
     *
     * @param alwaysVisible <code>true</code> to ignore invisible state; <code>false</code> is the default behavior
     */
    public void setAlwaysVisible(boolean alwaysVisible) {
        this.alwaysVisible = alwaysVisible;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE && alwaysVisible) {
            // Webview only draws new frames when visible
            // We want our frame-based animations to work even when user switches to another app
            // So we prevent webview from entering invisible state
            super.onWindowVisibilityChanged(View.VISIBLE);
        } else {
            super.onWindowVisibilityChanged(visibility);
        }
    }
}
