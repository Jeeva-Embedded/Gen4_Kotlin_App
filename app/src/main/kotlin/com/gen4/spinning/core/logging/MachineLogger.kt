package com.gen4.spinning.core.logging

import android.content.Context
import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val dfMotorNames: Map<UByte, String> = mapOf(
    0x01u.toUByte() to "Front Roller",
    0x02u.toUByte() to "Back Roller",
    0x03u.toUByte() to "Creel",
    0x05u.toUByte() to "Drafting",
    0x0Au.toUByte() to "Production",
)

val cardingMotorNames: Map<UByte, String> = mapOf(
    0x01u.toUByte() to "Cylinder",
    0x02u.toUByte() to "Beater",
    0x03u.toUByte() to "Cage",
    0x04u.toUByte() to "Cylinder Feed",
    0x05u.toUByte() to "Beater Feed",
    0x06u.toUByte() to "Coiler",
    0x07u.toUByte() to "Picker Cylinder",
    0x08u.toUByte() to "AF Feed",
    0x0Au.toUByte() to "Production",
)

val flyerMotorNames: Map<UByte, String> = mapOf(
    0x01u.toUByte() to "Flyer",
    0x02u.toUByte() to "Bobbin",
    0x03u.toUByte() to "Front Roller",
    0x04u.toUByte() to "Back Roller",
    0x05u.toUByte() to "Drafting",
    0x06u.toUByte() to "Winding",
    0x07u.toUByte() to "Lift",
    0x08u.toUByte() to "Lift Left",
    0x09u.toUByte() to "Lift Right",
    0x0Au.toUByte() to "Production",
)

val ringMotorNames: Map<UByte, String> = mapOf(
    0x01u.toUByte() to "Calender",
    0x07u.toUByte() to "Lift",
    0x08u.toUByte() to "Lift Left",
    0x09u.toUByte() to "Lift Right",
    0x0Au.toUByte() to "Production",
)

private const val CSV_HEADER =
    "Timestamp,Machine,State,Motor Name,MOSFET Temp (C),Motor Temp (C),RPM,Current (A),Power (W),Output (m),Delivery (m/min),Length (m),AL Status,Duct Sensor,Coiler Sensor,Pause State,Error Info,Error Source\n"

class LogSession(val file: File, private val writer: BufferedWriter) {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun writeRow(
        machine: String, state: String, motorName: String,
        mosfetTemp: String, motorTemp: String, rpm: String,
        current: String, power: String, outputMtrs: String,
        delivery: String, length: String,
        alStatus: String, ductSensor: String, coilerSensor: String,
        pauseState: String, errorInfo: String, errorSource: String,
    ) {
        val ts = sdf.format(Date())
        val row = "$ts,$machine,$state,$motorName,$mosfetTemp,$motorTemp,$rpm,$current,$power,$outputMtrs,$delivery,$length,$alStatus,$ductSensor,$coilerSensor,$pauseState,$errorInfo,$errorSource\n"
        try { writer.write(row); writer.flush() } catch (_: Exception) {}
    }

    fun close() { try { writer.close() } catch (_: Exception) {} }
}

fun createLogSession(context: Context, machine: String): LogSession? = try {
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    dir.mkdirs()
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val file = File(dir, "${machine}_$ts.csv")
    val writer = BufferedWriter(FileWriter(file))
    writer.write(CSV_HEADER)
    writer.flush()
    LogSession(file, writer)
} catch (_: Exception) { null }

fun listLogFiles(context: Context): List<File> {
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    return dir.listFiles { f -> f.extension == "csv" }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
}
