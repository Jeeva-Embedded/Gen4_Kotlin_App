package com.gen4.spinning.machines.flyer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
                    label = "Layer",
                    value = runState.layers.toString(),
                    modifier = Modifier.fillMaxWidth(),
                )
                LiftAnimation(leftLift = runState.leftLift, rightLift = runState.rightLift, compact = true)
            }

            Spacer(Modifier.height(96.dp))
            FlyerCarousel(vm = vm)
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
                when {
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
    }
}

@Composable
private fun LiftAnimation(leftLift: Float, rightLift: Float, compact: Boolean = false) {
    val diff = leftLift - rightLift
    val targetAngle = (diff / 4f) * (180f / 40f)
    val angleDeg by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "liftAngle",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Δ (mm) = ${"%.2f".format(diff)}",
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 13.sp else 15.sp,
        )
        if (!compact) {
            Text(text = "(Δ = L - R)", fontSize = 12.sp)
        }
        Spacer(Modifier.height(if (compact) 6.dp else 16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(if (compact) 16.dp else 30.dp)
                .rotate(angleDeg)
                .background(SpinColors.LightGreen),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("L", fontWeight = FontWeight.Bold, fontSize = if (compact) 14.sp else 18.sp)
                Text("%.2f".format(leftLift), fontSize = if (compact) 11.sp else 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("R", fontWeight = FontWeight.Bold, fontSize = if (compact) 14.sp else 18.sp)
                Text("%.2f".format(rightLift), fontSize = if (compact) 11.sp else 12.sp)
            }
        }
    }
}
