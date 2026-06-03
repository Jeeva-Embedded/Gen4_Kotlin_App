package com.gen4.spinning.machines.carding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.core.protocol.CardingProtocol
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.components.StatusBox
import com.gen4.spinning.ui.theme.SpinColors

private data class MotorEntry(val label: String, val id: UByte)

private val cardingMotors = listOf(
    MotorEntry("Cylinder",       CardingProtocol.MOTOR_CYLINDER),
    MotorEntry("Beater",         CardingProtocol.MOTOR_BEATER),
    MotorEntry("Cage",           CardingProtocol.MOTOR_CAGE),
    MotorEntry("Cylinder Feed",  CardingProtocol.MOTOR_CYLINDER_FEED),
    MotorEntry("Beater Feed",    CardingProtocol.MOTOR_BEATER_FEED),
    MotorEntry("Coiler",         CardingProtocol.MOTOR_COILER),
    MotorEntry("Picker Cylinder",CardingProtocol.MOTOR_PICKER_CYLINDER),
    MotorEntry("AF Feed",        CardingProtocol.MOTOR_AF_FEED),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardingTestsScreen(vm: CardingViewModel) {
    val diagnosisState by vm.diagnosisState.collectAsState()

    var motorIndex by remember { mutableIntStateOf(0) }
    var controlIndex by remember { mutableIntStateOf(0) }
    var directionIndex by remember { mutableIntStateOf(0) }
    var speedPct by remember { mutableFloatStateOf(50f) }
    var durationSec by remember { mutableFloatStateOf(5f) }

    var motorExpanded by remember { mutableStateOf(false) }
    var controlExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }

    if (diagnosisState.isDiagnosing) {
        CardingDiagnosisResultScreen(state = diagnosisState, onBack = { vm.stopAndClearDiagnosis() })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = motorExpanded,
            onExpandedChange = { motorExpanded = it },
        ) {
            OutlinedTextField(
                value = cardingMotors[motorIndex].label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Motor") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(motorExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = motorExpanded, onDismissRequest = { motorExpanded = false }) {
                cardingMotors.forEachIndexed { i, motor ->
                    DropdownMenuItem(
                        text = { Text(motor.label) },
                        onClick = { motorIndex = i; motorExpanded = false },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = controlExpanded,
            onExpandedChange = { controlExpanded = it },
        ) {
            OutlinedTextField(
                value = if (controlIndex == 0) "Open Loop" else "Closed Loop",
                onValueChange = {},
                readOnly = true,
                label = { Text("Control Loop") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(controlExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = controlExpanded, onDismissRequest = { controlExpanded = false }) {
                listOf("Open Loop", "Closed Loop").forEachIndexed { i, label ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { controlIndex = i; controlExpanded = false })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = directionExpanded,
            onExpandedChange = { directionExpanded = it },
        ) {
            OutlinedTextField(
                value = if (directionIndex == 0) "Default" else "Reverse",
                onValueChange = {},
                readOnly = true,
                label = { Text("Direction") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(directionExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = directionExpanded, onDismissRequest = { directionExpanded = false }) {
                listOf("Default", "Reverse").forEachIndexed { i, label ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { directionIndex = i; directionExpanded = false })
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Speed: ${speedPct.toInt()}%", fontSize = 14.sp)
        Slider(
            value = speedPct,
            onValueChange = { speedPct = it },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = SpinColors.LightGreen, activeTrackColor = SpinColors.LightGreen),
        )

        Spacer(Modifier.height(8.dp))

        Text("Duration: ${durationSec.toInt()} s", fontSize = 14.sp)
        Slider(
            value = durationSec,
            onValueChange = { durationSec = it },
            valueRange = 1f..60f,
            colors = SliderDefaults.colors(thumbColor = SpinColors.Blue, activeTrackColor = SpinColors.Blue),
        )

        Spacer(Modifier.height(24.dp))

        GradientButton(
            text = "RUN DIAGNOSE",
            onClick = {
                val motor = cardingMotors[motorIndex]
                vm.startDiagnosis(motor.label)
                vm.sendDiagnostic(
                    motorId     = motor.id,
                    controlType = if (controlIndex == 0) 0x01u else 0x02u,
                    direction   = if (directionIndex == 0) 0x00u else 0x01u,
                    speedPct    = speedPct.toInt(),
                    durationSec = durationSec.toInt(),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CardingDiagnosisResultScreen(state: CardingDiagnosisState, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.motorLabel,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = SpinColors.Blue,
        )
        Text(text = "Diagnosis Running", fontSize = 14.sp, color = SpinColors.Blue)
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusBox(label = "RPM",          value = state.rpm, modifier = Modifier.weight(1f))
            StatusBox(label = "PWM (0-1500)", value = state.pwm, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusBox(label = "Current (A)", value = state.current, modifier = Modifier.weight(1f))
            StatusBox(label = "Power (W)",   value = state.power,   modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.weight(1f))

        GradientButton(
            text = "Stop Diagnosis",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
