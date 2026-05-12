# Gen4 Spinning Kotlin — Work Log
Last updated: 2026-04-27

---

## Project Info
- **Location:** `C:\Users\Jeeva\Desktop\App_Discussion\Gen4_Spinning_Kotlin\`
- **Package:** `com.gen4.spinning`
- **Language:** Kotlin + Jetpack Compose + Material3
- **Min SDK:** 26 (Android 8.0)  |  **Target SDK:** 34  |  **Compile SDK:** 35
- **Architecture:** MVVM — ViewModel + StateFlow + SharedFlow + Coroutines
- **BT Protocol:** RFCOMM Serial over Bluetooth, UUID `00001101-0000-1000-8000-00805F9B34FB`, custom TLV binary frames

---

## Build Command (always use this)
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\Jeeva\AppData\Local\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location "C:\Users\Jeeva\Desktop\App_Discussion\Gen4_Spinning_Kotlin"
.\gradlew.bat assembleDebug
```
**APK output:** `app\build\outputs\apk\debug\app-debug.apk`  
**Copy to Desktop:**
```powershell
Copy-Item "app\build\outputs\apk\debug\app-debug.apk" "C:\Users\Jeeva\Desktop\Gen4Spinning-debug.apk" -Force
```

---

## Key Paths
| Resource | Path |
|---|---|
| Android SDK | `C:\Users\Jeeva\AppData\Local\Android\Sdk` |
| Java (JBR) | `C:\Program Files\Android\Android Studio\jbr` |
| Gradle 8.9 | `C:\Users\Jeeva\.gradle\wrapper\dists\gradle-8.9-bin\` |
| Company Logo | `C:\Users\Jeeva\Desktop\App_Discussion\logo TM.jpg` → copied to `app\src\main\res\drawable\logo.jpg` |
| Flutter Source | `C:\Users\Jeeva\Desktop\App_Discussion\` (reference only) |

---

## All Kotlin Files Created (43 total)

### Core
| File | Description |
|---|---|
| `core/bt/BtFrame.kt` | `BtFrame`, `Tlv` data classes; `SAVE_RESPONSE_SUCCESS/FAILURE`, `FSC_CONNECT/DISCONN_STRING` constants |
| `core/bt/ConnectionState.kt` | 6-state sealed class; timeout constants: `CONNECT_TIMEOUT_MS=12000`, `FIRST_FRAME_TIMEOUT_MS=5000`, `RECONNECT_WARN_TIMEOUT_MS=3000`, `RECONNECT_LOST_TIMEOUT_MS=15000` |
| `core/bt/FrameCodec.kt` | `parse()`, `build()`, `buildPairedFromPhone()`, `buildReqSettings()`, `buildCarouselRequest()`, `buildPidRequest()`, etc. |
| `core/bt/BtSessionRepository.kt` | Full BT session: connect, reconnect, watchdog, RX/TX loops, frame parsing. Accepts **nullable** `BluetoothAdapter`. |
| `core/protocol/CardingProtocol.kt` | TLV constants for Carding |
| `core/protocol/DfProtocol.kt` | TLV constants for Draw Frame |
| `core/protocol/FlyerProtocol.kt` | TLV constants for Flyer |
| `core/protocol/RingProtocol.kt` | TLV constants for Ring Doubler |

### App Entry
| File | Description |
|---|---|
| `Gen4SpinningApp.kt` | `Application` subclass — creates `BtSessionRepository(adapter?)` |
| `MainActivity.kt` | Requests BT permissions on start; sealed `Screen` class; `AppNavHost` composable |

### UI Theme & Components
| File | Description |
|---|---|
| `ui/theme/Color.kt` | `SpinColors` object: `Blue=#2196F3`, `LightGreen=#8BC34A`, etc. |
| `ui/theme/Theme.kt` | `Gen4SpinningTheme` wrapping Material3 `MaterialTheme` |
| `ui/components/SharedComponents.kt` | `GradientAppBar`, `GradientButton`, `StatusBox`, `SensorIndicator`, `FieldInput`, `SaveBanner`, `SettingsToolbar`, `DisconnectedScreen` |

### Screens
| File | Description |
|---|---|
| `ui/screens/SplashScreen.kt` | White BG, `R.drawable.logo` centered, 2.5s delay then `onDone()` |
| `ui/screens/SelectMachineScreen.kt` | 2-column grid, 4 machine tiles with gradient |
| `ui/screens/BluetoothScreen.kt` | BT toggle switch + Paired Devices button; **all BT calls wrapped in SecurityException try-catch** |
| `ui/screens/SelectDeviceScreen.kt` | Lists bonded devices; `bondedDevices` + `device.name` **wrapped in SecurityException try-catch** |

