package com.justapps.subscriptiontracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * Subscription Tracker: a calm, offline shell.
 * All UI lives in assets/www/index.html; this class provides WebView hosting,
 * real on-device renewal reminders via AlarmManager, and the small native
 * helpers the page needs to talk about notification permission honestly.
 */
public class MainActivity extends Activity {

    static final String PREFS = "reminders";
    static final String SEP = "\u0001";
    private static final String KEY_NOTIF_ASKED = "notif_permission_asked";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1002;

    private WebView webView;
    private volatile boolean overlayOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ReminderReceiver.ensureChannel(this);

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        getWindow().setStatusBarColor(0xFF0B0B0B);
        getWindow().setNavigationBarColor(0xFF0B0B0B);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        settings.setTextZoom(100);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new NativeBridge(), "NativeBridge");
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST && webView != null) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            webView.evaluateJavascript(
                    "window.onNotificationPermissionResult && window.onNotificationPermissionResult(" + granted + ")",
                    null);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript(
                    "if(window.handleNativeBack){window.handleNativeBack();}else{window.NativeBridge&&NativeBridge.exitApp();}",
                    null);
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("NativeBridge");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    /** Opens this app's notification settings page, with a safe fallback. UI thread only. */
    private void openNotificationSettingsInternal() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (Exception e) {
            openAppSettingsInternal();
        }
    }

    /** Opens this app's general settings page. UI thread only. */
    private void openAppSettingsInternal() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ignored) {
            // Nowhere left to send the user; stay quiet.
        }
    }

    /** Bridge exposed to the page as window.NativeBridge. */
    public class NativeBridge {

        @JavascriptInterface
        public void scheduleReminder(int requestCode, String title, String body, double whenMillis) {
            long when = (long) whenMillis;
            SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            prefs.edit().putString(String.valueOf(requestCode),
                    (title == null ? "Subscription" : title) + SEP +
                    (body == null ? "" : body) + SEP + when).apply();
            ReminderReceiver.schedule(MainActivity.this, requestCode,
                    title == null ? "Subscription" : title, body == null ? "" : body, when);
        }

        @JavascriptInterface
        public void cancelReminder(int requestCode) {
            SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            prefs.edit().remove(String.valueOf(requestCode)).apply();
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(ReminderReceiver.pendingIntent(MainActivity.this, requestCode, "", ""));
            }
        }

        /**
         * Asks for the notification permission properly. When Android will no
         * longer show the dialog (denied before), it opens the system settings
         * page instead of silently doing nothing.
         */
        @JavascriptInterface
        public void requestNotificationPermission() {
            if (Build.VERSION.SDK_INT < 33) return; // no runtime permission before Android 13
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) return;
            runOnUiThread(() -> {
                SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                boolean askedBefore = prefs.getBoolean(KEY_NOTIF_ASKED, false);
                boolean dialogCanShow = !askedBefore
                        || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS);
                if (dialogCanShow) {
                    prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply();
                    requestPermissions(
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST);
                } else {
                    openNotificationSettingsInternal();
                }
            });
        }

        /** True only when a reminder can actually reach the user right now. */
        @JavascriptInterface
        public boolean notificationsEnabled() {
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && !manager.areNotificationsEnabled()) return false;
            if (Build.VERSION.SDK_INT >= 33) {
                return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
            }
            return true;
        }

        /** True when reminders may fire at exactly 9 AM instead of an inexact window. */
        @JavascriptInterface
        public boolean exactAlarmsAllowed() {
            return ReminderReceiver.canScheduleExact(MainActivity.this);
        }

        @JavascriptInterface
        public void openNotificationSettings() {
            runOnUiThread(() -> openNotificationSettingsInternal());
        }

        @JavascriptInterface
        public void openExactAlarmSettings() {
            if (Build.VERSION.SDK_INT < 31) return;
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    openAppSettingsInternal();
                }
            });
        }

        @JavascriptInterface
        public void setOverlayOpen(boolean open) {
            overlayOpen = open;
        }

        @JavascriptInterface
        public boolean isOverlayOpen() {
            return overlayOpen;
        }

        @JavascriptInterface
        public void exitApp() {
            runOnUiThread(() -> finish());
        }
    }
}
