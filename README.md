# Gen4 Spinning — Android Controller App

Kotlin + Jetpack Compose Android app for monitoring and controlling Gen4 spinning machines over Bluetooth Classic (RFCOMM SPP).

---

## Supported Machines

| Machine | Key Features |
|---|---|
| **Draw Frame (DF)** | Delivery speed, draft, length limit, ramp times, creel tension factor; AL sensor state; Auto-Leveller settings + calibration (opcode 0x11–0x16) |
| **Flyer Frame** | 13 settings (spindle speed, draft, TPI, RTF, layers, lift geometry); live left/right lift position; gearbox teach-in; front/back cut counts |
| **Carding (Blow Card)** | 8 settings (delivery speed, card feed ratio, cylinder/beater speeds, feed params); duct + coiler sensor display; internal RPM calculations |
| **Ring Doubler** | 8 settings (yarn count, spindle speed, TPI, package geometry); live doff-percent weight; output yarn diameter auto-calculation |

All 4 machines share: live run-state display, motor carousel telemetry (RPM / current / temperatures / power / output metres), motor diagnostics (open-loop + closed-loop), PID editing, CSV logging, and RTF toggle.

---

## Architecture

```
MVVM — ViewModel + StateFlow + SharedFlow + Coroutines
BT layer  : BtSessionRepository (RFCOMM SPP, reconnect watchdog, RX/TX coroutines)
Protocol  : FrameCodec (build/parse TLV frames) + per-machine Protocol objects
UI        : Jetpack Compose + Material3, one Dashboard per machine (4-tab bottom nav)
Logging   : MachineLogger — CSV file per session, 1 s snapshot interval
```

- **Min SDK:** 26 (Android 8.0) | **Target SDK:** 34 | **Compile SDK:** 35
- **Package:** `com.gen4.spinning`
- **Language:** Kotlin

---

## Project Structure

