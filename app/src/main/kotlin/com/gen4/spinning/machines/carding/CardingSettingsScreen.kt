package com.gen4.spinning.machines.carding

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

private fun validateSettings(settings: CardingSettings): String? {
    val deliverySpeed = settings.deliverySpeed.toDoubleOrNull() ?: return "Invalid delivery speed"
    if (deliverySpeed < 2.5 || deliverySpeed > 50.0) return "Delivery speed must be between 2.5 and 50 m/min."
    val cardFeedRatio = settings.cardFeedRatio.toDoubleOrNull() ?: return "Invalid card feed ratio"
    if (cardFeedRatio < 0.4 || cardFeedRatio > 20.0) return "Card feed ratio must be between 0.4 and 20."
    val lengthLimit = settings.lengthLimit.toIntOrNull() ?: return "Invalid length limit"
    if (lengthLimit < 100 || lengthLimit > 1000) return "Length limit must be between 100 and 1000 m."
    val cylSpeed = settings.cylSpeed.toIntOrNull() ?: return "Invalid cylinder speed"
    if (cylSpeed < 300 || cylSpeed > 875) return "Cylinder speed must be between 300 and 875 RPM."
    val btrSpeed = settings.btrSpeed.toIntOrNull() ?: return "Invalid beater speed"
    if (btrSpeed < 300 || btrSpeed > 650) return "Beater speed must be between 300 and 650 RPM."
    val pickerCylSpeed = settings.pickerCylSpeed.toIntOrNull() ?: return "Invalid picker cylinder speed"
    if (pickerCylSpeed < 300 || pickerCylSpeed > 700) return "Picker cylinder speed must be between 300 and 700 RPM."
    val btrFeed = settings.btrFeed.toIntOrNull() ?: return "Invalid beater feed"
    if (btrFeed < 1 || btrFeed > 11) return "Beater feed must be between 1 and 11."
    val afFeed = settings.afFeed.toIntOrNull() ?: return "Invalid AF feed"
    if (afFeed < 1 || afFeed > 8) return "AF feed must be between 1 and 8."
    return null
}

@Composable
fun CardingSettingsScreen(vm: CardingViewModel, onNavigatePid: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val saveResult by vm.saveResult.collectAsState()
    val readResult by vm.readResult.collectAsState()
    val defaultApplied by vm.defaultApplied.collectAsState()
    val allowChange by vm.settingsChangeAllowed.collectAsState()
    var showParams by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    if (showParams) {
        CardingInternalParamsDialog(settings = settings, onDismiss = { showParams = false })
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
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            FieldInput(
                label = "Delivery Speed (m/min)",
                value = settings.deliverySpeed,
                onValueChange = { vm.updateSettings(settings.copy(deliverySpeed = it)) },
            )
            Spacer(Modifier.height(8.dp))
            FieldInput(
                label = "Card Feed Ratio",
                value = settings.cardFeedRatio,
                onValueChange = { vm.updateSettings(settings.copy(cardFeedRatio = it)) },
            )
            Spacer(Modifier.height(8.dp))
            FieldInput(
                label = "Length Limit (m)",
                value = settings.lengthLimit,
                onValueChange = { vm.updateSettings(settings.copy(lengthLimit = it)) },
            )
            Spacer(Modifier.height(8.dp))
            FieldInput(
                label = "Cylinder Speed (RPM)",
                value = settings.cylSpeed,
                onValueChange = { vm.updateSettings(settings.copy(cylSpeed = it)) },
                enabled = allowChange,
            )
            Spacer(Modifier.height(8.dp))
            FieldInput(
                label = "Beater Speed (RPM)",
                value = settings.btrSpeed,
                onValueChange = { vm.updateSettings(settings.copy(btrSpeed = it)) },
                enabled = allowChange,
            )
            Spacer(Modifier.height(8.dp))
            FieldInput(
                label = "Picker Cylinder Speed (RPM)",
                value = settings.pickerCylSpeed,
                onValueChange = { vm.updateSettings(settings.copy(pickerCylSpeed = it)) },
                enabled = allowChange,
            )
            Spacer(Modifier.height(8.dp))
            FieldInput(
                label = "Beater Feed",
                value = settings.btrFeed,
                onValueChange = { vm.updateSettings(settings.copy(btrFeed = it)) },
            )
            Spacer(Modifier.height(8.dp))
            FieldInput(
                label = "AF Feed",
                value = settings.afFeed,
                onValueChange = { vm.updateSettings(settings.copy(afFeed = it)) },
            )
            Spacer(Modifier.height(16.dp))
        }

        readResult?.let {
            SaveBanner(
                message = if (it) "Settings received" else "Settings not received",
                success = it,
            )
        }
        defaultApplied?.let { SaveBanner(message = "Defaults applied", success = true) }
        saveResult?.let {
            SaveBanner(
                message = if (it) "Settings saved successfully" else "Save failed",
                success = it,
            )
        }

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
private fun CardingInternalParamsDialog(settings: CardingSettings, onDismiss: () -> Unit) {
    val deliverySpeed = settings.deliverySpeed.toDoubleOrNull()
    val cardFeedRatio = settings.cardFeedRatio.toDoubleOrNull()
    val cylSpeed = settings.cylSpeed.toDoubleOrNull()
    val btrSpeed = settings.btrSpeed.toDoubleOrNull()

    val cylinderMotorRPM = cylSpeed?.let { it / 1.2 }
    val beaterMotorRPM = btrSpeed?.let { it / 1.2 }
    val cageMotorRPM = deliverySpeed?.let { (it * 1000.0 / 213.63) * 6.91 }
    val coilerMotorRPM = if (deliverySpeed != null && cardFeedRatio != null) {
        ((deliverySpeed * 1000.0 * cardFeedRatio) / 194.779) / 1.656 * 6.91
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Internal Parameters", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ParamRow("Cylinder Motor RPM", cylinderMotorRPM, "%.0f")
                ParamRow("Beater Motor RPM", beaterMotorRPM, "%.0f")
                ParamRow("Cage Motor RPM", cageMotorRPM, "%.0f")
                ParamRow("Coiler Motor RPM", coilerMotorRPM, "%.0f")
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
