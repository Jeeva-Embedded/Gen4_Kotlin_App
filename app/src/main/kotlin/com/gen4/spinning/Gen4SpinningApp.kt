package com.gen4.spinning

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import com.gen4.spinning.core.bt.BtSessionRepository

class Gen4SpinningApp : Application() {

    lateinit var repository: BtSessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashReporter()
        val adapter = try {
            (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        } catch (e: Exception) { null }
        repository = BtSessionRepository(adapter)
    }

    private fun installCrashReporter() {
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            try {
                val msg = "${ex.javaClass.name}: ${ex.message}\n${ex.stackTraceToString().take(2000)}"
                getSharedPreferences("gen4_crash", Context.MODE_PRIVATE)
                    .edit().putString("last_crash", msg).apply()
            } catch (_: Exception) {}
            default?.uncaughtException(thread, ex)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::repository.isInitialized) repository.onDestroy()
    }
}
