package com.gen4.spinning.machines.df

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.ui.components.FieldInput
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.components.SaveBanner
import com.gen4.spinning.ui.theme.SpinColors

@Composable
fun DfOptionsScreen(vm: DfViewModel) {
    val logEnabled   by vm.logEnabled.collectAsState()
    val logMessage   by vm.logMessage.collectAsState()
    val alSettings   by vm.alSettings.collectAsState()
    val alReadResult by vm.alReadResult.collectAsState()
    val alSaveResult by vm.alSaveResult.collectAsState()
    val calibration  by vm.calibrationState.collectAsState()
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Enable Logging ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Enable Logging", color = SpinColors.Blue, fontSize = 18.sp)
                Switch(
                    checked = logEnabled,
                    onCheckedChange = { vm.sendLog(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpinColors.LightGreen,
                        checkedTrackColor = SpinColors.LightGreen.copy(alpha = 0.4f),
                    ),
                )
            }

            HorizontalDivider()

            // ── AL Settings ───────────────────────────────────────────────────
            Text("AL Settings", color = SpinColors.Blue, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            FieldInput(
                label = "KP Gain",
                value = alSettings.kp,
                onValueChange = { vm.updateAlSettings(alSettings.copy(kp = it)) },
            )
            FieldInput(
                label = "Sliver 6 Threshold",
                value = alSettings.sliver6,
                onValueChange = { vm.updateAlSettings(alSettings.copy(sliver6 = it)) },
            )
            FieldInput(
                label = "Sliver 5 Threshold",
                value = alSettings.sliver5,
                onValueChange = { vm.updateAlSettings(alSettings.copy(sliver5 = it)) },
            )
            FieldInput(
                label = "Sliver 4 Threshold",
                value = alSettings.sliver4,
                onValueChange = { vm.updateAlSettings(alSettings.copy(sliver4 = it)) },
            )
            FieldInput(
                label = "Target g/m",
                value = alSettings.target,
                onValueChange = { vm.updateAlSettings(alSettings.copy(target = it)) },
            )

            validationError?.let {
                Text(text = it, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GradientButton(
                    text = "GET",
                    onClick = { validationError = null; vm.sendAlGet() },
                    modifier = Modifier.weight(1f),
                )
                GradientButton(
                    text = "SAVE",
                    onClick = {
                        val ok = vm.sendAlSave()
                        validationError = if (ok) null else
                            "Check: KP (0 < KP ≤ 1), Sliver 6 < Sliver 5 < Sliver 4, Target (4 < g/m ≤ 6)"
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider()

            // ── AL Calibration ────────────────────────────────────────────────
            Text("AL Calibration", color = SpinColors.Blue, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            Text(
                text = when (calibration.status) {
                    CalibrationStatus.IDLE       -> "Idle"
                    CalibrationStatus.COLLECTING -> "Collecting..."
                    CalibrationStatus.DONE       -> "Done"
                },
                fontSize = 14.sp,
                color = Color.Gray,
            )

            if (calibration.status == CalibrationStatus.COLLECTING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (calibration.status == CalibrationStatus.DONE) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = calibration.avgValue.toString(),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "avg  (${calibration.count} samples)",
                        fontSize = 13.sp,
                        color = Color.Gray,
                    )
                }
            }

            if (calibration.status == CalibrationStatus.COLLECTING) {
                OutlinedButton(
                    onClick = { vm.sendCalibrationStop() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("STOP") }
            } else {
                Button(
                    onClick = { vm.sendCalibrationStart() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("START") }
            }

            HorizontalDivider()

            // ── Reset Length Counter ──────────────────────────────────────────
            GradientButton(
                text = "Reset Length Counter",
                onClick = { vm.sendResetLengthCounter() },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
        }

        // ── Banners ───────────────────────────────────────────────────────────
        val alBanner = alSaveResult?.let { ok ->
            (if (ok) "AL Settings Saved" else "AL Save Failed") to ok
        } ?: alReadResult?.let { ok ->
            (if (ok) "AL Settings Received" else "No Response") to ok
        }

        (alBanner ?: logMessage?.let { it to true })?.let { (msg, success) ->
            SaveBanner(
                message = msg,
                success = success,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
