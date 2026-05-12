package com.gen4.spinning.ui.screens

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gen4.spinning.core.bt.BtSessionRepository
import com.gen4.spinning.ui.components.GradientAppBar

private fun deviceName(device: BluetoothDevice): String {
    return try { device.name } catch (e: SecurityException) { null } ?: device.address
}

@Composable
fun SelectDeviceScreen(
    repository: BtSessionRepository,
    onDeviceSelected: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bm.adapter

    val pairedDevices: List<BluetoothDevice> = remember {
        try { adapter?.bondedDevices?.toList() ?: emptyList() }
        catch (e: SecurityException) { emptyList() }
    }

    Scaffold(
        topBar = {
            GradientAppBar(
                title = "Paired Devices",
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color.White,
    ) { padding ->
        if (pairedDevices.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                Text(
                    "No paired Bluetooth devices found.\n\nPair your device in Android Settings → Bluetooth first.",
                    color = Color.Gray,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(pairedDevices) { device ->
                    ListItem(
                        headlineContent = { Text(deviceName(device)) },
                        supportingContent = { Text(device.address, color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                repository.connect(device)
                                onDeviceSelected()
                            },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
