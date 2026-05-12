package com.gen4.spinning.core.protocol

import com.gen4.spinning.core.bt.FrameCodec

object FlyerProtocol {
    // Settings TLV types (0x50–0x62)
    const val TLV_SPINDLE_SPEED: UByte = 0x50u
    const val TLV_DRAFT: UByte = 0x51u
    const val TLV_TWIST_PER_INCH: UByte = 0x52u
    const val TLV_RTF: UByte = 0x53u
    const val TLV_LAYERS: UByte = 0x54u
    const val TLV_MAX_HEIGHT: UByte = 0x55u
    const val TLV_ROVING_WIDTH: UByte = 0x56u
    const val TLV_DELTA_BOBBIN_DIA: UByte = 0x57u
    const val TLV_BARE_BOBBIN_DIA: UByte = 0x58u
    const val TLV_RAMP_UP_TIME: UByte = 0x59u
    const val TLV_RAMP_DOWN_TIME: UByte = 0x60u
    const val TLV_CHANGE_LAYER_TIME: UByte = 0x61u
    const val TLV_CONE_ANGLE_FACTOR: UByte = 0x62u

    // Running state TLV types
    const val TLV_LEFT_LIFT: UByte = 0x01u
    const val TLV_RIGHT_LIFT: UByte = 0x02u
    const val TLV_RUN_LAYERS: UByte = 0x03u
    const val TLV_MOTOR_TEMP: UByte = 0x04u
    const val TLV_MOSFET_TEMP: UByte = 0x05u
    const val TLV_CURRENT: UByte = 0x06u
    const val TLV_RPM: UByte = 0x07u
    const val TLV_OUTPUT_MTRS: UByte = 0x08u
    const val TLV_WHAT_INFO: UByte = 0x09u
    const val TLV_TOTAL_POWER: UByte = 0x0Au

    // Motor IDs for diagnostics
    const val MOTOR_FLYER: UByte = 0x01u
    const val MOTOR_BOBBIN: UByte = 0x02u
    const val MOTOR_FRONT_ROLLER: UByte = 0x03u
    const val MOTOR_BACK_ROLLER: UByte = 0x04u
    const val MOTOR_DRAFTING: UByte = 0x05u
    const val MOTOR_WINDING: UByte = 0x06u
    const val MOTOR_LIFT: UByte = 0x07u
    const val MOTOR_LIFT_LEFT: UByte = 0x08u
    const val MOTOR_LIFT_RIGHT: UByte = 0x09u

    // Field limits
    fun spindleSpeedValid(v: Int) = v in 500..1100
    fun draftValid(v: Float) = v in 5f..15f
    fun twistPerInchValid(v: Float) = v in 1f..1.6f
    fun rtfValid(v: Float) = v in 0.5f..1.5f
    fun layersValid(v: Int) = v in 1..100
    fun maxHeightValid(v: Int) = v in 250..300
    fun rovingWidthValid(v: Float) = v in 1f..8f
    fun deltaBobbinDiaValid(v: Float) = v in 0.3f..2.5f
    fun bareBobbinDiaValid(v: Int) = v in 46..52
    fun rampUpTimeValid(v: Int) = v in 5..20
    fun rampDownTimeValid(v: Int) = v in 5..20
    fun changeLayerTimeValid(v: Int) = v in 200..2500
    fun coneAngleFactorValid(v: Float) = v in 0.1f..3f

    fun buildSettingsFrame(
        spindleSpeed: Int, draft: Float, twistPerInch: Float, rtf: Float,
        layers: Int, maxHeight: Int, rovingWidth: Float, deltaBobbinDia: Float,
        bareBobbinDia: Int, rampUpTime: Int, rampDownTime: Int, changeLayerTime: Int,
        coneAngleFactor: Float,
    ): String = FrameCodec.build(
        0x01u, 0x01u, listOf(
            FrameCodec.buildTlvInt(TLV_SPINDLE_SPEED, spindleSpeed.toUShort()),
            FrameCodec.buildTlvFloat(TLV_DRAFT, draft),
            FrameCodec.buildTlvFloat(TLV_TWIST_PER_INCH, twistPerInch),
            FrameCodec.buildTlvFloat(TLV_RTF, rtf),
            FrameCodec.buildTlvInt(TLV_LAYERS, layers.toUShort()),
            FrameCodec.buildTlvInt(TLV_MAX_HEIGHT, maxHeight.toUShort()),
            FrameCodec.buildTlvFloat(TLV_ROVING_WIDTH, rovingWidth),
            FrameCodec.buildTlvFloat(TLV_DELTA_BOBBIN_DIA, deltaBobbinDia),
            FrameCodec.buildTlvInt(TLV_BARE_BOBBIN_DIA, bareBobbinDia.toUShort()),
            FrameCodec.buildTlvInt(TLV_RAMP_UP_TIME, rampUpTime.toUShort()),
            FrameCodec.buildTlvInt(TLV_RAMP_DOWN_TIME, rampDownTime.toUShort()),
            FrameCodec.buildTlvInt(TLV_CHANGE_LAYER_TIME, changeLayerTime.toUShort()),
            FrameCodec.buildTlvFloat(TLV_CONE_ANGLE_FACTOR, coneAngleFactor),
        )
    )
}
