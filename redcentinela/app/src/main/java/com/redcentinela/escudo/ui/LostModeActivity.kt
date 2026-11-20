package com.redcentinela.escudo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redcentinela.escudo.service.ServiceLocator
import com.redcentinela.escudo.ui.component.BotonDestello
import com.redcentinela.escudo.ui.component.Etiqueta
import com.redcentinela.escudo.ui.component.Iconos
import com.redcentinela.escudo.ui.component.RecompensaEnPantallaPerdido
import com.redcentinela.escudo.ui.component.TarjetaNoche
import com.redcentinela.escudo.ui.screen.ahora
import com.redcentinela.escudo.ui.theme.Destello
import com.redcentinela.escudo.ui.theme.Noche
import com.redcentinela.escudo.ui.theme.RedCentinelaTheme
import com.redcentinela.escudo.ui.theme.Sora
import com.redcentinela.escudo.ui.theme.TextoNoche
import com.redcentinela.escudo.ui.theme.Tipo
import com.redcentinela.escudo.util.Format

/**
 * B5 · Modo encontrado. Se muestra sobre la pantalla de bloqueo cuando el
 * celular fue reportado como perdido: quien lo encuentra puede avisar al
 * dueño sin desbloquearlo. No toma fotos ni actúa agresivamente.
 */
class LostModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val locator = ServiceLocator.from(this)
        setContent {
            RedCentinelaTheme {
                val visible by locator.lostScreen.collectAsStateWithLifecycle()
                // Se cierra sola cuando termina la alerta o cuando manda el apagado falso.
                LaunchedEffect(visible) { if (!visible) finish() }
                val contacto = locator.circle.members.collectAsStateWithLifecycle().value
                    .sortedByDescending { it.isGuardian }.firstOrNull { !it.phone.isNullOrBlank() }
                BackHandler { }
                var avisado by rememberSaveable { mutableStateOf(false) }
                val now = ahora(10_000)
                val dueno = locator.config.userName.ifBlank { "su dueño" }
                val recompensa = locator.rewards.reward.collectAsStateWithLifecycle().value
                    ?.takeIf { it.showOnLostScreen }

                TarjetaNoche(Modifier.fillMaxSize(), radio = 0.dp) {
                    Column(
                        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(start = 28.dp, end = 28.dp, top = if (recompensa != null) 32.dp else 64.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column {
                            Text(
                                Format.clock(now), fontFamily = Sora, fontWeight = FontWeight(200),
                                fontSize = if (recompensa != null) 52.sp else 76.sp,
                                letterSpacing = (-0.05).em, color = Color.White
                            )
                            Text(Format.day(now), style = Tipo.Cuerpo, color = TextoNoche)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Etiqueta("Celular perdido", color = Destello)
                            Text(
                                "Este celular está perdido. Ayuda a que vuelva con $dueno.",
                                style = Tipo.Titulo, color = Color.White
                            )
                            Text(
                                "No necesitas desbloquearlo. $dueno y su círculo recibirán tu aviso con la ubicación.".replaceFirstChar { it.uppercase() },
                                style = Tipo.Cuerpo, color = TextoNoche
                            )
                        }
                        if (recompensa != null) RecompensaEnPantallaPerdido(recompensa)
                        if (contacto != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Etiqueta("O llama a ${contacto.name}", color = Destello)
                                Text(
                                    contacto.phone.orEmpty(), fontFamily = Sora, fontWeight = FontWeight(300),
                                    fontSize = 26.sp, color = Color.White
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (!avisado) {
                            BotonDestello("Toca aquí para avisar", alto = 58.dp) {
                                avisado = true
                                locator.coordinator.notifyFound()
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth().border(1.dp, Destello.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Iconos.Check, null, tint = Destello, modifier = Modifier.size(22.dp))
                                Text("Aviso enviado. Gracias: quédate cerca, vendrán a buscarlo.", style = Tipo.Cuerpo, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
