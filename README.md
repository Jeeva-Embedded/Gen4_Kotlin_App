# Gen4_Kotlin_App

Android (Kotlin + Jetpack Compose) controller app for Gen4 spinning machines.

## Supported Machines
- **Draw Frame** — settings, run state, carousel motor data, diagnosis, length reset
- **Flyer** — settings, run state (layer/lift animation), carousel, diagnosis
- **Carding** — settings, run state, sensor display, carousel, diagnosis
- **Ring Doubler** — settings, run state (doff percent), carousel, diagnosis

## Architecture
- MVVM: ViewModel + StateFlow + Coroutines
- Bluetooth RFCOMM SPP (classic BT)
- TLV binary frame protocol (`7E [PL] [INFO] [SUBSTATE] [ATTR_COUNT] [TLVs…] 7E`)

## Build
```
# Requires Android Studio / SDK
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Min SDK: 26 | Target SDK: 34
