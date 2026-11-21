import { StrictMode, lazy, Suspense } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'

/*
 * Un solo sitio, tres rutas. Cada una carga solo lo suyo:
 *   /         landing (Three.js y los fondos animados)
 *   /r/:id    página de reclamo de la recompensa (Pollar, Stellar)
 *   /panel    panel web del dueño (mapa, descifrado en el navegador)
 */
const raiz = createRoot(document.getElementById('root')!)
const ruta = location.pathname

if (ruta === '/' || ruta === '/index.html') {
  const Landing = lazy(() => import('./landing/Landing'))
  raiz.render(
    <StrictMode>
      <Suspense fallback={<div className="landing-cargando" />}>
        <Landing />
      </Suspense>
    </StrictMode>,
  )
} else if (ruta.startsWith('/panel')) {
  document.title = 'Recompensa garantizada · Dendra'
  // El panel no usa Pollar: solo descifra en el navegador y habla con el relevo.
  import('./panel/Panel').then(({ default: Panel }) =>
    raiz.render(
      <StrictMode>
        <Panel />
      </StrictMode>,
    ),
  )
} else {
  document.title = 'Recompensa garantizada · Dendra'
  import('./reclamo').then(({ iniciarReclamo }) => iniciarReclamo(raiz))
}
