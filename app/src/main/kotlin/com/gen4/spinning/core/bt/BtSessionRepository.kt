package com.gen4.spinning.core.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.UUID

class BtSessionRepository(private val bluetoothAdapter: BluetoothAdapter?) {

    companion object {
        private const val TAG = "BtSession"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val RX_BUFFER_SIZE = 512
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _inboundFrames = MutableSharedFlow<BtFrame>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val inboundFrames: SharedFlow<BtFrame> = _inboundFrames.asSharedFlow()

    private val _saveResponse = MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val saveResponse: SharedFlow<Boolean> = _saveResponse.asSharedFlow()

    private val txChannel = Channel<String>(capacity = 16)

    @Volatile private var socket: BluetoothSocket? = null
    private var rxJob: Job? = null
    private var txJob: Job? = null
    private var watchdogJob: Job? = null
    @Volatile private var lastMcStateMs: Long = 0L

    fun enterDemoMode() {
        _connectionState.value = ConnectionState.Connected(
            deviceName = "Demo Device",
            deviceAddress = "00:00:00:00:00:00",
        )
    }

    fun connect(device: BluetoothDevice) {
        val current = _connectionState.value
        if (current is ConnectionState.Connecting || current is ConnectionState.Connected) return
        val name = try { device.name } catch (e: SecurityException) { null } ?: device.address
        _connectionState.value = ConnectionState.Connecting(name, device.address)
        scope.launch { connectInternal(device, name) }
    }

    fun disconnect() {
        scope.launch { tearDown(goToState = ConnectionState.Disconnected) }
    }

    fun reconnect() {
        val state = _connectionState.value
        val address = when (state) {
            is ConnectionState.Lost -> state.deviceAddress
            is ConnectionState.Reconnecting -> state.deviceAddress
            else -> return
        }
        val name = when (state) {
            is ConnectionState.Lost -> state.deviceName
            is ConnectionState.Reconnecting -> state.deviceName
            else -> address
        }
        val device = try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (e: SecurityException) { null } ?: return
        _connectionState.value = ConnectionState.Connecting(name, address)
        scope.launch { connectInternal(device, name) }
    }

    fun sendFrame(frame: String): Boolean {
        val state = _connectionState.value
        if (state !is ConnectionState.Connected && state !is ConnectionState.Reconnecting) return false
        return txChannel.trySend(frame).isSuccess
    }

    fun drainTxQueue() {
        while (txChannel.tryReceive().isSuccess) { /* drop stale queued frames */ }
    }

    private suspend fun connectInternal(device: BluetoothDevice, name: String) {
        if (bluetoothAdapter == null) {
            _connectionState.value = ConnectionState.Disconnected
            return
        }
        tearDownJobs()
        val newSocket = try {
            withTimeout(CONNECT_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    try { bluetoothAdapter.cancelDiscovery() } catch (e: SecurityException) { /* ignore */ }
                    s.connect()
                    s
                }
            }
        } catch (e: TimeoutCancellationException) {
            _connectionState.value = ConnectionState.Disconnected; return
        } catch (e: IOException) {
            _connectionState.value = ConnectionState.Disconnected; return
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.Disconnected; return
        }
        socket = newSocket
        startRxTx(newSocket)
        txChannel.trySend(FrameCodec.buildPairedFromPhone())

        val ok = try {
            withTimeout(FIRST_FRAME_TIMEOUT_MS) { while (lastMcStateMs == 0L) delay(50); true }
        } catch (_: TimeoutCancellationException) { false }

        if (!ok) { tearDown(ConnectionState.Disconnected); return }
        _connectionState.value = ConnectionState.Connected(name, device.address)
        startWatchdog(name, device.address)
    }

    private fun startRxTx(s: BluetoothSocket) {
        rxJob = scope.launch(Dispatchers.IO) { rxLoop(s) }
        txJob = scope.launch(Dispatchers.IO) { txLoop(s) }
    }

