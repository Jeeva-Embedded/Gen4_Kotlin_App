package com.gen4.spinning.machines.df

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gen4.spinning.core.bt.FrameCodec
import com.gen4.spinning.core.bt.BtSessionRepository
import com.gen4.spinning.core.protocol.DfProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DfSettings(
    val deliverySpeed: String = "80",
    val draft: String = "8.0",
    val lengthLimit: String = "400",
    val rampUpTime: String = "6",
    val rampDownTime: String = "6",
    val creelTensionFactor: String = "1.0",
)

data class DfRunState(
    val substate: UByte = 0x00u,
    val deliveryMtrsPerMin: Float = 0f,
    val currentLength: Float = 0f,
    val pauseReason: String = "",
    val pauseLength: Float = 0f,
    val errorSource: String = "",
    val errorReason: String = "",
)

data class DfDiagnosisState(
    val isDiagnosing: Boolean = false,
    val motorLabel: String = "",
    val rpm: String = "-",
    val pwm: String = "-",
    val current: String = "-",
    val power: String = "-",
)

class DfViewModel(private val repository: BtSessionRepository) : ViewModel() {

    private val _settings = MutableStateFlow(DfSettings())
    val settings: StateFlow<DfSettings> = _settings.asStateFlow()

    private val _runState = MutableStateFlow(DfRunState())
    val runState: StateFlow<DfRunState> = _runState.asStateFlow()

    private val _carouselData = MutableStateFlow<Map<UByte, Map<String, String>>>(emptyMap())
    val carouselData: StateFlow<Map<UByte, Map<String, String>>> = _carouselData.asStateFlow()

    private val _saveResult = MutableStateFlow<Boolean?>(null)
    val saveResult: StateFlow<Boolean?> = _saveResult.asStateFlow()

    private val _readResult = MutableStateFlow<Boolean?>(null)
    val readResult: StateFlow<Boolean?> = _readResult.asStateFlow()

    private val _defaultApplied = MutableStateFlow<Boolean?>(null)
    val defaultApplied: StateFlow<Boolean?> = _defaultApplied.asStateFlow()

