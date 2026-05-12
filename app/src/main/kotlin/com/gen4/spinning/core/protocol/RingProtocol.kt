package com.gen4.spinning.core.protocol

import com.gen4.spinning.core.bt.FrameCodec
import kotlin.math.sqrt

object RingProtocol {
    // Settings TLV types (0x90–0x97)
    const val TLV_INPUT_YARN: UByte = 0x90u
    const val TLV_OUTPUT_YARN_DIA: UByte = 0x91u
    const val TLV_SPINDLE_SPEED: UByte = 0x92u
    const val TLV_TWIST_PER_INCH: UByte = 0x93u
    const val TLV_PACKAGE_HEIGHT: UByte = 0x94u
    const val TLV_DIA_BUILD_FACTOR: UByte = 0x95u
    const val TLV_WINDING_CLOSENESS: UByte = 0x96u
    const val TLV_WINDING_OFFSET_COILS: UByte = 0x97u

    // Running state TLV types
    const val TLV_LEFT_LIFT: UByte = 0x01u
    const val TLV_RIGHT_LIFT: UByte = 0x02u
    const val TLV_LAYERS: UByte = 0x03u
    const val TLV_MOTOR_TEMP: UByte = 0x04u
    const val TLV_MOSFET_TEMP: UByte = 0x05u
    const val TLV_CURRENT: UByte = 0x06u
    const val TLV_RPM: UByte = 0x07u
    const val TLV_OUTPUT_MTRS: UByte = 0x08u
    const val TLV_WHAT_INFO: UByte = 0x09u
    const val TLV_TOTAL_POWER: UByte = 0x0Au
    const val TLV_WEIGHT: UByte = 0x10u

    // Motor IDs
    const val MOTOR_CALENDER: UByte = 0x01u
    const val MOTOR_LIFT: UByte = 0x07u
    const val MOTOR_LIFT_LEFT: UByte = 0x08u
    const val MOTOR_LIFT_RIGHT: UByte = 0x09u

    // Field limits
    fun inputYarnValid(v: Int) = v in 10..80
    fun outputYarnDiaValid(v: Float) = v in 0.05f..1.5f
    fun spindleSpeedValid(v: Int) = v in 6000..12000
    fun twistPerInchValid(v: Int) = v in 12..30
    fun packageHeightValid(v: Int) = v in 50..250
    fun diaBuildFactorValid(v: Float) = v in 0.05f..2.5f
    fun windingClosenessValid(v: Int) = v in 50..200
    fun windingOffsetCoilsValid(v: Int) = v in 1..5

    fun calcOutputYarnDia(inputYarn: Int): Float =
        (-0.10284f + 1.592f / sqrt(inputYarn.toFloat() / 2f))

    fun buildSettingsFrame(
        inputYarn: Int, outputYarnDia: Float, spindleSpeed: Int, twistPerInch: Int,
        packageHeight: Int, diaBuildFactor: Float, windingCloseness: Int, windingOffsetCoils: Int,
    ): String = FrameCodec.build(
        0x01u, 0x01u, listOf(
            FrameCodec.buildTlvInt(TLV_INPUT_YARN, inputYarn.toUShort()),
            FrameCodec.buildTlvFloat(TLV_OUTPUT_YARN_DIA, outputYarnDia),
            FrameCodec.buildTlvInt(TLV_SPINDLE_SPEED, spindleSpeed.toUShort()),
            FrameCodec.buildTlvInt(TLV_TWIST_PER_INCH, twistPerInch.toUShort()),
            FrameCodec.buildTlvInt(TLV_PACKAGE_HEIGHT, packageHeight.toUShort()),
            FrameCodec.buildTlvFloat(TLV_DIA_BUILD_FACTOR, diaBuildFactor),
            FrameCodec.buildTlvInt(TLV_WINDING_CLOSENESS, windingCloseness.toUShort()),
            FrameCodec.buildTlvInt(TLV_WINDING_OFFSET_COILS, windingOffsetCoils.toUShort()),
        )
    )
}
