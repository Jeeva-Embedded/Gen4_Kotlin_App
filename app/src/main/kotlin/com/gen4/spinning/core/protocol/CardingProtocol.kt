package com.gen4.spinning.core.protocol

import com.gen4.spinning.core.bt.FrameCodec

object CardingProtocol {
    // Settings TLV types
    const val TLV_DELIVERY_SPEED: UByte = 0x80u
    const val TLV_CARD_FEED_RATIO: UByte = 0x81u
    const val TLV_LENGTH_LIMIT: UByte = 0x82u
    const val TLV_CYL_SPEED: UByte = 0x83u
    const val TLV_BTR_SPEED: UByte = 0x84u
    const val TLV_PICKER_CYL_SPEED: UByte = 0x85u
    const val TLV_BTR_FEED: UByte = 0x86u
    const val TLV_AF_FEED: UByte = 0x87u

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
    const val TLV_DUCT_SENSOR: UByte = 0x0Bu
    const val TLV_COILER_SENSOR: UByte = 0x0Cu
    const val TLV_DELIVERY_MTRS_PER_MIN: UByte = 0x0Du

    // Motor IDs for diagnostics
    const val MOTOR_CYLINDER: UByte = 0x01u
    const val MOTOR_BEATER: UByte = 0x02u
    const val MOTOR_CAGE: UByte = 0x03u
    const val MOTOR_CYLINDER_FEED: UByte = 0x04u
    const val MOTOR_BEATER_FEED: UByte = 0x05u
    const val MOTOR_COILER: UByte = 0x06u
    const val MOTOR_PICKER_CYLINDER: UByte = 0x07u
    const val MOTOR_AF_FEED: UByte = 0x08u

    // Carousel production ID
    const val CAROUSEL_PRODUCTION: UByte = 0x0Au

    // Field limits
    fun deliverySpeedValid(v: Float) = v in 2.5f..50f
    fun cardFeedRatioValid(v: Float) = v in 3f..20f
    fun lengthLimitValid(v: Int) = v in 100..1000
    fun cylSpeedValid(v: Int) = v in 300..875
    fun btrSpeedValid(v: Int) = v in 300..650
    fun pickerCylSpeedValid(v: Int) = v in 300..700
    fun btrFeedValid(v: Int) = v in 1..11
    fun afFeedValid(v: Int) = v in 1..8

    fun buildSettingsFrame(
        deliverySpeed: Float, cardFeedRatio: Float, lengthLimit: Int,
        cylSpeed: Int, btrSpeed: Int, pickerCylSpeed: Int, btrFeed: Int, afFeed: Int,
        save: Boolean,
    ): String {
        val subState: UByte = if (save) 0x00u else 0x01u
        return FrameCodec.build(
            0x01u, subState, listOf(
                FrameCodec.buildTlvFloat(TLV_DELIVERY_SPEED, deliverySpeed),
                FrameCodec.buildTlvFloat(TLV_CARD_FEED_RATIO, cardFeedRatio),
                FrameCodec.buildTlvInt(TLV_LENGTH_LIMIT, lengthLimit.toUShort()),
                FrameCodec.buildTlvInt(TLV_CYL_SPEED, cylSpeed.toUShort()),
                FrameCodec.buildTlvInt(TLV_BTR_SPEED, btrSpeed.toUShort()),
                FrameCodec.buildTlvInt(TLV_PICKER_CYL_SPEED, pickerCylSpeed.toUShort()),
                FrameCodec.buildTlvInt(TLV_BTR_FEED, btrFeed.toUShort()),
                FrameCodec.buildTlvInt(TLV_AF_FEED, afFeed.toUShort()),
            )
        )
    }
}
