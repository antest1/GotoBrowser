package com.antest1.gotobrowser.Browser;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.antest1.gotobrowser.R;

public class CustomDrawerLayout extends DrawerLayout
{
    public CustomDrawerLayout(@NonNull Context context) {
        super(context);
        setScrimColor(Color.TRANSPARENT);
        setDrawerElevation(0);
    }

    public CustomDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setScrimColor(Color.TRANSPARENT);
        setDrawerElevation(0);
    }

    public CustomDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setScrimColor(Color.TRANSPARENT);
        setDrawerElevation(0);
    }

    boolean isClosing = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!this.isDrawerOpen(GravityCompat.START)) {
            isClosing = false;
            return super.dispatchTouchEvent(event);
        }

        //  Only pass the event to main panel if not handled by side panel
        // To avoid touching both at the same time
        boolean handledBySidePanel = getChildAt(1).dispatchTouchEvent(event);
        if (!handledBySidePanel) {
            getChildAt(0).dispatchTouchEvent(event);
        }

        boolean clickedOutside = false;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            View content = findViewById(R.id.navigation);
            int[] contentLocation = new int[2];
            content.getLocationInWindow(contentLocation);
            clickedOutside = contentLocation[0] + content.getWidth() < (int) event.getX();
        }

        if (clickedOutside) {
            if (!isClosing) {
                this.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
            }
            return true;
        }
        this.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void closeDrawer(int gravity) {
        isClosing = true;
        super.closeDrawer(gravity);
    }
}
