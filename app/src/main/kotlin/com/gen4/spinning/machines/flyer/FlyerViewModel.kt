package com.gen4.spinning.machines.flyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gen4.spinning.core.bt.FrameCodec
import com.gen4.spinning.core.bt.BtSessionRepository
import com.gen4.spinning.core.protocol.FlyerProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FlyerSettings(
    val spindleSpeed: String = "",
    val draft: String = "",
    val twistPerInch: String = "",
    val rtf: String = "",
    val layers: String = "",
    val maxHeight: String = "",
    val rovingWidth: String = "",
    val deltaBobbinDia: String = "",
    val bareBobbinDia: String = "",
    val rampUpTime: String = "",
    val rampDownTime: String = "",
    val changeLayerTime: String = "",
    val coneAngleFactor: String = "",
)

data class FlyerRunState(
    val substate: UByte = 0x00u,
    val leftLift: Float = 0f,
    val rightLift: Float = 0f,
    val layers: UShort = 0u,
    val pauseReason: String = "",
    val pauseLayer: UShort = 0u,
    val errorInformation: String = "",
    val errorCode: String = "",
    val errorSource: String = "",
    val errorLayer: UShort = 0u,
)

data class FlyerDiagnosisState(
    val isDiagnosing: Boolean = false,
    val motorLabel: String = "",
    val rpm: String = "-",
    val pwm: String = "-",
    val current: String = "-",
    val power: String = "-",
)

class FlyerViewModel(private val repository: BtSessionRepository) : ViewModel() {

    private val _settings = MutableStateFlow(FlyerSettings())
    val settings: StateFlow<FlyerSettings> = _settings.asStateFlow()

    private val _runState = MutableStateFlow(FlyerRunState())
    val runState: StateFlow<FlyerRunState> = _runState.asStateFlow()

    private val _saveResult = MutableStateFlow<Boolean?>(null)
    val saveResult: StateFlow<Boolean?> = _saveResult.asStateFlow()

    private val _readResult = MutableStateFlow<Boolean?>(null)
    val readResult: StateFlow<Boolean?> = _readResult.asStateFlow()

    private val _carouselData = MutableStateFlow<Map<UByte, Map<String, String>>>(emptyMap())
    val carouselData: StateFlow<Map<UByte, Map<String, String>>> = _carouselData.asStateFlow()

    private val _diagnosisState = MutableStateFlow(FlyerDiagnosisState())
    val diagnosisState: StateFlow<FlyerDiagnosisState> = _diagnosisState.asStateFlow()

    private val _gearboxLeft = MutableStateFlow("-")
    val gearboxLeft: StateFlow<String> = _gearboxLeft.asStateFlow()

    private val _gearboxRight = MutableStateFlow("-")
    val gearboxRight: StateFlow<String> = _gearboxRight.asStateFlow()

    private val _defaultApplied = MutableStateFlow<Boolean?>(null)
    val defaultApplied: StateFlow<Boolean?> = _defaultApplied.asStateFlow()

    private val _logEnabled = MutableStateFlow(false)
    val logEnabled: StateFlow<Boolean> = _logEnabled.asStateFlow()

    private val _logMessage = MutableStateFlow<String?>(null)
    val logMessage: StateFlow<String?> = _logMessage.asStateFlow()

    @Volatile private var readPending = false

    init {
        viewModelScope.launch { collectFrames() }
        viewModelScope.launch { collectSaveResponse() }
    }

