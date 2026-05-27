package com.gen4.spinning.machines.carding

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gen4.spinning.core.bt.BtSessionRepository
import com.gen4.spinning.core.bt.FrameCodec
import com.gen4.spinning.core.logging.LogSession
import com.gen4.spinning.core.logging.cardingMotorNames
import com.gen4.spinning.core.logging.createLogSession
import com.gen4.spinning.core.protocol.CardingProtocol
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CardingSettings(
    val deliverySpeed: String = "",
    val cardFeedRatio: String = "",
    val lengthLimit: String = "",
    val cylSpeed: String = "",
    val btrSpeed: String = "",
    val pickerCylSpeed: String = "",
    val btrFeed: String = "",
    val afFeed: String = "",
)

data class CardingRunState(
    val substate: UByte = 0x00u,
    val deliveryMtrsPerMin: Float = 0f,
    val ductSensor: Boolean = false,
    val coilerSensor: Boolean = false,
    val pauseReason: String = "",
    val errorInformation: String = "",
    val errorCode: String = "",
    val errorSource: String = "",
)

data class CardingDiagnosisState(
    val isDiagnosing: Boolean = false,
    val motorLabel: String = "",
    val rpm: String = "-",
    val pwm: String = "-",
    val current: String = "-",
    val power: String = "-",
)

class CardingViewModel(app: Application, private val repository: BtSessionRepository) : AndroidViewModel(app) {

    private var logSession: LogSession? = null
    private var logJob: Job? = null
    private var logFetchJob: Job? = null
    private val cardingLogMotors = listOf(
        CardingProtocol.CAROUSEL_PRODUCTION,
        CardingProtocol.MOTOR_CYLINDER,
        CardingProtocol.MOTOR_BEATER,
        CardingProtocol.MOTOR_CAGE,
        CardingProtocol.MOTOR_CYLINDER_FEED,
        CardingProtocol.MOTOR_BEATER_FEED,
        CardingProtocol.MOTOR_COILER,
        CardingProtocol.MOTOR_PICKER_CYLINDER,
        CardingProtocol.MOTOR_AF_FEED,
    )

    private val _settings = MutableStateFlow(CardingSettings())
    val settings: StateFlow<CardingSettings> = _settings.asStateFlow()

    private val _runState = MutableStateFlow(CardingRunState())
    val runState: StateFlow<CardingRunState> = _runState.asStateFlow()

    private val _carouselData = MutableStateFlow<Map<UByte, Map<String, String>>>(emptyMap())
    val carouselData: StateFlow<Map<UByte, Map<String, String>>> = _carouselData.asStateFlow()

    private val _saveResult = MutableStateFlow<Boolean?>(null)
    val saveResult: StateFlow<Boolean?> = _saveResult.asStateFlow()

    private val _readResult = MutableStateFlow<Boolean?>(null)
    val readResult: StateFlow<Boolean?> = _readResult.asStateFlow()

    private val _defaultApplied = MutableStateFlow<Boolean?>(null)
    val defaultApplied: StateFlow<Boolean?> = _defaultApplied.asStateFlow()

    private val _diagnosisState = MutableStateFlow(CardingDiagnosisState())
    val diagnosisState: StateFlow<CardingDiagnosisState> = _diagnosisState.asStateFlow()

    private val _logEnabled = MutableStateFlow(false)
    val logEnabled: StateFlow<Boolean> = _logEnabled.asStateFlow()

    private val _logMessage = MutableStateFlow<String?>(null)
    val logMessage: StateFlow<String?> = _logMessage.asStateFlow()

    private val _settingsChangeAllowed = MutableStateFlow(true)
    val settingsChangeAllowed: StateFlow<Boolean> = _settingsChangeAllowed.asStateFlow()

    @Volatile private var readPending = false

    init {
        viewModelScope.launch { collectFrames() }
        viewModelScope.launch { collectSaveResponse() }
    }

