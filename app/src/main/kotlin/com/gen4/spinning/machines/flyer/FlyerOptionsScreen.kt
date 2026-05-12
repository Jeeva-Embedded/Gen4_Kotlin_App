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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.theme.SpinColors

@Composable
fun FlyerOptionsScreen(vm: FlyerViewModel) {
    val settings by vm.settings.collectAsState()
    var logEnabled by remember { mutableStateOf(false) }
    var rtfDisplay by remember(settings.rtf) { mutableStateOf(settings.rtf) }
    var gearboxRunning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Enable Logging", color = SpinColors.Blue, fontSize = 18.sp)
            Switch(
                checked = logEnabled,
                onCheckedChange = { logEnabled = it; vm.sendLog(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SpinColors.LightGreen,
                    checkedTrackColor = SpinColors.LightGreen.copy(alpha = 0.4f),
                ),
            )
        }

        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Gear Box Settings", color = SpinColors.Blue, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        GearboxValueRow(label = "Left")
        Spacer(Modifier.height(12.dp))
        GearboxValueRow(label = "Right")
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

        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

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
                    .width(80.dp)
                    .size(48.dp)
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

        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        GradientButton(
            text = "Reset Length Counter",
            onClick = { vm.sendResetLengthCounter() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GearboxValueRow(label: String) {
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
            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun adjustValue(current: String, delta: Double): String {
    val v = current.toDoubleOrNull() ?: return current
    return "%.2f".format(v + delta)
}
