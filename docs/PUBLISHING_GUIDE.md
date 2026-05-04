# Focus Dial — Testing, Building & Publishing Guide

## Prerequisites

| Tool | Status | Notes |
|------|--------|-------|
| Android SDK | ✅ Installed | `/Users/lokesh.rongali/Library/Android/sdk` |
| Java 17 | ✅ Installed | Zulu 17.0.16 |
| Gradle | ✅ Via wrapper | `./gradlew` |
| Wear OS Emulator | ❌ Need to create | No Wear AVD exists |
| Keystore | ❌ Need to generate | `keystore/` dir is empty |
| Firebase project | ❌ Placeholder config | `google-services.json` needs real project |
| Google Play Console | ❌ Need account | $25 one-time fee |

---

## Part 1: Testing in WearOS Emulator

### Step 1.1: Download Wear OS System Image

```bash
# Download Wear OS 4 (API 33) system image for Apple Silicon Mac
/Users/lokesh.rongali/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager \
  "system-images;android-33;android-wear;arm64-v8a"
```

If that image isn't available, use Android Studio:
- Open Android Studio → Tools → SDK Manager → SDK Platforms tab
- Check "Show Package Details"
- Under Android 13 (API 33) or Android 14 (API 34), find "Wear OS" image
- Install it

### Step 1.2: Create Wear OS AVD

```bash
# Create the AVD (round watch, 384x384 display)
/Users/lokesh.rongali/Library/Android/sdk/cmdline-tools/latest/bin/avdmanager create avd \
  --name "WearOS_Round_API33" \
  --device "wearos_large_round" \
  --package "system-images;android-33;android-wear;arm64-v8a"
```

**Alternative via Android Studio:**
- Tools → Device Manager → Create Device
- Choose "Wear OS" category → "Wear OS Large Round"
- Select API 33 or 34 system image
- Name it "WearOS_Round_API33"

### Step 1.3: Launch Emulator

```bash
# Launch from command line
/Users/lokesh.rongali/Library/Android/sdk/emulator/emulator -avd WearOS_Round_API33 &
```

Or launch from Android Studio Device Manager (click play button).

### Step 1.4: Install & Run the App

```bash
cd /Users/lokesh.rongali/projects/everest/wearos-themes

# Build debug APK
./gradlew :focus-dial:assembleDebug

# Install on emulator
/Users/lokesh.rongali/Library/Android/sdk/platform-tools/adb install \
  focus-dial/build/outputs/apk/debug/focus-dial-debug.apk
```

### Step 1.5: Set Watch Face

1. On the emulator, long-press the watch face
2. Swipe to "Add" or browse watch faces
3. Select "Focus Dial"
4. Tap the watch face to start a focus session

### Step 1.6: Test Checklist

| Feature | How to Test |
|---------|-------------|
| Tap to start focus | Tap watch face in idle mode |
| Focus timer arc | Watch progress arc fill over time |
| Break transition | Wait for timer or set 1min focus duration |
| Breathing animation | Observe pulsing circle during break |
| Settings | Open app list → Focus Dial → Settings |
| Theme picker | Change theme, verify watch face updates |
| Onboarding | Clear app data, relaunch |
| Tile | Swipe left to tiles, add Focus Dial tile |
| Complications | Edit another watch face, add Focus Dial complication |

