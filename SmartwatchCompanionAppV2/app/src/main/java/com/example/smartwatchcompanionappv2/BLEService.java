package com.example.smartwatchcompanionappv2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.smartwatchcompanionappv2.MainActivity; // Ensure this import is present

public class BLEService extends Service {

    private static String TAG = "BLEService";
    private BLEGATT blegatt;
    private static BLEService reference;
    public static final String CHANNEL_ID = "com.companionApp.UPDATE_SERVICE";

    public static Boolean isRunning = false;

    public void onCreate() {
        super.onCreate();
        reference = this;
        Log.i(TAG, "onCreate: Called");

        createNotificationChannel();

        // Intent notificationIntent = new Intent(this.getApplicationContext(), BLEService.class);
        // Using MainActivity for the notification tap action for now
        Intent notificationIntent = new Intent(this.getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this.getApplicationContext(), 300, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this.getApplicationContext(), CHANNEL_ID)
                .setContentTitle("ESP32 Smartwatch")
                .setContentText("BLE Gatt Server Is Running...")
                .setSmallIcon(R.mipmap.ic_launcher) // Make sure this resource exists
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Started BLE Handler Service with ID:" + startId);
        isRunning = true;

        MainActivity.updateStatusText(); // Call to static method in MainActivity

        blegatt = new BLEGATT(this.getApplicationContext());

        // Ensure MainActivity.currentDevice is not null before trying to get its address
        if (MainActivity.currentDevice != null) {
            // THIS IS THE CORRECTED LINE:
            blegatt.connect(MainActivity.currentDevice.getAddress());
        } else {
            Log.e(TAG, "MainActivity.currentDevice is null, cannot connect.");
            // Handle the case where currentDevice is null, perhaps stop the service or attempt to scan
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "BLEService is now ending");
        if (blegatt != null) {
            blegatt.disconnect();
            blegatt.close();
        }
        isRunning = false;
        MainActivity.updateStatusText(); // Update UI if needed
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    public static BLEService getReference() {
        return reference;
    }
}
