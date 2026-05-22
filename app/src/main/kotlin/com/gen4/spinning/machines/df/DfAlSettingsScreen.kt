package com.gen4.spinning.machines.df

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.gen4.spinning.ui.components.GradientAppBar
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.components.SaveBanner

@Composable
fun DfAlSettingsScreen(vm: DfViewModel, onBack: () -> Unit) {
    val alSettings by vm.alSettings.collectAsState()
    val alReadResult by vm.alReadResult.collectAsState()
    val alSaveResult by vm.alSaveResult.collectAsState()
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            GradientAppBar(title = "AL Settings", onBack = onBack)

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
                                "Check: KP (0 < KP ≤ 1), Sliver 6 < Sliver 5 < Sliver 4, Target (4 < g/m ≤ 6)"
                        },
                        modifier = Modifier.weight(1f),
                    )
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
