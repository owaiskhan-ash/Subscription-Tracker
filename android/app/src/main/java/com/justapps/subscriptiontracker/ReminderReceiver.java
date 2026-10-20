package com.justapps.subscriptiontracker;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Fires one gentle notification per renewal reminder. */
public class ReminderReceiver extends BroadcastReceiver {

    static final String CHANNEL_ID = "renewals";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_BODY = "body";
    static final String EXTRA_REQUEST_CODE = "requestCode";

    static void ensureChannel(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Renewal reminders", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("One gentle reminder before a subscription renews.");
            manager.createNotificationChannel(channel);
        }
    }

    static PendingIntent pendingIntent(Context context, int requestCode, String title, String body) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_BODY, body);
        intent.putExtra(EXTRA_REQUEST_CODE, requestCode);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** True when Android lets this app fire alarms at an exact time. */
    static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    static void schedule(Context context, int requestCode, String title, String body, long whenMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent intent = pendingIntent(context, requestCode, title, body);
        if (whenMillis <= System.currentTimeMillis()) return; // never schedule in the past
        try {
            if (canScheduleExact(context)) {
                // Exact, so the reminder lands at 9 AM as promised, doze or not.
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, intent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, intent);
            }
        } catch (SecurityException e) {
            // Exact access was revoked between the check and the call: fall back quietly.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, intent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ensureChannel(context);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String body = intent.getStringExtra(EXTRA_BODY);
        int requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 1);
        if (title == null || title.isEmpty()) title = "Subscription renewing soon";

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestCode, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_reminder)
                .setContentTitle(title)
                .setContentText(body == null ? "" : body)
                .setStyle(new Notification.BigTextStyle().bigText(body == null ? "" : body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(requestCode, notification);

        // The reminder served its purpose, so forget it and it never repeats.
        context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                .edit().remove(String.valueOf(requestCode)).apply();
    }
}
