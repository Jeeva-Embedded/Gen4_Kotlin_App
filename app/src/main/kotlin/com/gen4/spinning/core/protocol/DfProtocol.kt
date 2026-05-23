package com.gen4.spinning.core.protocol

import com.gen4.spinning.core.bt.FrameCodec

object DfProtocol {
    // Settings TLV types
    const val TLV_DELIVERY_SPEED: UByte = 0x70u
    const val TLV_DRAFT: UByte = 0x71u
    const val TLV_LENGTH_LIMIT: UByte = 0x72u
    const val TLV_RAMP_UP_TIME: UByte = 0x73u
    const val TLV_RAMP_DOWN_TIME: UByte = 0x74u
    const val TLV_CREEL_TENSION_FACTOR: UByte = 0x75u

    // Running state TLV types
    const val TLV_MOTOR_TEMP: UByte = 0x04u
    const val TLV_MOSFET_TEMP: UByte = 0x05u
    const val TLV_CURRENT: UByte = 0x06u
    const val TLV_RPM: UByte = 0x07u
    const val TLV_OUTPUT_MTRS: UByte = 0x08u
    const val TLV_WHAT_INFO: UByte = 0x09u
    const val TLV_TOTAL_POWER: UByte = 0x0Au
    const val TLV_DELIVERY_MTRS_PER_MIN: UByte = 0x0Du
    const val TLV_CURRENT_LENGTH: UByte = 0x0Eu
    const val TLV_AL_SENSOR: UByte = 0x0Fu   // RUN_SENSOR_TOGGLE_STATE

    // AL settings TLV types (firmware: BT_Fns.h)
    const val TLV_AL_KP: UByte = 0x20u
    const val TLV_AL_SLIVER6: UByte = 0x21u
    const val TLV_AL_SLIVER5: UByte = 0x22u
    const val TLV_AL_SLIVER4: UByte = 0x23u
    const val TLV_AL_TARGET: UByte = 0x24u

    // Calibration result TLV types (INFO=0x15)
    const val TLV_CAL_AVG: UByte = 0x30u
    const val TLV_CAL_COUNT: UByte = 0x31u

    // Info bytes from mainboard → app
    const val INFO_AL_RESPONSE: UByte = 0x12u   // AL settings GET response
    const val INFO_CAL_RESULT: UByte = 0x15u    // calibration result

    // Motor IDs for diagnostics
    const val MOTOR_FRONT_ROLLER: UByte = 0x01u
    const val MOTOR_BACK_ROLLER: UByte = 0x02u
    const val MOTOR_CREEL: UByte = 0x03u
    const val MOTOR_DRAFTING: UByte = 0x05u

    // Field limits
    fun deliverySpeedValid(v: Int) = v in 50..300
    fun draftValid(v: Float) = v in 3f..12f
    fun lengthLimitValid(v: Int) = v in 5..1250
    fun rampUpTimeValid(v: Int) = v in 3..15
    fun rampDownTimeValid(v: Int) = v in 3..15
    fun creelTensionFactorValid(v: Float) = v in 0.25f..5f

    fun buildSettingsFrame(
        deliverySpeed: Int, draft: Float, lengthLimit: Int,
        rampUpTime: Int, rampDownTime: Int, creelTensionFactor: Float,
    ): String = FrameCodec.build(
        0x01u, 0x01u, listOf(
            FrameCodec.buildTlvInt(TLV_DELIVERY_SPEED, deliverySpeed.toUShort()),
            FrameCodec.buildTlvFloat(TLV_DRAFT, draft),
            FrameCodec.buildTlvInt(TLV_LENGTH_LIMIT, lengthLimit.toUShort()),
            FrameCodec.buildTlvInt(TLV_RAMP_UP_TIME, rampUpTime.toUShort()),
            FrameCodec.buildTlvInt(TLV_RAMP_DOWN_TIME, rampDownTime.toUShort()),
            FrameCodec.buildTlvFloat(TLV_CREEL_TENSION_FACTOR, creelTensionFactor),
        )
    )

    fun buildAlGetFrame(): String = FrameCodec.build(0x13u, 0x01u)

    fun buildAlSaveFrame(
        kp: Float, sliver6: Int, sliver5: Int, sliver4: Int, target: Float,
    ): String = FrameCodec.build(
        0x11u, 0x01u, listOf(
            FrameCodec.buildTlvFloat(TLV_AL_KP, kp),
            FrameCodec.buildTlvInt(TLV_AL_SLIVER6, sliver6.toUShort()),
            FrameCodec.buildTlvInt(TLV_AL_SLIVER5, sliver5.toUShort()),
            FrameCodec.buildTlvInt(TLV_AL_SLIVER4, sliver4.toUShort()),
            FrameCodec.buildTlvFloat(TLV_AL_TARGET, target),
        )
    )
}