```
app/src/main/kotlin/com/gen4/spinning/
│
├── Gen4SpinningApp.kt          — Application class; creates BtSessionRepository
├── MainActivity.kt             — Permission requests; sealed Screen nav; AppNavHost
│
├── core/
│   ├── bt/
│   │   ├── BtFrame.kt          — BtFrame + Tlv data classes; frame constants
│   │   ├── ConnectionState.kt  — 6-state sealed class + timeout constants
│   │   ├── FrameCodec.kt       — build() / parse() TLV frames
│   │   └── BtSessionRepository.kt — Full BT session: connect, reconnect watchdog, RX/TX loops
│   ├── protocol/
│   │   ├── DfProtocol.kt       — TLV type constants for Draw Frame
│   │   ├── CardingProtocol.kt  — TLV type constants for Carding
│   │   ├── FlyerProtocol.kt    — TLV type constants for Flyer
│   │   └── RingProtocol.kt     — TLV type constants for Ring Doubler
│   └── logging/
│       └── MachineLogger.kt    — CSV log file writer
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt            — SpinColors palette
│   │   └── Theme.kt            — Gen4SpinningTheme (Material3)
│   ├── components/
│   │   └── SharedComponents.kt — GradientAppBar, StatusBox, SensorIndicator,
│   │                             FieldInput, SaveBanner, SettingsToolbar, DisconnectedScreen
│   └── screens/
│       ├── SplashScreen.kt     — Logo splash, 2.5 s
│       ├── SelectMachineScreen.kt — 2-column machine tile grid + drawer (Log Files / Exit)
│       ├── BluetoothScreen.kt  — BT enable toggle + open device list
│       ├── SelectDeviceScreen.kt — Bonded device list
│       └── LogsScreen.kt       — Browse and open CSV log files
│
├── machines/
│   ├── carding/
│   │   ├── CardingViewModel.kt — Settings (8 fields), RunState, frame dispatch, logFetchJob
│   │   ├── CardingDashboard.kt — 4-tab nav (Status / Settings / Tests / Options)
│   │   ├── CardingStatusScreen.kt — Live status: speed, sensors, carousel
│   │   ├── CardingCarousel.kt  — 9-page HorizontalPager motor telemetry
│   │   ├── CardingSettingsScreen.kt — 8 FieldInput widgets
│   │   ├── CardingTestsScreen.kt — Motor / Control / Direction dropdowns + sliders
│   │   └── CardingOptionsScreen.kt — RTF switch + CSV log toggle
│   │
│   ├── df/
│   │   ├── DfViewModel.kt      — Settings (6 fields), RunState + AL state, logFetchJob
│   │   ├── DfDashboard.kt      — 4-tab nav
│   │   ├── DfStatusScreen.kt   — Speed, length, AL sensor state, carousel
│   │   ├── DfCarousel.kt       — 4-page carousel (Production, FrontRoller, BackRoller, Creel)
│   │   ├── DfSettingsScreen.kt — 6 FieldInput widgets
│   │   ├── DfAlSettingsScreen.kt — Kp, Sliver thresholds, target g/m; AL GET/SAVE + calibration
│   │   ├── DfTestsScreen.kt    — Motor diagnosis
│   │   └── DfOptionsScreen.kt  — AL toggle + calibration + RTF + log
│   │
│   ├── flyer/
│   │   ├── FlyerViewModel.kt   — Settings (13 fields), RunState (lift L/R, layers, cut counts)
│   │   ├── FlyerDashboard.kt   — 4-tab nav
│   │   ├── FlyerStatusScreen.kt — Lift bar animation, layers, cut counts, carousel
│   │   ├── FlyerCarousel.kt    — 10-page carousel (all motors + production)
│   │   ├── FlyerSettingsScreen.kt — 13 FieldInput widgets + Reset Length button
│   │   ├── FlyerTestsScreen.kt — Lift motors: LiftCommand + BedDistance slider
│   │   └── FlyerOptionsScreen.kt — Gearbox teach-in + RTF + log
│   │
│   └── ring/
│       ├── RingViewModel.kt    — Settings (8 fields), RunState (weight/doff%), calcOutputYarnDia()
│       ├── RingDashboard.kt    — 4-tab nav
│       ├── RingStatusScreen.kt — Doff percent, carousel
│       ├── RingCarousel.kt     — 5-page carousel (Production, Calender, LiftLeft, LiftRight)
│       ├── RingSettingsScreen.kt — 8 FieldInput widgets
│       ├── RingTestsScreen.kt  — Lift motors: LiftCommand + BedDistance slider
│       └── RingOptionsScreen.kt — RTF + log
│
└── shared/
    └── PidScreen.kt            — Kp / Ki / FF / SO PID editor (all 4 machines)
```

---

## Bluetooth Protocol

**Transport:** Bluetooth Classic RFCOMM SPP  
**UUID:** `00001101-0000-1000-8000-00805F9B34FB`

### Frame Structure
```
7E [LL] [INFO] [SS] [CC] [TLV…] 7E
│   │    │      │    │    └─ zero or more TLV fields
│   │    │      │    └─ ATTR_COUNT (informational)
│   │    │      └─ SUBSTATE — context depends on opcode
│   │    └─ INFO — opcode byte
│   └─ LENGTH — hex-char count of body (INFO through EOF inclusive)
└─ SOF/EOF = 0x7E
```

### TLV Encoding
Each TLV is `[TYPE 1 byte][WIRELEN 1 byte][VALUE WL bytes]`.  
WIRELEN is the **hex-char count** of the value (02 = 1 byte, 04 = 2 bytes, 08 = 4 bytes).

| Data type | WireLen | Example |
|---|---|---|
| UInt8 / Compact | `02` | `0B 02 01` |
| UInt16 | `04` | `80 04 00 50` |
| Float (IEEE 754) | `08` | `70 08 42 A0 00 00` |

### Key Opcodes

