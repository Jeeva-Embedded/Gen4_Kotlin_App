package com.gen4.spinning.machines.flyer

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

private fun validateSettings(settings: FlyerSettings): String? {
    val spindleSpeed = settings.spindleSpeed.toIntOrNull() ?: return "Invalid spindle speed"
    if (spindleSpeed < 500 || spindleSpeed > 1100) return "Spindle speed must be between 500 and 1100 RPM."
    val draft = settings.draft.toDoubleOrNull() ?: return "Invalid draft"
    if (draft < 5.0 || draft > 15.0) return "Draft must be between 5 and 15."
    val twistPerInch = settings.twistPerInch.toDoubleOrNull() ?: return "Invalid twist per inch"
    if (twistPerInch < 1.0 || twistPerInch > 1.6) return "Twist per inch must be between 1.0 and 1.6."
    val rtf = settings.rtf.toDoubleOrNull() ?: return "Invalid RTF"
    if (rtf < 0.5 || rtf > 1.5) return "RTF must be between 0.5 and 1.5."
    val layers = settings.layers.toIntOrNull() ?: return "Invalid layers"
    if (layers < 1 || layers > 100) return "Layers must be between 1 and 100."
    val maxHeight = settings.maxHeight.toIntOrNull() ?: return "Invalid max height"
    if (maxHeight < 250 || maxHeight > 300) return "Max height must be between 250 and 300 mm."
    val rovingWidth = settings.rovingWidth.toDoubleOrNull() ?: return "Invalid roving width"
    if (rovingWidth < 1.0 || rovingWidth > 8.0) return "Roving width must be between 1 and 8 mm."
    val deltaBobbinDia = settings.deltaBobbinDia.toDoubleOrNull() ?: return "Invalid delta bobbin dia"
    if (deltaBobbinDia < 0.3 || deltaBobbinDia > 2.5) return "Delta bobbin dia must be between 0.3 and 2.5 mm."
    val bareBobbinDia = settings.bareBobbinDia.toIntOrNull() ?: return "Invalid bare bobbin dia"
    if (bareBobbinDia < 46 || bareBobbinDia > 52) return "Bare bobbin dia must be between 46 and 52 mm."
    val rampUp = settings.rampUpTime.toIntOrNull() ?: return "Invalid ramp up time"
    if (rampUp < 5 || rampUp > 20) return "Ramp up time must be between 5 and 20 s."
    val rampDown = settings.rampDownTime.toIntOrNull() ?: return "Invalid ramp down time"
    if (rampDown < 5 || rampDown > 20) return "Ramp down time must be between 5 and 20 s."
    val changeLayerTime = settings.changeLayerTime.toIntOrNull() ?: return "Invalid change layer time"
    if (changeLayerTime < 200 || changeLayerTime > 2500) return "Change layer time must be between 200 and 2500 ms."
    val coneAngleFactor = settings.coneAngleFactor.toDoubleOrNull() ?: return "Invalid cone angle factor"
    if (coneAngleFactor < 0.1 || coneAngleFactor > 3.0) return "Cone angle factor must be between 0.1 and 3."
    return null
}

