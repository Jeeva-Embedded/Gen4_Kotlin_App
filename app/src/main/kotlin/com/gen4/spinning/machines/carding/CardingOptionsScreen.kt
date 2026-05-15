package com.gen4.spinning.machines.carding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.components.SaveBanner
import com.gen4.spinning.ui.theme.SpinColors

@Composable
fun CardingOptionsScreen(vm: CardingViewModel) {
    val logEnabled by vm.logEnabled.collectAsState()
    val logMessage by vm.logMessage.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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

            // ── Reset Length Counter ────────────────────────────────────────
            GradientButton(
                text = "Reset Length Counter",
                onClick = { vm.sendResetLengthCounter() },
                modifier = Modifier.fillMaxWidth(),
            )
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
}