| Opcode | Name | Direction |
|---|---|---|
| `0x01` | Settings From App | App → Board |
| `0x02` | Settings To App | Board → App |
| `0x03` | Request Settings | App → Board |
| `0x04` | Diagnostics start/stop | App → Board |
| `0x05` | Diagnosis Response (live) | Board → App |
| `0x06` | Machine State (heartbeat) | Board → App |
| `0x07` | Carousel Request / Response | Both |
| `0x08` | Gearbox Command | App → Board |
| `0x09` | Gearbox Response | Board → App |
| `0x0A` | Reset Length / Reset Grams | App → Board |
| `0x0B` | RTF Toggle | App → Board |
| `0x0C` | CSV Log Control | App → Board |
| `0x0D` | PID Request | App → Board |
| `0x0E` | PID Response | Board → App |
| `0x0F` | PID Update | App → Board |
| `0x11` | AL Save (DF) / Reset Cuts (Flyer) | App → Board |
| `0x12` | AL Response | Board → App |
| `0x13` | AL GET | App → Board |
| `0x15` | AL Calibration Result | Board → App |
| `0x16` | Auto Leveller Toggle | App → Board |
| `0x99` | Paired From Phone | App → Board |

Full protocol reference (all TLVs, all machines) is documented in `Gen4_HMI_BT_Protocol.xlsx`.

### Connection Handshake
1. App detects `OK+CONN` string in RX stream → connected state
2. App sends **Paired From Phone** (`7E 08 99 00 00 7E`) — wait ~2 s
3. App sends **Request Settings** (`7E 08 03 00 00 7E`)
4. Board replies with opcode `0x02` containing all current EEPROM settings

### Machine Substates (opcode 0x06 SS field)
| Value | State |
|---|---|
| `0x00` | Idle |
| `0x01` | Running |
| `0x02` | Pause (includes pause-reason TLVs) |
| `0x03` | Error (includes error-source + reason TLVs) |
| `0x04` | Homing |
| `0x05` | Inching |

---

## Key Features

### Live Motor Carousel
- Background `logFetchJob` coroutine cycles all motor IDs every 400 ms while running
- Each machine's HorizontalPager carousel shows per-motor telemetry: RPM, current (A), motor °C, MOSFET °C, output metres, total power
- Carousel data also embedded in opcode `0x06` running frames for the active motor

### Diagnostics
- Available for all motors on all 4 machines
- Start: `drainTxQueue()` → send opcode `0x04` (SS=`0x01`) with motor ID, direction, control type, speed %, duration
- Lift motors (Flyer/Ring): use `BedDistance` TLV instead of speed%/duration
- Live response: opcode `0x05` — RPM, PWM, current, power
- Stop: `drainTxQueue()` → send opcode `0x04` (SS=`0x06`) **3×** for reliability
- `logFetchJob` pauses carousel sends while `isDiagnosing=true` to avoid BT queue flooding

### CSV Logging
- 1 s snapshot interval; `logFetchJob` ensures all motor data is populated before writing
- Files saved to app-private storage; browsable via the **Log Files** drawer item
- Board logging flag toggled via opcode `0x0C`

### Auto-Leveller (Draw Frame only)
- GET current AL settings: opcode `0x13` (SS=`0x01`)
- SAVE AL settings: opcode `0x11` (SS=`0x01`) with TLVs 0x20–0x24 (Kp, Sliver thresholds, target g/m)
- Enable/Disable: opcode `0x16`
- Open-loop ADC calibration: opcode `0x04` (SS=`0x05` start / `0x02` stop) → result via opcode `0x15`

### Gearbox Teach-In (Flyer Frame)
- Start (SS=`0x01`) → Stop (SS=`0x02`) → Save Left (SS=`0x03`) / Save Right (SS=`0x04`)
- Board responds with opcode `0x09` containing left + right encoder values

---

## Build

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\Jeeva\AppData\Local\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat assembleDebug
```

**APK output:** `app\build\outputs\apk\debug\app-debug.apk`

### Runtime Permissions
On first launch Android will prompt:
- **Android 12+:** `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN`
- **Android 11 and below:** `ACCESS_FINE_LOCATION`

---

## Protocol Reference Document

`Gen4_HMI_BT_Protocol.xlsx` — multi-sheet Excel workbook covering every opcode, TLV type, wire encoding, machine-specific settings, carousel, diagnostics, and AL protocol in full detail.
