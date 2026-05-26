package com.gen4.spinning.machines.ring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.gen4.spinning.ui.components.StatusBox

private fun substateLabel(s: UByte): String = when (s) {
    0x00u.toUByte() -> "Idle"
    0x01u.toUByte() -> "Running"
    0x02u.toUByte() -> "Paused"
    0x03u.toUByte() -> "Error"
    0x04u.toUByte() -> "Homing"
    else             -> "Unknown"
}

@Composable
fun RingStatusScreen(vm: RingViewModel) {
    val runState by vm.runState.collectAsState()
    val isRunning = runState.substate == 0x01u.toUByte()
    val isPaused  = runState.substate == 0x02u.toUByte()
    val isError   = runState.substate == 0x03u.toUByte()

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Top: status info ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
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
                        label = "Doff Percent",
                        value = "${"%.1f".format(runState.weight)} %",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                isPaused -> {
                    StatusBox(
                        label = "Doff Percent",
                        value = "${"%.1f".format(runState.weight)} %",
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
                }
                isError -> {
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

        // ── Bottom: full-width carousel (running only) ────────────────────────
        if (isRunning) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                RingCarousel(vm = vm)
            }
        }
    }
}
