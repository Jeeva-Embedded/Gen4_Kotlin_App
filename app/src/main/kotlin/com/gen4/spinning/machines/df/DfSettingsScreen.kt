package com.gen4.spinning.machines.df

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

private fun validateSettings(settings: DfSettings): String? {
    val speed = settings.deliverySpeed.toIntOrNull() ?: return "Invalid delivery speed"
    if (speed < 50 || speed > 300) return "Delivery speed must be between 50 and 300 m/min."
    val draft = settings.draft.toDoubleOrNull() ?: return "Invalid draft"
    if (draft < 3.0 || draft > 12.0) return "Draft must be between 3 and 12."
    val lengthLimit = settings.lengthLimit.toIntOrNull() ?: return "Invalid length limit"
    if (lengthLimit < 5 || lengthLimit > 1250) return "Length limit must be between 5 and 1250 m."
    val rampUp = settings.rampUpTime.toIntOrNull() ?: return "Invalid ramp up time"
    if (rampUp < 3 || rampUp > 15) return "Ramp up time must be between 3 and 15 s."
    val rampDown = settings.rampDownTime.toIntOrNull() ?: return "Invalid ramp down time"
    if (rampDown < 3 || rampDown > 15) return "Ramp down time must be between 3 and 15 s."
    val creelTension = settings.creelTensionFactor.toDoubleOrNull() ?: return "Invalid creel tension factor"
    if (creelTension < 0.25 || creelTension > 5.0) return "Creel tension factor must be between 0.25 and 5."
    val frMotorRpm = speed * 1000.0 / 125.6 * 0.5
    val brMotorRpm = (speed * 1000.0 / draft) / 94.2 * 3.07
    val maxRpm = maxOf(frMotorRpm, brMotorRpm)
    if (maxRpm > 1450) return "Motor RPM (${maxRpm.toInt()}) exceeds 1450. Reduce delivery speed or increase draft."
    return null
}

@Composable
fun DfSettingsScreen(vm: DfViewModel, onNavigatePid: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val saveResult by vm.saveResult.collectAsState()
    val readResult by vm.readResult.collectAsState()
    val defaultApplied by vm.defaultApplied.collectAsState()
    var showParams by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    if (showParams) {
        DfInternalParamsDialog(settings = settings, onDismiss = { showParams = false })
    }

    validationError?.let { msg ->
        AlertDialog(
            onDismissRequest = { validationError = null },
            confirmButton = { TextButton(onClick = { validationError = null }) { Text("OK") } },
            title = { Text("Invalid Settings", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            FieldInput(label = "Delivery Speed (m/min)", value = settings.deliverySpeed,
                onValueChange = { vm.updateSettings(settings.copy(deliverySpeed = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Draft", value = settings.draft,
                onValueChange = { vm.updateSettings(settings.copy(draft = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Length Limit (m)", value = settings.lengthLimit,
                onValueChange = { vm.updateSettings(settings.copy(lengthLimit = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Ramp Up Time (s)", value = settings.rampUpTime,
                onValueChange = { vm.updateSettings(settings.copy(rampUpTime = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Ramp Down Time (s)", value = settings.rampDownTime,
                onValueChange = { vm.updateSettings(settings.copy(rampDownTime = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Creel Tension Factor", value = settings.creelTensionFactor,
                onValueChange = { vm.updateSettings(settings.copy(creelTensionFactor = it)) })
            Spacer(Modifier.height(16.dp))
        }

        readResult?.let { SaveBanner(message = if (it) "Settings received" else "Settings not received", success = it) }
        defaultApplied?.let { SaveBanner(message = "Defaults received", success = true) }
        saveResult?.let { SaveBanner(message = if (it) "Settings saved successfully" else "Save failed", success = it) }

        SettingsToolbar(
            onRead = { vm.sendRead() },
            onAll = { vm.resetToDefaults() },
            onSave = {
                val err = validateSettings(settings)
                if (err != null) validationError = err else vm.sendSave()
            },
            onPid = onNavigatePid,
            onLimits = { showParams = true },
        )
    }
}

@Composable
private fun DfInternalParamsDialog(settings: DfSettings, onDismiss: () -> Unit) {
    val deliverySpeed = settings.deliverySpeed.toDoubleOrNull()
    val draft = settings.draft.toDoubleOrNull()

    val frRpm = deliverySpeed?.let { it * 1000.0 / 125.6 }
    val frMotorRPM = frRpm?.let { it * 0.5 }
    val brRpm = if (deliverySpeed != null && draft != null && draft != 0.0)
        (deliverySpeed * 1000.0 / draft) / 94.2 else null
    val brMotorRPM = brRpm?.let { it * 3.07 }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Internal Parameters", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ParamRow("FR Roller RPM", frRpm, "%.1f")
                ParamRow("FR Motor RPM", frMotorRPM, "%.1f")
                ParamRow("BR Roller RPM", brRpm, "%.1f")
                ParamRow("BR Motor RPM", brMotorRPM, "%.1f")
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
