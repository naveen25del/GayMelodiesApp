package com.gaymelodies.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public class MusicService extends Service {

    private static final String CHANNEL_ID = "gm_playback";
    private static final int NOTIF_ID = 1;
    public static final String ACTION_STOP = "com.gaymelodies.app.STOP";
    public static final String ACTION_PAUSE_AUDIO = "com.gaymelodies.app.PAUSE_AUDIO";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Music playback notification");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            // Tell the activity to pause the actual audio, then stop ourselves
            Intent pause = new Intent(ACTION_PAUSE_AUDIO);
            pause.setPackage(getPackageName());
            sendBroadcast(pause);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        showNotification();
        return START_STICKY;
    }

    private void showNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        PendingIntent openPi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent stopPi = PendingIntent.getService(this, 0,
                new Intent(this, MusicService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.setSmallIcon(R.drawable.ic_stat_note)
                .setContentTitle("Gay Melodies")
                .setContentText("Music playing")
                .setContentIntent(openPi)
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(0, "Stop", stopPi);

        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