    private suspend fun collectFrames() {
        repository.inboundFrames.collect { frame ->
            try {
                when (frame.info) {
                    0x06u.toUByte() -> {
                        val ss = frame.subState
                        var leftLift = _runState.value.leftLift
                        var rightLift = _runState.value.rightLift
                        var layers = _runState.value.layers
                        var pauseReason = ""
                        var pauseLayer: UShort = 0u
                        var errorInformation = ""
                        var errorCode = ""
                        var errorSource = ""
                        var errorLayer: UShort = 0u
                        var carouselMotorId: UByte = 0x00u
                        val carouselFields = mutableMapOf<String, String>()

                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                FlyerProtocol.TLV_WHAT_INFO   -> carouselMotorId = tlv.valueAsUInt8()
                                FlyerProtocol.TLV_MOSFET_TEMP -> carouselFields["mosfetTemp"] = tlv.valueAsUInt16().toString()
                                FlyerProtocol.TLV_CURRENT     -> carouselFields["current"]    = "%.2f".format(tlv.valueAsFloat())
                                FlyerProtocol.TLV_RPM         -> carouselFields["rpm"]        = tlv.valueAsUInt16().toString()
                                FlyerProtocol.TLV_OUTPUT_MTRS -> carouselFields["outputMtrs"] = "%.1f".format(tlv.valueAsFloat())
                                FlyerProtocol.TLV_TOTAL_POWER -> carouselFields["totalPower"] = "%.1f".format(tlv.valueAsFloat())
                                // TLV codes 0x01–0x04 are substate-dependent (running/homing/pause/error share the same codes)
                                FlyerProtocol.TLV_LEFT_LIFT -> when (ss) {
                                    0x01u.toUByte(), 0x04u.toUByte() -> leftLift = tlv.valueAsFloat()
                                    0x02u.toUByte() -> pauseReason = flyerPauseReason(tlv.valueAsUInt16().toInt())
                                    0x03u.toUByte() -> errorInformation = flyerErrorReason(tlv.valueAsUInt16().toInt())
                                }
                                FlyerProtocol.TLV_RIGHT_LIFT -> when (ss) {
                                    0x01u.toUByte(), 0x04u.toUByte() -> rightLift = tlv.valueAsFloat()
                                    0x02u.toUByte() -> pauseLayer = tlv.valueAsUInt16()
                                    0x03u.toUByte() -> errorSource = flyerErrorSource(tlv.valueAsUInt16().toInt())
                                }
                                FlyerProtocol.TLV_RUN_LAYERS -> when (ss) {
                                    0x01u.toUByte() -> layers = tlv.valueAsUInt16()
                                    0x03u.toUByte() -> errorCode = tlv.valueAsUInt16().toString()
                                }
                                FlyerProtocol.TLV_MOTOR_TEMP -> when (ss) {
                                    0x03u.toUByte() -> errorLayer = tlv.valueAsUInt16()
                                    else -> carouselFields["motorTemp"] = tlv.valueAsUInt16().toString()
                                }
                            }
                        }
                        _runState.value = FlyerRunState(
                            substate = ss,
                            leftLift = leftLift,
                            rightLift = rightLift,
                            layers = layers,
                            pauseReason = pauseReason,
                            pauseLayer = pauseLayer,
                            errorInformation = errorInformation,
                            errorCode = errorCode,
                            errorSource = errorSource,
                            errorLayer = errorLayer,
                        )
                        if (carouselMotorId != 0x00u.toUByte()) {
                            _carouselData.value = _carouselData.value + (carouselMotorId to carouselFields)
                        }
                    }

                    0x02u.toUByte() -> {
                        var s = _settings.value
                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                FlyerProtocol.TLV_SPINDLE_SPEED     -> s = s.copy(spindleSpeed = tlv.valueAsUInt16().toString())
                                FlyerProtocol.TLV_DRAFT             -> s = s.copy(draft = "%.2f".format(tlv.valueAsFloat()))
                                FlyerProtocol.TLV_TWIST_PER_INCH    -> s = s.copy(twistPerInch = "%.2f".format(tlv.valueAsFloat()))
                                FlyerProtocol.TLV_RTF               -> s = s.copy(rtf = "%.2f".format(tlv.valueAsFloat()))
                                FlyerProtocol.TLV_LAYERS            -> s = s.copy(layers = tlv.valueAsUInt16().toString())
                                FlyerProtocol.TLV_MAX_HEIGHT        -> s = s.copy(maxHeight = tlv.valueAsUInt16().toString())
                                FlyerProtocol.TLV_ROVING_WIDTH      -> s = s.copy(rovingWidth = "%.2f".format(tlv.valueAsFloat()))
                                FlyerProtocol.TLV_DELTA_BOBBIN_DIA  -> s = s.copy(deltaBobbinDia = "%.2f".format(tlv.valueAsFloat()))
                                FlyerProtocol.TLV_BARE_BOBBIN_DIA   -> s = s.copy(bareBobbinDia = tlv.valueAsUInt16().toString())
                                FlyerProtocol.TLV_RAMP_UP_TIME      -> s = s.copy(rampUpTime = tlv.valueAsUInt16().toString())
                                FlyerProtocol.TLV_RAMP_DOWN_TIME    -> s = s.copy(rampDownTime = tlv.valueAsUInt16().toString())
                                FlyerProtocol.TLV_CHANGE_LAYER_TIME -> s = s.copy(changeLayerTime = tlv.valueAsUInt16().toString())
                                FlyerProtocol.TLV_CONE_ANGLE_FACTOR -> s = s.copy(coneAngleFactor = "%.2f".format(tlv.valueAsFloat()))
                            }
                        }
                        _settings.value = s
                        readPending = false
                        viewModelScope.launch {
                            _readResult.value = true
                            kotlinx.coroutines.delay(2_000)
                            _readResult.value = null
                        }
                    }

