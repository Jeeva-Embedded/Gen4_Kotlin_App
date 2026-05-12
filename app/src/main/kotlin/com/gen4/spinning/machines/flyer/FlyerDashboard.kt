package com.gen4.spinning.machines.flyer

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gen4.spinning.core.bt.BtSessionRepository
import com.gen4.spinning.core.bt.ConnectionState
import com.gen4.spinning.ui.components.DisconnectedScreen
import com.gen4.spinning.ui.components.GradientAppBar
import com.gen4.spinning.ui.theme.SpinColors
import kotlinx.coroutines.launch

@Composable
fun FlyerDashboard(
    repository: BtSessionRepository,
    onDisconnect: () -> Unit,
    onNavigatePid: () -> Unit,
) {
    val vm: FlyerViewModel = viewModel(factory = flyerVmFactory(repository))
    val connectionState by repository.connectionState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gen4_devices", Context.MODE_PRIVATE) }
    val connectedAddress = (connectionState as? ConnectionState.Connected)?.deviceAddress ?: ""
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(connectedAddress) { mutableStateOf(prefs.getString(connectedAddress, "") ?: "") }

    val tabs = listOf("Status", "Settings", "Tests", "Options")
    val icons = listOf(Icons.Default.Dashboard, Icons.Default.Settings, Icons.Default.Build, Icons.Default.Tune)

    when (val cs = connectionState) {
        is ConnectionState.Lost        -> { DisconnectedScreen(message = "Connection lost to ${cs.deviceName}", onReconnect = { repository.reconnect() }, onBack = onDisconnect); return }
        is ConnectionState.Disconnected -> { DisconnectedScreen(onBack = onDisconnect); return }
        else -> Unit
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Device Name") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { prefs.edit().putString(connectedAddress, renameText).apply(); showRenameDialog = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(label = { Text("Change Device Name") }, selected = false,
                    onClick = { scope.launch { drawerState.close() }; showRenameDialog = true })
                NavigationDrawerItem(label = { Text("Exit App") }, selected = false,
                    onClick = { (context as? Activity)?.finish() })
            }
        }
    ) {
        Scaffold(
            topBar = {
                GradientAppBar(title = "Flyer Frame", actions = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                    IconButton(onClick = { vm.disconnect(); onDisconnect() }) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Disconnect", tint = Color.White)
                    }
                })
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icons[index], contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpinColors.LightGreen,
                                selectedTextColor = SpinColors.LightGreen,
                                indicatorColor = SpinColors.LightGreen.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                            ),
                        )
                    }
                }
            },
            containerColor = Color.White,
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    0 -> FlyerStatusScreen(vm = vm)
                    1 -> FlyerSettingsScreen(vm = vm, onNavigatePid = onNavigatePid)
                    2 -> FlyerTestsScreen(vm = vm)
                    3 -> FlyerOptionsScreen(vm = vm)
                }
            }
        }
    }
}

fun flyerVmFactory(repository: BtSessionRepository) =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FlyerViewModel(repository) as T
    }
