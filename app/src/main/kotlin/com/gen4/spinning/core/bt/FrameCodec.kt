package com.gen4.spinning.core.bt

object FrameCodec {

    private const val SOF = "7E"
    private const val EOF = "7E"

    sealed class ParseResult {
        data class Ok(val frame: BtFrame) : ParseResult()
        data class Skip(val reason: String) : ParseResult()
        data class Error(val reason: String) : ParseResult()
    }

    fun parse(raw: String): ParseResult {
        val s = raw.trim()
        if (s.contains(FSC_CONNECT_STRING)) return ParseResult.Skip("FSC CONNECT")
        if (s.contains(FSC_DISCONN_STRING)) return ParseResult.Skip("FSC DISCONN")
        if (s == SAVE_RESPONSE_SUCCESS) return ParseResult.Skip("save-ack SUCCESS")
        if (s == SAVE_RESPONSE_FAILURE) return ParseResult.Skip("save-ack FAILURE")
        if (s.length < 12) return ParseResult.Error("too short: ${s.length}")
        if (!s.startsWith(SOF)) return ParseResult.Error("no SOF")

        val lengthHex = s.substring(2, 4)
        val length = lengthHex.toIntOrNull(16) ?: return ParseResult.Error("bad length: $lengthHex")
        val expectedTotal = 4 + length
        if (s.length < expectedTotal) return ParseResult.Error("truncated: expected $expectedTotal got ${s.length}")

        val eofOffset = 4 + length - 2
        if (s.substring(eofOffset, eofOffset + 2) != EOF) return ParseResult.Error("EOF mismatch")

        val info = s.substring(4, 6).toUByteOrNull(16) ?: return ParseResult.Error("bad info")
        val subState = s.substring(6, 8).toUByteOrNull(16) ?: return ParseResult.Error("bad substate")
        val attributeCount = s.substring(8, 10).toIntOrNull(16) ?: return ParseResult.Error("bad attrCount")

        val tlvs = mutableListOf<Tlv>()
        var cursor = 10
        repeat(attributeCount) { i ->
            if (cursor + 4 > eofOffset) return ParseResult.Error("TLV $i overruns EOF")
            val type = s.substring(cursor, cursor + 2).toUByteOrNull(16) ?: return ParseResult.Error("TLV $i bad type")
            val wireLen = s.substring(cursor + 2, cursor + 4).toUByteOrNull(16) ?: return ParseResult.Error("TLV $i bad len")
            val valueChars = when (wireLen.toInt()) { 0x02 -> 2; 0x04 -> 4; 0x08 -> 8; else -> return ParseResult.Error("TLV $i unknown len") }
            if (cursor + 4 + valueChars > eofOffset) return ParseResult.Error("TLV $i value overruns EOF")
            tlvs.add(Tlv(type, wireLen, s.substring(cursor + 4, cursor + 4 + valueChars)))
            cursor += 4 + valueChars
        }
        return ParseResult.Ok(BtFrame(info, subState, attributeCount, tlvs))
    }

    fun build(info: UByte, subState: UByte, tlvs: List<String> = emptyList()): String {
        val body = buildString {
            append("%02X".format(info.toInt()))
            append("%02X".format(subState.toInt()))
            append("%02X".format(tlvs.size))
            for (tlv in tlvs) append(tlv)
            append(EOF)
        }
        return SOF + "%02X".format(body.length) + body
    }

    fun buildTlvCompact(type: UByte, value: UByte): String = "%02X02%02X".format(type.toInt(), value.toInt())
    fun buildTlvInt(type: UByte, value: UShort): String = "%02X04%04X".format(type.toInt(), value.toInt())
    fun buildTlvFloat(type: UByte, value: Float): String {
        val bits = java.lang.Float.floatToRawIntBits(value)
        return "%02X08%08X".format(type.toInt(), bits)
    }

    fun buildPairedFromPhone(): String = build(0x99u, 0x00u)
    fun buildReqSettings(): String = build(0x03u, 0x00u)
    fun buildResetLengthCounter(): String = build(0x0Au, 0x00u)

    // Motor ID goes in subState field — matches Flutter: 7E 08 07 {motorId} 00 7E
    fun buildCarouselRequest(motorId: UByte): String = build(0x07u, motorId)

    fun buildPidRequest(motorId: UByte): String = build(0x0Du, 0x00u, listOf(buildTlvCompact(0x01u, motorId)))

    fun buildDiagnostic(motorId: UByte, controlType: UByte, direction: UByte, speedPct: UShort, durationSec: UShort, bedDistanceMm: UShort? = null): String {
        val tlvs = mutableListOf(
            buildTlvCompact(0x40u, motorId),
            buildTlvCompact(0x44u, direction),
            buildTlvCompact(0x41u, controlType),
            buildTlvInt(0x42u, speedPct),
            buildTlvInt(0x43u, durationSec),
        )
        if (bedDistanceMm != null) tlvs.add(buildTlvInt(0x45u, bedDistanceMm))
        return build(0x04u, 0x01u, tlvs)
    }
}
