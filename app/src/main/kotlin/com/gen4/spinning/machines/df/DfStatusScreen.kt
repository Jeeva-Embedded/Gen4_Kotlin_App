package com.gen4.spinning.machines.df

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gen4.spinning.ui.components.StatusBox

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
fun DfStatusScreen(vm: DfViewModel) {
    val runState by vm.runState.collectAsState()
    val settings by vm.settings.collectAsState()
    val isRunning = runState.substate == 0x01u.toUByte()
    val isPaused  = runState.substate == 0x02u.toUByte()
    val isError   = runState.substate == 0x03u.toUByte()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
            isRunning -> {
                StatusBox(
                    label = "Length (m)",
                    value = "${"%.1f".format(runState.currentLength)} m",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                StatusBox(
                    label = "Delivery (m/min)",
                    value = if (runState.deliveryMtrsPerMin > 0f)
                                "${"%.1f".format(runState.deliveryMtrsPerMin)} m/min"
                            else
                                "${settings.deliverySpeed} m/min",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                DfCarousel(vm = vm)
            }
            isPaused -> {
                StatusBox(
                    label = "Length (m)",
                    value = "${"%.1f".format(runState.currentLength)} m",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (runState.pauseReason.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    StatusBox(
                        label = "Pause Reason",
                        value = runState.pauseReason,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (runState.pauseLength != 0f) {
                    Spacer(Modifier.height(8.dp))
                    StatusBox(
                        label = "Pause Length",
                        value = "${"%.1f".format(runState.pauseLength)} m",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            isError -> {
                StatusBox(
                    label = "Current Length",
                    value = "${"%.1f".format(runState.currentLength)} m",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                StatusBox(
                    label = "Error Information",
                    value = if (runState.errorReason.isNotEmpty()) runState.errorReason else "-",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                StatusBox(
                    label = "Error Source",
                    value = if (runState.errorSource.isNotEmpty()) runState.errorSource else "-",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
