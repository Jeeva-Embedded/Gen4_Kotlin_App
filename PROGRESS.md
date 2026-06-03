# Gen4 Spinning Kotlin Port — Progress

## Status: ALL FILES COMPLETE. Ready to build.

---

## DONE (files created)

### Build files
- `settings.gradle`
- `build.gradle`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/themes.xml`

### Core BT (`core/bt/`)
- `BtFrame.kt` — BtFrame, Tlv, constants (SAVE_RESPONSE_SUCCESS/FAILURE, FSC strings)
- `ConnectionState.kt` — 6 states + timeout constants
- `FrameCodec.kt` — parse + build + TLV builders + buildPairedFromPhone/buildReqSettings/buildResetLengthCounter/buildCarouselRequest/buildDiagnostic/buildPidRequest
- `BtSessionRepository.kt` — socket lifecycle, RX/TX coroutines, watchdog, 6-state machine

### Protocol (`core/protocol/`)
- `CardingProtocol.kt` — all TLV codes, limits, buildSettingsFrame
- `DfProtocol.kt` — all TLV codes, limits, buildSettingsFrame
- `FlyerProtocol.kt` — all TLV codes, limits, buildSettingsFrame
- `RingProtocol.kt` — all TLV codes, limits, calcOutputYarnDia, buildSettingsFrame

### Theme (`ui/theme/`)
- `Color.kt` — SpinColors object
- `Theme.kt` — Gen4SpinningTheme (always light, lightGreen+blue)

### App skeleton
- `Gen4SpinningApp.kt` — Application singleton, creates BtSessionRepository from BluetoothManager
- `MainActivity.kt` — sealed Screen class + NavHost wiring all routes
- `ui/components/SharedComponents.kt` — GradientAppBar, GradientButton, StatusBox, SensorIndicator, FieldInput, SaveBanner, SettingsToolbar, DisconnectedScreen

### Entry flow screens (`ui/screens/`)
- `SplashScreen.kt` — white BG, logo fills screen, 2.5s delay, nav to SelectMachine
- `SelectMachineScreen.kt` — 2×2 gradient tile grid, 4 machines
- `BluetoothScreen.kt` — BT enable switch, Paired Devices button, connection status text
- `SelectDeviceScreen.kt` — bondedDevices list, tap → repository.connect() + popBackStack

### Carding machine (`machines/carding/`)
- `CardingViewModel.kt` — 8-field settings StateFlow, runState (substate/deliveryMtrsPerMin/ductSensor/coilerSensor), carouselData map, saveResult; collects inboundFrames opcode 06/02/07
- `CardingDashboard.kt` — AppBar "Blow Card", disconnect button, 4-tab BottomNav
- `CardingStatusScreen.kt` — substate label, running: delivery speed + 2 SensorIndicators + CardingCarousel
- `CardingCarousel.kt` — HorizontalPager 9 pages (PRODUCTION + 8 motors), dot indicator, sends buildCarouselRequest on page change
- `CardingSettingsScreen.kt` — 8 FieldInputs, SaveBanner, SettingsToolbar (All button enabled when idle)
- `CardingTestsScreen.kt` — Motor/ControlLoop/Direction dropdowns, speed+duration sliders, RUN DIAGNOSE
- `CardingOptionsScreen.kt` — Gearbox Start/Stop/SaveLeft/SaveRight, RTF toggle, Log toggle

### Draw Frame machine (`machines/df/`)
- `DfViewModel.kt` — 6-field settings, runState (substate/deliveryMtrsPerMin/currentLength)
- `DfDashboard.kt` — AppBar "Draw Frame", 4-tab BottomNav
- `DfStatusScreen.kt` — substate label, running: delivery speed + length boxes
- `DfSettingsScreen.kt` — 6 FieldInputs, SettingsToolbar (no All button)
- `DfTestsScreen.kt` — 4 motors (Front/Back Roller/Creel/Drafting), same controls as Carding
- `DfOptionsScreen.kt` — Gearbox + RTF + Log

### Flyer machine (`machines/flyer/`)
- `FlyerViewModel.kt` — 13-field settings, runState (substate/leftLift/rightLift/layers)
- `FlyerDashboard.kt` — AppBar "Flyer Frame", 4-tab BottomNav
- `FlyerStatusScreen.kt` — substate label, running: leftLift/rightLift/layers boxes
- `FlyerSettingsScreen.kt` — 13 FieldInputs, SettingsToolbar
- `FlyerTestsScreen.kt` — 9 motors; lift motors show LiftCommand(Up/Down/Stop)+BedDistance slider instead of Direction; Reset Length Counter button
- `FlyerOptionsScreen.kt` — Gearbox + RTF + Log

### Ring Doubler machine (`machines/ring/`)
- `RingViewModel.kt` — 8-field settings; outputYarnDia auto-computed via calcOutputYarnDia if blank; runState (substate/weight)
- `RingDashboard.kt` — AppBar "Ring Doubler", 4-tab BottomNav
- `RingStatusScreen.kt` — substate label, weight StatusBox
- `RingSettingsScreen.kt` — 8 FieldInputs (outputYarnDia hint: blank=auto), SettingsToolbar
- `RingTestsScreen.kt` — 4 motors (Calender/Lift/LiftLeft/LiftRight); lift motors show LiftCommand+BedDistance
- `RingOptionsScreen.kt` — Gearbox + RTF + Log

### Shared
- `shared/PidScreen.kt` — Kp/Ki/FF/SO fields, Request PID (opcode 0D) + Send PID (opcode 0F) buttons; receives opcode 0E to populate fields

---

## PHASE 2 ADDITIONS (2026-05-22 to 2026-05-27)

### Fixed Phase 2 Corrections
- Substate labels corrected across all 4 machines ✅
- Error / Pause / Homing sub-UI added to all 4 status screens ✅
- `DfCarousel.kt` created (4 pages: Production, Front Roller, Back Roller, Creel) ✅
- Ring "Doff Percent" label ✅
- Carding sensor "Open/Blocked" StatusBox rows ✅
- ModalNavigationDrawer in all dashboards + SelectMachineScreen (Log Files + Exit) ✅

### New Features
- **AL Settings (Draw Frame):** GET (0x13) / SAVE (0x11) / response (0x12) full BT round-trip ✅
- **AL Calibration (Draw Frame):** open-loop motor run → ADC avg result → shows in DfOptionsScreen ✅
- **CSV Logging — all 4 machines:** 1s interval; logFetchJob cycles all motor IDs (200ms); all motors in log from first second ✅
- **Running screen layout:** compact status at top → fixed 3cm Spacer → carousel card (2cm taller) → 1cm bottom gap ✅
- **Diagnosis reliability:** `drainTxQueue()` before stop; stop sent 3×; logFetchJob pauses during diagnosis ✅
- **Diagnosis stop race fix (2026-06-03):** `stopAndClearDiagnosis()` — drain + 3×stop + 600ms delay; removed drain from `sendDiagnostic()` ✅
- **Carousel refresh rate (2026-06-03):** logFetchJob delay 400ms → 200ms across all 4 machines ✅
- **LogsScreen UX (2026-06-03):** tap row = open in Excel (ACTION_VIEW); green Download button = save to Downloads (MediaStore); Share button retained ✅

### New Files
| File | Description |
|---|---|
| `machines/df/DfCarousel.kt` | 4-page carousel for Draw Frame |
| `machines/df/DfAlSettingsScreen.kt` | AL settings GET/SAVE UI |

### Still Pending
- Ring: Reset Grams Per Spindle button (opcode 0x0A, `vm.sendResetGrams()`)
- Carding: Internal Parameters popup (derived RPM calculations from settings)
- Ring: Tilt bar lift animation in RingStatusScreen homing state (FlyerStatusScreen already has LiftAnimation)

---

## REMAINING (one item — from Phase 1)

- Add `app/src/main/res/drawable/logo.png` (or `logo.xml`) — SplashScreen references `R.drawable.logo`. Without this the project won't compile. Add any placeholder PNG/vector as `logo`.

---

## KEY FACTS

### Colors
- LightGreen = #8BC34A, Blue = #2196F3, white background
- AppBar gradient: Blue → LightGreen (left→right)
- BottomNav selected = LightGreen, unselected = grey

### Protocol
- PairedFromPhone = `7E08990000007E`
- RequestSettings = opcode 03
- SettingsFromApp = opcode 01 (substate 00=save, 01=update; Carding only uses save=true/false; others always 01)
- MachineState = opcode 06
- CarouselInfo = opcode 07
- ResetLengthCounter = opcode 0A
- Gearbox from app = opcode 08 (substate 00=Stop, 01=Start, 02=SaveLeft, 03=SaveRight)
- RTF = opcode 0B (substate 01=on, 00=off)
- Log = opcode 0C (substate 01=on, 00=off)
- PID request = opcode 0D, PID response = opcode 0E, PID send = opcode 0F
- PID TLV codes: 0x01=Kp(float), 0x02=Ki(float), 0x03=FF(float), 0x04=SO(float)
- Diagnostics = opcode 04, Diagnostic response = opcode 05
- Save success = `7E017E`, Save failure = `7E007E`

### ViewModel factory pattern
Each machine dashboard uses `viewModel(factory = xyzVmFactory(repository))` where `xyzVmFactory` returns a `ViewModelProvider.Factory` via `initializer { XyzViewModel(repository) }`.

### Navigation
Top-level routes defined as sealed `Screen` objects in MainActivity.kt. All dashboards receive `repository` and two callbacks: `onDisconnect` and `onNavigatePid`.
