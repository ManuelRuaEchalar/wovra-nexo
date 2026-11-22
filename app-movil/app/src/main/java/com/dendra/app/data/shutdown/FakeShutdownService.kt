package com.dendra.app.data.shutdown

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.dendra.app.domain.model.ShieldStatus
import com.dendra.app.domain.model.SystemSignal
import com.dendra.app.service.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Apagado falso. Android no deja que una app bloquee el botón de encendido,
 * pero un servicio de accesibilidad sí puede enterarse de que apareció el
 * menú de apagado, cerrarlo y dibujar encima de todo (incluida la pantalla
 * de bloqueo). Es el mismo mecanismo que usan Cerberus o Hammer Security.
 *
 * Solo actúa con el Escudo activo y el equipo BLOQUEADO: el dueño, que sí
 * puede desbloquear, apaga su celular con normalidad. Un apagado forzado
 * (mantener el botón ~10 s) no se puede impedir: para eso el Escudo retoma
 * la alerta al volver a encender.
 */
class FakeShutdownService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null

    private val dialogsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // El sistema avisa con este motivo al abrir el menú de apagado.
            if (intent?.getStringExtra("reason") == "globalactions") onPowerMenu()
        }
    }

    override fun onServiceConnected() {
        instance = this
        @Suppress("DEPRECATION")
        val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dialogsReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(dialogsReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val name = event.className?.toString() ?: return
        if (POWER_MENU_HINTS.any { name.contains(it, ignoreCase = true) }) {
            Log.d(TAG, "Menú de apagado: ${event.packageName}/$name")
            onPowerMenu()
        }
    }

    private fun onPowerMenu() {
        if (overlay != null) return
        val locator = ServiceLocator.from(this)
        val armed = locator.coordinator.state.value.status != ShieldStatus.INACTIVO
        val locked = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        if (!locator.config.fakeShutdown || !armed || !locked) return
        show()
        SignalBus.publish(SystemSignal.APAGADO_FALSO)
    }

    /** Cierra el menú de apagado real y cubre la pantalla con el apagado simulado. */
    fun show() {
        if (overlay != null) return
        dismissPowerMenu()
        handler.postDelayed(::dismissPowerMenu, 250)
        handler.postDelayed(::dismissPowerMenu, 700)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.OPAQUE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = FakeShutdownView(
            this,
            onDark = {
                // Brillo al mínimo: en pantallas AMOLED el negro es "apagado".
                params.screenBrightness = 0f
                overlay?.let { runCatching { wm.updateViewLayout(it, params) } }
            },
            onExit = ::hide
        )
        @Suppress("DEPRECATION")
        view.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        runCatching { wm.addView(view, params) }.onSuccess {
            overlay = view
            _active.value = true
        }
    }

    fun hide() {
        val view = overlay ?: return
        overlay = null
        _active.value = false
        runCatching { (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view) }
    }

    private fun dismissPowerMenu() {
        runCatching { performGlobalAction(GLOBAL_ACTION_BACK) }
        @Suppress("DEPRECATION")
        runCatching { sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) }
    }

    /** Abre una pantalla propia aunque la app esté en segundo plano. */
    fun launch(intent: Intent) = runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        runCatching { unregisterReceiver(dialogsReceiver) }
        hide()
        instance = null
        return super.onUnbind(intent)
    }

    companion object {
        private const val TAG = "ApagadoFalso"

        /** Nombres de la ventana del menú de apagado (Android puro, MIUI, One UI…). */
        private val POWER_MENU_HINTS = listOf("globalactions", "ShutdownDialog", "PowerMenu", "PowerOff")

        @Volatile
        var instance: FakeShutdownService? = null
            private set

        private val _active = MutableStateFlow(false)
        /** El apagado simulado está cubriendo la pantalla ahora. */
        val active: StateFlow<Boolean> = _active.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val me = ComponentName(context, FakeShutdownService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
