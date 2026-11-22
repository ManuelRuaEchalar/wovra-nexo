package com.dendra.app.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.dendra.app.data.shutdown.FakeShutdownView

/**
 * Apagado simulado en una pantalla normal, para probarlo desde Ajustes cuando
 * todavía no se activó el servicio de accesibilidad. El apagado falso real
 * (que reacciona al botón de encendido) lo hace
 * [com.dendra.app.data.shutdown.FakeShutdownService].
 */
class FakeShutdownActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = FakeShutdownView(
            this,
            onDark = {
                window.attributes = window.attributes.apply { screenBrightness = 0f }
            },
            onExit = ::finish
        )
        @Suppress("DEPRECATION")
        view.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(view)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = Unit
}