    private val _diagnosisState = MutableStateFlow(DfDiagnosisState())
    val diagnosisState: StateFlow<DfDiagnosisState> = _diagnosisState.asStateFlow()

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
                        var deliveryMtrsPerMin = _runState.value.deliveryMtrsPerMin
                        var currentLength = _runState.value.currentLength
                        var pauseReason = ""
                        var pauseLength = 0f
                        var errorSource = ""
                        var errorReason = ""
                        // Carousel data arrives embedded in 0x06 frames (Flutter protocol)
                        var carouselMotorId: UByte = 0x00u
                        val carouselFields = mutableMapOf<String, String>()

                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                DfProtocol.TLV_WHAT_INFO             -> carouselMotorId = tlv.valueAsUInt8()
                                DfProtocol.TLV_RPM                   -> carouselFields["rpm"]               = tlv.valueAsUInt16().toString()
                                DfProtocol.TLV_CURRENT               -> carouselFields["current"]           = "%.2f".format(tlv.valueAsFloat())
                                DfProtocol.TLV_MOTOR_TEMP            -> carouselFields["motorTemp"]         = tlv.valueAsUInt16().toString()
                                DfProtocol.TLV_MOSFET_TEMP           -> carouselFields["mosfetTemp"]        = tlv.valueAsUInt16().toString()
                                DfProtocol.TLV_OUTPUT_MTRS           -> carouselFields["outputMtrs"]        = "%.1f".format(tlv.valueAsFloat())
                                DfProtocol.TLV_TOTAL_POWER           -> carouselFields["totalPower"]        = "%.1f".format(tlv.valueAsFloat())
                                DfProtocol.TLV_DELIVERY_MTRS_PER_MIN -> {
                                    deliveryMtrsPerMin = tlv.valueAsFloat()
                                    carouselFields["deliveryMtrsPerMin"] = "%.1f".format(tlv.valueAsFloat())
                                }
                                DfProtocol.TLV_CURRENT_LENGTH -> {
                                    currentLength = tlv.valueAsFloat()
                                    carouselFields["currentLength"] = "%.1f".format(tlv.valueAsFloat())
                                }
                                0x01u.toUByte() -> when (ss) {
                                    0x02u.toUByte() -> pauseReason = dfPauseReason(tlv.valueAsUInt16().toInt())
                                    0x03u.toUByte() -> errorReason = dfErrorReason(tlv.valueAsUInt16().toInt())
                                }
                                0x02u.toUByte() -> when (ss) {
                                    0x02u.toUByte() -> pauseLength = tlv.valueAsFloat()
                                    0x03u.toUByte() -> errorSource = dfErrorSource(tlv.valueAsUInt16().toInt())
                                }
                            }
                        }
                        _runState.value = DfRunState(
                            substate = ss,
                            deliveryMtrsPerMin = deliveryMtrsPerMin,
                            currentLength = currentLength,
                            pauseReason = pauseReason,
                            pauseLength = pauseLength,
                            errorSource = errorSource,
                            errorReason = errorReason,
                        )
                        if (carouselMotorId != 0x00u.toUByte()) {
                            _carouselData.value = _carouselData.value + (carouselMotorId to carouselFields)
                        }
                    }

                    0x02u.toUByte() -> {
                        var s = _settings.value
                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                DfProtocol.TLV_DELIVERY_SPEED       -> s = s.copy(deliverySpeed = tlv.valueAsUInt16().toString())
                                DfProtocol.TLV_DRAFT                -> s = s.copy(draft = "%.2f".format(tlv.valueAsFloat()))
                                DfProtocol.TLV_LENGTH_LIMIT         -> s = s.copy(lengthLimit = tlv.valueAsUInt16().toString())
                                DfProtocol.TLV_RAMP_UP_TIME         -> s = s.copy(rampUpTime = tlv.valueAsUInt16().toString())
                                DfProtocol.TLV_RAMP_DOWN_TIME       -> s = s.copy(rampDownTime = tlv.valueAsUInt16().toString())
                                DfProtocol.TLV_CREEL_TENSION_FACTOR -> s = s.copy(creelTensionFactor = "%.2f".format(tlv.valueAsFloat()))
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

                    0x07u.toUByte() -> {
                        var motorId: UByte = 0x00u
                        val data = mutableMapOf<String, String>()
                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                DfProtocol.TLV_WHAT_INFO              -> motorId = tlv.valueAsUInt8()
                                DfProtocol.TLV_OUTPUT_MTRS            -> data["outputMtrs"]         = "%.1f".format(tlv.valueAsFloat())
                                DfProtocol.TLV_TOTAL_POWER            -> data["totalPower"]          = "%.1f".format(tlv.valueAsFloat())
                                DfProtocol.TLV_MOTOR_TEMP             -> data["motorTemp"]           = tlv.valueAsUInt16().toString()
                                DfProtocol.TLV_MOSFET_TEMP            -> data["mosfetTemp"]          = tlv.valueAsUInt16().toString()
                                DfProtocol.TLV_CURRENT                -> data["current"]             = "%.2f".format(tlv.valueAsFloat())
                                DfProtocol.TLV_RPM                    -> data["rpm"]                 = tlv.valueAsUInt16().toString()
                                DfProtocol.TLV_DELIVERY_MTRS_PER_MIN -> data["deliveryMtrsPerMin"]  = "%.1f".format(tlv.valueAsFloat())
                                DfProtocol.TLV_CURRENT_LENGTH         -> data["currentLength"]       = "%.1f".format(tlv.valueAsFloat())
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

    fun updateSettings(s: DfSettings) { _settings.value = s }

    fun resetToDefaults() {
        _settings.value = DfSettings()
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

    fun sendSave() { buildFrame()?.let { repository.sendFrame(it) } }
    fun sendAll() { sendRead() }
    fun startDiagnosis(motorLabel: String) { _diagnosisState.value = DfDiagnosisState(isDiagnosing = true, motorLabel = motorLabel) }
    fun clearDiagnosis() { _diagnosisState.value = DfDiagnosisState() }
    fun sendCarouselRequest(motorId: UByte) { repository.sendFrame(FrameCodec.buildCarouselRequest(motorId)) }
    fun sendResetLengthCounter() { repository.sendFrame(FrameCodec.buildResetLengthCounter()) }

    fun sendDiagnostic(motorId: UByte, controlType: UByte, direction: UByte, speedPct: Int, durationSec: Int) {
        repository.sendFrame(
            FrameCodec.buildDiagnostic(motorId, controlType, direction, speedPct.toUShort(), durationSec.toUShort())
        )
    }

    fun sendStopDiagnosis() { repository.sendFrame(FrameCodec.build(0x04u, 0x06u)) }
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

    private fun dfPauseReason(code: Int): String = when (code) {
        1 -> "User Paused"
        2 -> "Creel Sliver Cut"
        3 -> "Coiler Sliver Cut"
        4 -> "Lapping"
        else -> "Unknown"
    }

    private fun dfErrorSource(code: Int): String = when (code) {
        1  -> "Front Roller"
        2  -> "Back Roller"
        3  -> "Creel"
        11 -> "Mother Board"
        12 -> "Can Bus"
        13 -> "Lifts"
        14 -> "System"
        else -> "Unknown ($code)"
    }

    private fun dfErrorReason(code: Int): String = when (code) {
        2     -> "Over Current"
        4     -> "Over Voltage"
        8     -> "Under Voltage"
        16    -> "Motor Thermistor Fault"
        32    -> "MOSFET Thermistor Fault"
        64    -> "Motor Over Temperature"
        128   -> "MOSFET Over Temperature"
        256   -> "EEPROM Write Error"
        512   -> "EEPROM Bad Values"
        1024  -> "Tracking Error"
        2048  -> "Motor Encoder Setup Error"
        4096  -> "Lift Pos Tracking Error"
        8192  -> "Lift Synchronicity Fail"
        16384 -> "Lift Out Of Bounds"
        32768 -> "EEPROM Bad Homing Position"
        96    -> "SMPS Error"
        97    -> "Ack Error"
        98    -> "Can Cut Error"
        99    -> "Lift Relative Position Error"
        else  -> "Error Code $code"
    }

    private fun buildFrame(): String? {
        val s = _settings.value
        val deliverySpeed = s.deliverySpeed.toIntOrNull() ?: return null
        val draft = s.draft.toFloatOrNull() ?: return null
        val lengthLimit = s.lengthLimit.toIntOrNull() ?: return null
        val rampUp = s.rampUpTime.toIntOrNull() ?: return null
        val rampDown = s.rampDownTime.toIntOrNull() ?: return null
        val creelTension = s.creelTensionFactor.toFloatOrNull() ?: return null
        return DfProtocol.buildSettingsFrame(deliverySpeed, draft, lengthLimit, rampUp, rampDown, creelTension)
    }
}
