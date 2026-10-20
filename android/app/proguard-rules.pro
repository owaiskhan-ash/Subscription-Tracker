# Subscription Tracker — release builds ship unminified in v1 for maximum
# reliability of the WebView bridge. Keep the bridge explicit if minify is
# ever enabled:
-keepclassmembers class com.justapps.subscriptiontracker.MainActivity$NativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}
