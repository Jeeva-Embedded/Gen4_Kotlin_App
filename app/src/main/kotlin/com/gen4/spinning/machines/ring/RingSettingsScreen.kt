package com.gen4.spinning.machines.ring

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.ui.components.FieldInput
import com.gen4.spinning.ui.components.SaveBanner
import com.gen4.spinning.ui.components.SettingsToolbar
import kotlin.math.PI

@Composable
fun RingSettingsScreen(vm: RingViewModel, onNavigatePid: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val saveResult by vm.saveResult.collectAsState()
    val readResult by vm.readResult.collectAsState()
    var showParams by remember { mutableStateOf(false) }

    if (showParams) {
        RingInternalParamsDialog(settings = settings, onDismiss = { showParams = false })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            FieldInput(label = "Input Yarn (count)", value = settings.inputYarn,
                onValueChange = { vm.updateSettings(settings.copy(inputYarn = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Output Yarn Dia (mm, blank=auto)", value = settings.outputYarnDia,
                onValueChange = { vm.updateSettings(settings.copy(outputYarnDia = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Spindle Speed (RPM)", value = settings.spindleSpeed,
                onValueChange = { vm.updateSettings(settings.copy(spindleSpeed = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Twist Per Inch", value = settings.twistPerInch,
                onValueChange = { vm.updateSettings(settings.copy(twistPerInch = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Package Height (mm)", value = settings.packageHeight,
                onValueChange = { vm.updateSettings(settings.copy(packageHeight = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Dia Build Factor", value = settings.diaBuildFactor,
                onValueChange = { vm.updateSettings(settings.copy(diaBuildFactor = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Winding Closeness (%)", value = settings.windingCloseness,
                onValueChange = { vm.updateSettings(settings.copy(windingCloseness = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Winding Offset Coils", value = settings.windingOffsetCoils,
                onValueChange = { vm.updateSettings(settings.copy(windingOffsetCoils = it)) })
            Spacer(Modifier.height(16.dp))
        }

        readResult?.let { SaveBanner(message = "Settings received", success = true) }
        saveResult?.let { SaveBanner(message = if (it) "Settings saved successfully" else "Save failed", success = it) }

        SettingsToolbar(
            onRead = { vm.sendRead() },
            onAll = null,
            onSave = { vm.sendSave() },
            onPid = onNavigatePid,
            onLimits = { showParams = true },
        )
    }
}

@Composable
private fun RingInternalParamsDialog(settings: RingSettings, onDismiss: () -> Unit) {
    val inputYarnCount = settings.inputYarn.toDoubleOrNull()
    val outputYarnDia = settings.outputYarnDia.toDoubleOrNull()
    val twistPerInch = settings.twistPerInch.toDoubleOrNull()
    val spindleSpeed = settings.spindleSpeed.toDoubleOrNull()
    val packageHeight = settings.packageHeight.toDoubleOrNull()
    val diaBuildFactor = settings.diaBuildFactor.toDoubleOrNull()
    val windingCloseness = settings.windingCloseness.toDoubleOrNull()
    val windingOffsetCoils = settings.windingOffsetCoils.toDoubleOrNull()

    val deliveryMtrMin = if (spindleSpeed != null && twistPerInch != null && twistPerInch != 0.0)
        (spindleSpeed / twistPerInch) * 0.0254 else null
    val calMotorRPM = deliveryMtrMin?.let { (it * 1000.0 / 141.37) * 6.91 }
    val outputCountInNm = inputYarnCount?.let { (it / 2.0) / 0.591 }

    val windingVelocity: Double? = if (deliveryMtrMin != null && outputYarnDia != null && windingCloseness != null) {
        val emptyBobbinCirc = 24.0 * PI
        ((deliveryMtrMin * 1000.0) / emptyBobbinCirc) * outputYarnDia * (windingCloseness / 100.0) / 60.0
    } else null
    val bindingVelocity = windingVelocity?.let { it * 2.0 }

    val chaseLength = 55.0
    val windingTimeSec = windingVelocity?.let { if (it > 0) chaseLength / it else null }
    val bindingTimeSec = bindingVelocity?.let { if (it > 0) chaseLength / it else null }
    val bindingLiftMotorRPM = windingVelocity?.let {
        val windingVelMmMin = it * 60.0
        (windingVelMmMin / 4.0) * 24.0 * 2.0
    }

    val htDiffPerStroke = if (windingOffsetCoils != null && windingCloseness != null
        && outputYarnDia != null && diaBuildFactor != null && diaBuildFactor != 0.0
    ) windingOffsetCoils * (windingCloseness / 100.0) * outputYarnDia / diaBuildFactor else null

    val strokesPerDoff = if (packageHeight != null && htDiffPerStroke != null && htDiffPerStroke != 0.0)
        (packageHeight - chaseLength) / htDiffPerStroke else null
    val maxStrokesPerZ = if (htDiffPerStroke != null && htDiffPerStroke != 0.0)
        chaseLength / htDiffPerStroke else null
    val estFullBobbinWidth = if (maxStrokesPerZ != null && outputYarnDia != null)
        maxStrokesPerZ * (outputYarnDia * 2.0) + 24.0 else null

    val doffTime = if (strokesPerDoff != null && windingTimeSec != null && bindingTimeSec != null)
        strokesPerDoff * (windingTimeSec + bindingTimeSec) / 60.0 else null
    val totalYarnLength = if (doffTime != null && deliveryMtrMin != null) doffTime * deliveryMtrMin else null
    val totalYarnWeightGrams = if (totalYarnLength != null && outputCountInNm != null && outputCountInNm != 0.0)
        totalYarnLength / outputCountInNm else null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Internal Parameters", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                deliveryMtrMin?.let { Text("Delivery:  ${"%.2f".format(it)} m/min", fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
                ParamRow("Calender Motor RPM", calMotorRPM, "%.1f")
                outputCountInNm?.let { Text("Output Count (Nm):  ${"%.2f".format(it)}", fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
                ParamRow("Winding Velocity", windingVelocity, "%.2f mm/s", warnAbove = Double.MAX_VALUE)
                ParamRow("Binding Velocity", bindingVelocity, "%.2f mm/s", warnAbove = Double.MAX_VALUE)
                ParamRow("Winding Time", windingTimeSec, "%.1f sec", warnAbove = Double.MAX_VALUE)
                ParamRow("Binding Time", bindingTimeSec, "%.1f sec", warnAbove = Double.MAX_VALUE)
                ParamRow("Lift Motor RPM (Binding)", bindingLiftMotorRPM, "%.0f")
                ParamRow("Ht Diff Per Stroke", htDiffPerStroke, "%.2f mm", warnAbove = Double.MAX_VALUE)
                ParamRow("Strokes Per Doff", strokesPerDoff, "%.0f", warnAbove = Double.MAX_VALUE)
                ParamRow("Max Strokes Per Z", maxStrokesPerZ, "%.0f", warnAbove = Double.MAX_VALUE)
                ParamRow("Est Full Bobbin Width", estFullBobbinWidth, "%.2f", warnAbove = Double.MAX_VALUE)
                ParamRow("Doff Time", doffTime, "%.2f min", warnAbove = Double.MAX_VALUE)
                ParamRow("Total Yarn Length", totalYarnLength, "%.2f m", warnAbove = Double.MAX_VALUE)
                ParamRow("Total Yarn Weight", totalYarnWeightGrams, "%.2f g", warnAbove = Double.MAX_VALUE)
            }
        },
    )
}

@Composable
private fun ParamRow(label: String, value: Double?, fmt: String, warnAbove: Double = 1450.0) {
    val text = if (value != null) fmt.format(value) else "–"
    val color = if (value != null && value > warnAbove) Color.Red else Color.Unspecified
    Text(
        text = "$label:  $text",
        fontSize = 15.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}
