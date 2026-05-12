package com.gen4.spinning.machines.ring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.theme.SpinColors

@Composable
fun RingOptionsScreen(vm: RingViewModel) {
    var logEnabled by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        GradientButton(
            text = "Reset Grams/Spindle",
            onClick = { vm.sendResetGrams() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Log", fontSize = 16.sp)
            Switch(
                checked = logEnabled,
                onCheckedChange = { logEnabled = it; vm.sendLog(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SpinColors.LightGreen,
                    checkedTrackColor = SpinColors.LightGreen.copy(alpha = 0.4f),
                ),
            )
        }
    }
}
