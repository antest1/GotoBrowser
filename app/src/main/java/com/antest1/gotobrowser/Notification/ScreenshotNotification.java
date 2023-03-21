package com.antest1.gotobrowser.Notification;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.antest1.gotobrowser.R;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.noti_screenshot_name);
            NotificationChannel mChannel = new NotificationChannel(NOTI_CHANNEL_SCREENSHOT, name, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(mChannel);
        }
    }

    public void showNotification(Bitmap bitmap, Uri uri) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/png");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(activity, NOTI_CHANNEL_SCREENSHOT)
                .setSmallIcon(R.drawable.ic_noti_screenshot)
                .setContentTitle(activity.getString(R.string.noti_screenshot_title))
                .setContentText(activity.getString(R.string.noti_screenshot_description))
                .setContentIntent(contentIntent)
                .setLargeIcon(bitmap)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null))
                .build();
        notificationManager.notify(SCREENSHOT, notification);
    }
}
