# EZE — Personal Native Android AI Assistant & Companion

**EZE** is a native, production-quality Android personal assistant and AI companion built from the ground up for modern Android versions (supporting Android 11+ and optimized for low-memory 2 GB RAM devices).

EZE combines natural voice speech recognition, an animated holographic orb interface, local deterministic phone control (alarms, timers, reminders, flashlight, app launcher, dialer, calculator), and conversational intelligence powered by Groq AI.

---

## Key Features

1. **Groq AI Intelligence**:
   - Ultra-fast conversational answers and companion chat.
   - User-configurable API key stored with hardware-backed encryption using the **Android Keystore** (`AES/GCM/NoPadding`).
   - Dynamic model switching (e.g., `llama-3.3-70b-versatile`, `mixtral-8x7b-32768`, `gemma2-9b-it`) without rebuilding the app.

2. **Real Android Phone & System Control**:
   - **Flashlight**: Direct Camera2 torch control.
   - **Clock & Alarms**: Real `AlarmClock.ACTION_SET_ALARM`, `ACTION_SET_TIMER`, and exact scheduled reminders via `AlarmManager`.
   - **Phone & SMS**: Dialer opening, prefilled SMS drafting, contact lookup, and direct calling when permitted.
   - **Apps**: Package manager search to open installed applications.
   - **System Settings**: Wi-Fi, Bluetooth, battery, sound, display, and default assistant settings.
   - **Media**: System media playback controls (`KEYCODE_MEDIA_PLAY_PAUSE`, track skipping).
   - **Navigation**: Native Google Maps and geo intent routing.

3. **Deterministic Local Offline Calculator**:
   - Executes arithmetic (`25 times 8`, `500 divided by 4`, `15 percent of 200`) and unit conversions (`5 km to miles`, `celsius to fahrenheit`, `kg to lbs`) instantly on-device without network latency or wasting API tokens.

4. **Natural Voice Interaction & Wake Word**:
   - Speech-to-Text (`SpeechRecognizer`) and Text-to-Speech (`TextToSpeech`).
   - Dedicated lightweight acoustic monitor for wake phrase **"Hey EZE"** / **"EZE"**.
   - Android `VoiceInteractionService` architecture for default digital assistant support.
   - Foreground service with notification for background microphone operation.

5. **AI Friend & Dual Personality**:
   - Easily switch between **Smart Assistant** mode (efficient, direct, concise) and **AI Friend** mode (warm, encouraging, humorous companion).

6. **Local Memory System (Room Database)**:
   - Stores user preferences, goals, and notes locally in Room.
   - Full control to view, add, delete, or clear memories at any time.

---

## Getting Started

### 1. Opening the Project
- Open **Android Studio** (Ladybug / Hedgehog or newer).
- Select **Open** and choose the root directory of this project.
- Allow Gradle to sync.

### 2. Entering your Groq API Key and Model Name
1. Launch the app on your device or emulator.
2. Complete the onboarding wizard, or tap the **Settings** icon on the top right of the main screen.
3. In **Groq AI Configuration**:
   - Enter your Groq API key (starts with `gsk_...` from [Groq Console](https://console.groq.com)).
   - Select or type your preferred model (e.g. `llama-3.3-70b-versatile`).
   - Tap **Test Connection** to verify your key.
   - Tap **Save Settings**.
4. The key is securely encrypted on-device in the Android Keystore.

### 3. Setting EZE as Default Assistant
1. Open **Settings** in EZE.
2. Scroll to **System Assistant Integration** and tap **Set EZE as Default Assistant**.
3. In Android system settings, choose **EZE Voice Interaction Service** under "Default digital assistant app".
4. Long-pressing the home button or swiping from screen corners will now summon EZE.

### 4. Enabling Wake-Word ("Hey EZE")
1. In **Settings** -> **Voice & Wake Word**, toggle **Wake phrase ("Hey EZE" / "EZE")**.
2. EZE will start a low-overhead foreground service with an ongoing notification to monitor audio.
3. Say "Hey EZE" to wake the assistant.

---

## Android 11 & Background Execution Notes

- **2 GB RAM Optimization**: EZE uses zero heavyweight frameworks, minimal background polling, and an efficient acoustic monitor with lightweight PCM buffers.
- **Microphone Restrictions**: Android 11+ restricts background microphone access when an app does not have an active foreground service or VoiceInteraction session. EZE provides:
  - A registered `ForegroundService` with notification so Android does not kill the listener.
  - Guidance to disable battery optimizations (`Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`) on aggressive OEM skins (e.g., Xiaomi MIUI, Samsung OneUI, Huawei) to prevent background killing.

---

## Building the APK

### Command Line
To build the debug APK:
```bash
./gradlew assembleDebug
```
The output APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions APK Download
1. Push this repository to GitHub.
2. The GitHub Actions workflow `.github/workflows/build-apk.yml` will trigger automatically.
3. Navigate to the **Actions** tab on GitHub, click the latest workflow run.
4. Under **Artifacts**, download `EZE-debug-APK`.

---

## Permissions Overview

| Permission | Purpose |
|------------|---------|
| `RECORD_AUDIO` | Voice speech recognition and wake-word monitoring. |
| `INTERNET` | Sending chat queries to Groq AI and performing web searches. |
| `ACCESS_NETWORK_STATE` | Offline detection and graceful degradation. |
| `FOREGROUND_SERVICE` | Running the background voice assistant listener. |
| `CAMERA` | Flashlight / torch on-off toggle. |
| `MODIFY_AUDIO_SETTINGS` | Audio focus and media volume control. |
| `POST_NOTIFICATIONS` | Ongoing assistant indicator (Android 13+) and alarm alerts. |
| `SCHEDULE_EXACT_ALARM` | Scheduling exact timers and reminders via AlarmManager. |
| `RECEIVE_BOOT_COMPLETED` | Restoring wake-word service after device restart. |
