# Phase 2 Corrections — Gen4 Spinning Kotlin Port

Generated: 2026-04-27  
Scope: Gaps found by comparing the Kotlin port against the original Flutter source.

---

## Priority 1 — Functional Bugs (must fix before release)

### 1. Substate Enum Values — All 4 Machines
**File:** All `XyzStatusScreen.kt` — `substateLabel()` helper  
**Problem:** Kotlin has wrong numeric values for pause/error/homing states.  
**Flutter reference:** `lib/core/enums.dart`

| State   | Kotlin (wrong) | Flutter (correct) |
|---------|---------------|-------------------|
| Pause   | 0x03          | 0x02              |
| Error   | 0x04          | 0x03              |
| Homing  | 0x02          | 0x04              |

**Fix:** Update all `substateLabel()` functions in the 4 Status screens.

---

### 2. Missing Error / Pause / Homing Sub-UI — All 4 Machines
**File:** `CardingStatusScreen.kt`, `DfStatusScreen.kt`, `FlyerStatusScreen.kt`, `RingStatusScreen.kt`  
**Problem:** Flutter shows extra fields when machine is in Error, Pause, or Homing state — `errorInformation`, `errorCode`, `errorSource`, `pauseReason`. Kotlin shows nothing extra.  
**Fix:** Add a conditional block in each status screen that shows these fields when the substate matches.

---

### 3. Missing DrawFrame Carousel
**File:** `DfStatusScreen.kt`  
**Problem:** Flutter has a 4-page carousel for Draw Frame (PRODUCTION, Front Roller, Back Roller, Creel), same pattern as Carding's 9-page carousel. Kotlin has no carousel at all in DfStatusScreen.  
**Fix:** Create `DfCarousel.kt` mirroring `CardingCarousel.kt` with 4 pages. Add `snapshotFlow` trigger → `vm.sendCarouselRequest(motorId)`. Wire up in `DfStatusScreen`.  
**Motor IDs:** PRODUCTION=0x0A, FrontRoller=0x01, BackRoller=0x02, Creel=0x03

---

### 4. Ring "Doff Percent" Label Wrong
**File:** `RingStatusScreen.kt`  
**Problem:** The TLV field `weight` in `RingRunState` is displayed as "Weight" but Flutter labels it "Doff Percent".  
**Fix:** Change the status display label from `"Weight"` to `"Doff Percent"`.

---

### 5. Missing Ring "Reset Grams Per Spindle" Button
**File:** `RingOptionsScreen.kt` or `RingSettingsScreen.kt`  
**Problem:** Flutter has a button that sends opcode `0x0A` to reset grams-per-spindle counter. Not present in Kotlin.  
**Fix:** Add a `GradientButton("Reset Grams/Spindle") { vm.sendResetGrams() }` and add `fun sendResetGrams()` to `RingViewModel` sending `FrameCodec.build(0x0Au, 0x00u, emptyList())`.

---

## Priority 2 — UI / UX Gaps (fix for production quality)

### 6. Missing Carding Internal Parameters Popup
**File:** `CardingSettingsScreen.kt`  
**Problem:** Flutter has a popup page (`settingsPopUpPage.dart`) that calculates and displays 6 derived motor RPMs from the entered settings (Cylinder RPM, Doffer RPM, Flat speed, Licker-in RPM, etc.). Kotlin has no equivalent.  
**Fix:** Add a `showInternalParams` boolean state + `AlertDialog` or `BottomSheet` composable that computes and displays derived values. Formulas are in `settingsPopUpPage.dart`.

---

### 7. Ring Lift Animation (Tilting Bar Visual)
**File:** `RingStatusScreen.kt`  
**Problem:** Flutter draws a tilting bar graphic showing the left/right lift position difference visually. Kotlin shows no lift visual.  
**Fix:** Add a `Canvas`-based composable that draws a horizontal bar rotated by `(leftLift - rightLift)` angle. Values come from `RingRunState`.

---

### 8. Sensor Labels — "Open / Blocked" Text
**File:** `CardingStatusScreen.kt` (at minimum)  
**Problem:** Flutter shows "Open" or "Blocked" text next to the sensor indicator dot. Kotlin's `SensorIndicator` only colors the dot with no text label.  
**Fix:** Update `SensorIndicator` in `SharedComponents.kt` to accept an optional `statusText` param, or pass the open/blocked string as the label suffix. Update all callers.

---

### 9. Drawer Menu — Missing from All Machines
**File:** All 4 Dashboard composables (`CardingDashboard.kt`, `DfDashboard.kt`, `FlyerDashboard.kt`, `RingDashboard.kt`)  
**Problem:** Flutter has a hamburger drawer on every machine with two options:
  - "Change Device Name" → opens a dialog, saves a name string associated with the connected BT device MAC
  - "Exit App" → calls `SystemNavigator.pop()`  

Kotlin has no drawer at all.  
**Fix:**
  1. Add `drawerContent` to each `Scaffold` with two items.
  2. "Change Device Name": `AlertDialog` with a `TextField`, persist with `SharedPreferences` keyed by device MAC.
  3. "Exit App": `(context as? Activity)?.finish()` or `exitProcess(0)`.

---

## Priority 3 — Polish (nice to have)

### 10. Warnings Seen During Build (non-fatal)
| Warning | File | Fix |
|---------|------|-----|
| `BluetoothAdapter.getDefaultAdapter()` deprecated | `Gen4SpinningApp.kt:16` | Use `bm.adapter` only (already the primary call, `?: getDefaultAdapter()` is the fallback — remove the fallback) |
| `bluetoothAdapter.disable()` deprecated | `BluetoothScreen.kt:120` | Remove or guard with `Build.VERSION.SDK_INT < 33` |
| `Variable 'machine' is never used` | `MainActivity.kt:97` | Rename to `_` or remove |

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
