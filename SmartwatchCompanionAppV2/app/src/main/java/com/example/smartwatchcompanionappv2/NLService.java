package com.example.smartwatchcompanionappv2;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
// import android.os.Build; // REMOVED - Unused import
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.util.Objects;

public class NLService extends NotificationListenerService {

    final static int maxBigTextLength = 240;

    public final static String NOTIFICATION_ACTION = "com.companionApp.NOTIFICATION_LISTENER_EXAMPLE";
    public final static String GET_NOTIFICATION_INTENT = "com.companionApp.NOTIFICATION_LISTENER_SERVICE_EXAMPLE";

    // Made TAG static and initialized with class name
    private static final String TAG = NLService.class.getSimpleName();
    private NLServiceReceiver nlservicereciver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "Notification Listener Service created");
        nlservicereciver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(GET_NOTIFICATION_INTENT);
        registerReceiver(nlservicereciver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG, "**********  onNotificationPosted");
        Intent i = new Intent(NOTIFICATION_ACTION);
        i.putExtra("notification_status_event", "onNotificationPosted :\" + sbn.getPackageName() + \"n");
        i.putExtra("event_type", "posted");
        sendBroadcast(i);
        MainActivity.updateNotifications();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG, "********** onNotificationRemoved");
        Intent i = new Intent(NOTIFICATION_ACTION);
        i.putExtra("notification_status_event", "onNotificationRemoved :\" + sbn.getPackageName() + \"n");
        i.putExtra("event_type", "removed");
        sendBroadcast(i);
        MainActivity.updateNotifications();
    }

    class NLServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("inform", "NLServiceReciever has received a broadcast");
            if (Objects.equals(intent.getStringExtra("command"), "clearall")) {
                NLService.this.cancelAllNotifications();
            } else if (Objects.equals(intent.getStringExtra("command"), "list")) {
                Log.i("inform", "Processing Request to list notifications");
                Intent i1 = new Intent(NOTIFICATION_ACTION);
                i1.putExtra("notification_event", "");
                sendBroadcast(i1);
                for (StatusBarNotification sbn : NLService.this.getActiveNotifications()) {
                    Intent i2 = new Intent(NOTIFICATION_ACTION);
                    try {
                        String data = ifNotNull(getAppNameFromPkgName(context, sbn.getPackageName())) + ","
                                + ifNotNull(sbn.getNotification().extras.getString(Notification.EXTRA_TITLE)).replace("\n", "").replace(";", ",") + ";" 
                                + ifNotNull(sbn.getNotification().extras.getString(Notification.EXTRA_TEXT)).replace("\n", "").replace(";", ",") + ";" 
                                + ifNotNull(sbn.getNotification().extras.getString(Notification.EXTRA_INFO_TEXT)).replace("\n", "").replace(";", ",") + ";" 
                                + ifNotNull(sbn.getNotification().extras.getString(Notification.EXTRA_SUB_TEXT)).replace("\n", "").replace(";", ",") + ";" 
                                + ifNotNull(sbn.getNotification().extras.getString(Notification.EXTRA_TITLE_BIG)).replace("\n", "").replace(";", ",") + ";";
                        data = data.replaceAll("[^\\p{ASCII}]", "");
                        try {
                            String category = sbn.getNotification().category;
                            if (Notification.CATEGORY_EMAIL.equals(category)) {
                                CharSequence bigTextChars = sbn.getNotification().extras.getCharSequence("android.bigText");
                                if (bigTextChars != null) {
                                    data += shortenString(bigTextChars).replace("\n", "").replace(";", ",");
                                }
                            } else if (Notification.CATEGORY_MESSAGE.equals(category)) {
                                data += ifNotNull(sbn.getNotification().extras.getString(Notification.EXTRA_MESSAGES));
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error processing bigText/messages for " + getAppNameFromPkgName(context, sbn.getPackageName()), e);
                        }
                        if (!data.contains("ESP32 Smartwatch Companion App")) {
                            i2.putExtra("notification_event", data + "\n");
                            sendBroadcast(i2);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Could not parse data for: " + getAppNameFromPkgName(context, sbn.getPackageName()) + " due to " + e.getMessage(), e);
                    }
                }
                Intent i3 = new Intent(NOTIFICATION_ACTION);
                i3.putExtra("notification_event", "");
                sendBroadcast(i3);
            }
        }
    }

    public static String shortenString(CharSequence s) {
        if (s == null) return "";
        if (s.length() > maxBigTextLength) {
            return s.toString().substring(0, maxBigTextLength) + "...";
        } else {
            return s.toString();
        }
    }

    public static String ifNotNull(String str) {
        return Objects.toString(str, "");
    }

    public static String getAppNameFromPkgName(Context context, String Packagename) {
        if (Packagename == null || context == null) return "";
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(Packagename, PackageManager.GET_META_DATA);
            return (String) packageManager.getApplicationLabel(info);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get app name for package: " + Packagename, e); // Now correctly references static TAG
            return "";
        }
    }
}
