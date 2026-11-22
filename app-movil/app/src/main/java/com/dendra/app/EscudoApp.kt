package com.dendra.app

import android.app.Application
import com.dendra.app.service.ServiceLocator

/**
 * Application: inicializa el contenedor de dependencias para que el servicio
 * y la UI compartan las mismas instancias (coordinador, bitácora, buscador).
 */
class EscudoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Crea (o recupera) el singleton del contenedor.
        ServiceLocator.from(this)
    }
}
