package com.gen4.spinning.machines.ring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gen4.spinning.core.bt.FrameCodec
import com.gen4.spinning.core.bt.BtSessionRepository
import com.gen4.spinning.core.protocol.RingProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RingSettings(
    val inputYarn: String = "30",
    val outputYarnDia: String = "",   // blank = auto-computed from inputYarn
    val spindleSpeed: String = "8000",
    val twistPerInch: String = "20",
    val packageHeight: String = "150",
    val diaBuildFactor: String = "0.5",
    val windingCloseness: String = "100",
    val windingOffsetCoils: String = "2",
)

data class RingRunState(
    val substate: UByte = 0x00u,
    val weight: Float = 0f,
    val pauseReason: String = "",
    val errorSource: String = "",
    val errorReason: String = "",
)

data class RingDiagnosisState(
    val isDiagnosing: Boolean = false,
    val motorLabel: String = "",
    val rpm: String = "-",
    val pwm: String = "-",
    val current: String = "-",
    val power: String = "-",
)

class RingViewModel(private val repository: BtSessionRepository) : ViewModel() {

    private val _settings = MutableStateFlow(RingSettings())
    val settings: StateFlow<RingSettings> = _settings.asStateFlow()

    private val _runState = MutableStateFlow(RingRunState())
    val runState: StateFlow<RingRunState> = _runState.asStateFlow()

    private val _saveResult = MutableStateFlow<Boolean?>(null)
    val saveResult: StateFlow<Boolean?> = _saveResult.asStateFlow()

    private val _readResult = MutableStateFlow<Boolean?>(null)
    val readResult: StateFlow<Boolean?> = _readResult.asStateFlow()

    private val _carouselData = MutableStateFlow<Map<UByte, Map<String, String>>>(emptyMap())
    val carouselData: StateFlow<Map<UByte, Map<String, String>>> = _carouselData.asStateFlow()