**Tip: Speed up testing by temporarily setting focus duration to 1 minute:**
- Open Settings → reduce Focus Duration to minimum (15min isn't great for testing)
- Or use `adb shell` to write directly to DataStore preferences

---

## Part 2: Firebase Setup (Real Config)

### Step 2.1: Create Firebase Project

1. Go to https://console.firebase.google.com
2. Click "Create a project" → Name: "Focus Dial" → Continue
3. Disable Google Analytics (optional for now, can enable later)
4. Click "Create project"

### Step 2.2: Register Android App

1. In Firebase console → Project Settings (gear icon)
2. Click "Add app" → Android
3. Package name: `com.focusdial.app`
4. App nickname: "Focus Dial WearOS"
5. SHA-1 (needed for billing): see Step 3.2 below for how to get it
6. Click "Register app"
7. Download `google-services.json`
8. Replace the placeholder file:

```bash
cp ~/Downloads/google-services.json \
  /Users/lokesh.rongali/projects/everest/wearos-themes/focus-dial/google-services.json
```

### Step 2.3: Enable Crashlytics

1. In Firebase console → Build → Crashlytics
2. Click "Enable Crashlytics"
3. Build and run once — crashes will start appearing

---

## Part 3: Release Signing (Keystore)

### Step 3.1: Generate Keystore

```bash
mkdir -p /Users/lokesh.rongali/projects/everest/wearos-themes/keystore

keytool -genkey -v \
  -keystore /Users/lokesh.rongali/projects/everest/wearos-themes/keystore/focus-dial.keystore \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias focus-dial \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Focus Dial, OU=Mobile, O=YourCompany, L=City, ST=State, C=IN"
```

Replace `YOUR_STORE_PASSWORD` and `YOUR_KEY_PASSWORD` with strong passwords.

### Step 3.2: Get SHA-1 for Firebase

```bash
keytool -list -v \
  -keystore /Users/lokesh.rongali/projects/everest/wearos-themes/keystore/focus-dial.keystore \
  -alias focus-dial
```

Copy the SHA-1 fingerprint → paste in Firebase console (Project Settings → Your app → Add fingerprint).

### Step 3.3: Set Environment Variables

Add to your shell profile (`~/.zshrc`):

```bash
export KEYSTORE_PASSWORD="your_store_password_here"
export KEY_PASSWORD="your_key_password_here"
```

Then reload:
```bash
source ~/.zshrc
```

**IMPORTANT:** Never commit these passwords. The build.gradle.kts reads them from env:
```kotlin
storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
keyPassword = System.getenv("KEY_PASSWORD") ?: ""
```

### Step 3.4: Backup Your Keystore

**Critical:** If you lose the keystore, you can NEVER update the app on Play Store.

```bash
# Backup to a secure location (external drive, encrypted cloud, etc.)
cp keystore/focus-dial.keystore ~/secure-backup/focus-dial.keystore
```

---

## Part 4: Build Release AAB

### Step 4.1: Build

```bash
cd /Users/lokesh.rongali/projects/everest/wearos-themes

# Ensure env vars are set
echo $KEYSTORE_PASSWORD  # Should not be empty

# Build release AAB (required by Play Store)
./gradlew :focus-dial:bundleRelease
```

Output: `focus-dial/build/outputs/bundle/release/focus-dial-release.aab`

### Step 4.2: Verify the AAB

```bash
# Check it's signed correctly
jarsigner -verify -verbose \
  focus-dial/build/outputs/bundle/release/focus-dial-release.aab
```

### Step 4.3: Test Release Build on Emulator

```bash
# Build release APK (for local testing — Play Store uses AAB)
./gradlew :focus-dial:assembleRelease

# Install
adb install focus-dial/build/outputs/apk/release/focus-dial-release.apk
```

Verify ProGuard didn't break anything:
- Watch face renders correctly
- Tap to start works
- Settings activity opens
- Theme changes apply
- Billing flow doesn't crash (won't complete without Play Store)

---

## Part 5: Play Store Assets

### Step 5.1: Screenshots (required: 3-8 screenshots)

Take screenshots from the emulator:

```bash
# Take screenshot on emulator
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ./docs/screenshots/

# Required: 384x384 round screenshots (Wear OS)
# Take at least 4:
# 1. Idle state (showing time + stats)
# 2. Focus state (arc filling, timer counting)
# 3. Break state (breathing animation)
# 4. Settings screen
```

Crop to circle if needed using any image editor.

### Step 5.2: Feature Graphic (required: 1024x500)

Create in Figma/Canva:
- Show watch with Focus Dial face
- Tagline: "The focus timer that IS your watch face"
- Clean, dark background

### Step 5.3: App Icon (512x512 PNG)

- Use the existing app icon scaled to 512x512
- Or create a new one showing a stylized timer/dial

---

## Part 6: Google Play Console Setup

### Step 6.1: Create Developer Account

1. Go to https://play.google.com/console
2. Pay $25 one-time registration fee
3. Complete identity verification (can take 1-3 days)

### Step 6.2: Create App

1. Click "Create app"
2. App name: "Focus Dial - WearOS Timer"
3. Default language: English
4. App type: App (not Game)
5. Free or paid: Free (freemium model — IAP is inside)

### Step 6.3: Store Listing

Use content from `docs/PLAY_STORE_LISTING.md`:
- Short description (80 chars max)
- Full description (4000 chars max)
- Upload screenshots + feature graphic
- Category: Productivity → Tools
- Tags: pomodoro, focus, timer, wear os

### Step 6.4: Content Rating

1. Dashboard → Policy → Content rating → Start questionnaire
2. Answer questions (this is a utility app, no violence/gambling/etc.)
3. Most answers will be "No" — you'll get an "Everyone" rating

### Step 6.5: Data Safety

