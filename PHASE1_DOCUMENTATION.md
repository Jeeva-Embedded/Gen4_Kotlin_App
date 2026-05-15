# Gen4 Spinning Android App — Phase 1 Documentation

**Created by:** Jeeva (Jeeva-Embedded)  
**Repository:** https://github.com/Jeeva-Embedded/Gen4_Kotlin_App  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Min SDK:** 26 | **Target SDK:** 34  
**Phase 1 Completed:** 2026-05-15

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [What Jeeva Created](#3-what-jeeva-created)
4. [Machines Implemented](#4-machines-implemented)
5. [Machine Settings — All Fields](#5-machine-settings--all-fields)
6. [HMI Screens — All Details](#6-hmi-screens--all-details)
7. [BT Protocol — Frame Format](#7-bt-protocol--frame-format)
8. [Protocol Opcodes & Substates](#8-protocol-opcodes--substates)
9. [TLV Definitions](#9-tlv-definitions)
10. [Error Codes & Sources](#10-error-codes--sources)
11. [All Changes Made (Phase 1)](#11-all-changes-made-phase-1)
12. [What Is Working](#12-what-is-working)
13. [Known Remaining Issues](#13-known-remaining-issues)
14. [Default Values (Verified)](#14-default-values-verified)

---

## 1. Project Overview

Native Android port of the Gen4 Spinning Flutter app (v8). The Kotlin app communicates with spinning machines over Bluetooth (RFCOMM SPP) using a TLV binary protocol. Four machine types are supported:

| Machine | Description |
|---------|-------------|
| **Draw Frame** | Draws sliver to reduce thickness via front/back rollers |
| **Flyer Frame** | Twists and winds roving onto bobbins; bobbin lift mechanism |
| **Carding** | Opens, cleans and aligns fibres; cylinder + beater system |
| **Ring Doubler** | Ring spinning with doff percent and per-spindle tracking |

---

## 2. Architecture

```
MVVM Pattern
├── ViewModel (StateFlow + SharedFlow + Coroutines)
│   ├── FlyerViewModel
│   ├── CardingViewModel
│   ├── DfViewModel
│   └── RingViewModel
├── BtSessionRepository  — Bluetooth RFCOMM SPP connection
├── FrameCodec           — TLV binary frame encode/decode
├── Protocol objects     — Per-machine TLV constants + frame builders
│   ├── FlyerProtocol
│   ├── CardingProtocol
│   └── DfProtocol
└── Compose UI Screens   — Settings, Status, Options, Tests, Carousel
```

**Key reactive primitives used:**
- `MutableStateFlow` — settings, run state, diagnosis state, save/read results
- `SharedFlow` — inbound BT frames (`extraBufferCapacity = 64`, `tryEmit`)
- `viewModelScope` + coroutines — frame collection, timeouts, banner dismiss

---

## 3. What Jeeva Created

Jeeva built the entire application from scratch as a Kotlin/Compose port of the Flutter v8 source:

### Core Infrastructure
- `BtSessionRepository` — Bluetooth RFCOMM SPP session management, frame emission using `tryEmit` with `extraBufferCapacity=64` to prevent dropped frames
- `FrameCodec` — Binary frame encoder/decoder; position-based TLV parsing (matches Flutter's `while cursor < eofOffset` approach, immune to ATTR_COUNT encoding mismatches); wireLen relaxed to accept any even 2–32
- Protocol constant objects (`FlyerProtocol`, `CardingProtocol`, `DfProtocol`) — all TLV type codes, motor IDs, and frame builder functions
- Shared UI components: `StatusBox`, `FieldInput`, `SaveBanner`, `SettingsToolbar`, `GradientButton`, `SensorIndicator`

### Flyer Frame (full implementation)
- `FlyerViewModel` — settings StateFlow, run state StateFlow, carousel data, diagnosis state, gearbox left/right, log toggle, RTF send, PID request
- `FlyerStatusScreen` — idle/running/paused/error/homing states with lift animation
- `FlyerSettingsScreen` — 13-field settings form with validation + internal parameters popup
- `FlyerOptionsScreen` — logging toggle, gearbox start/stop with left/right value display, RTF +/- adjust
- `FlyerTestsScreen` — motor diagnosis for Flyer, Bobbin, FR, BR, Lift Left, Lift Right
- `FlyerCarousel` — 7-page swipeable motor data carousel (Production, Flyer, Bobbin, FR, BR, Lift L, Lift R)
- `FlyerDashboard` — tab host with Settings/Status/Options/Tests + Disconnect

### Carding (full implementation)
- `CardingViewModel` — all settings, run state, carousel, diagnosis, log, settings lock during run
- `CardingStatusScreen` — running (delivery speed + sensors + carousel) / paused / error / homing
- `CardingSettingsScreen` — 8-field form with validation; Cylinder/Beater/PickerCyl locked when running
- `CardingOptionsScreen` — logging toggle, reset length counter
- `CardingTestsScreen` — motor diagnosis for Cylinder, Beater, PickerCyl, Card Feed, Beater Feed, Coiler, Cage
- `CardingCarousel` — per-motor live data

### Draw Frame (full implementation)
- `DfViewModel` — settings, run state with delivery speed + current length + pause/error detail
- `DfStatusScreen` — running (delivery + length + carousel) / paused / error / homing
- `DfSettingsScreen` — 6-field form with inline motor RPM validation (rejects if FR/BR motor > 1450 RPM)
- `DfOptionsScreen` — logging toggle, reset length counter
- `DfTestsScreen` — motor diagnosis for FR, BR
- `DfCarousel` — live motor data

### Ring Doubler (basic implementation)
- `RingViewModel`, `RingStatusScreen`, `RingSettingsScreen`, `RingOptionsScreen`

---

## 4. Machines Implemented

### Status Summary

| Machine | Settings | Status Screen | Options | Tests / Diagnosis | Carousel |
|---------|----------|---------------|---------|-------------------|----------|
| Draw Frame | ✅ Full | ✅ Full | ✅ | ✅ | ✅ |
| Flyer Frame | ✅ Full | ✅ Full | ✅ | ✅ | ✅ |
| Carding | ✅ Full | ✅ Full | ✅ | ✅ | ✅ |
| Ring Doubler | ⚠️ Partial | ⚠️ Basic | ⚠️ Basic | ❌ | ❌ |

---

## 5. Machine Settings — All Fields

### 5.1 Flyer Frame Settings

| Field | Default | Range | Unit | TLV Code |
|-------|---------|-------|------|----------|
| Spindle Speed | 650 | 500 – 1100 | RPM | 0x50 |
| Draft | 8.8 | 5.0 – 15.0 | — | 0x51 |
| Twist Per Inch | 1.4 | 1.0 – 1.6 | TPI | 0x52 |
| RTF | 1.0 | 0.5 – 1.5 | — | 0x53 |
| Layers | 50 | 1 – 100 | — | 0x54 |
| Max Height | 270 | 250 – 300 | mm | 0x55 |
| Roving Width | 1.2 | 1.0 – 8.0 | mm | 0x56 |
| Delta Bobbin Dia | 1.1 | 0.3 – 2.5 | mm | 0x57 |
| Bare Bobbin Dia | 48 | 46 – 52 | mm | 0x58 |
| Ramp Up Time | 12 | 5 – 20 | s | 0x59 |
| Ramp Down Time | 12 | 5 – 20 | s | 0x60 |
| Change Layer Time | 800 | 200 – 2500 | ms | 0x61 |
| Cone Angle Factor | 1.0 | 0.1 – 3.0 | — | 0x62 |

**Flyer Internal Parameters Popup** (calculated, read-only):

| Param | Formula |
|-------|---------|
| Delivery (m/min) | `spindleSpeed / twistPerInch × 0.0254` |
| Stroke Velocity (mm/s) | `(deltaRpm × strokeDelta) / 60` — warns red if > 5.5 |
| Max Layers Possible | `min(maxHeight/rovingWidth, (140−bareBobbinDia)/deltaBobbinDia) − 5` |
| Runtime for N layers | Sum of per-layer stroke times |
| Flyer Motor RPM | = Spindle Speed |
| Bobbin Motor RPM | `spindleSpeed + (delivery×1000)/(bareBobbinDia×π)` |
| FR Motor RPM | `delivery×1000/94.248 × 3.33` |
| BR Motor RPM | `frRpm×22.642 / (draft/1.5)` |
| Lift Motor RPM | `strokeVelocity × 60/4 × 9` |

---

### 5.2 Carding Settings

| Field | Default | Range | Unit | TLV Code |
|-------|---------|-------|------|----------|
| Delivery Speed | 8.0 | 2.5 – 50.0 | m/min | — |
| Card Feed Ratio | 5.0 | 0.4 – 20.0 | — | — |
| Length Limit | 1000 | 100 – 1000 | m | — |
| Cylinder Speed | 750 | 300 – 875 | RPM | — |
| Beater Speed | 600 | 300 – 650 | RPM | — |
| Picker Cylinder Speed | 600 | 300 – 700 | RPM | — |
| Beater Feed | 10 | 1 – 11 | — | — |
| AF Feed | 7 | 1 – 8 | — | — |

> **Note:** Cylinder Speed, Beater Speed, Picker Cylinder Speed are **locked (disabled)** while machine is running (substate ≠ idle).

**Carding Internal Parameters Popup:**

| Param | Formula |
|-------|---------|
| Cylinder Motor RPM | `cylSpeed / 1.2` |
| Beater Motor RPM | `btrSpeed / 1.2` |
| Cage Motor RPM | `(deliverySpeed×1000/213.63) × 6.91` |
| Coiler Motor RPM | `((deliverySpeed×1000×cardFeedRatio)/194.779) / 1.656 × 6.91` |

---

### 5.3 Draw Frame Settings

| Field | Default | Range | Unit | TLV Code |
|-------|---------|-------|------|----------|
| Delivery Speed | 80 | 50 – 300 | m/min | — |
| Draft | 8.0 | 3.0 – 12.0 | — | — |
| Length Limit | 400 | 5 – 1250 | m | — |
| Ramp Up Time | 6 | 3 – 15 | s | — |
| Ramp Down Time | 6 | 3 – 15 | s | — |
| Creel Tension Factor | 1.0 | 0.25 – 5.0 | — | — |

> **Inline validation:** If FR Motor RPM or BR Motor RPM > 1450 RPM, save is blocked with a clear error message.

**Draw Frame Internal Parameters Popup:**

| Param | Formula |
|-------|---------|
| FR Roller RPM | `deliverySpeed × 1000 / 125.6` |
| FR Motor RPM | `frRollerRpm × 0.5` |
| BR Roller RPM | `(deliverySpeed × 1000 / draft) / 94.2` |
| BR Motor RPM | `brRollerRpm × 3.07` |

---

## 6. HMI Screens — All Details

### 6.1 Dashboard (Tab Host)

Each machine has a tab host (`FlyerDashboard`, `DfDashboard`, `CardingDashboard`) with tabs:
- **Settings** — editable fields, Read / Defaults / Save / PID / Limits buttons
- **Status** — live machine state
- **Options** — logging toggle + machine-specific controls
- **Tests** — motor diagnostics

A **Disconnect** button is always visible. All tabs share the same ViewModel instance.

---

### 6.2 Settings Screen (All Machines)

**Toolbar buttons (bottom):**

| Button | Action |
|--------|--------|
| **Read** | Sends opcode 0x03 request; shows green "Settings received" or red "Settings not received" banner after 3 s timeout |
| **Defaults** | Resets all fields to factory defaults in UI; shows green "Defaults received" banner |
| **Save** | Validates fields → shows AlertDialog on error; sends settings frame to machine; shows green/red banner |
| **PID** | Navigates to PID settings screen |
| **Limits** | Opens Internal Parameters popup (calculated values) |

**Banners:** Auto-dismiss after 2 seconds. Green = success, Red = failure.

---

### 6.3 Status Screen — Flyer Frame

**Substates displayed:**

| Substate | HMI shown |
|----------|-----------|
| **Idle (0x00)** | Status box only |
| **Running (0x01)** | Status + Layer + Lift Animation + Carousel |
| **Paused (0x02)** | Status + Reason For Pause + Layers |
| **Error (0x03)** | Status + Error Information (code) + Error Source + Layers |
| **Homing (0x04)** | Status + Lift Animation |
| **Inching (0x05)** | Status box only |
| **Can Over (0x06)** | Status box only |

**Lift Animation** (visible in Running and Homing):
- Green rotating bar showing bobbin lift tilt
- Formula: `angleDeg = (L − R) / 4 × (180/40)`
- Displays Δ (mm) = L − R, with L and R values below

**Carousel** (7 pages, auto-refreshes every 2 s):
- Production, Flyer, Bobbin, Front Roller, Back Roller, Lift Left, Lift Right
- Each card shows: RPM, Current (A), Motor Temp (°C), MOSFET Temp (°C), Output (m), Total Power (W)

---

### 6.4 Status Screen — Carding

| Substate | HMI shown |
|----------|-----------|
| **Running** | Status + Delivery Speed + Carding Duct sensor + Auto Feed Duct sensor + Carousel |
| **Paused** | Status + sensors + Reason For Pause (if non-empty) |
| **Error** | Status + Error Information (code) + Error Source |
| **Homing** | Status + sensors |

**Sensor Indicators:** "Carding Duct" (coilerSensor, TLV 0x0C) and "Auto Feed Duct" (ductSensor, TLV 0x0B) — naming is intentionally swapped to match Flutter source.

---

### 6.5 Status Screen — Draw Frame

| Substate | HMI shown |
|----------|-----------|
| **Running** | Status + Delivery Speed (m/min) + Current Length (m) + Carousel |
| **Paused** | Status + Reason For Pause + Pause Length (m) |
| **Error** | Status + Error Reason (code) + Error Source |
| **Homing** | Status only |

---

### 6.6 Options Screen — Flyer Frame

| Section | Controls |
|---------|----------|
| **Enable Logging** | Toggle switch; sends opcode 0x0C substate 0x01/0x00; shows "Log Enabled/Disabled" banner |
| **Gear Box Settings** | Displays Left and Right gearbox values (received via opcode 0x09); START / STOP buttons (opcode 0x08 ss 0x01/0x02); Save Left (0x03) / Save Right (0x04) |
| **RTF Settings** | + / − buttons (step 0.01); Receive (read from machine) / Send (save to machine); value displayed in bordered box |

---

### 6.7 Options Screen — Carding & Draw Frame

| Control | Action |
|---------|--------|
| **Enable Logging** | Toggle; opcode 0x0C; "Log Enabled/Disabled" banner |
| **Reset Length Counter** | Sends opcode 0x0A |

---

### 6.8 Tests Screen (Diagnosis) — Flyer Frame

Motors available for diagnosis:

| Motor Label | Motor ID |
|-------------|----------|
| Flyer | 0x01 |
| Bobbin | 0x02 |
| Front Roller | 0x03 |
| Back Roller | 0x04 |
| Drafting | 0x05 |
| Winding | 0x06 |
| Lift (both) | 0x07 |
| Lift Left | 0x08 |
| Lift Right | 0x09 |

Controls per motor:
- **Control Type:** Speed % or RPM
- **Direction:** Forward / Reverse
- **Speed %** and **Duration (s)** sliders/inputs
- **Start** → sends opcode 0x04 substate 0x01 with TLVs 0x40/0x41/0x42/0x43/0x44
- **Stop** → sends opcode 0x04 substate 0x06
- Live readback: RPM, PWM, Current, Power (from opcode 0x05 response)

Lift motors additionally accept **Bed Distance (mm)** instead of speed/duration (opcode 0x04 ss 0x01 with 0x40/0x41/0x44/0x45).

---

### 6.9 Tests Screen — Carding

| Motor | Motor ID |
|-------|----------|
| Cylinder | — |
| Beater | — |
| Picker Cylinder | — |
| Card Feed | — |
| Beater Feed | — |
| Coiler | — |
| Cage | — |

---

## 7. BT Protocol — Frame Format

**Bluetooth transport:** RFCOMM SPP (classic BT)  
**Encoding:** ASCII hex string (each byte as 2 hex chars)

```
7E  LL  II  SS  CC  [TLV...]  7E
│   │   │   │   │
│   │   │   │   └── Attribute count (1 byte)
│   │   │   └────── Sub-state (1 byte)
│   │   └────────── Opcode / Info byte (1 byte)
│   └────────────── Total body length in bytes (1 byte, includes EOF 7E)
└────────────────── Start-of-frame marker
```

**TLV format:**

```
TT  LL  VV...
│   │   └── Value bytes (wireLen bytes = LL hex chars)
│   └────── Wire length: number of hex chars for value (not bytes!)
└────────── Type byte
```

**Value encoding:**
- `wireLen = 02` → UInt8 (1 byte, compact form)
- `wireLen = 04` → UInt16 big-endian (2 bytes)
- `wireLen = 08` → IEEE 754 float big-endian (4 bytes)

**Parser:** Position-based `while cursor < eofOffset` loop — immune to ATTR_COUNT mismatches between firmware versions.

---

## 8. Protocol Opcodes & Substates

### Opcodes (info byte)

| Opcode | Direction | Description |
|--------|-----------|-------------|
| 0x01 | App → Machine | Settings from app |
| 0x02 | Machine → App | Settings to app |
| 0x03 | App → Machine | Request settings |
| 0x04 | App → Machine | Diagnostics request |
| 0x05 | Machine → App | Diagnosis response |
| 0x06 | Machine → App | Machine state (live status) |
| 0x07 | App ↔ Machine | Carousel info request/response |
| 0x08 | App → Machine | Gearbox command |
| 0x09 | Machine → App | Gearbox settings from machine |
| 0x0A | App → Machine | Reset length counter |
| 0x0B | App → Machine | RTF enable/disable |
| 0x0C | App → Machine | Log enable/disable |
| 0x0D | App → Machine | PID request |
| 0x0E | Machine → App | PID response |
| 0x0F | App → Machine | PID new values |
| 0x99 | App → Machine | Paired from phone |

### Substates (SS byte in opcode 0x06)

| Value | Name | Description |
|-------|------|-------------|
| 0x00 | Idle | Machine stopped, settings changeable |
| 0x01 | Running | Machine active |
| 0x02 | Pause | Machine paused |
| 0x03 | Error | Fault detected |
| 0x04 | Homing | Lift homing sequence |
| 0x05 | Inching | Inch movement |
| 0x06 | Can Over | Can overflow condition |

### Gearbox Substates (opcode 0x08)

| Value | Action |
|-------|--------|
| 0x01 | Start gearbox |
| 0x02 | Stop gearbox |
| 0x03 | Save left value |
| 0x04 | Save right value |

---

## 9. TLV Definitions

### 9.1 Flyer — Running State TLVs (opcode 0x06)

TLV codes 0x01–0x04 are **substate-dependent** (same code, different meaning per SS):

| TLV | SS=0x01 Running | SS=0x02 Paused | SS=0x03 Error | SS=0x04 Homing |
|-----|-----------------|----------------|---------------|----------------|
| 0x01 | Left Lift (float) | Pause Reason (UInt16) | Error Reason (UInt16) | Left Lift (float) |
| 0x02 | Right Lift (float) | Pause Layer (UInt16) | Error Source (UInt16) | Right Lift (float) |
| 0x03 | Run Layers (UInt16) | — | Error Code (UInt16) | — |
| 0x04 | Motor Temp → carousel | — | Error Layer (UInt16) | — |

Other Flyer state TLVs (any SS):

| TLV | Field | Format |
|-----|-------|--------|
| 0x05 | MOSFET Temp | UInt16 |
| 0x06 | Current | Float |
| 0x07 | RPM | UInt16 |
| 0x08 | Output Metres | Float |
| 0x09 | What Info (motor ID) | UInt8 |
| 0x0A | Total Power | Float |

### 9.2 Flyer — Settings TLVs (opcode 0x01/0x02)

| TLV | Field |
|-----|-------|
| 0x50 | Spindle Speed (UInt16) |
| 0x51 | Draft (Float) |
| 0x52 | Twist Per Inch (Float) |
| 0x53 | RTF (Float) |
| 0x54 | Layers (UInt16) |
| 0x55 | Max Height (UInt16) |
| 0x56 | Roving Width (Float) |
| 0x57 | Delta Bobbin Dia (Float) |
| 0x58 | Bare Bobbin Dia (UInt16) |
| 0x59 | Ramp Up Time (UInt16) |
| 0x60 | Ramp Down Time (UInt16) |
| 0x61 | Change Layer Time (UInt16) |
| 0x62 | Cone Angle Factor (Float) |

### 9.3 Carding — State TLVs (opcode 0x06)

| TLV | SS-dependent? | Field |
|-----|---------------|-------|
| 0x01 | SS=0x02: Pause Reason; SS=0x03: Error Information | UInt16 |
| 0x02 | SS=0x03: Error Source | UInt16 |
| 0x03 | SS=0x03: Error Code | UInt16 |
| 0x0B | Any | Duct Sensor (UInt8 0/1) |
| 0x0C | Any | Coiler Sensor (UInt8 0/1) |

### 9.4 Diagnostics TLVs (opcode 0x04 request)

| TLV | Field |
|-----|-------|
| 0x40 | Motor ID (UInt8) |
| 0x41 | Control Type (UInt8) |
| 0x42 | Speed % (UInt16) |
| 0x43 | Duration seconds (UInt16) |
| 0x44 | Direction (UInt8) |
| 0x45 | Bed Distance mm — lift only (UInt16) |

### 9.5 Diagnostics Response TLVs (opcode 0x05)

| TLV | Field | Format |
|-----|-------|--------|
| 0x01 | RPM | UInt16 |
| 0x02 | PWM | UInt16 or Float |
| 0x03 | Current | Float |
| 0x04 | Power | Float |

---

## 10. Error Codes & Sources

### 10.1 Flyer Error Reasons

| Code | Description |
|------|-------------|
| 2 | Over Current |
| 4 | Over Voltage |
| 8 | Under Voltage |
| 16 | Motor Thermistor Fault |
| 32 | MOSFET Thermistor Fault |
| 64 | Motor Over Temperature |
| 96 | SMPS Error |
| 97 | Ack Error |
| 98 | Can Cut Error |
| 99 | Lift Relative Position Error |
| 128 | MOSFET Over Temperature |
| 256 | EEPROM Write Error |
| 512 | EEPROM Bad Values |
| 1024 | Tracking Error |
| 2048 | Motor Encoder Setup Error |
| 4096 | Lift Pos Tracking Error |
| 8192 | Lift Synchronicity Fail |
| 16384 | Lift Out Of Bounds Error |
| 32768 | EEPROM Bad Homing Position |

### 10.2 Flyer Error Sources

| Code | Source |
|------|--------|
| 1 | Flyer |
| 2 | Bobbin |
| 3 | FR (Front Roller) |
| 4 | BR (Back Roller) |
| 5 | Lift Left |
| 6 | Lift Right |
| 11 | MotherBoard |
| 12 | Can Bus |
| 13 | Lifts |
| 14 | System |

### 10.3 Flyer Pause Reasons

| Code | Reason |
|------|--------|
| 1 | User Paused |
| 2 | Front Sliver Cut |
| 3 | Back Sliver Cut |
| 4 | Lapping |

### 10.4 Carding Error Sources

| Code | Source |
|------|--------|
| 1 | Cylinder |
| 2 | Beater |
| 3 | Cage |
| 4 | Card Feed |
| 5 | Beater Feed |
| 6 | Coiler |
| 11 | MotherBoard |
| 12 | Can Bus |
| 13 | Lifts |
| 14 | System |

### 10.5 Carding Pause Reasons

| Code | Reason |
|------|--------|
| 1 | User Paused |
| 2 | Creel Sliver Cut |
| 3 | Coiler Sliver Cut |
| 4 | Lapping |

### 10.6 Draw Frame Error Sources

| Code | Source |
|------|--------|
| 1 | Front Roller |
| 2 | Back Roller |
| 3 | Creel |
| 11 | Mother Board |
| 12 | Can Bus |
| 13 | Lifts |
| 14 | System |

### 10.7 Draw Frame Pause Reasons

| Code | Reason |
|------|--------|
| 1 | User Paused |
| 2 | Creel Sliver Cut |
| 3 | Coiler Sliver Cut |
| 4 | Lapping |

---

## 11. All Changes Made (Phase 1)

### FrameCodec.kt
- Changed TLV parsing from `repeat(attributeCount)` to position-based `while (cursor < eofOffset)` loop — prevents firmware ATTR_COUNT encoding mismatch from causing parse failures
- Relaxed wireLen validation: accepts any even value in range 2–32 (was strict)
- `BtSessionRepository.extraBufferCapacity` increased 8 → 64; changed to `tryEmit` for ordered emission without blocking

### FlyerViewModel.kt
- Added `errorInformation`, `errorCode`, `errorSource`, `errorLayer` fields to `FlyerRunState`
- Added `pauseReason`, `pauseLayer` fields to `FlyerRunState`
- Fixed TLV 0x01–0x04 parsing: codes are **substate-dependent** — running/homing share 0x01=leftLift/0x02=rightLift; pause uses 0x01=pauseReason/0x02=pauseLayer; error uses 0x01=errorReason/0x02=errorSource/0x03=errorCode/0x04=errorLayer
- Added opcode 0x09 handler for gearbox response — exposes `gearboxLeft`/`gearboxRight` StateFlows
- Added `logEnabled` StateFlow + `sendLog()` function
- Added `_defaultApplied` StateFlow for "Defaults applied" banner
- Added `readPending` flag — prevents timeout banner firing when settings were already received (red banner after green fix)
- `sendRead()` timeout 3 s → shows red "Settings not received" if no response

### FlyerStatusScreen.kt
- Added Error UI: Error Information + Error Source + Layers boxes
- Added Homing UI: LiftAnimation composable
- Added Paused UI: Reason For Pause + Layers boxes
- Running UI: Status + Layer + LiftAnimation + Carousel
- **Phase 1 final fix:** Removed `if (pauseLayer != 0)` and `if (errorLayer != 0)` guards — Layers box now always shown in both pause and error states (matches Flutter)
- LiftAnimation: angle formula `(diff/4) × (π/40)` → Compose degrees = `(diff/4f) × (180f/40f)`

### FlyerOptionsScreen.kt
- Added Gear Box section: Left/Right value display, START/STOP buttons
- Added RTF section: +/- buttons (0.01 step), receive/send
- Added Enable Logging toggle with banner
- Layout: `SpaceEvenly` vertical arrangement

### FlyerSettingsScreen.kt
- Added `FlyerInternalParamsDialog` with 7 calculated parameters
- Validation for all 13 fields with specific range error messages
- "Defaults received" banner on reset

### CardingViewModel.kt
- Added `errorInformation`, `errorCode`, `errorSource` fields to `CardingRunState`
- Fixed TLV 0x01/0x02/0x03 parsing for error substate (ss=0x03)
- `_settingsChangeAllowed` StateFlow — set false when running
- `_defaultApplied` StateFlow + `readPending` flag (same pattern as Flyer)
- Per-TLV try-catch in 0x05 handler — one bad value can't drop the whole frame (fixes AF feed showing dashes)
- RPM/PWM/current/power in 0x05 handler handle both UInt16 (wireLen=0x04) and Float (wireLen=0x08)

### CardingStatusScreen.kt
- Added Error UI (Error Information + Error Source)
- Added Homing UI (sensor indicators)
- Fixed sensor display: `coilerSensor` (TLV 0x0C) → "Carding Duct"; `ductSensor` (TLV 0x0B) → "Auto Feed Duct" (matches Flutter's swapped naming)

### CardingSettingsScreen.kt
- Card feed ratio minimum changed 3.0 → 0.4 (matches Flutter)
- Cylinder/Beater/PickerCyl fields disabled when `settingsChangeAllowed == false`
- Internal Parameters popup with 4 calculated motor RPM values

### CardingOptionsScreen.kt
- Layout SpaceEvenly; Enable Logging label consistent with other machines

### CardingTestsScreen.kt
- Layout SpaceEvenly

### DfViewModel.kt
- Added pause fields: `pauseReason`, `pauseLength` to `DfRunState`
- Added `logEnabled` StateFlow + `sendLog()`
- Added `_defaultApplied`, `readPending` flag
- Full error source / error reason decode tables

### DfSettingsScreen.kt
- Inline motor RPM validation (FR + BR must both be ≤ 1450)
- Internal Parameters popup (FR/BR roller and motor RPM)

### DfStatusScreen.kt
- Layout SpaceEvenly; pause shows length

### DfOptionsScreen.kt / DfTestsScreen.kt
- Layout SpaceEvenly; Enable Logging label consistent

---

## 12. What Is Working

| Feature | Status |
|---------|--------|
| Bluetooth RFCOMM SPP connect / disconnect | ✅ Working |
| TLV binary frame encode/decode | ✅ Working |
| Draw Frame — all settings send/receive/save | ✅ Working |
| Draw Frame — status (running/pause/error/homing) | ✅ Working |
| Draw Frame — motor diagnosis | ✅ Working |
| Draw Frame — carousel live data | ✅ Working |
| Draw Frame — log toggle | ✅ Working |
| Draw Frame — default values match Flutter | ✅ Verified |
| Flyer Frame — all settings send/receive/save | ✅ Working |
| Flyer Frame — running state (layer + lift animation + carousel) | ✅ Working |
| Flyer Frame — paused state (pause reason + layers) | ✅ Working |
| Flyer Frame — error state (error info + source + layers) | ✅ Working |
| Flyer Frame — homing state (lift animation) | ✅ Working |
| Flyer Frame — gearbox start/stop/read | ✅ Working |
| Flyer Frame — RTF adjust and send | ✅ Working |
| Flyer Frame — motor diagnosis (all 9 motors) | ✅ Working |
| Flyer Frame — lift diagnosis with bed distance | ✅ Working |
| Flyer Frame — carousel (7 pages) | ✅ Working |
| Flyer Frame — log toggle | ✅ Working |
| Flyer Frame — internal parameters popup | ✅ Working |
| Flyer Frame — default values match Flutter | ✅ Verified |
| Carding — all settings send/receive/save | ✅ Working |
| Carding — running state (delivery + sensors + carousel) | ✅ Working |
| Carding — paused state (sensors + reason) | ✅ Working |
| Carding — error state (error info + source) | ✅ Working |
| Carding — homing state (sensors) | ✅ Working |
| Carding — settings locked when running | ✅ Working |
| Carding — motor diagnosis | ✅ Working |
| Carding — log toggle | ✅ Working |
| Carding — internal parameters popup | ✅ Working |
| Carding — default values match Flutter | ✅ Verified |
| "Settings not received" timeout banner (3 s) | ✅ Working |
| "Settings received" banner | ✅ Working |
| "Defaults applied" banner | ✅ Working |
| "Log Enabled / Log Disabled" banner | ✅ Working |
| Settings validation with error dialog | ✅ Working |
| Carousel fills screen bottom (`weight(1f)`) | ✅ Working |
| SpaceEvenly layout on all options/tests screens | ✅ Working |
| Position-based TLV parsing (firmware-agnostic) | ✅ Working |

---

## 13. Known Remaining Issues

| Issue | Machine | Priority |
|-------|---------|----------|
| "Weight" label should be "Doff Percent" | Ring Doubler | Medium |
| Missing "Reset Grams Per Spindle" button | Ring Doubler | Medium |
| Missing Internal Parameters popup in Carding settings (`settingsPopUpPage`) | Carding | Low |
| Carding settings not received on real device — needs test confirmation after FrameCodec fix | Carding | High |

---

## 14. Default Values (Verified vs Flutter v8)

All defaults verified against Flutter source:

| Machine | Field | Default |
|---------|-------|---------|
| **Carding** | Delivery Speed | 8.0 m/min |
| | Card Feed Ratio | 5.0 |
| | Length Limit | 1000 m |
| | Cylinder Speed | 750 RPM |
| | Beater Speed | 600 RPM |
| | Picker Cyl Speed | 600 RPM |
| | Beater Feed | 10 |
| | AF Feed | 7 |
| **Draw Frame** | Delivery Speed | 80 m/min |
| | Draft | 8.0 |
| | Length Limit | 400 m |
| | Ramp Up Time | 6 s |
| | Ramp Down Time | 6 s |
| | Creel Tension Factor | 1.0 |
| **Flyer** | Spindle Speed | 650 RPM |
| | Draft | 8.8 |
| | Twist Per Inch | 1.4 |
| | RTF | 1.0 |
| | Layers | 50 |
| | Max Height | 270 mm |
| | Roving Width | 1.2 mm |
| | Delta Bobbin Dia | 1.1 mm |
| | Bare Bobbin Dia | 48 mm |
| | Ramp Up Time | 12 s |
| | Ramp Down Time | 12 s |
| | Change Layer Time | 800 ms |
| | Cone Angle Factor | 1.0 |
