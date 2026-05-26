package com.gen4.spinning

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gen4.spinning.machines.carding.CardingDashboard
import com.gen4.spinning.machines.df.DfDashboard
import com.gen4.spinning.machines.flyer.FlyerDashboard
import com.gen4.spinning.machines.ring.RingDashboard
import com.gen4.spinning.shared.PidScreen
import com.gen4.spinning.ui.screens.BluetoothScreen
import com.gen4.spinning.ui.screens.LogsScreen
import com.gen4.spinning.ui.screens.SelectDeviceScreen
import com.gen4.spinning.ui.screens.SelectMachineScreen
import com.gen4.spinning.ui.screens.SplashScreen
import com.gen4.spinning.ui.theme.Gen4SpinningTheme

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object SelectMachine : Screen("select_machine")
    data object Bluetooth : Screen("bluetooth/{machine}") {
        fun go(machine: String) = "bluetooth/$machine"
    }
    data object SelectDevice : Screen("select_device/{machine}") {
        fun go(machine: String) = "select_device/$machine"
    }
    data object CardingDashboard : Screen("carding_dashboard")
    data object DfDashboard : Screen("df_dashboard")
    data object FlyerDashboard : Screen("flyer_dashboard")
    data object RingDashboard : Screen("ring_dashboard")
    data object Pid : Screen("pid/{machine}") {
        fun go(machine: String) = "pid/$machine"
    }
    data object Logs : Screen("logs")
}

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled gracefully inside each screen */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show previous crash info for diagnostics
        val crashPrefs = getSharedPreferences("gen4_crash", Context.MODE_PRIVATE)
        val lastCrash = crashPrefs.getString("last_crash", null)
        if (lastCrash != null) {
            crashPrefs.edit().remove("last_crash").apply()
            val shortMsg = lastCrash.lines().take(4).joinToString("\n")
            Toast.makeText(this, "Prev crash:\n$shortMsg", Toast.LENGTH_LONG).show()
        }

        val app = application as Gen4SpinningApp
        setContent {
            Gen4SpinningTheme {
                AppNavHost(app)
            }
        }

        // Request Bluetooth permissions after UI is ready
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(perms)
    }
}

@Composable
private fun AppNavHost(app: Gen4SpinningApp) {
    var splashDone by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val machineArg = navArgument("machine") { type = NavType.StringType }

    if (!splashDone) {
        SplashScreen(onDone = { splashDone = true })
        return
    }

    NavHost(navController = navController, startDestination = Screen.SelectMachine.route) {

        composable(Screen.SelectMachine.route) {
            SelectMachineScreen(
                onMachineSelected = { machine ->
                    navController.navigate(Screen.Bluetooth.go(machine))
                },
                onNavigateLogs = { navController.navigate(Screen.Logs.route) },
            )
        }

        composable(Screen.Bluetooth.route, arguments = listOf(machineArg)) { back ->
            val machine = back.arguments?.getString("machine") ?: return@composable
            BluetoothScreen(
                machine = machine,
                repository = app.repository,
                onSelectDevice = { navController.navigate(Screen.SelectDevice.go(machine)) },
                onConnected = {
                    val dest = when (machine) {
                        "carding" -> Screen.CardingDashboard.route
                        "df"      -> Screen.DfDashboard.route
                        "flyer"   -> Screen.FlyerDashboard.route
                        "ring"    -> Screen.RingDashboard.route
                        else      -> Screen.SelectMachine.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.SelectMachine.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.SelectDevice.route, arguments = listOf(machineArg)) { back ->
            back.arguments?.getString("machine") ?: return@composable
            SelectDeviceScreen(
                repository = app.repository,
                onDeviceSelected = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.CardingDashboard.route) {
            CardingDashboard(
                repository = app.repository,
                onDisconnect = {
                    navController.navigate(Screen.SelectMachine.route) {
                        popUpTo(Screen.SelectMachine.route) { inclusive = true }
                    }
                },
                onNavigatePid = { navController.navigate(Screen.Pid.go("carding")) },
                onNavigateLogs = { navController.navigate(Screen.Logs.route) },
            )
        }

        composable(Screen.DfDashboard.route) {
            DfDashboard(
                repository = app.repository,
                onDisconnect = {
                    navController.navigate(Screen.SelectMachine.route) {
                        popUpTo(Screen.SelectMachine.route) { inclusive = true }
                    }
                },
                onNavigatePid = { navController.navigate(Screen.Pid.go("df")) },
                onNavigateLogs = { navController.navigate(Screen.Logs.route) },
            )
        }

        composable(Screen.FlyerDashboard.route) {
            FlyerDashboard(
                repository = app.repository,
                onDisconnect = {
                    navController.navigate(Screen.SelectMachine.route) {
                        popUpTo(Screen.SelectMachine.route) { inclusive = true }
                    }
                },
                onNavigatePid = { navController.navigate(Screen.Pid.go("flyer")) },
                onNavigateLogs = { navController.navigate(Screen.Logs.route) },
            )
        }

        composable(Screen.RingDashboard.route) {
            RingDashboard(
                repository = app.repository,
                onDisconnect = {
                    navController.navigate(Screen.SelectMachine.route) {
                        popUpTo(Screen.SelectMachine.route) { inclusive = true }
                    }
                },
                onNavigatePid = { navController.navigate(Screen.Pid.go("ring")) },
                onNavigateLogs = { navController.navigate(Screen.Logs.route) },
            )
        }

        composable(Screen.Logs.route) {
            LogsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Pid.route, arguments = listOf(machineArg)) { back ->
            val machine = back.arguments?.getString("machine") ?: return@composable
            PidScreen(
                machine = machine,
                repository = app.repository,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
