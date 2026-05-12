package com.gen4.spinning.machines.carding

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gen4.spinning.ui.components.SensorIndicator
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
fun CardingStatusScreen(vm: CardingViewModel) {
    val runState by vm.runState.collectAsState()
    val isRunning = runState.substate == 0x01u.toUByte()
    val isPaused  = runState.substate == 0x02u.toUByte()
    val isError   = runState.substate == 0x03u.toUByte()
    val isHoming  = runState.substate == 0x04u.toUByte()

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
            value = if (runState.substate == 0x00u.toUByte()) "--"
                    else substateLabel(runState.substate).uppercase(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        when {
            isRunning -> {
                StatusBox(
                    label = "Delivery Speed",
                    value = "${"%.1f".format(runState.deliveryMtrsPerMin)} m/min",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                // Flutter uses coilerSensor (TLV 0x0C) for "Carding Duct" and ductSensor (TLV 0x0B) for "Auto Feed Duct"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SensorIndicator(label = "Carding Duct", active = runState.coilerSensor)
                    SensorIndicator(label = "Auto Feed Duct", active = runState.ductSensor)
                }
                Spacer(Modifier.height(16.dp))
                CardingCarousel(vm = vm)
            }
            isPaused -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SensorIndicator(label = "Carding Duct", active = runState.coilerSensor)
                    SensorIndicator(label = "Auto Feed Duct", active = runState.ductSensor)
                }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SensorIndicator(label = "Carding Duct", active = runState.coilerSensor)
                    SensorIndicator(label = "Auto Feed Duct", active = runState.ductSensor)
                }
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