    private suspend fun collectFrames() {
        repository.inboundFrames.collect { frame ->
            Log.d("CardingVM", "frame info=0x${frame.info.toString(16).uppercase()}")
            try {
                when (frame.info) {
                    0x06u.toUByte() -> {
                        val ss = frame.subState
                        var deliveryMtrsPerMin = _runState.value.deliveryMtrsPerMin
                        var ductSensor = _runState.value.ductSensor
                        var coilerSensor = _runState.value.coilerSensor
                        var pauseReason = ""
                        var errorInformation = ""
                        var errorCode = ""
                        var errorSource = ""
                        var carouselMotorId: UByte = 0x00u
                        val carouselFields = mutableMapOf<String, String>()

                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                CardingProtocol.TLV_WHAT_INFO              -> carouselMotorId = tlv.valueAsUInt8()
                                CardingProtocol.TLV_RPM                    -> carouselFields["rpm"]       = tlv.valueAsUInt16().toString()
                                CardingProtocol.TLV_CURRENT                -> carouselFields["current"]   = "%.2f".format(tlv.valueAsFloat())
                                CardingProtocol.TLV_MOTOR_TEMP             -> carouselFields["motorTemp"]  = tlv.valueAsUInt16().toString()
                                CardingProtocol.TLV_MOSFET_TEMP            -> carouselFields["mosfetTemp"] = tlv.valueAsUInt16().toString()
                                CardingProtocol.TLV_OUTPUT_MTRS            -> carouselFields["outputMtrs"] = "%.1f".format(tlv.valueAsFloat())
                                CardingProtocol.TLV_TOTAL_POWER            -> carouselFields["totalPower"] = "%.1f".format(tlv.valueAsFloat())
                                CardingProtocol.TLV_DELIVERY_MTRS_PER_MIN -> {
                                    deliveryMtrsPerMin = tlv.valueAsFloat()
                                    carouselFields["deliveryMtrsPerMin"] = "%.1f".format(tlv.valueAsFloat())
                                }
                                CardingProtocol.TLV_DUCT_SENSOR            -> ductSensor = tlv.valueAsUInt8() == 1u.toUByte()
                                CardingProtocol.TLV_COILER_SENSOR          -> coilerSensor = tlv.valueAsUInt8() == 1u.toUByte()
                                0x01u.toUByte() -> when (ss) {
                                    0x02u.toUByte() -> pauseReason = cardingPauseReason(tlv.valueAsUInt16().toInt())
                                    0x03u.toUByte() -> errorInformation = cardingErrorReason(tlv.valueAsUInt16().toInt())
                                }
                                0x02u.toUByte() -> if (ss == 0x03u.toUByte()) errorSource = cardingErrorSource(tlv.valueAsUInt16().toInt())
                                0x03u.toUByte() -> if (ss == 0x03u.toUByte()) errorCode = tlv.valueAsUInt16().toString()
                            }
                        }
                        _runState.value = CardingRunState(
                            substate = ss,
                            deliveryMtrsPerMin = deliveryMtrsPerMin,
                            ductSensor = ductSensor,
                            coilerSensor = coilerSensor,
                            pauseReason = pauseReason,
                            errorInformation = errorInformation,
                            errorCode = errorCode,
                            errorSource = errorSource,
                        )
                        _settingsChangeAllowed.value = ss == 0x00u.toUByte()
                        if (carouselMotorId != 0x00u.toUByte()) {
                            _carouselData.value = _carouselData.value + (carouselMotorId to carouselFields)
                        }
                    }

                    0x02u.toUByte() -> {
                        var s = _settings.value
                        for (tlv in frame.tlvs) {
                            when (tlv.type) {
                                CardingProtocol.TLV_DELIVERY_SPEED   -> s = s.copy(deliverySpeed = "%.2f".format(tlv.valueAsFloat()))
                                CardingProtocol.TLV_CARD_FEED_RATIO  -> s = s.copy(cardFeedRatio = "%.2f".format(tlv.valueAsFloat()))
                                CardingProtocol.TLV_LENGTH_LIMIT     -> s = s.copy(lengthLimit = tlv.valueAsUInt16().toString())
                                CardingProtocol.TLV_CYL_SPEED        -> s = s.copy(cylSpeed = tlv.valueAsUInt16().toString())
                                CardingProtocol.TLV_BTR_SPEED        -> s = s.copy(btrSpeed = tlv.valueAsUInt16().toString())
                                CardingProtocol.TLV_PICKER_CYL_SPEED -> s = s.copy(pickerCylSpeed = tlv.valueAsUInt16().toString())
                                CardingProtocol.TLV_BTR_FEED         -> s = s.copy(btrFeed = tlv.valueAsUInt16().toString())
                                CardingProtocol.TLV_AF_FEED          -> s = s.copy(afFeed = tlv.valueAsUInt16().toString())
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
                                try {
                                    val isInt = tlv.length == 0x04u.toUByte()
                                    when (tlv.type) {
                                        0x01u.toUByte() -> rpm     = if (isInt) tlv.valueAsUInt16().toString() else "%.0f".format(tlv.valueAsFloat())
                                        0x02u.toUByte() -> pwm     = if (isInt) tlv.valueAsUInt16().toString() else "%.1f".format(tlv.valueAsFloat())
                                        0x03u.toUByte() -> current = if (isInt) tlv.valueAsUInt16().toString() else "%.2f".format(tlv.valueAsFloat())
                                        0x04u.toUByte() -> power   = if (isInt) tlv.valueAsUInt16().toString() else "%.1f".format(tlv.valueAsFloat())
                                    }
                                } catch (_: Exception) { }
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
                                CardingProtocol.TLV_WHAT_INFO   -> motorId = tlv.valueAsUInt8()
                                CardingProtocol.TLV_OUTPUT_MTRS -> data["outputMtrs"] = "%.1f".format(tlv.valueAsFloat())
                                CardingProtocol.TLV_TOTAL_POWER -> data["totalPower"] = "%.1f".format(tlv.valueAsFloat())
                                CardingProtocol.TLV_MOTOR_TEMP  -> data["motorTemp"]  = tlv.valueAsUInt16().toString()
                                CardingProtocol.TLV_MOSFET_TEMP -> data["mosfetTemp"] = tlv.valueAsUInt16().toString()
                                CardingProtocol.TLV_CURRENT     -> data["current"]    = "%.2f".format(tlv.valueAsFloat())
                                CardingProtocol.TLV_RPM         -> data["rpm"]        = tlv.valueAsUInt16().toString()
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

    fun updateSettings(s: CardingSettings) { _settings.value = s }

    fun resetToDefaults() {
        _settings.value = CardingSettings(
            deliverySpeed = "8.0",
            cardFeedRatio = "5.0",
            lengthLimit = "1000",
            cylSpeed = "750",
            btrSpeed = "600",
            pickerCylSpeed = "600",
            btrFeed = "10",
            afFeed = "7",
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

    fun sendSave() {
        val s = _settings.value
        val frame = buildFrame(s, save = true) ?: return
        repository.sendFrame(frame)
    }

    fun sendCarouselRequest(motorId: UByte) { repository.sendFrame(FrameCodec.buildCarouselRequest(motorId)) }
    fun sendResetLengthCounter() { repository.sendFrame(FrameCodec.buildResetLengthCounter()) }

    fun sendDiagnostic(motorId: UByte, controlType: UByte, direction: UByte, speedPct: Int, durationSec: Int) {
        repository.drainTxQueue()
        repository.sendFrame(
            FrameCodec.buildDiagnostic(motorId, controlType, direction, speedPct.toUShort(), durationSec.toUShort())
        )
    }

    fun startDiagnosis(motorLabel: String) { _diagnosisState.value = CardingDiagnosisState(isDiagnosing = true, motorLabel = motorLabel) }
    fun clearDiagnosis() { _diagnosisState.value = CardingDiagnosisState() }
    fun sendStopDiagnosis() {
        repository.drainTxQueue()
        repeat(3) { repository.sendFrame(FrameCodec.build(0x04u, 0x06u)) }
    }

    fun sendGearbox(substate: UByte) { repository.sendFrame(FrameCodec.build(0x08u, substate)) }
    fun sendRtf(enabled: Boolean) { repository.sendFrame(FrameCodec.build(0x0Bu, if (enabled) 0x01u else 0x00u)) }

    fun sendLog(enabled: Boolean) {
        _logEnabled.value = enabled
        repository.sendFrame(FrameCodec.build(0x0Cu, if (enabled) 0x01u else 0x00u))
        if (enabled) {
            logSession = createLogSession(getApplication(), "BlowCard")
            logJob = viewModelScope.launch {
                while (true) { delay(1_000); writeLogSnapshot() }
            }
            logFetchJob = viewModelScope.launch {
                while (true) {
                    for (motorId in cardingLogMotors) {
                        if (_diagnosisState.value.isDiagnosing) break
                        sendCarouselRequest(motorId)
                        delay(400)
                    }
                    if (_diagnosisState.value.isDiagnosing) delay(500)
                }
            }
        } else {
            logJob?.cancel(); logJob = null
            logFetchJob?.cancel(); logFetchJob = null
            logSession?.close(); logSession = null
        }
        viewModelScope.launch {
            _logMessage.value = if (enabled) "Log Enabled" else "Log Disabled"
            delay(2_000)
            _logMessage.value = null
        }
    }

    private fun writeLogSnapshot() {
        val session = logSession ?: return
        val rs = _runState.value
        val state = cardingSubstateLabel(rs.substate)
        val duct = if (rs.ductSensor) "ON" else "OFF"
        val coiler = if (rs.coilerSensor) "ON" else "OFF"
        val pauseState = rs.pauseReason.ifEmpty { "-" }
        val errorInfo = rs.errorInformation.ifEmpty { "-" }
        val errorSrc = rs.errorSource.ifEmpty { "-" }
        val delivery = if (rs.deliveryMtrsPerMin > 0f) "%.1f".format(rs.deliveryMtrsPerMin) else "-"
        val cd = _carouselData.value
        for (motorId in cardingLogMotors) {
            val name = cardingMotorNames[motorId] ?: "Motor ${motorId.toString(16).uppercase()}"
            val f = cd[motorId] ?: emptyMap()
            val outMtrs = if (motorId == CardingProtocol.CAROUSEL_PRODUCTION) f["outputMtrs"] ?: "-" else "-"
            session.writeRow("Blow Card", state, name,
                f["mosfetTemp"] ?: "-", f["motorTemp"] ?: "-", f["rpm"] ?: "-",
                f["current"] ?: "-", f["totalPower"] ?: "-", outMtrs,
                delivery, "-", "-", duct, coiler, pauseState, errorInfo, errorSrc)
        }
    }

    private fun cardingSubstateLabel(s: UByte) = when (s) {
        0x00u.toUByte() -> "Idle"; 0x01u.toUByte() -> "Running"
        0x02u.toUByte() -> "Paused"; 0x03u.toUByte() -> "Error"
        0x04u.toUByte() -> "Homing"; 0x05u.toUByte() -> "Inching"
        else -> "Unknown"
    }

    override fun onCleared() {
        logJob?.cancel()
        logFetchJob?.cancel()
        logSession?.close()
        super.onCleared()
    }

    fun disconnect() { repository.disconnect() }

    private fun cardingPauseReason(code: Int): String = when (code) {
        1 -> "User Paused"
        2 -> "Creel Sliver Cut"
        3 -> "Coiler Sliver Cut"
        4 -> "Lapping"
        else -> "Unknown"
    }

    private fun cardingErrorReason(code: Int): String = when (code) {
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

    private fun cardingErrorSource(code: Int): String = when (code) {
        1 -> "Cylinder"; 2 -> "Beater"; 3 -> "Cage"
        4 -> "Card Feed"; 5 -> "Beater Feed"; 6 -> "Coiler"
        11 -> "MotherBoard"; 12 -> "Can Bus"; 13 -> "Lifts"; 14 -> "System"
        else -> "Unknown"
    }

    private fun buildFrame(s: CardingSettings, save: Boolean): String? {
        val deliverySpeed  = s.deliverySpeed.toFloatOrNull() ?: return null
        val cardFeedRatio  = s.cardFeedRatio.toFloatOrNull() ?: return null
        val lengthLimit    = s.lengthLimit.toIntOrNull() ?: return null
        val cylSpeed       = s.cylSpeed.toIntOrNull() ?: return null
        val btrSpeed       = s.btrSpeed.toIntOrNull() ?: return null
        val pickerCylSpeed = s.pickerCylSpeed.toIntOrNull() ?: return null
        val btrFeed        = s.btrFeed.toIntOrNull() ?: return null
        val afFeed         = s.afFeed.toIntOrNull() ?: return null
        return CardingProtocol.buildSettingsFrame(
            deliverySpeed, cardFeedRatio, lengthLimit,
            cylSpeed, btrSpeed, pickerCylSpeed, btrFeed, afFeed, save,
        )
    }
}