                    0x05u.toUByte() -> {
                        if (_diagnosisState.value.isDiagnosing) {
                            var rpm = "-"; var pwm = "-"; var current = "-"; var power = "-"
                            for (tlv in frame.tlvs) {
                                when (tlv.type) {
                                    0x01u.toUByte() -> rpm     = tlv.valueAsUInt16().toString()
                                    0x02u.toUByte() -> pwm     = if (tlv.length == 0x04u.toUByte()) tlv.valueAsUInt16().toString()
                                                                 else "%.1f".format(tlv.valueAsFloat())
                                    0x03u.toUByte() -> current = "%.2f".format(tlv.valueAsFloat())
                                    0x04u.toUByte() -> power   = "%.1f".format(tlv.valueAsFloat())
                                }
                            }
                            _diagnosisState.value = _diagnosisState.value.copy(
                                isDiagnosing = true, rpm = rpm, pwm = pwm, current = current, power = power
                            )
                        }
                    }

                    0x09u.toUByte() -> {
                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                0x01u.toUByte() -> _gearboxLeft.value =
                                    if (tlv.length == 0x04u.toUByte()) tlv.valueAsUInt16().toString()
                                    else "%.2f".format(tlv.valueAsFloat())
                                0x02u.toUByte() -> _gearboxRight.value =
                                    if (tlv.length == 0x04u.toUByte()) tlv.valueAsUInt16().toString()
                                    else "%.2f".format(tlv.valueAsFloat())
                            }
                        }
                    }

                    0x07u.toUByte() -> {
                        var motorId: UByte = 0x00u
                        val data = mutableMapOf<String, String>()
                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                FlyerProtocol.TLV_WHAT_INFO   -> motorId = tlv.valueAsUInt8()
                                FlyerProtocol.TLV_OUTPUT_MTRS -> data["outputMtrs"] = "%.1f".format(tlv.valueAsFloat())
                                FlyerProtocol.TLV_TOTAL_POWER -> data["totalPower"] = "%.1f".format(tlv.valueAsFloat())
                                FlyerProtocol.TLV_MOTOR_TEMP  -> data["motorTemp"]  = tlv.valueAsUInt16().toString()
                                FlyerProtocol.TLV_MOSFET_TEMP -> data["mosfetTemp"] = tlv.valueAsUInt16().toString()
                                FlyerProtocol.TLV_CURRENT     -> data["current"]    = "%.2f".format(tlv.valueAsFloat())
                                FlyerProtocol.TLV_RPM         -> data["rpm"]        = tlv.valueAsUInt16().toString()
                            }
                        }
                        if (motorId != 0x00u.toUByte()) {
                            _carouselData.value = _carouselData.value + (motorId to data)
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private suspend fun collectSaveResponse() {
        repository.saveResponse.collect { ok ->
            _saveResult.value = ok
            kotlinx.coroutines.delay(2_000)
            _saveResult.value = null
        }
    }

    fun updateSettings(s: FlyerSettings) { _settings.value = s }

    fun resetToDefaults() {
        _settings.value = FlyerSettings(
            spindleSpeed = "650",
            draft = "8.8",
            twistPerInch = "1.4",
            rtf = "1.0",
            layers = "50",
            maxHeight = "270",
            rovingWidth = "1.2",
            deltaBobbinDia = "1.1",
            bareBobbinDia = "48",
            rampUpTime = "12",
            rampDownTime = "12",
            changeLayerTime = "800",
            coneAngleFactor = "1.0",
        )
        viewModelScope.launch {
            _defaultApplied.value = true
            kotlinx.coroutines.delay(2_000)
            _defaultApplied.value = null
        }
    }

    fun sendRead() {
        readPending = true
        repository.sendFrame(FrameCodec.buildReqSettings())
        viewModelScope.launch {
            kotlinx.coroutines.delay(3_000)
            if (readPending) {
                readPending = false
                _readResult.value = false
                kotlinx.coroutines.delay(2_000)
                _readResult.value = null
            }
        }
    }

    fun sendAll() { sendRead() }
    fun sendSave() { buildFrame()?.let { repository.sendFrame(it) } }

    fun sendDiagnostic(motorId: UByte, controlType: UByte, direction: UByte, speedPct: Int, durationSec: Int, bedDistanceMm: Int? = null) {
        repository.sendFrame(
            FrameCodec.buildDiagnostic(motorId, controlType, direction, speedPct.toUShort(), durationSec.toUShort(), bedDistanceMm?.toUShort())
        )
    }

    fun startDiagnosis(motorLabel: String) { _diagnosisState.value = FlyerDiagnosisState(isDiagnosing = true, motorLabel = motorLabel) }
    fun clearDiagnosis() { _diagnosisState.value = FlyerDiagnosisState() }
    fun sendStopDiagnosis() { repository.sendFrame(FrameCodec.build(0x04u, 0x06u)) }

    fun sendCarouselRequest(motorId: UByte) { repository.sendFrame(FrameCodec.buildCarouselRequest(motorId)) }
    fun sendResetLengthCounter() { repository.sendFrame(FrameCodec.buildResetLengthCounter()) }
    fun sendGearbox(substate: UByte) { repository.sendFrame(FrameCodec.build(0x08u, substate)) }
    fun sendRtf(enabled: Boolean) { repository.sendFrame(FrameCodec.build(0x0Bu, if (enabled) 0x01u else 0x00u)) }
    fun sendLog(enabled: Boolean) {
        _logEnabled.value = enabled
        repository.sendFrame(FrameCodec.build(0x0Cu, if (enabled) 0x01u else 0x00u))
        viewModelScope.launch {
            _logMessage.value = if (enabled) "Log Enabled" else "Log Disabled"
            kotlinx.coroutines.delay(2_000)
            _logMessage.value = null
        }
    }
    fun disconnect() { repository.disconnect() }

    private fun flyerPauseReason(code: Int): String = when (code) {
        1 -> "User Paused"
        2 -> "Front Sliver Cut"
        3 -> "Back Sliver Cut"
        4 -> "Lapping"
        else -> "Unknown"
    }

    private fun flyerErrorReason(code: Int): String = when (code) {
        2 -> "Over Current"; 4 -> "Over Voltage"; 8 -> "Under Voltage"
        16 -> "Motor Thermistor Fault"; 32 -> "MOSFET Thermistor Fault"
        64 -> "Motor Over Temperature"; 96 -> "SMPS Error"
        97 -> "Ack Error"; 98 -> "Can Cut Error"; 99 -> "Lift Relative Position Error"
        128 -> "MOSFET Over Temperature"; 256 -> "EEPROM Write Error"
        512 -> "EEPROM Bad Values"; 1024 -> "Tracking Error"
        2048 -> "Motor Encoder Setup Error"; 4096 -> "Lift Pos Tracking Error"
        8192 -> "Lift Synchronicity Fail"; 16384 -> "Lift Out Of Bounds Error"
        32768 -> "EEPROM Bad Homing Position"; else -> "Unknown Error"
    }

    private fun flyerErrorSource(code: Int): String = when (code) {
        1 -> "Flyer"; 2 -> "Bobbin"; 3 -> "FR"; 4 -> "BR"
        5 -> "Lift Left"; 6 -> "Lift Right"
        11 -> "MotherBoard"; 12 -> "Can Bus"; 13 -> "Lifts"; 14 -> "System"
        else -> "Unknown"
    }

    private fun buildFrame(): String? {
        val s = _settings.value
        return FlyerProtocol.buildSettingsFrame(
            spindleSpeed    = s.spindleSpeed.toIntOrNull() ?: return null,
            draft           = s.draft.toFloatOrNull() ?: return null,
            twistPerInch    = s.twistPerInch.toFloatOrNull() ?: return null,
            rtf             = s.rtf.toFloatOrNull() ?: return null,
            layers          = s.layers.toIntOrNull() ?: return null,
            maxHeight       = s.maxHeight.toIntOrNull() ?: return null,
            rovingWidth     = s.rovingWidth.toFloatOrNull() ?: return null,
            deltaBobbinDia  = s.deltaBobbinDia.toFloatOrNull() ?: return null,
            bareBobbinDia   = s.bareBobbinDia.toIntOrNull() ?: return null,
            rampUpTime      = s.rampUpTime.toIntOrNull() ?: return null,
            rampDownTime    = s.rampDownTime.toIntOrNull() ?: return null,
            changeLayerTime = s.changeLayerTime.toIntOrNull() ?: return null,
            coneAngleFactor = s.coneAngleFactor.toFloatOrNull() ?: return null,
        )
    }
}
