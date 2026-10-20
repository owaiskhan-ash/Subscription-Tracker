# Subscription Tracker

**A calm, subtle, clutter-free place for every recurring expense.**

Subscription Tracker helps you keep all your recurring expenses in one simple place. Add subscriptions and regular bills like Netflix, Amazon Prime, Spotify, Wi-Fi and broadband, mobile plans, cloud storage, apps, gyms and memberships, and enter their cost, billing frequency, and next renewal date. The app quietly shows how much you are really spending each month and over a year, helps you see exactly where your money is going, and reminds you gently before each payment is due, so you can keep, pause, cancel, or adjust anything you no longer need, while there is still time. Everything lives only on your phone: no account, no sign-in, no tracking.

<p>
  <a href="https://github.com/owaiskhan-ash/Subscription-Tracker/releases/tag/v1.1.0"><b>⬇ Download v1.1.0 APK</b></a>
</p>

## Design

Modelled on the supplied reference (Screen Time Reminder): deep black `#0B0B0B`, a single cream card `#F3F0E9`, thin oversized numbers, small uppercase labels, hairline dividers, and a quiet three-tab bar, **Today · All · Settings**, with a single calm `＋` to add. Nothing else asks for your attention.

## Features (v1.1.0)

- **Today**: this month's true total (weekly / quarterly / yearly costs all normalised), next renewal with a live "due in" count, an optional monthly-budget bar, and a six-month rhythm chart of what is set to renew.
- **All**: every subscription in date order, with search, Active / Due soon / Paused filters, and monthly + yearly totals.
- **Add in seconds**: name, amount, billing cycle, next renewal, category, reminder timing (on the day, or 1 / 3 / 7 days before at 9 AM), optional note; one-tap starters for Netflix, Prime, Spotify, Wi-Fi and more.
- **Reminders that reach you**: real Android notifications scheduled on-device for exactly 9 AM, re-armed after reboot and after app updates, silenced automatically when you pause or delete. Settings shows your real notification status and takes you to the right system setting when notifications are off. One reminder per renewal, never more.
- **Keep it current in one tap**: *Mark as paid* rolls a subscription to its next cycle (catching up automatically after a lapse); pause/resume, edit, and delete live on each detail sheet.
- **Settings**: currency (₹, $, €, £, د.إ), optional monthly budget, default reminder, notification controls, and erase-everything.
- **Private by construction**: offline, on-device `localStorage` only, no permissions beyond notifications and exact alarms, works in airplane mode forever.

## Get the app

Download **`subscription-tracker-v1.1.0.apk`** from [Release v1.1.0](https://github.com/owaiskhan-ash/Subscription-Tracker/releases/tag/v1.1.0) and install it on any phone running **Android 8.0 (API 26) or newer**. Allow notifications on first use so renewal reminders can reach you.

> The APK is signed with a debug key so it installs directly (sideload). To publish on Google Play, generate your own keystore and build with `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` set; the build picks it up automatically.

## Build it yourself

Requirements: JDK 17 and the Android SDK (platform 34).

```bash
cd android
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

The UI is a single self-contained page at `android/app/src/main/assets/www/index.html`. The exact same file runs in the APK (WebView) and in any browser for preview:

```bash
python3 -m http.server 5173 --directory android/app/src/main/assets/www
```

Releases are built by [`.github/workflows/release.yml`](.github/workflows/release.yml): run it from the Actions tab (or push a `v*` tag) and it builds the signed APK and publishes/updates the GitHub Release.

## Project layout

```
android/
  app/src/main/assets/www/index.html   ← the whole calm UI (HTML/CSS/JS, no dependencies)
  app/src/main/java/.../               ← WebView shell + AlarmManager reminders (Java, no dependencies)
  app/build.gradle                     ← versionCode 2 · versionName 1.1.0 · minSdk 26 · targetSdk 34
.github/workflows/release.yml          ← builds & publishes the v1.1.0 APK
docs/STORE_LISTING.md                  ← full-length store description & release notes
```

Version 1.1.0 · Made to be looked at for a moment, then put down.
