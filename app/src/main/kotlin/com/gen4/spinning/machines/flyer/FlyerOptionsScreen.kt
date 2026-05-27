package com.gen4.spinning.machines.flyer

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.components.SaveBanner
import com.gen4.spinning.ui.theme.SpinColors

@Composable
fun FlyerOptionsScreen(vm: FlyerViewModel) {
    val settings by vm.settings.collectAsState()
    val runState by vm.runState.collectAsState()
    val logEnabled by vm.logEnabled.collectAsState()
    val logMessage by vm.logMessage.collectAsState()
    val gearboxLeft by vm.gearboxLeft.collectAsState()
    val gearboxRight by vm.gearboxRight.collectAsState()
    var rtfDisplay by remember(settings.rtf) { mutableStateOf(settings.rtf) }
    var gearboxRunning by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {

            // ── Enable Logging ──────────────────────────────────────────────
            Column {
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
            }

            // ── Sensor Cut Counts ───────────────────────────────────────────
            Column {
                CutCountRow(label = "Front Sensor Cut Count", count = runState.frontCutCount)
                Spacer(Modifier.height(8.dp))
                CutCountRow(label = "Back Sensor Cut Count",  count = runState.backCutCount)
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    text = "Reset Cut Counts",
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            }

            // ── Gear Box Settings ───────────────────────────────────────────
            Column {
                Text("Gear Box Settings", color = SpinColors.Blue, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                GearboxValueRow(label = "Left", value = gearboxLeft)
                Spacer(Modifier.height(12.dp))
                GearboxValueRow(label = "Right", value = gearboxRight)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GradientButton(
                        text = "START",
                        onClick = { vm.sendGearbox(0x01u); gearboxRunning = true },
                        modifier = Modifier.weight(1f),
                        enabled = !gearboxRunning,
                    )
                    GradientButton(
                        text = "STOP",
                        onClick = { vm.sendGearbox(0x02u); gearboxRunning = false },
                        modifier = Modifier.weight(1f),
                        enabled = gearboxRunning,
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // ── RTF Settings ────────────────────────────────────────────────
            Column {
                Text("RTF Settings", color = SpinColors.Blue, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { vm.sendRead() }) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "receive", tint = SpinColors.Blue)
                        }
                        Text("receive", fontSize = 12.sp, color = SpinColors.Blue)
                    }
                    IconButton(onClick = { rtfDisplay = adjustValue(rtfDisplay, 0.01) }) {
                        Icon(Icons.Default.Add, contentDescription = "+", tint = SpinColors.Blue)
                    }
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(48.dp)
                            .border(1.dp, Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(rtfDisplay, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { rtfDisplay = adjustValue(rtfDisplay, -0.01) }) {
                        Icon(Icons.Default.Remove, contentDescription = "-", tint = SpinColors.Blue)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            vm.updateSettings(settings.copy(rtf = rtfDisplay))
                            vm.sendSave()
                        }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "send", tint = SpinColors.Blue)
                        }
                        Text("send", fontSize = 12.sp, color = SpinColors.Blue)
                    }
                }
            }
        }

        // ── Log banner overlay ──────────────────────────────────────────────
        logMessage?.let {
            SaveBanner(
                message = it,
                success = true,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Cut Counts") },
            text = { Text("Reset both Front and Back sensor cut counts to 0?") },
            confirmButton = {
                TextButton(onClick = { vm.sendResetCutCounts(); showResetConfirm = false }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CutCountRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = Color.DarkGray)
        Text(count.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GearboxValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .border(1.dp, Color.Black)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun adjustValue(current: String, delta: Double): String {
    val v = current.toDoubleOrNull() ?: return current
    return "%.2f".format(v + delta)
}
