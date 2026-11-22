import { useEffect, useState } from 'react'
import { Logo } from './Logo'
import type { TextosSitio } from './textos'
import './sitio.css'

const SECCIONES = ['senal', 'red', 'recompensa', 'privacidad', 'equipo'] as const

/**
 * Barra de todo el sitio. En la landing enlaza a sus secciones y marca la que se
 * está leyendo; en el panel y en la página de reclamo, las mismas secciones llevan
 * a la landing. "Reportar mi celular" abre el panel web desde cualquier página.
 */
export function Navbar({
  t,
  cambiarIdioma,
  pagina,
}: {
  t: TextosSitio['nav']
  cambiarIdioma: () => void
  pagina: 'landing' | 'panel' | 'reclamo'
}) {
  const enLanding = pagina === 'landing'
  const base = enLanding ? '' : '/'
  const [scrolleada, setScrolleada] = useState(false)
  const [abierto, setAbierto] = useState(false)
  const [actual, setActual] = useState<string | null>(null)

  useEffect(() => {
    const alMover = () => setScrolleada(scrollY > 40)
    alMover()
    addEventListener('scroll', alMover, { passive: true })
    if (!enLanding) return () => removeEventListener('scroll', alMover)
    // La sección activa es la que cruza la franja central de la pantalla.
    const io = new IntersectionObserver(
      (entradas) => {
        for (const e of entradas) if (e.isIntersecting) setActual(e.target.id === 'arriba' ? null : e.target.id)
      },
      { rootMargin: '-45% 0px -50% 0px' },
    )
    for (const id of ['arriba', ...SECCIONES, 'unete']) {
      const el = document.getElementById(id)
      if (el) io.observe(el)
    }
    return () => {
      removeEventListener('scroll', alMover)
      io.disconnect()
    }
  }, [enLanding])

  useEffect(() => {
    if (!abierto) return
    const cerrar = (e: KeyboardEvent) => e.key === 'Escape' && setAbierto(false)
    addEventListener('keydown', cerrar)
    return () => removeEventListener('keydown', cerrar)
  }, [abierto])

  const nombres: Record<(typeof SECCIONES)[number], string> = {
    senal: t.senal,
    red: t.red,
    recompensa: t.recompensa,
    privacidad: t.privacidad,
    equipo: t.equipo,
  }
  const cerrar = () => setAbierto(false)
  // Fuera de la landing la barra siempre es sólida: el contenido es claro.
  const solida = scrolleada || abierto || !enLanding

  return (
    <header className={`nav${solida ? ' solida' : ''}${abierto ? ' abierta' : ''}`}>
      <a className="saltar" href="#contenido">
        {t.saltar}
      </a>
      <nav className="nav-barra" aria-label={t.principal}>
        <a className="nav-marca" href={enLanding ? '#arriba' : '/'} onClick={cerrar}>
          <Logo /> Dendra
        </a>
        <button
          className="nav-menu"
          aria-expanded={abierto}
          aria-controls="nav-enlaces"
          aria-label={abierto ? t.cerrar : t.menu}
          onClick={() => setAbierto(!abierto)}
        >
          <span />
          <span />
        </button>
        <div className="nav-enlaces" id="nav-enlaces">
          <ul>
            {SECCIONES.map((id) => (
              <li key={id}>
                <a href={`${base}#${id}`} aria-current={actual === id ? 'location' : undefined} onClick={cerrar}>
                  {nombres[id]}
                </a>
              </li>
            ))}
          </ul>
          <div className="nav-acciones">
            <button className="nav-idioma" onClick={cambiarIdioma} aria-label={t.idiomaEtiqueta} title={t.idioma}>
              {t.idiomaCorto}
            </button>
            <a className="nav-reportar" href="/panel" aria-current={pagina === 'panel' ? 'page' : undefined} onClick={cerrar}>
              <i aria-hidden="true" />
              {t.reportar}
            </a>
            <a className="nav-cta" href="/dendra.apk" download onClick={cerrar}>
              {t.unete}
            </a>
          </div>
        </div>
      </nav>
    </header>
  )
}
