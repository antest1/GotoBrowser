// Based on https://stackoverflow.com/questions/41025200/android-view-inflateexception-error-inflating-class-android-webkit-webview/41721789#41721789

package com.antest1.gotobrowser.Browser;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.webkit.WebView;

public class WebViewL extends WebView {

    public WebViewL(Context context) {
        super(getFixedContext(context));
    }

    public WebViewL(Context context, AttributeSet attrs) {
        super(getFixedContext(context), attrs);
    }

    public WebViewL(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getFixedContext(context), attrs, defStyleAttr);
    }

    public static Context getFixedContext(Context context) {
        return context.createConfigurationContext(new Configuration());
    }
}
