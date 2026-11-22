package com.dendra.app.data.comm

import com.dendra.app.data.ble.BleHub
import com.dendra.app.domain.model.CommChannel
import com.dendra.app.domain.model.Outbound
import com.dendra.app.domain.port.CommLink

/**
 * Tercer eslabón: Bluetooth entre teléfonos (sin datos ni chip). En alerta,
 * el aviso BLE del equipo lleva su ubicación cifrada; cualquier teléfono de
 * la red que pase cerca lo capta y lo reenvía cuando tenga conexión
 * ("guardar y reenviar").
 *
 * El aviso lo mantiene al día el servicio del Escudo en cada cambio de
 * estado o de ubicación; este eslabón solo confirma que se está emitiendo.
 * "Entregado" aquí significa que la difusión está activa.
 */
class BleCommLink(private val hub: BleHub) : CommLink {

    override val channel = CommChannel.BLUETOOTH

    override fun isAvailable(): Boolean = hub.canAdvertise()

    override suspend fun send(message: Outbound): Boolean =
        message is Outbound.Report && message.inAlert && hub.advertising
}
