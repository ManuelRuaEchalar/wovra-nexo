package com.redcentinela.escudo.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redcentinela.escudo.data.alarm.Alarm
import com.redcentinela.escudo.service.ServiceLocator
import com.redcentinela.escudo.ui.theme.RedCentinelaTheme
import com.redcentinela.escudo.ui.theme.Tinta
import com.redcentinela.escudo.ui.theme.Tipo

/**
 * "Hacer sonar": mientras el celular suena, la pantalla se enciende en blanco al
 * brillo máximo, encima del bloqueo. Junto con la sirena y la linterna, delata al
 * equipo en la oscuridad, dentro de un bolso o bajo un cojín. Tocarla lo silencia;
 * se cierra sola cuando la alarma termina.
 */
class SonandoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }
        val quien = intent.getStringExtra(EXTRA_QUIEN)
        val locator = ServiceLocator.from(this)
        setContent {
            RedCentinelaTheme {
                val sonando by Alarm.sonando.collectAsStateWithLifecycle()
                LaunchedEffect(sonando) { if (!sonando) finish() }
                Column(
                    Modifier.fillMaxSize().background(Color.White)
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            Alarm.stop(this@SonandoActivity)
                            locator.notifier.cancelRing()
                            finish()
                        }
                        .padding(40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        quien?.let { "${it.replaceFirstChar { c -> c.uppercase() }} está buscando este celular" }
                            ?: "Están buscando este celular",
                        style = Tipo.Fila, color = Tinta.copy(alpha = 0.55f), textAlign = TextAlign.Center
                    )
                    Text("Toca para silenciar", style = Tipo.Nota, color = Tinta.copy(alpha = 0.35f))
                }
            }
        }
    }

    companion object {
        const val EXTRA_QUIEN = "quien"
    }
}
