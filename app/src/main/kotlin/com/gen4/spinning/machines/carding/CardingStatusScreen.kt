package com.gen4.spinning.machines.carding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gen4.spinning.ui.components.StatusBox
import com.gen4.spinning.ui.theme.SpinColors

private fun substateLabel(s: UByte): String = when (s) {
    0x00u.toUByte() -> "Idle"
    0x01u.toUByte() -> "Running"
    0x02u.toUByte() -> "Paused"
    0x03u.toUByte() -> "Error"
    0x04u.toUByte() -> "Homing"
    0x05u.toUByte() -> "Inching"
    0x06u.toUByte() -> "Can Over"
    else             -> "Unknown"
}

@Composable
fun CardingStatusScreen(vm: CardingViewModel) {
    val runState by vm.runState.collectAsState()
    val settings by vm.settings.collectAsState()
    val isRunning = runState.substate == 0x01u.toUByte()
    val isPaused  = runState.substate == 0x02u.toUByte()
    val isError   = runState.substate == 0x03u.toUByte()
    val isHoming  = runState.substate == 0x04u.toUByte()

    Column(modifier = Modifier.fillMaxSize()) {

        if (isRunning) {
            // ── Status info (natural height, no weight) ──────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusBox(
                    label = "Status",
                    value = substateLabel(runState.substate).uppercase(),
                    modifier = Modifier.fillMaxWidth(),
                )
                StatusBox(
                    label = "Delivery Speed",
                    value = when {
                        runState.deliveryMtrsPerMin > 0f -> "${"%.1f".format(runState.deliveryMtrsPerMin)} m/min"
                        settings.deliverySpeed.isNotEmpty() -> "${settings.deliverySpeed} m/min"
                        else -> "—"
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusBox(
                        label = "Carding Duct",
                        value = if (runState.coilerSensor) "Open" else "Blocked",
                        modifier = Modifier.weight(1f),
                        valueColor = if (runState.coilerSensor) SpinColors.LightGreen else Color.Red,
                    )
                    StatusBox(
                        label = "Auto Feed Duct",
                        value = if (runState.ductSensor) "Open" else "Blocked",
                        modifier = Modifier.weight(1f),
                        valueColor = if (runState.ductSensor) SpinColors.LightGreen else Color.Red,
                    )
                }
            }

            Spacer(Modifier.height(114.dp))
            CardingCarousel(vm = vm)
            Spacer(Modifier.height(38.dp))

        } else {
            // ── Non-running: status info fills the page ──────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                StatusBox(
                    label = "Status",
                    value = substateLabel(runState.substate).uppercase(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                when {
                    isPaused -> {
                        StatusBox(
                            label = "Carding Duct",
                            value = if (runState.coilerSensor) "Open" else "Blocked",
                            modifier = Modifier.fillMaxWidth(),
                            valueColor = if (runState.coilerSensor) SpinColors.LightGreen else Color.Red,
                        )
                        Spacer(Modifier.height(8.dp))
                        StatusBox(
                            label = "Auto Feed Duct",
                            value = if (runState.ductSensor) "Open" else "Blocked",
                            modifier = Modifier.fillMaxWidth(),
                            valueColor = if (runState.ductSensor) SpinColors.LightGreen else Color.Red,
                        )
                        if (runState.pauseReason.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            StatusBox(
                                label = "Reason For Pause",
                                value = runState.pauseReason,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    isHoming -> {
                        StatusBox(
                            label = "Carding Duct",
                            value = if (runState.coilerSensor) "Open" else "Blocked",
                            modifier = Modifier.fillMaxWidth(),
                            valueColor = if (runState.coilerSensor) SpinColors.LightGreen else Color.Red,
                        )
                        Spacer(Modifier.height(8.dp))
                        StatusBox(
                            label = "Auto Feed Duct",
                            value = if (runState.ductSensor) "Open" else "Blocked",
                            modifier = Modifier.fillMaxWidth(),
                            valueColor = if (runState.ductSensor) SpinColors.LightGreen else Color.Red,
                        )
                    }
                    isError -> {
                        StatusBox(
                            label = "Error Information",
                            value = "${runState.errorInformation} (${runState.errorCode})",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        StatusBox(
                            label = "Error Source",
                            value = runState.errorSource,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
