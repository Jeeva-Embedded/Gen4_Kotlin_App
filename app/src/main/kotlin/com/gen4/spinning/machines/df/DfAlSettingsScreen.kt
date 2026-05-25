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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.gen4.spinning.ui.components.FieldInput
import com.gen4.spinning.ui.components.GradientAppBar
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.components.SaveBanner

@Composable
fun DfAlSettingsScreen(vm: DfViewModel, onBack: () -> Unit) {
    val alSettings by vm.alSettings.collectAsState()
    val alReadResult by vm.alReadResult.collectAsState()
    val alSaveResult by vm.alSaveResult.collectAsState()
    val calibration by vm.calibrationState.collectAsState()
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            GradientAppBar(
                title = "AL Settings",
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = androidx.compose.ui.graphics.Color.White)
                    }
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                FieldInput(
                    label = "KP Gain",
                    value = alSettings.kp,
                    onValueChange = { vm.updateAlSettings(alSettings.copy(kp = it)) },
                )

                FieldInput(
                    label = "Sliver N+1 Threshold",
                    value = alSettings.sliver6,
                    onValueChange = { vm.updateAlSettings(alSettings.copy(sliver6 = it)) },
                )

                FieldInput(
                    label = "Sliver N Threshold",
                    value = alSettings.sliver5,
                    onValueChange = { vm.updateAlSettings(alSettings.copy(sliver5 = it)) },
                )

                FieldInput(
                    label = "Sliver N-1 Threshold",
                    value = alSettings.sliver4,
                    onValueChange = { vm.updateAlSettings(alSettings.copy(sliver4 = it)) },
                )

                FieldInput(
                    label = "Target g/m",
                    value = alSettings.target,
                    onValueChange = { vm.updateAlSettings(alSettings.copy(target = it)) },
                )

                validationError?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GradientButton(
                        text = "GET",
                        onClick = {
                            validationError = null
                            vm.sendAlGet()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    GradientButton(
                        text = "SAVE",
                        onClick = {
                            val ok = vm.sendAlSave()
                            validationError = if (ok) null else
                                "Check: KP (0 < KP ≤ 1), Sliver N+1 < Sliver N < Sliver N-1, Target (4 < g/m ≤ 6)"
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "AL Sensor Calibration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )

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
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
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
                    ) {
                        Text("STOP")
                    }
                } else {
                    Button(
                        onClick = { vm.sendCalibrationStart() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("START")
                    }
                }
            }
        }

        // Banner: save result takes priority, then read result
        val banner = alSaveResult?.let { ok ->
            (if (ok) "Settings Saved" else "Save Failed") to ok
        } ?: alReadResult?.let { ok ->
            (if (ok) "Settings Received" else "No Response") to ok
        }

        banner?.let { (msg, success) ->
            SaveBanner(
                message = msg,
                success = success,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
