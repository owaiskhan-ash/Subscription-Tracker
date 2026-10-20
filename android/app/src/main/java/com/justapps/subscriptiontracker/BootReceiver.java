package com.justapps.subscriptiontracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Map;

/** After a reboot, quietly re-arm every reminder that is still in the future. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!(entry.getValue() instanceof String)) continue;
            String[] parts = ((String) entry.getValue()).split(MainActivity.SEP, -1);
            if (parts.length < 3) continue;
            try {
                int requestCode = Integer.parseInt(entry.getKey());
                long when = Long.parseLong(parts[2]);
                if (when > now) {
                    ReminderReceiver.schedule(context, requestCode, parts[0], parts[1], when);
                } else {
                    editor.remove(entry.getKey());
                }
            } catch (NumberFormatException ignored) {
                // Corrupt entry — skip it and keep the rest working.
            }
        }
        editor.apply();
    }
}
