package com.gen4.spinning.ui.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gen4.spinning.core.bt.BtSessionRepository
import com.gen4.spinning.core.bt.ConnectionState
import com.gen4.spinning.ui.components.GradientAppBar
import com.gen4.spinning.ui.components.GradientButton
import com.gen4.spinning.ui.theme.SpinColors

private fun btIsEnabled(adapter: BluetoothAdapter?): Boolean {
    return try { adapter?.isEnabled == true } catch (e: SecurityException) { false }
}

@Composable
fun BluetoothScreen(
    machine: String,
    repository: BtSessionRepository,
    onSelectDevice: () -> Unit,
    onConnected: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter: BluetoothAdapter? = bm.adapter

    val connectionState by repository.connectionState.collectAsState()
    var btEnabled by remember { mutableStateOf(btIsEnabled(adapter)) }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { btEnabled = btIsEnabled(adapter) }

    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) onConnected()
    }

    val machineName = when (machine) {
        "carding" -> "Carding"
        "df"      -> "Draw Frame"
        "flyer"   -> "Flyer Frame"
        "ring"    -> "Ring Doubler"
        else      -> machine
    }

    val statusText = when (val s = connectionState) {
        is ConnectionState.Disconnected  -> "Disconnected"
        is ConnectionState.Scanning      -> "Scanning…"
        is ConnectionState.Connecting    -> "Connecting to ${s.deviceName}…"
        is ConnectionState.Connected     -> "Connected to ${s.deviceName}"
        is ConnectionState.Reconnecting  -> "Reconnecting to ${s.deviceName}…"
        is ConnectionState.Lost          -> "Connection lost"
    }

    Scaffold(
        topBar = {
            GradientAppBar(
                title = machineName,
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Bluetooth", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = btEnabled,
                    onCheckedChange = { on ->
                        if (on) {
                            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        } else {
                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                                try {
                                    @Suppress("DEPRECATION")
                                    adapter?.disable()
                                } catch (e: SecurityException) { /* ignore */ }
                            }
                            btEnabled = false
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpinColors.LightGreen,
                        checkedTrackColor = SpinColors.LightGreen.copy(alpha = 0.4f),
                    ),
                )
            }

            Spacer(Modifier.height(32.dp))

            GradientButton(
                text = "Paired Devices",
                onClick = onSelectDevice,
                enabled = btEnabled,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            GradientButton(
                text = "Demo Mode (No Bluetooth)",
                onClick = { repository.enterDemoMode() },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = statusText,
                fontSize = 15.sp,
                color = when (connectionState) {
                    is ConnectionState.Connected    -> SpinColors.LightGreen
                    is ConnectionState.Lost         -> Color.Red
                    is ConnectionState.Reconnecting -> Color(0xFFFFA000)
                    else                            -> Color.Gray
                },
            )
        }
    }
}