    private val _diagnosisState = MutableStateFlow(RingDiagnosisState())
    val diagnosisState: StateFlow<RingDiagnosisState> = _diagnosisState.asStateFlow()

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
                        var weight = _runState.value.weight
                        var pauseReason = ""
                        var errorSource = ""
                        var errorReason = ""
                        for (tlv in frame.tlvs) {
                            when (ss) {
                                0x01u.toUByte() -> if (tlv.type == RingProtocol.TLV_WEIGHT) weight = tlv.valueAsFloat()
                                0x02u.toUByte() -> if (tlv.type == 0x01u.toUByte()) pauseReason = ringPauseReason(tlv.valueAsUInt16().toInt())
                                0x03u.toUByte() -> when (tlv.type) {
                                    0x01u.toUByte() -> errorReason = ringErrorReason(tlv.valueAsUInt16().toInt())
                                    0x02u.toUByte() -> errorSource = ringErrorSource(tlv.valueAsUInt16().toInt())
                                }
                            }
                        }
                        _runState.value = RingRunState(
                            substate = ss,
                            weight = weight,
                            pauseReason = pauseReason,
                            errorSource = errorSource,
                            errorReason = errorReason,
                        )
                    }

                    0x07u.toUByte() -> {
                        var motorId: UByte = 0x00u
                        val data = mutableMapOf<String, String>()
                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                RingProtocol.TLV_WHAT_INFO   -> motorId = tlv.valueAsUInt8()
                                RingProtocol.TLV_OUTPUT_MTRS -> data["outputMtrs"] = "%.1f".format(tlv.valueAsFloat())
                                RingProtocol.TLV_TOTAL_POWER -> data["totalPower"] = "%.1f".format(tlv.valueAsFloat())
                                RingProtocol.TLV_MOTOR_TEMP  -> data["motorTemp"]  = tlv.valueAsUInt16().toString()
                                RingProtocol.TLV_MOSFET_TEMP -> data["mosfetTemp"] = tlv.valueAsUInt16().toString()
                                RingProtocol.TLV_CURRENT     -> data["current"]    = "%.2f".format(tlv.valueAsFloat())
                                RingProtocol.TLV_RPM         -> data["rpm"]        = tlv.valueAsUInt16().toString()
                            }
                        }
                        if (motorId != 0x00u.toUByte()) {
                            _carouselData.value = _carouselData.value + (motorId to data)
                        }
                    }

                    0x02u.toUByte() -> {
                        var s = _settings.value
                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                RingProtocol.TLV_INPUT_YARN           -> s = s.copy(inputYarn = tlv.valueAsUInt16().toString())
                                RingProtocol.TLV_OUTPUT_YARN_DIA      -> s = s.copy(outputYarnDia = "%.3f".format(tlv.valueAsFloat()))
                                RingProtocol.TLV_SPINDLE_SPEED        -> s = s.copy(spindleSpeed = tlv.valueAsUInt16().toString())
                                RingProtocol.TLV_TWIST_PER_INCH       -> s = s.copy(twistPerInch = tlv.valueAsUInt16().toString())
                                RingProtocol.TLV_PACKAGE_HEIGHT       -> s = s.copy(packageHeight = tlv.valueAsUInt16().toString())
                                RingProtocol.TLV_DIA_BUILD_FACTOR     -> s = s.copy(diaBuildFactor = "%.2f".format(tlv.valueAsFloat()))
                                RingProtocol.TLV_WINDING_CLOSENESS    -> s = s.copy(windingCloseness = tlv.valueAsUInt16().toString())
                                RingProtocol.TLV_WINDING_OFFSET_COILS -> s = s.copy(windingOffsetCoils = tlv.valueAsUInt16().toString())
                            }
                        }
                        _settings.value = s
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

    fun updateSettings(s: RingSettings) {
        val inputChanged = s.inputYarn != _settings.value.inputYarn
        val effective = if (inputChanged && s.outputYarnDia.isBlank()) {
            val inputInt = s.inputYarn.toIntOrNull()
            if (inputInt != null) s.copy(outputYarnDia = "%.3f".format(RingProtocol.calcOutputYarnDia(inputInt)))
            else s
        } else s
        _settings.value = effective
    }

    fun sendRead() { repository.sendFrame(FrameCodec.buildReqSettings()) }
    fun sendCarouselRequest(motorId: UByte) { repository.sendFrame(FrameCodec.buildCarouselRequest(motorId)) }
    fun sendResetGrams() { repository.sendFrame(FrameCodec.build(0x0Au, 0x00u)) }
    fun sendSave() { buildFrame()?.let { repository.sendFrame(it) } }
    fun startDiagnosis(motorLabel: String) { _diagnosisState.value = RingDiagnosisState(isDiagnosing = true, motorLabel = motorLabel) }
    fun clearDiagnosis() { _diagnosisState.value = RingDiagnosisState() }
    fun sendStopDiagnosis() { repository.sendFrame(FrameCodec.build(0x04u, 0x06u)) }

    fun sendDiagnostic(motorId: UByte, controlType: UByte, direction: UByte, speedPct: Int, durationSec: Int, bedDistanceMm: Int? = null) {
        repository.sendFrame(
            FrameCodec.buildDiagnostic(motorId, controlType, direction, speedPct.toUShort(), durationSec.toUShort(), bedDistanceMm?.toUShort())
        )
    }

    fun sendGearbox(substate: UByte) { repository.sendFrame(FrameCodec.build(0x08u, substate)) }
    fun sendRtf(enabled: Boolean) { repository.sendFrame(FrameCodec.build(0x0Bu, if (enabled) 0x01u else 0x00u)) }
    fun sendLog(enabled: Boolean) { repository.sendFrame(FrameCodec.build(0x0Cu, if (enabled) 0x01u else 0x00u)) }
    fun disconnect() { repository.disconnect() }

    private fun ringPauseReason(code: Int): String = when (code) {
        1 -> "User Paused"
        2 -> "Creel Sliver Cut"
        else -> "Unknown"
    }

    private fun ringErrorReason(code: Int): String = when (code) {
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

    private fun ringErrorSource(code: Int): String = when (code) {
        1 -> "Calender"; 2 -> "Lift"
        8 -> "Lift Left"; 9 -> "Lift Right"
        11 -> "MotherBoard"; 12 -> "Can Bus"; 13 -> "Lifts"; 14 -> "System"
        else -> "Unknown"
    }

    private fun buildFrame(): String? {
        val s = _settings.value
        val inputYarn = s.inputYarn.toIntOrNull() ?: return null
        val outputYarnDia = if (s.outputYarnDia.isBlank()) RingProtocol.calcOutputYarnDia(inputYarn)
                            else s.outputYarnDia.toFloatOrNull() ?: return null
        return RingProtocol.buildSettingsFrame(
            inputYarn          = inputYarn,
            outputYarnDia      = outputYarnDia,
            spindleSpeed       = s.spindleSpeed.toIntOrNull() ?: return null,
            twistPerInch       = s.twistPerInch.toIntOrNull() ?: return null,
            packageHeight      = s.packageHeight.toIntOrNull() ?: return null,
            diaBuildFactor     = s.diaBuildFactor.toFloatOrNull() ?: return null,
            windingCloseness   = s.windingCloseness.toIntOrNull() ?: return null,
            windingOffsetCoils = s.windingOffsetCoils.toIntOrNull() ?: return null,
        )
    }
}
