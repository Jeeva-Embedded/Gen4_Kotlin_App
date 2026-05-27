# Phase 2 Corrections — Gen4 Spinning Kotlin Port

Generated: 2026-04-27  
Last updated: 2026-05-27  
Scope: Gaps found by comparing the Kotlin port against the original Flutter source.

---

## Priority 1 — Functional Bugs (must fix before release)

### 1. Substate Enum Values — All 4 Machines ✅ FIXED
**File:** All `XyzStatusScreen.kt` — `substateLabel()` helper  
**Fix applied:** All `substateLabel()` functions corrected: Idle=0x00, Running=0x01, Paused=0x02, Error=0x03, Homing=0x04.

---

### 2. Missing Error / Pause / Homing Sub-UI — All 4 Machines ✅ FIXED
**File:** `CardingStatusScreen.kt`, `DfStatusScreen.kt`, `FlyerStatusScreen.kt`, `RingStatusScreen.kt`  
**Fix applied:** All 4 status screens now show sub-UI blocks for Error (errorInformation + errorSource), Pause (pauseReason + related fields), and Homing states.

---

### 3. Missing DrawFrame Carousel ✅ FIXED
**File:** `DfCarousel.kt` (new), `DfStatusScreen.kt`  
**Fix applied:** `DfCarousel.kt` created with 4 pages: Production(0x0A), Front Roller(0x01), Back Roller(0x02), Creel(0x03). LaunchedEffect sends carousel requests. Wired in `DfStatusScreen`.

---

### 4. Ring "Doff Percent" Label Wrong ✅ FIXED
**File:** `RingStatusScreen.kt`  
**Fix applied:** Label changed from "Weight" to "Doff Percent".

---

### 5. Missing Ring "Reset Grams Per Spindle" Button
**File:** `RingOptionsScreen.kt` or `RingSettingsScreen.kt`  
**Problem:** Flutter has a button that sends opcode `0x0A` to reset grams-per-spindle counter. Not present in Kotlin.  
**Fix:** Add a `GradientButton("Reset Grams/Spindle") { vm.sendResetGrams() }` and add `fun sendResetGrams()` to `RingViewModel` sending `FrameCodec.build(0x0Au, 0x00u, emptyList())`.

---

## Priority 2 — UI / UX Gaps (fix for production quality)

### 6. Missing Carding Internal Parameters Popup ⏳ PENDING
**File:** `CardingSettingsScreen.kt`  
**Problem:** Flutter has a popup page (`settingsPopUpPage.dart`) that calculates and displays 6 derived motor RPMs from the entered settings (Cylinder RPM, Doffer RPM, Flat speed, Licker-in RPM, etc.). Kotlin has no equivalent.  
**Fix:** Add a `showInternalParams` boolean state + `AlertDialog` or `BottomSheet` composable that computes and displays derived values. Formulas are in `settingsPopUpPage.dart`.

---

### 7. Ring Lift Animation (Tilting Bar Visual) — PARTIAL ✅
**File:** `RingStatusScreen.kt`, `FlyerStatusScreen.kt`  
**Status:** `LiftAnimation` composable created in `FlyerStatusScreen.kt` for homing state (tilting bar). Ring has lift in carousel but no tilt bar in status screen. Full Ring tilt bar still pending.

---

### 8. Sensor Labels — "Open / Blocked" Text ✅ FIXED
**File:** `CardingStatusScreen.kt`  
**Fix applied:** Carding running/paused/homing states now show two full-width StatusBox rows — "Carding Duct" and "Auto Feed Duct" — with value text "Open" (green) or "Blocked" (red).

---

### 9. Drawer Menu — Log Files + Exit ✅ FIXED
**File:** All 4 Dashboard composables + `SelectMachineScreen.kt`  
**Fix applied:** `ModalNavigationDrawer` added to all machine dashboards with hamburger icon in AppBar. Drawer items: "Log Files" (navigates to log viewer) + "Exit App". SelectMachineScreen also has hamburger → "Log Files" drawer.

---

## Priority 3 — Polish (nice to have)

### 10. Warnings Seen During Build (non-fatal)
| Warning | File | Fix |
|---------|------|-----|
| `BluetoothAdapter.getDefaultAdapter()` deprecated | `Gen4SpinningApp.kt:16` | Use `bm.adapter` only (already the primary call, `?: getDefaultAdapter()` is the fallback — remove the fallback) |
| `bluetoothAdapter.disable()` deprecated | `BluetoothScreen.kt:120` | Remove or guard with `Build.VERSION.SDK_INT < 33` |
| `Variable 'machine' is never used` | `MainActivity.kt:97` | Rename to `_` or remove |

---

## Phase 2 — New Features Added (2026-05-22 to 2026-05-27)