@Composable
fun FlyerSettingsScreen(vm: FlyerViewModel, onNavigatePid: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val saveResult by vm.saveResult.collectAsState()
    val readResult by vm.readResult.collectAsState()
    val defaultApplied by vm.defaultApplied.collectAsState()
    var showParams by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    if (showParams) {
        FlyerInternalParamsDialog(settings = settings, onDismiss = { showParams = false })
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
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            FieldInput(label = "Spindle Speed (RPM)", value = settings.spindleSpeed,
                onValueChange = { vm.updateSettings(settings.copy(spindleSpeed = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Draft", value = settings.draft,
                onValueChange = { vm.updateSettings(settings.copy(draft = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Twist Per Inch", value = settings.twistPerInch,
                onValueChange = { vm.updateSettings(settings.copy(twistPerInch = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "RTF", value = settings.rtf,
                onValueChange = { vm.updateSettings(settings.copy(rtf = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Layers", value = settings.layers,
                onValueChange = { vm.updateSettings(settings.copy(layers = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Max Height (mm)", value = settings.maxHeight,
                onValueChange = { vm.updateSettings(settings.copy(maxHeight = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Roving Width (mm)", value = settings.rovingWidth,
                onValueChange = { vm.updateSettings(settings.copy(rovingWidth = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Delta Bobbin Dia (mm)", value = settings.deltaBobbinDia,
                onValueChange = { vm.updateSettings(settings.copy(deltaBobbinDia = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Bare Bobbin Dia (mm)", value = settings.bareBobbinDia,
                onValueChange = { vm.updateSettings(settings.copy(bareBobbinDia = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Ramp Up Time (s)", value = settings.rampUpTime,
                onValueChange = { vm.updateSettings(settings.copy(rampUpTime = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Ramp Down Time (s)", value = settings.rampDownTime,
                onValueChange = { vm.updateSettings(settings.copy(rampDownTime = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Change Layer Time (ms)", value = settings.changeLayerTime,
                onValueChange = { vm.updateSettings(settings.copy(changeLayerTime = it)) })
            Spacer(Modifier.height(8.dp))
            FieldInput(label = "Cone Angle Factor", value = settings.coneAngleFactor,
                onValueChange = { vm.updateSettings(settings.copy(coneAngleFactor = it)) })
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
private fun FlyerInternalParamsDialog(settings: FlyerSettings, onDismiss: () -> Unit) {
    val spindleSpeed = settings.spindleSpeed.toDoubleOrNull()
    val draft = settings.draft.toDoubleOrNull()
    val twistPerInch = settings.twistPerInch.toDoubleOrNull()
    val layers = settings.layers.toDoubleOrNull()
    val maxHeight = settings.maxHeight.toDoubleOrNull()
    val rovingWidth = settings.rovingWidth.toDoubleOrNull()
    val deltaBobbinDia = settings.deltaBobbinDia.toDoubleOrNull()
    val bareBobbinDia = settings.bareBobbinDia.toDoubleOrNull()
    val coneAngleFactor = settings.coneAngleFactor.toDoubleOrNull()

    val flyerMotorRPM = spindleSpeed
    val deliveryMtrMin = if (spindleSpeed != null && twistPerInch != null && twistPerInch != 0.0)
        (spindleSpeed / twistPerInch) * 0.0254 else null

    val frRpm = if (deliveryMtrMin != null) (deliveryMtrMin * 1000.0) / 94.248 else null
    val frMotorRPM = frRpm?.let { it * 3.33 }
    val brMotorRPM = if (frRpm != null && draft != null && draft != 0.0)
        (frRpm * 22.642) / (draft / 1.5) else null

    val maxLayers: Double? = if (maxHeight != null && rovingWidth != null && rovingWidth != 0.0
        && bareBobbinDia != null && deltaBobbinDia != null && deltaBobbinDia != 0.0
    ) {
        val ml1 = maxHeight / rovingWidth
        val ml2 = (140.0 - bareBobbinDia) / deltaBobbinDia
        (if (ml1 >= ml2) ml2 else ml1) - 5.0
    } else null

    val bobbinMotorRPM = if (deliveryMtrMin != null && bareBobbinDia != null && spindleSpeed != null) {
        val deltaRpm = (deliveryMtrMin * 1000.0) / (bareBobbinDia * PI)
        (spindleSpeed + deltaRpm)
    } else null

    val strokeDistPerSec: Double? = if (deliveryMtrMin != null && bareBobbinDia != null
        && rovingWidth != null && coneAngleFactor != null
    ) {
        val deltaRpm = (deliveryMtrMin * 1000.0) / (bareBobbinDia * PI)
        val strokeDelta = rovingWidth * coneAngleFactor
        (deltaRpm * strokeDelta) / 60.0
    } else null

    val liftMotorRPM = strokeDistPerSec?.let { (it * 60.0 / 4.0) * 9.0 }

    val totalRunTimeMin: Double? = if (deliveryMtrMin != null && bareBobbinDia != null
        && deltaBobbinDia != null && rovingWidth != null && coneAngleFactor != null && maxLayers != null
    ) {
        val strokeDelta = rovingWidth * coneAngleFactor
        var totalS = 0.0
        for (i in 0 until maxLayers.toInt()) {
            val bd = bareBobbinDia + i * deltaBobbinDia
            val dRpm = (deliveryMtrMin * 1000.0) / (bd * PI)
            val strokeHt = maxHeight!! - strokeDelta * i
            val strokeDistSec = (dRpm * strokeDelta) / 60.0
            if (strokeDistSec > 0.0) totalS += strokeHt / strokeDistSec
        }
        totalS / 60.0
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Internal Parameters", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                deliveryMtrMin?.let { ParamRow("Delivery", it, "%.2f m/min") }
                ParamRow("Stroke Velocity", strokeDistPerSec, "%.2f mm/s", warnAbove = 5.5)
                ParamRow("Max Layers Possible", maxLayers, "%.0f")
                if (layers != null && maxLayers != null) {
                    Text(
                        "Runtime ${layers.toInt()} layers: ${totalRunTimeMin?.let { "%.2f min".format(it) } ?: "–"}",
                        fontSize = 15.sp,
                        color = if (layers > maxLayers) Color.Red else Color.Unspecified,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
                ParamRow("Flyer Motor RPM", flyerMotorRPM, "%.0f")
                ParamRow("Bobbin Motor RPM", bobbinMotorRPM, "%.0f")
                ParamRow("FR Motor RPM", frMotorRPM, "%.0f")
                ParamRow("BR Motor RPM", brMotorRPM, "%.0f")
                ParamRow("Lift Motor RPM", liftMotorRPM, "%.0f")
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
