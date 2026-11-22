package com.dendra.app.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Único punto de acceso a Bluetooth LE. Emite el aviso del equipo (para el
 * guardián, la búsqueda cercana y el relevo entre teléfonos) y escucha los
 * avisos de los demás. Guardián, búsqueda y relevo consumen el mismo flujo de
 * [sightings]: así hay un solo escaneo, de bajo consumo.
 *
 * El aviso es de una sola vía. Para las órdenes sin internet ni SMS (hacer
 * sonar, reportar) el equipo acepta además una conexión corta: quien está
 * cerca se conecta, escribe la orden firmada y se desconecta.
 */
@SuppressLint("MissingPermission")
class BleHub(context: Context, private val scope: CoroutineScope) {

    enum class ScanLevel { OFF, FONDO, GUARDIAN, BUSQUEDA }

    private val appContext = context.applicationContext
    private val manager by lazy { appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    private val adapter by lazy { manager?.adapter }

    private class Seen(val device: BluetoothDevice, val at: Long)

    /** Último contacto Bluetooth con cada equipo: a quién conectarse para darle una orden. */
    private val seen = ConcurrentHashMap<String, Seen>()
    private val orderLock = Mutex()
    private var server: BluetoothGattServer? = null
    private var lastAdvert: Pair<ByteArray, Boolean>? = null

    private val _sightings = MutableSharedFlow<Sighting>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sightings: SharedFlow<Sighting> = _sightings.asSharedFlow()

    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null
    private var level = ScanLevel.OFF
    private var boost = false
    private var renewJob: Job? = null

    @Volatile
    var advertising = false
        private set

    fun isOn(): Boolean = runCatching { adapter?.isEnabled == true }.getOrDefault(false)

    fun canAdvertise(): Boolean = isOn() && has(android.Manifest.permission.BLUETOOTH_ADVERTISE) &&
        runCatching { adapter?.bluetoothLeAdvertiser != null }.getOrDefault(false)

    fun canScan(): Boolean = isOn() && has(android.Manifest.permission.BLUETOOTH_SCAN)

    /** Publica (o reemplaza) el aviso del equipo. [urgent] sube potencia y cadencia. */
    @Synchronized
    fun advertise(payload: ByteArray, urgent: Boolean) {
        stopAdvertising()
        lastAdvert = payload to urgent
        if (!canAdvertise()) return
        val advertiser = adapter?.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(
                if (urgent) AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
                else AdvertiseSettings.ADVERTISE_MODE_BALANCED
            )
            .setTxPowerLevel(
                if (urgent) AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                else AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
            )
            // Conectable solo si este equipo puede recibir órdenes por Bluetooth.
            .setConnectable(server != null)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(BleCodec.MANUFACTURER_ID, payload)
            .build()
        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                advertising = true
            }

            override fun onStartFailure(errorCode: Int) {
                advertising = false
            }
        }
        advertiseCallback = callback
        runCatching { advertiser.startAdvertising(settings, data, callback) }
    }

    @Synchronized
    fun stopAdvertising() {
        val callback = advertiseCallback ?: return
        runCatching { adapter?.bluetoothLeAdvertiser?.stopAdvertising(callback) }
        advertiseCallback = null
        advertising = false
    }

    /** Nivel de escaneo de fondo que pide el servicio del Escudo. */
    fun setLevel(newLevel: ScanLevel) {
        level = newLevel
        restartScan()
    }

    /** La pantalla de búsqueda cercana pide el escaneo más rápido mientras está abierta. */
    fun setBoost(on: Boolean) {
        boost = on
        restartScan()
    }

    @Synchronized
    private fun restartScan() {
        scanCallback?.let { cb -> runCatching { adapter?.bluetoothLeScanner?.stopScan(cb) } }
        scanCallback = null
        renewJob?.cancel()
        val effective = if (boost) ScanLevel.BUSQUEDA else level
        if (effective == ScanLevel.OFF || !canScan()) return
        val scanner = adapter?.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setManufacturerData(BleCodec.MANUFACTURER_ID, byteArrayOf(BleCodec.MAGIC), byteArrayOf(0xFF.toByte()))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(
                when (effective) {
                    ScanLevel.BUSQUEDA -> ScanSettings.SCAN_MODE_LOW_LATENCY
                    ScanLevel.GUARDIAN -> ScanSettings.SCAN_MODE_BALANCED
                    else -> ScanSettings.SCAN_MODE_LOW_POWER
                }
            )
            .build()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.getManufacturerSpecificData(BleCodec.MANUFACTURER_ID)
                BleCodec.parse(data, result.rssi, System.currentTimeMillis())?.let {
                    seen[it.deviceId] = Seen(result.device, it.at)
                    _sightings.tryEmit(it)
                }
            }
        }
        scanCallback = callback
        runCatching { scanner.startScan(listOf(filter), settings, callback) }
        // Android degrada los escaneos de más de 30 min: se renueva antes.
        renewJob = scope.launch {
            delay(RENEW_MS)
            if (isActive) restartScan()
        }
    }

    // --- Órdenes por conexión directa --------------------------------------------

    /** ¿Hay señal Bluetooth reciente de ese equipo? (se le puede dar una orden directa) */
    fun isNear(deviceId: String): Boolean =
        seen[deviceId]?.let { System.currentTimeMillis() - it.at < NEAR_MS } == true

    /** Abre el receptor de órdenes de este equipo. [onOrder] recibe los bytes tal cual. */
    @Synchronized
    fun startServer(onOrder: (ByteArray) -> Unit) {
        if (server != null || !isOn() || !has(android.Manifest.permission.BLUETOOTH_CONNECT)) return
        val callback = object : BluetoothGattServerCallback() {
            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
            ) {
                val ok = characteristic.uuid == ORDER_UUID && !preparedWrite && offset == 0 && value != null
                if (responseNeeded) {
                    runCatching {
                        server?.sendResponse(
                            device, requestId,
                            if (ok) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE, 0, null
                        )
                    }
                }
                if (ok) onOrder(value!!)
            }

            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                // Algunos equipos dejan de emitir mientras atienden una conexión.
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    lastAdvert?.let { (payload, urgent) -> advertise(payload, urgent) }
                }
            }
        }
        val opened = runCatching { manager?.openGattServer(appContext, callback) }.getOrNull() ?: return
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                ORDER_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE
            )
        )
        runCatching { opened.addService(service) }
        server = opened
    }

    @Synchronized
    fun stopServer() {
        runCatching { server?.close() }
        server = null
    }

    /**
     * Entrega [payload] al equipo [deviceId] conectándose a él. Solo funciona
     * con su aviso a la vista (unos metros). true = el otro equipo la recibió.
     */
    suspend fun sendOrder(deviceId: String, payload: ByteArray): Boolean {
        if (!isOn() || !has(android.Manifest.permission.BLUETOOTH_CONNECT)) return false
        return orderLock.withLock {
            repeat(ORDER_TRIES) { attempt ->
                // La dirección Bluetooth del otro equipo cambia: se usa siempre la más reciente.
                val target = seen[deviceId]?.takeIf { System.currentTimeMillis() - it.at < NEAR_MS }
                if (target != null && writeOnce(target.device, payload)) return@withLock true
                if (attempt < ORDER_TRIES - 1) delay(ORDER_RETRY_MS)
            }
            false
        }
    }

    private suspend fun writeOnce(device: BluetoothDevice, payload: ByteArray): Boolean {
        val done = CompletableDeferred<Boolean>()
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> if (!gatt.discoverServices()) done.complete(false)
                    BluetoothProfile.STATE_DISCONNECTED -> done.complete(false)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val characteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(ORDER_UUID)
                if (characteristic == null) {
                    done.complete(false)
                    return
                }
                val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = payload
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(characteristic)
                }
                if (!started) done.complete(false)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
            ) {
                done.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
        }
        val gatt = runCatching {
            device.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
        }.getOrNull() ?: return false
        val ok = withTimeoutOrNull(ORDER_TIMEOUT_MS) { done.await() } ?: false
        runCatching { gatt.disconnect() }
        runCatching { gatt.close() }
        return ok
    }

    private fun has(permission: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val RENEW_MS = 20 * 60_000L
        const val NEAR_MS = 12_000L
        const val ORDER_TRIES = 3
        const val ORDER_RETRY_MS = 1_500L
        const val ORDER_TIMEOUT_MS = 8_000L
        val SERVICE_UUID: UUID = UUID.fromString("7e5c0001-52c1-4e1a-9d0b-6c2a5e0d1c71")
        val ORDER_UUID: UUID = UUID.fromString("7e5c0002-52c1-4e1a-9d0b-6c2a5e0d1c71")
    }
}