### A. AL Settings — Draw Frame ✅
Full BT round-trip for auto-leveller settings: GET (0x13) / SAVE (0x11) / response (0x12).
Firmware changes: ISR staging parse, SettingsState reload, CAN identifier fix, motor ACK.

### B. AL Sensor Calibration — Draw Frame ✅
`DfOptionsScreen` calibration flow: START → firmware runs open-loop at 450 duty → result arrives via INFO 0x15 TLVs → shows average ADC value + sample count.

### C. CSV Logging — All 4 Machines ✅
- Log interval: 1 second (was 5 seconds)
- `logFetchJob` background coroutine cycles through ALL motor IDs at 400ms intervals while logging is active, ensuring all motors are logged from the first second even if the carousel page was never visited
- `writeLogSnapshot()` iterates the full motor list, using `emptyMap()` for motors not yet responded
- Logging pauses carousel requests automatically during diagnosis to avoid BT queue interference

### D. Running Screen Layout — All 4 Machines ✅
- Status boxes (natural height) at top with `spacedBy(6.dp)` between boxes
- StatusBox vertical padding 6 → 14dp (slightly taller boxes)
- Fixed 3cm gap (114dp Spacer) between status section and carousel
- Carousel card +2cm height (76dp Spacer at bottom of each card's Column)
- 1cm gap (38dp Spacer) at bottom below carousel
- Layout matches Flutter's `spaceAround` + compact top section style

### E. Diagnosis Fix — All 4 Machines ✅
Root cause: `logFetchJob` pumped carousel request frames (0x07) into BT tx queue every 400ms, interfering with diagnosis start/stop commands.
- `BtSessionRepository.drainTxQueue()` added to flush pending frames
- `sendDiagnostic()`: drains queue before sending start command
- `sendStopDiagnosis()`: drains queue then sends stop 3× for reliability
- `logFetchJob`: checks `isDiagnosing` each cycle and skips carousel requests during active diagnosis

---

## Already Fixed in Phase 1
- Gearbox substate opcodes corrected (Stop=0x02, SaveLeft=0x03, SaveRight=0x04) ✓
- `ViewModelProvider.Factory` anonymous object pattern (was using wrong DSL) ✓
- `HorizontalDivider` replacing deprecated `Divider` ✓
- `local.properties` forward-slash path format ✓
- `gradle.properties` with `android.useAndroidX=true` ✓
- BtSessionRepository inline-lambda `break/continue` refactored ✓
- Launcher icon pointing to `@drawable/logo` ✓

---

## File Inventory — Phase 1 Complete

```
app/src/main/kotlin/com/gen4/spinning/
├── Gen4SpinningApp.kt
├── MainActivity.kt
├── core/
│   ├── bt/
│   │   ├── BtFrame.kt
│   │   ├── BtSessionRepository.kt
│   │   ├── ConnectionState.kt
│   │   └── FrameCodec.kt
│   └── protocol/
│       ├── CardingProtocol.kt
│       ├── DfProtocol.kt
│       ├── FlyerProtocol.kt
│       └── RingProtocol.kt
├── machines/
│   ├── carding/
│   │   ├── CardingCarousel.kt
│   │   ├── CardingDashboard.kt
│   │   ├── CardingOptionsScreen.kt
│   │   ├── CardingSettingsScreen.kt
│   │   ├── CardingStatusScreen.kt
│   │   ├── CardingTestsScreen.kt
│   │   └── CardingViewModel.kt
│   ├── df/
│   │   ├── DfDashboard.kt
│   │   ├── DfOptionsScreen.kt
│   │   ├── DfSettingsScreen.kt
│   │   ├── DfStatusScreen.kt      ← missing carousel (see item 3)
│   │   ├── DfTestsScreen.kt
│   │   └── DfViewModel.kt
│   ├── flyer/
│   │   ├── FlyerDashboard.kt
│   │   ├── FlyerOptionsScreen.kt
│   │   ├── FlyerSettingsScreen.kt
│   │   ├── FlyerStatusScreen.kt
│   │   ├── FlyerTestsScreen.kt
│   │   └── FlyerViewModel.kt
│   └── ring/
│       ├── RingDashboard.kt
│       ├── RingOptionsScreen.kt
│       ├── RingSettingsScreen.kt
│       ├── RingStatusScreen.kt    ← missing lift animation + Doff label (see items 4, 7)
│       ├── RingTestsScreen.kt
│       └── RingViewModel.kt
├── shared/
│   └── PidScreen.kt
└── ui/
    ├── components/
    │   └── SharedComponents.kt
    ├── screens/
    │   ├── BluetoothScreen.kt
    │   ├── SelectDeviceScreen.kt
    │   ├── SelectMachineScreen.kt
    │   └── SplashScreen.kt
    └── theme/
        ├── Color.kt
        └── Theme.kt
```