    private suspend fun rxLoop(s: BluetoothSocket) {
        val buffer = ByteArray(RX_BUFFER_SIZE)
        val inputStream = try { s.inputStream } catch (e: IOException) { return }
        val accumulator = StringBuilder()

        while (currentCoroutineContext().isActive) {
            val bytesRead = try { inputStream.read(buffer) } catch (e: IOException) {
                if (currentCoroutineContext().isActive) onSocketLost()
                return
            }
            if (bytesRead <= 0) continue
            accumulator.append(String(buffer, 0, bytesRead, Charsets.US_ASCII))

            var consumed = 0
            val acc = accumulator.toString()
            var searchFrom = 0
            while (true) {
                val sofIdx = acc.indexOf("7E", searchFrom)
                if (sofIdx == -1) { consumed = acc.length; break }
                val remaining = acc.substring(sofIdx)
                if (remaining.length >= 6 && remaining.substring(0, 6) == SAVE_RESPONSE_SUCCESS) {
                    processRawReceive(SAVE_RESPONSE_SUCCESS); consumed = sofIdx + 6; searchFrom = sofIdx + 6; continue
                }
                if (remaining.length >= 6 && remaining.substring(0, 6) == SAVE_RESPONSE_FAILURE) {
                    processRawReceive(SAVE_RESPONSE_FAILURE); consumed = sofIdx + 6; searchFrom = sofIdx + 6; continue
                }
                if (sofIdx + 4 > acc.length) break
                val lengthHex = acc.substring(sofIdx + 2, sofIdx + 4)
                val lengthOrNull = lengthHex.toIntOrNull(16)
                if (lengthOrNull == null) { searchFrom = sofIdx + 2; consumed = sofIdx + 2; continue }
                val length = lengthOrNull
                val frameEnd = sofIdx + 4 + length
                if (frameEnd > acc.length) break
                processRawReceive(acc.substring(sofIdx, frameEnd))
                consumed = frameEnd; searchFrom = frameEnd
            }
            if (consumed > 0) accumulator.delete(0, consumed)
            if (accumulator.length > RX_BUFFER_SIZE * 2) accumulator.clear()
        }
    }

    private fun processRawReceive(raw: String) {
        if (raw.contains(FSC_CONNECT_STRING)) { scope.launch { } }
        if (raw.contains(FSC_DISCONN_STRING)) { scope.launch { onSocketLost() }; return }
        if (raw == SAVE_RESPONSE_SUCCESS) { _saveResponse.tryEmit(true); return }
        if (raw == SAVE_RESPONSE_FAILURE) { _saveResponse.tryEmit(false); return }

        // Frame is already length-extracted by rxLoop — parse directly without splitting.
        // Splitting on "7E" patterns is wrong: TLV data values can contain "7E" (e.g. RPM=126 → "007E").
        when (val result = FrameCodec.parse(raw)) {
            is FrameCodec.ParseResult.Ok -> {
                val frame = result.frame
                Log.d(TAG, "RX OK: info=0x${frame.info.toString(16).uppercase()} sub=0x${frame.subState.toString(16).uppercase()} tlvs=${frame.tlvs.size}")
                if (frame.info == 0x06u.toUByte()) {
                    lastMcStateMs = System.currentTimeMillis()
                    val state = _connectionState.value
                    if (state is ConnectionState.Reconnecting) {
                        _connectionState.value = ConnectionState.Connected(state.deviceName, state.deviceAddress)
                    }
                }
                _inboundFrames.tryEmit(frame)
            }
            is FrameCodec.ParseResult.Error -> Log.w(TAG, "RX ERR: ${result.reason} raw=${raw.take(40)}")
            is FrameCodec.ParseResult.Skip  -> Log.d(TAG, "RX SKIP: ${result.reason}")
        }
    }

    private suspend fun txLoop(s: BluetoothSocket) {
        val outputStream = try { s.outputStream } catch (e: IOException) { return }
        try {
            for (frame in txChannel) {
                try {
                    withContext(Dispatchers.IO) { outputStream.write(frame.toByteArray(Charsets.US_ASCII)) }
                } catch (e: IOException) {
                    if (currentCoroutineContext().isActive) onSocketLost()
                    return
                }
            }
        } catch (_: ClosedReceiveChannelException) {}
    }

    private fun startWatchdog(deviceName: String, deviceAddress: String) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(1_000)
                val silenceMs = System.currentTimeMillis() - lastMcStateMs
                val state = _connectionState.value
                when {
                    silenceMs > RECONNECT_LOST_TIMEOUT_MS && state is ConnectionState.Reconnecting -> {
                        tearDown(ConnectionState.Lost(deviceName, deviceAddress)); return@launch
                    }
                    silenceMs > RECONNECT_WARN_TIMEOUT_MS && state is ConnectionState.Connected -> {
                        _connectionState.value = ConnectionState.Reconnecting(deviceName, deviceAddress)
                        txChannel.trySend(FrameCodec.buildPairedFromPhone())
                    }
                }
            }
        }
    }

    private fun onSocketLost() {
        val state = _connectionState.value
        val (name, address) = when (state) {
            is ConnectionState.Connected -> state.deviceName to state.deviceAddress
            is ConnectionState.Reconnecting -> state.deviceName to state.deviceAddress
            is ConnectionState.Connecting -> state.deviceName to state.deviceAddress
            else -> return
        }
        scope.launch { tearDown(ConnectionState.Lost(name, address)) }
    }

    private suspend fun tearDown(goToState: ConnectionState) {
        tearDownJobs()
        withContext(Dispatchers.IO) { try { socket?.close() } catch (_: IOException) {} }
        socket = null; lastMcStateMs = 0L
        _connectionState.value = goToState
    }

    private fun tearDownJobs() {
        rxJob?.cancel(); rxJob = null
        txJob?.cancel(); txJob = null
        watchdogJob?.cancel(); watchdogJob = null
    }

    fun onDestroy() { scope.cancel(); socket?.let { runCatching { it.close() } } }
}
