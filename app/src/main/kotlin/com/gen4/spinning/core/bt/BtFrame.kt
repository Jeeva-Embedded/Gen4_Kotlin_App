package com.gen4.spinning.core.bt

data class BtFrame(
    val info: UByte,
    val subState: UByte,
    val attributeCount: Int,
    val tlvs: List<Tlv>,
)

data class Tlv(
    val type: UByte,
    val length: UByte,
    val rawValue: String,
) {
    fun valueAsUInt16(): UShort = rawValue.toUShort(16)
    fun valueAsUInt8(): UByte = rawValue.toUByte(16)
    fun valueAsFloat(): Float {
        val bits = rawValue.toLong(16).toInt()
        return java.lang.Float.intBitsToFloat(bits)
    }
}

const val SAVE_RESPONSE_SUCCESS = "7E017E"
const val SAVE_RESPONSE_FAILURE = "7E007E"
const val FSC_CONNECT_STRING = "CONNECT"
const val FSC_DISCONN_STRING = "DISCONN"
