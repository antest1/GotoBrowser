package com.antest1.gotobrowser.Notification;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.antest1.gotobrowser.R;

import static com.antest1.gotobrowser.Constants.PREF_NC_SCREENSHOT_SET;

public class ScreenshotNotification {
    public static final String NOTI_CHANNEL_SCREENSHOT = "NOTI_CHANNEL_SCREENSHOT";
    public static final int SCREENSHOT = 1;
    Activity activity;
    NotificationManagerCompat notificationManager;

    public ScreenshotNotification(Activity ac) {
        activity = ac;
        notificationManager = NotificationManagerCompat.from(activity);
        registerScreenshotNotificationChannel(activity);
    }

    private void registerScreenshotNotificationChannel(Context context) {
        String preference_key = context.getString(R.string.preference_key);
        SharedPreferences sharedPref = context.getSharedPreferences(preference_key, Context.MODE_PRIVATE);
        boolean isNcScreenshotSet = sharedPref.getBoolean(PREF_NC_SCREENSHOT_SET, false);
        if (!isNcScreenshotSet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            String name = context.getString(R.string.noti_screenshot_name);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(NOTI_CHANNEL_SCREENSHOT, name, importance);
            manager.createNotificationChannel(mChannel);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(PREF_NC_SCREENSHOT_SET, true).apply();
        }
    }

    public void showNotification(Bitmap bitmap, Uri uri) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/png");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(activity, NOTI_CHANNEL_SCREENSHOT)
                .setSmallIcon(R.drawable.ic_noti_screenshot)
                .setContentTitle("Screenshot Captured")
                .setContentText("Tap to view your screenshot")
                .setContentIntent(contentIntent)
                .setLargeIcon(bitmap)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null))
                .build();
        notificationManager.notify(SCREENSHOT, notification);
    }
}
