package com.gen4.spinning.machines.ring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.derivedStateOf
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
import com.gen4.spinning.core.protocol.RingProtocol
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.components.StatusBox
import com.gen4.spinning.ui.theme.SpinColors

private data class RingMotorEntry(val label: String, val id: UByte, val isLift: Boolean = false)

private val ringMotors = listOf(
    RingMotorEntry("Calender",   RingProtocol.MOTOR_CALENDER),
    RingMotorEntry("Lift",       RingProtocol.MOTOR_LIFT,       isLift = true),
    RingMotorEntry("Lift Left",  RingProtocol.MOTOR_LIFT_LEFT,  isLift = true),
    RingMotorEntry("Lift Right", RingProtocol.MOTOR_LIFT_RIGHT, isLift = true),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingTestsScreen(vm: RingViewModel) {
    val diagnosisState by vm.diagnosisState.collectAsState()

    if (diagnosisState.isDiagnosing) {
        RingDiagnosisResultScreen(state = diagnosisState, onBack = { vm.sendStopDiagnosis(); vm.clearDiagnosis() })
        return
    }

    var motorIndex by remember { mutableIntStateOf(0) }
    var controlIndex by remember { mutableIntStateOf(0) }
    var directionIndex by remember { mutableIntStateOf(0) }
    var liftCmdIndex by remember { mutableIntStateOf(0) }
    var speedPct by remember { mutableFloatStateOf(50f) }
    var durationSec by remember { mutableFloatStateOf(5f) }
    var bedDistance by remember { mutableFloatStateOf(10f) }

    var motorExpanded by remember { mutableStateOf(false) }
    var controlExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }
    var liftCmdExpanded by remember { mutableStateOf(false) }

    val isLift by remember { derivedStateOf { ringMotors[motorIndex].isLift } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        ExposedDropdownMenuBox(expanded = motorExpanded, onExpandedChange = { motorExpanded = it }) {
            OutlinedTextField(
                value = ringMotors[motorIndex].label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Motor") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(motorExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = motorExpanded, onDismissRequest = { motorExpanded = false }) {
                ringMotors.forEachIndexed { i, m ->
                    DropdownMenuItem(text = { Text(m.label) }, onClick = { motorIndex = i; motorExpanded = false })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(expanded = controlExpanded, onExpandedChange = { controlExpanded = it }) {
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

        if (isLift) {
            ExposedDropdownMenuBox(expanded = liftCmdExpanded, onExpandedChange = { liftCmdExpanded = it }) {
                OutlinedTextField(
                    value = listOf("Up", "Down", "Stop")[liftCmdIndex],
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Lift Command") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(liftCmdExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = liftCmdExpanded, onDismissRequest = { liftCmdExpanded = false }) {
                    listOf("Up", "Down", "Stop").forEachIndexed { i, label ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { liftCmdIndex = i; liftCmdExpanded = false })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Bed Distance: ${bedDistance.toInt()} mm", fontSize = 14.sp)
            Slider(
                value = bedDistance, onValueChange = { bedDistance = it }, valueRange = 1f..200f,
                colors = SliderDefaults.colors(thumbColor = SpinColors.Blue, activeTrackColor = SpinColors.Blue),
            )
        } else {
            ExposedDropdownMenuBox(expanded = directionExpanded, onExpandedChange = { directionExpanded = it }) {
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
        }

        Spacer(Modifier.height(16.dp))

        Text("Speed: ${speedPct.toInt()}%", fontSize = 14.sp)
        Slider(
            value = speedPct, onValueChange = { speedPct = it }, valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = SpinColors.LightGreen, activeTrackColor = SpinColors.LightGreen),
        )

        Spacer(Modifier.height(8.dp))

        Text("Duration: ${durationSec.toInt()} s", fontSize = 14.sp)
        Slider(
            value = durationSec, onValueChange = { durationSec = it }, valueRange = 1f..60f,
            colors = SliderDefaults.colors(thumbColor = SpinColors.Blue, activeTrackColor = SpinColors.Blue),
        )

        Spacer(Modifier.height(24.dp))

        GradientButton(
            text = "RUN DIAGNOSE",
            onClick = {
                val motor = ringMotors[motorIndex]
                val control: UByte = if (controlIndex == 0) 0x01u else 0x02u
                val dir: UByte = if (isLift) liftCmdIndex.toUByte() else if (directionIndex == 0) 0x00u else 0x01u
                val bed = if (isLift) bedDistance.toInt() else null
                vm.startDiagnosis(motor.label)
                vm.sendDiagnostic(motor.id, control, dir, speedPct.toInt(), durationSec.toInt(), bed)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RingDiagnosisResultScreen(state: RingDiagnosisState, onBack: () -> Unit) {
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
