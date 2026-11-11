package com.redcentinela.escudo.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

/**
 * Llavero guardián Bluetooth tipo iTag (1 a 3 USD). El teléfono mantiene una
 * conexión con el llavero: si se corta, se separaron; si aprietan su botón,
 * el celular queda reportado como robado. El pitido al alejarse lo hace el
 * propio llavero (los iTag suenan solos cuando pierden la conexión).
 */
@SuppressLint("MissingPermission")
class TagLink(context: Context) {

    enum class State { SIN_LLAVERO, BUSCANDO, CERCA, LEJOS }

    data class Found(val name: String, val address: String, val rssi: Int)

    private val appContext = context.applicationContext
    private val adapter by lazy {
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private val _state = MutableStateFlow(State.SIN_LLAVERO)
    val state: StateFlow<State> = _state.asStateFlow()

    var onButton: () -> Unit = {}
    var onAway: () -> Unit = {}
    var onBack: () -> Unit = {}

    private var gatt: BluetoothGatt? = null
    private var everConnected = false

    /** Dispositivos Bluetooth cercanos, para elegir el llavero al vincular. */
    fun discover(): Flow<List<Found>> = callbackFlow {
        val scanner = if (hasPermission(android.Manifest.permission.BLUETOOTH_SCAN)) adapter?.bluetoothLeScanner else null
        if (scanner == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val found = LinkedHashMap<String, Found>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull()
                // Los llaveros se anuncian con nombre ("iTAG", "ITAG"...).
                if (name.isNullOrBlank()) return
                found[result.device.address] = Found(name.trim(), result.device.address, result.rssi)
                trySend(found.values.sortedByDescending { it.rssi })
            }
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        runCatching { scanner.startScan(null, settings, callback) }
        awaitClose { runCatching { scanner.stopScan(callback) } }
    }

    @Synchronized
    fun connect(address: String) {
        disconnect()
        if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) return
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return
        _state.value = State.BUSCANDO
        everConnected = false
        // autoConnect: Android reconecta solo cuando el llavero vuelve a estar cerca.
        gatt = runCatching { device.connectGatt(appContext, true, callback, android.bluetooth.BluetoothDevice.TRANSPORT_LE) }.getOrNull()
    }

    @Synchronized
    fun disconnect() {
        gatt?.let { runCatching { it.disconnect() }; runCatching { it.close() } }
        gatt = null
        _state.value = State.SIN_LLAVERO
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                val wasAway = _state.value == State.LEJOS
                _state.value = State.CERCA
                everConnected = true
                runCatching { g.discoverServices() }
                if (wasAway) onBack()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (_state.value == State.CERCA) {
                    _state.value = State.LEJOS
                    onAway()
                } else if (!everConnected) {
                    _state.value = State.BUSCANDO
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            // Botón del iTag: servicio FFE0, característica FFE1 (notificación).
            val button = g.getService(BUTTON_SERVICE)?.getCharacteristic(BUTTON_CHAR) ?: return
            runCatching {
                g.setCharacteristicNotification(button, true)
                button.getDescriptor(CLIENT_CONFIG)?.let { descriptor ->
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(descriptor)
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == BUTTON_CHAR) onButton()
        }
    }

    private fun hasPermission(permission: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        val BUTTON_SERVICE: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val BUTTON_CHAR: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