### Machines — Carding
| File | Description |
|---|---|
| `machines/carding/CardingViewModel.kt` | `CardingSettings` (8 fields), `CardingRunState`, frame dispatch by `frame.info`, 2s save banner |
| `machines/carding/CardingDashboard.kt` | `cardingVmFactory`, 4-tab bottom nav, `DisconnectedScreen` on lost/disconnected |
| `machines/carding/CardingStatusScreen.kt` | `substateLabel()` mapper, running state shows speed + 2 sensors + carousel |
| `machines/carding/CardingCarousel.kt` | `@OptIn(ExperimentalFoundationApi::class)` HorizontalPager, 9 pages, dot indicator |
| `machines/carding/CardingSettingsScreen.kt` | 8 `FieldInput` widgets, `SaveBanner`, `SettingsToolbar` |
| `machines/carding/CardingTestsScreen.kt` | 3 dropdowns (Motor/Control/Direction), 2 sliders, "RUN DIAGNOSE" button |
| `machines/carding/CardingOptionsScreen.kt` | Gearbox 4 buttons + RTF switch + Log switch |

### Machines — Draw Frame
| File | Description |
|---|---|
| `machines/df/DfViewModel.kt` | `DfSettings` (6 fields), `DfRunState`, same pattern as Carding |
| `machines/df/DfDashboard.kt` | `dfVmFactory`, 4-tab bottom nav |
| `machines/df/DfStatusScreen.kt` | ⚠️ **Missing carousel** — see PHASE2_CORRECTIONS.md item #3 |
| `machines/df/DfSettingsScreen.kt` | 6 `FieldInput` widgets |
| `machines/df/DfTestsScreen.kt` | Same as Carding |
| `machines/df/DfOptionsScreen.kt` | Gearbox + RTF + Log |

### Machines — Flyer
| File | Description |
|---|---|
| `machines/flyer/FlyerViewModel.kt` | `FlyerSettings` (13 fields), `FlyerRunState` |
| `machines/flyer/FlyerDashboard.kt` | `flyerVmFactory`, 4-tab bottom nav |
| `machines/flyer/FlyerStatusScreen.kt` | Status display |
| `machines/flyer/FlyerSettingsScreen.kt` | 13 `FieldInput` widgets, `sendResetLengthCounter()` button |
| `machines/flyer/FlyerTestsScreen.kt` | Lift motors: `LiftCommand` dropdown + BedDistance slider instead of Direction |
| `machines/flyer/FlyerOptionsScreen.kt` | Gearbox + RTF + Log |

### Machines — Ring Doubler
| File | Description |
|---|---|
| `machines/ring/RingViewModel.kt` | `RingSettings` (8 fields), `RingRunState` with `weight` field, `calcOutputYarnDia()` |
| `machines/ring/RingDashboard.kt` | `ringVmFactory`, 4-tab bottom nav |
| `machines/ring/RingStatusScreen.kt` | ⚠️ **"Weight" label should be "Doff Percent"** — see PHASE2_CORRECTIONS.md item #4 |
| `machines/ring/RingSettingsScreen.kt` | 8 `FieldInput` widgets |
| `machines/ring/RingTestsScreen.kt` | Lift motors: `LiftCommand` + BedDistance slider |
| `machines/ring/RingOptionsScreen.kt` | Gearbox + RTF + Log |

### Shared
| File | Description |
|---|---|
| `shared/PidScreen.kt` | Kp/Ki/FF/SO float TLVs, request (0x0E) + send (0x0F) |

---

## Gearbox Opcode Reference (corrected from Flutter source)
| Button | Opcode |
|---|---|
| Start | `0x01` |
| Stop | `0x02` |
| Save Left | `0x03` |
| Save Right | `0x04` |

---

## Build Fixes Applied (all resolved)

| # | Problem | Fix |
|---|---|---|
| 1 | `local.properties` had escaped backslashes (`C\:\\`) causing Windows path error | Changed to forward slashes: `sdk.dir=C:/Users/Jeeva/AppData/Local/Android/Sdk` |
| 2 | Missing `gradle.properties` — AndroidX not enabled | Created with `android.useAndroidX=true`, `android.enableJetifier=true` |
| 3 | Missing mipmap launcher icons | Changed manifest to use `@drawable/logo` |
| 4 | `BtSessionRepository.kt:159` — `continue` inside `run {}` inline lambda (experimental feature) | Replaced with `if (null)` check |
| 5 | `ViewModelProvider.Factory` used wrong DSL initializer | Changed all 4 dashboards to `object : ViewModelProvider.Factory { ... }` pattern |
| 6 | `Divider()` deprecated in Material3 | Changed to `HorizontalDivider()` |
| 7 | `PidScreen.kt` — `Spacer` with wrong modifier | Fixed to `Spacer(modifier = Modifier.width(8.dp))` |

