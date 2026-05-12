package com.gen4.spinning.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.core.bt.BtSessionRepository
import com.gen4.spinning.core.bt.FrameCodec
import com.gen4.spinning.ui.components.FieldInput
import com.gen4.spinning.ui.components.GradientAppBar
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.components.SaveBanner

private const val PID_PIN = "123456"

private const val TLV_WHICH_MOTOR: UByte = 0x01u
private const val TLV_KP: UByte = 0x02u
private const val TLV_KI: UByte = 0x03u
private const val TLV_FF: UByte = 0x04u
private const val TLV_SO: UByte = 0x05u

private data class PidMotor(val label: String, val id: UByte)

private fun motorList(machine: String): List<PidMotor> = when (machine) {
    "carding" -> listOf(
        PidMotor("Cylinder",        0x01u),
        PidMotor("Beater",          0x02u),
        PidMotor("Cage",            0x03u),
        PidMotor("Cylinder Feed",   0x04u),
        PidMotor("Beater Feed",     0x05u),
        PidMotor("Coiler",          0x06u),
        PidMotor("Picker Cylinder", 0x07u),
        PidMotor("AF Feed",         0x08u),
    )
    "df" -> listOf(
        PidMotor("Front Roller", 0x01u),
        PidMotor("Back Roller",  0x02u),
        PidMotor("Creel",        0x03u),
    )
    "flyer" -> listOf(
        PidMotor("Flyer",        0x01u),
        PidMotor("Bobbin",       0x02u),
        PidMotor("Front Roller", 0x03u),
        PidMotor("Back Roller",  0x04u),
    )
    else -> listOf(PidMotor("Calender", 0x01u))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PidScreen(
    machine: String,
    repository: BtSessionRepository,
    onBack: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GradientAppBar(
                title = "PID Settings",
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
            )
        },
        containerColor = Color.White,
    ) { padding ->
        if (!unlocked) {
            PinGate(
                pin = pin,
                pinError = pinError,
                padding = padding,
                onPinChange = { pin = it; pinError = false },
                onUnlock = {
                    if (pin == PID_PIN) unlocked = true else pinError = true
                },
            )
        } else {
            PidContent(machine = machine, repository = repository, padding = padding)
        }
    }
}

@Composable
private fun PinGate(
    pin: String,
    pinError: Boolean,
    padding: PaddingValues,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Enter PIN", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = pinError,
            supportingText = if (pinError) ({ Text("Incorrect PIN") }) else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        GradientButton(text = "Unlock", onClick = onUnlock, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PidContent(machine: String, repository: BtSessionRepository, padding: PaddingValues) {
    val motors = remember(machine) { motorList(machine) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var kp by remember { mutableStateOf("") }
    var ki by remember { mutableStateOf("") }
    var ff by remember { mutableStateOf("") }
    var so by remember { mutableStateOf("") }
    var pidReceived by remember { mutableStateOf(false) }
    var pidSaved by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedIndex) {
        kp = ""; ki = ""; ff = ""; so = ""
        repository.sendFrame(FrameCodec.buildPidRequest(motors[selectedIndex].id))
    }

    LaunchedEffect(Unit) {
        repository.inboundFrames.collect { frame ->
            if (frame.info == 0x0Eu.toUByte()) {
                for (tlv in frame.tlvs) {
                    when (tlv.type) {
                        TLV_KP -> kp = tlv.valueAsUInt16().toString()
                        TLV_KI -> ki = tlv.valueAsUInt16().toString()
                        TLV_FF -> ff = tlv.valueAsUInt16().toString()
                        TLV_SO -> so = tlv.valueAsUInt16().toString()
                    }
                }
                pidReceived = true
                scope.launch { delay(2_000); pidReceived = false }
            }
        }
    }

    LaunchedEffect("pidSave") {
        repository.saveResponse.collect { ok ->
            pidSaved = ok
            delay(2_000)
            pidSaved = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        if (motors.size > 1) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
            ) {
                OutlinedTextField(
                    value = motors[selectedIndex].label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Motor") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                ) {
                    motors.forEachIndexed { index, motor ->
                        DropdownMenuItem(
                            text = { Text(motor.label) },
                            onClick = {
                                selectedIndex = index
                                dropdownExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        FieldInput(label = "Kp", value = kp, onValueChange = { kp = it })
        Spacer(Modifier.height(8.dp))
        FieldInput(label = "Ki", value = ki, onValueChange = { ki = it })
        Spacer(Modifier.height(8.dp))
        FieldInput(label = "FF (Feed Forward)", value = ff, onValueChange = { ff = it })
        Spacer(Modifier.height(8.dp))
        FieldInput(label = "SO (Start Offset)", value = so, onValueChange = { so = it })
        Spacer(Modifier.height(24.dp))

        if (pidReceived) {
            SaveBanner(message = "PID received", success = true)
            Spacer(Modifier.height(8.dp))
        }
        pidSaved?.let {
            SaveBanner(message = if (it) "PID saved successfully" else "PID save failed", success = it)
            Spacer(Modifier.height(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            GradientButton(
                text = "Request PID",
                onClick = {
                    repository.sendFrame(FrameCodec.buildPidRequest(motors[selectedIndex].id))
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            GradientButton(
                text = "Send PID",
                onClick = {
                    val kpV = kp.toUShortOrNull() ?: return@GradientButton
                    val kiV = ki.toUShortOrNull() ?: return@GradientButton
                    val ffV = ff.toUShortOrNull() ?: return@GradientButton
                    val soV = so.toUShortOrNull() ?: return@GradientButton
                    val frame = FrameCodec.build(
                        0x0Fu, 0x00u, listOf(
                            FrameCodec.buildTlvCompact(TLV_WHICH_MOTOR, motors[selectedIndex].id),
                            FrameCodec.buildTlvInt(TLV_KP, kpV),
                            FrameCodec.buildTlvInt(TLV_KI, kiV),
                            FrameCodec.buildTlvInt(TLV_FF, ffV),
                            FrameCodec.buildTlvInt(TLV_SO, soV),
                        )
                    )
                    repository.sendFrame(frame)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
