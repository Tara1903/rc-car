package com.example.roboracer.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.roboracer.bluetooth.protocol.CommandFrame
import com.example.roboracer.bluetooth.protocol.TelemetryFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothSppTransport(
    private val context: Context,
    private val scope: CoroutineScope
) : VehicleTransport {

    companion object {
        private const val TAG = "RoboRacerSPP"
        // Standard SPP UUID for Bluetooth Serial Port Profile
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _telemetryFlow = MutableStateFlow<TelemetryFrame?>(null)
    override val telemetryFlow: StateFlow<TelemetryFrame?> = _telemetryFlow.asStateFlow()

    private val _rttLatencyMs = MutableStateFlow(0L)
    override val rttLatencyMs: StateFlow<Long> = _rttLatencyMs.asStateFlow()

    private val _activeDeviceName = MutableStateFlow<String?>(null)
    override val activeDeviceName: StateFlow<String?> = _activeDeviceName.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null
    private var lastCommandSentTime = 0L

    // Dedicated conflated channel for strictly ordered FIFO command dispatch without stale queuing
    private val commandChannel = Channel<CommandFrame>(Channel.CONFLATED)

    @SuppressLint("MissingPermission")
    override fun getBondedDevices(): List<DiscoveredDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices.map {
                DiscoveredDevice(
                    name = it.name ?: "Unknown Device",
                    address = it.address
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying bonded devices: ${e.message}")
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(deviceAddress: String, deviceName: String) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }

        disconnect()
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _activeDeviceName.value = deviceName

        scope.launch(Dispatchers.IO) {
            try {
                adapter.cancelDiscovery() // Always cancel discovery before connecting
                val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                
                // Try standard secure RFCOMM socket first, fallback to reflection if needed
                val btSocket = try {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                } catch (e: Exception) {
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    m.invoke(device, 1) as BluetoothSocket
                }

                btSocket.connect()
                socket = btSocket
                outputStream = btSocket.outputStream
                inputStream = btSocket.inputStream

                _connectionStatus.value = ConnectionStatus.CONNECTED
                startWritingLoop()
                startReadingLoop()

            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                _connectionStatus.value = ConnectionStatus.ERROR
                disconnect()
            }
        }
    }

    override fun disconnect() {
        writeJob?.cancel()
        writeJob = null
        readJob?.cancel()
        readJob = null

        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}

        outputStream = null
        inputStream = null
        socket = null

        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _activeDeviceName.value = null
    }

    override fun sendCommand(command: CommandFrame) {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        // Offer to FIFO conflated channel; consumed sequentially by single writer coroutine
        commandChannel.trySend(command)
    }

    private fun startWritingLoop() {
        writeJob?.cancel()
        writeJob = scope.launch(Dispatchers.IO) {
            for (cmd in commandChannel) {
                val out = outputStream
                if (out == null || _connectionStatus.value != ConnectionStatus.CONNECTED) {
                    continue
                }
                try {
                    val bytes = cmd.toByteArray()
                    lastCommandSentTime = System.currentTimeMillis()
                    out.write(bytes)
                    out.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Send command failed: ${e.message}")
                    withContext(Dispatchers.Main) {
                        _connectionStatus.value = ConnectionStatus.ERROR
                    }
                    break
                }
            }
        }
    }

    private fun startReadingLoop() {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val inStream = inputStream ?: return@launch
            val buffer = ByteArray(256)
            val ringBuffer = ByteArray(64)
            var ringCount = 0

            while (isActive && _connectionStatus.value == ConnectionStatus.CONNECTED) {
                try {
                    val bytesRead = inStream.read(buffer)
                    if (bytesRead < 0) break

                    // Calculate rough RTT
                    if (lastCommandSentTime > 0) {
                        val latency = System.currentTimeMillis() - lastCommandSentTime
                        _rttLatencyMs.value = latency.coerceIn(5, 500)
                    }

                    // Feed incoming bytes to ring buffer for frame synchronizer
                    for (i in 0 until bytesRead) {
                        val b = buffer[i]

                        if (ringCount == 0) {
                            if (b == TelemetryFrame.START_BYTE) {
                                ringBuffer[ringCount++] = b
                            }
                        } else {
                            ringBuffer[ringCount++] = b
                            if (ringCount == TelemetryFrame.FRAME_SIZE) {
                                val frame = TelemetryFrame.parse(ringBuffer, 0)
                                if (frame != null) {
                                    _telemetryFlow.value = frame
                                }
                                ringCount = 0
                            } else if (ringCount >= ringBuffer.size) {
                                ringCount = 0 // Overflow protection
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Socket read error: ${e.message}")
                        withContext(Dispatchers.Main) {
                            _connectionStatus.value = ConnectionStatus.ERROR
                        }
                    }
                    break
                }
            }
        }
    }
}