---

## Runtime / Crash Fixes Applied (session 2 — APK crash fix)

| # | Problem | Fix |
|---|---|---|
| 1 | **App crashed after splash** — No runtime Bluetooth permission request | Added `registerForActivityResult(RequestMultiplePermissions)` in `MainActivity.onCreate()` — requests `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN` (Android 12+) or `ACCESS_FINE_LOCATION` (Android 11 and below) |
| 2 | `adapter.isEnabled` throws `SecurityException` on Android 12+ | Wrapped in try-catch in `BluetoothScreen.kt` via `btIsEnabled()` helper |
| 3 | `adapter.bondedDevices` throws `SecurityException` | Wrapped in try-catch in `SelectDeviceScreen.kt` |
| 4 | `device.name` throws `SecurityException` | Wrapped in try-catch in `SelectDeviceScreen.kt` and `BtSessionRepository.kt` |
| 5 | `BluetoothAdapter.getDefaultAdapter()` deprecated + potential NPE | Removed — `Gen4SpinningApp` now uses `(getSystemService(...) as? BluetoothManager)?.adapter` |
| 6 | `BtSessionRepository` had non-nullable `BluetoothAdapter` parameter | Changed to nullable `BluetoothAdapter?`, all usages null-guarded |
| 7 | `cancelDiscovery()` throws `SecurityException` | Wrapped in try-catch in `BtSessionRepository.kt` |
| 8 | Missing `ACCESS_FINE_LOCATION` for Android 11 and below BT scanning | Added to `AndroidManifest.xml` with `maxSdkVersion="30"` |
| 9 | `BLUETOOTH_SCAN` could trigger location warning | Added `android:usesPermissionFlags="neverForLocation"` |
| 10 | Theme missing explicit window attributes | Added `windowBackground`, `windowNoTitle`, etc. to `themes.xml` |

---

## First Launch Behavior (expected)
1. Android shows a permission dialog: **"Allow Gen4 Spinning to access nearby devices?"**
2. User taps **Allow** (or **Allow while using app**)
3. App proceeds normally — logo splash → machine select → Bluetooth → connect

---

## Phase 2 Corrections Still Needed
See `PHASE2_CORRECTIONS.md` for full details. Key items:

1. Substate enum values wrong (Pause=0x02, Error=0x03, Homing=0x04 — currently reversed)
2. Missing Error/Pause/Homing sub-UI in all Status screens
3. **Missing DrawFrame carousel** (`DfStatusScreen.kt`) — 4 pages: PRODUCTION(0x0A), FrontRoller(0x01), BackRoller(0x02), Creel(0x03)
4. Ring "Doff Percent" label (currently shows "Weight")
5. Missing Ring "Reset Grams Per Spindle" button (opcode 0x0A)
6. Missing Carding Internal Parameters popup (derived RPM calculations)
7. Missing Ring lift animation (tilting bar visual)
8. Sensor labels should show "Open/Blocked" text
9. Missing Drawer menu ("Change Device Name" + "Exit App") in all dashboards

---

## ViewModel Factory Pattern Used
```kotlin
fun cardingVmFactory(repository: BtSessionRepository) =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CardingViewModel(repository) as T
    }
```
Same pattern for `dfVmFactory`, `flyerVmFactory`, `ringVmFactory`.

---

## Frame Opcode Reference
| Opcode | Direction | Meaning |
|---|---|---|
| `0x01` | Phone→MC | Paired from phone |
| `0x02` | MC→Phone | Settings response |
| `0x06` | MC→Phone | Run state (heartbeat) |
| `0x07` | MC→Phone | Carousel motor data |
| `0x08` | Phone→MC | Request settings |
| `0x09` | Phone→MC | Save settings |
| `0x0A` | Phone→MC | Reset length counter (Flyer) / Reset grams (Ring) |
| `0x0B` | Phone→MC | Gearbox command |
| `0x0C` | Phone→MC | RTF toggle |
| `0x0D` | Phone→MC | Log toggle |
| `0x0E` | MC→Phone | PID response |
| `0x0F` | Phone→MC | Send PID values |
| `0x10` | Phone→MC | Carousel request |
| `0x11` | Phone→MC | Diagnostic run |