Fill out based on what the app collects:

| Question | Answer |
|----------|--------|
| Does your app collect user data? | Yes |
| Data collected | App activity (focus sessions), app info (crash logs) |
| Is data encrypted in transit? | Yes (Firebase uses HTTPS) |
| Can users request deletion? | Yes (clear app data) |
| Data shared with third parties? | Crash reports to Firebase |
| Data collected required or optional? | Optional (can turn off Health Connect) |

### Step 6.6: Target Audience

- Target age: 13+ (general productivity tool)
- Not primarily designed for children

### Step 6.7: App Access

- App has in-app purchases
- All features accessible after purchase (no special logins needed)
- If asked for instructions: "Install on Wear OS device, select as watch face"

---

## Part 7: In-App Purchase Setup

### Step 7.1: Create Product in Play Console

1. Monetize → Products → In-app products → Create product
2. Product ID: `focus_dial_pro` (must match `BillingManager.PRODUCT_PRO`)
3. Name: "Focus Dial Pro"
4. Description: "Unlock all themes, profiles, insights, and premium features"
5. Price: $4.99
6. Status: Active

### Step 7.2: License Testing

1. Settings → License testing
2. Add your test email(s)
3. License response: "RESPOND_NORMALLY" for real testing, or "LICENSED" to simulate purchase

---

## Part 8: Release & Testing Track

### Step 8.1: Closed Testing (Required First)

1. Testing → Closed testing → Create track
2. Create a testers list (add your own email + any beta testers)
3. Upload AAB: `focus-dial/build/outputs/bundle/release/focus-dial-release.aab`
4. Complete release notes: "Initial release of Focus Dial"
5. Review → Start rollout to closed testing

**14-day wait:** Google requires apps to be in closed testing for 14 days with at least 12 testers who have opted in, before you can apply for production access.

### Step 8.2: Get 12 Testers

- Share the opt-in link from Play Console with friends/family/colleagues
- They need to:
  1. Click the opt-in link
  2. Install the app from Play Store (closed testing)
  3. Use it at least once

### Step 8.3: Open Testing (Optional)

After 14 days of closed testing, you can:
- Move to open testing (anyone can join)
- Or go straight to production if you have 12 testers

### Step 8.4: Production Release

1. Production → Create new release
2. Upload same AAB (or updated version)
3. Fill in release notes
4. Submit for review (takes 1-7 days, usually ~24h)

---

## Part 9: Post-Launch Checklist

- [ ] Verify Crashlytics is receiving data
- [ ] Test purchase flow end-to-end with license testing
- [ ] Monitor reviews and respond to feedback
- [ ] Set up Google Play Console alerts (crashes > threshold, bad reviews)
- [ ] Host privacy policy at a public URL and add to Play Store listing
  - Easy option: GitHub Pages from this repo
  - Add URL to Settings activity as well

---

## Environment Variables Summary

| Variable | Purpose | Where to Set |
|----------|---------|-------------|
| `KEYSTORE_PASSWORD` | Release signing | `~/.zshrc` |
| `KEY_PASSWORD` | Release key alias | `~/.zshrc` |
| `ANDROID_HOME` | SDK path (optional, Gradle uses local.properties) | Already configured |

---

## Quick Reference Commands

```bash
# --- Development ---
./gradlew :focus-dial:assembleDebug          # Debug APK
./gradlew :focus-dial:installDebug           # Install on connected device
adb devices                                   # List connected devices/emulators

# --- Release ---
./gradlew :focus-dial:bundleRelease          # Release AAB (for Play Store)
./gradlew :focus-dial:assembleRelease        # Release APK (for local testing)

# --- Emulator ---
emulator -avd WearOS_Round_API33             # Launch Wear OS emulator
adb shell screencap /sdcard/screen.png       # Take screenshot
adb pull /sdcard/screen.png .                # Download screenshot

# --- Debugging ---
adb logcat | grep -i "focusdial\|billing\|crash"
adb shell pm clear com.focusdial.app         # Reset app (test onboarding again)
```

---

## Timeline

| Day | Action |
|-----|--------|
| Day 1 | Set up Firebase, generate keystore, build release, test on emulator |
| Day 2 | Take screenshots, create feature graphic, write listing |
| Day 3 | Create Play Console account, fill out all forms, upload to closed testing |
| Day 3-17 | 14-day closed testing period (recruit 12 testers) |
| Day 17 | Apply for production access |
| Day 18-20 | Google review (usually fast for utility apps) |
| Day 20 | **LIVE on Play Store** 🎉 |
