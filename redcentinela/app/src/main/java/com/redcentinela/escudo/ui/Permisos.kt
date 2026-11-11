package com.redcentinela.escudo.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.redcentinela.escudo.data.shutdown.FakeShutdownService

/** Consultas de permisos y accesos directos a los ajustes del sistema. */
object Permisos {

    val UBICACION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    val BLUETOOTH: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else emptyArray()

    val SMS = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)

    val NOTIFICACIONES: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        else emptyArray()

    fun tiene(context: Context, permisos: Array<String>): Boolean =
        permisos.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    fun ubicacion(context: Context) = tiene(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))

    fun ubicacionSiempre(context: Context): Boolean =
        ubicacion(context) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            tiene(context, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)))

    fun sinAhorroBateria(context: Context): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)

    fun apagadoFalso(context: Context) = FakeShutdownService.isEnabled(context)

    /** ¿El celular tiene PIN, patrón o huella? (requisito de entrada de la propuesta). */
    fun bloqueoSeguro(context: Context): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure

    fun abrirSeguridad(context: Context) = abrir(context, Intent(Settings.ACTION_SECURITY_SETTINGS))

    fun abrirAccesibilidad(context: Context) = abrir(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun abrirAjustesApp(context: Context) = abrir(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )

    @SuppressLint("BatteryLife")
    fun pedirSinAhorro(context: Context) {
        val directo = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")
        )
        if (!abrir(context, directo)) abrir(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    private fun abrir(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}
