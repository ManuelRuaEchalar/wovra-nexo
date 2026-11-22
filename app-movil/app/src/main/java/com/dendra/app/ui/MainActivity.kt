package com.dendra.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dendra.app.ui.component.BarraInferior
import com.dendra.app.ui.component.Pestana
import com.dendra.app.ui.screen.AgregarScreen
import com.dendra.app.ui.screen.AjustesScreen
import com.dendra.app.ui.screen.BuscarScreen
import com.dendra.app.ui.screen.CirculoScreen
import com.dendra.app.ui.screen.CiudadScreen
import com.dendra.app.ui.screen.EscudoScreen
import com.dendra.app.ui.screen.GuardianScreen
import com.dendra.app.ui.screen.Inicio
import com.dendra.app.ui.screen.MiembroScreen
import com.dendra.app.ui.screen.RegistroScreen
import com.dendra.app.ui.screen.ReportarScreen
import com.dendra.app.ui.screen.SeguimientoScreen
import com.dendra.app.ui.theme.Fondo
import com.dendra.app.ui.theme.Noche
import com.dendra.app.ui.theme.DendraTheme

/** Pantallas que se abren encima de las pestañas. */
private sealed interface Pantalla {
    data class Reportar(val memberId: String?) : Pantalla
    data class Seguimiento(val memberId: String) : Pantalla
    data class Registro(val memberId: String?) : Pantalla
    data class Buscar(val memberId: String) : Pantalla
    data class Miembro(val memberId: String) : Pantalla
    data object Agregar : Pantalla
    data object Guardian : Pantalla
}

/** Única Activity de la app: primer uso o las cuatro pestañas del diseño. */
class MainActivity : ComponentActivity() {

    /** Miembro cuyo seguimiento hay que abrir (se llegó tocando una notificación). */
    private var pendingTrack by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingTrack = intent?.getStringExtra(EXTRA_TRACK)
        setContent {
            DendraTheme {
                val vm: EscudoViewModel = viewModel()
                App(vm, pendingTrack) { pendingTrack = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_TRACK)?.let { pendingTrack = it }
    }

    companion object {
        const val EXTRA_TRACK = "track"
    }
}

@Composable
private fun App(vm: EscudoViewModel, pendingTrack: String?, onTrackConsumed: () -> Unit) {
    val context = LocalContext.current
    var onboarded by remember { mutableStateOf(vm.config.onboarded) }
    var pestana by remember { mutableStateOf(Pestana.ESCUDO) }
    val pila = remember { mutableStateListOf<Pantalla>() }

    // Al volver de los ajustes del sistema se releen permisos y estados.
    var refresco by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresco++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { vm.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() } }
    LaunchedEffect(pendingTrack, onboarded) {
        if (pendingTrack != null && onboarded) {
            pila.clear()
            pila.add(Pantalla.Seguimiento(pendingTrack))
            onTrackConsumed()
        }
    }

    fun abrir(p: Pantalla) = pila.add(p)
    fun volver() {
        if (pila.isNotEmpty()) pila.removeAt(pila.lastIndex)
    }
    BackHandler(enabled = pila.isNotEmpty()) { volver() }
    BackHandler(enabled = pila.isEmpty() && onboarded && pestana != Pestana.ESCUDO) { pestana = Pestana.ESCUDO }

    val actual = pila.lastOrNull()
    val noche = actual is Pantalla.Buscar
    Box(Modifier.fillMaxSize().background(if (noche) Noche else Fondo).windowInsetsPadding(WindowInsets.safeDrawing)) {
        when {
            !onboarded -> Inicio(vm, refresco) { onboarded = true }

            actual is Pantalla.Reportar -> ReportarScreen(
                vm, actual.memberId, onBack = ::volver,
                onAdd = { volver(); abrir(Pantalla.Agregar) },
                onReported = { volver(); abrir(Pantalla.Seguimiento(it.id)) }
            )
            actual is Pantalla.Seguimiento -> SeguimientoScreen(
                vm, actual.memberId, onBack = ::volver,
                onRegistro = { abrir(Pantalla.Registro(actual.memberId)) },
                onBuscar = { abrir(Pantalla.Buscar(actual.memberId)) },
                onReport = { abrir(Pantalla.Reportar(it.id)) }
            )
            actual is Pantalla.Registro -> RegistroScreen(vm, actual.memberId, onBack = ::volver)
            actual is Pantalla.Buscar -> BuscarScreen(vm, actual.memberId, onBack = ::volver)
            actual is Pantalla.Miembro -> MiembroScreen(
                vm, actual.memberId, onBack = ::volver,
                onReport = { abrir(Pantalla.Reportar(it.id)) },
                onTrack = { abrir(Pantalla.Seguimiento(it.id)) }
            )
            actual is Pantalla.Agregar -> AgregarScreen(vm, onBack = ::volver)
            actual is Pantalla.Guardian -> GuardianScreen(vm, onBack = ::volver)

            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (pestana) {
                        Pestana.ESCUDO -> EscudoScreen(
                            vm,
                            onReport = { abrir(Pantalla.Reportar(null)) },
                            onGuardian = { abrir(Pantalla.Guardian) },
                            onRegistro = { abrir(Pantalla.Registro(null)) },
                            onAjustes = { pestana = Pestana.AJUSTES },
                            onCiudad = { pestana = Pestana.CIUDAD }
                        )
                        Pestana.CIUDAD -> CiudadScreen()
                        Pestana.CIRCULO -> CirculoScreen(
                            vm,
                            onAdd = { abrir(Pantalla.Agregar) },
                            onMember = { abrir(Pantalla.Miembro(it.id)) },
                            onReport = { abrir(Pantalla.Reportar(it.id)) },
                            onTrack = { abrir(Pantalla.Seguimiento(it.id)) }
                        )
                        Pestana.AJUSTES -> AjustesScreen(vm, refresco, onGuardian = { abrir(Pantalla.Guardian) })
                    }
                }
                BarraInferior(pestana) { pestana = it }
            }
        }
    }
}
