package com.gen4.spinning.machines.flyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun FlyerStatusScreen(vm: FlyerViewModel) {
    val runState by vm.runState.collectAsState()
    val isRunning = runState.substate == 0x01u.toUByte()
    val isPaused  = runState.substate == 0x02u.toUByte()
    val isError   = runState.substate == 0x03u.toUByte()
    val isHoming  = runState.substate == 0x04u.toUByte()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        StatusBox(
            label = "Status",
            value = substateLabel(runState.substate).uppercase(),
            modifier = Modifier.fillMaxWidth(),
        )

        when {
            isRunning -> {
                Spacer(Modifier.height(12.dp))
                StatusBox(
                    label = "Layer",
                    value = runState.layers.toString(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                LiftAnimation(leftLift = runState.leftLift, rightLift = runState.rightLift)
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    FlyerCarousel(vm = vm)
                }
            }
            isPaused -> {
                Spacer(Modifier.height(12.dp))
                StatusBox(
                    label = "Reason For Pause",
                    value = runState.pauseReason,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                StatusBox(
                    label = "Layers",
                    value = runState.pauseLayer.toString(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            isHoming -> {
                Spacer(Modifier.height(16.dp))
                LiftAnimation(leftLift = runState.leftLift, rightLift = runState.rightLift)
            }
            isError -> {
                Spacer(Modifier.height(12.dp))
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
                Spacer(Modifier.height(12.dp))
                StatusBox(
                    label = "Layers",
                    value = runState.errorLayer.toString(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LiftAnimation(leftLift: Float, rightLift: Float) {
    val diff = leftLift - rightLift
    // Flutter: angle_rad = (diff/4) * PI/40  →  angle_deg = diff/4 * 180/40
    val angleDeg = (diff / 4f) * (180f / 40f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Δ (mm) = ${"%.2f".format(diff)}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Text(text = "(Δ = L - R)", fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(30.dp)
                .rotate(angleDeg)
                .background(SpinColors.LightGreen),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("L", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("%.2f".format(leftLift), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("R", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("%.2f".format(rightLift), fontSize = 12.sp)
            }
        }
    }
}
