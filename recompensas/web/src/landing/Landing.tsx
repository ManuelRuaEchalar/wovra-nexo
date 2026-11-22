import { useEffect, useRef, useState } from 'react'
import { Logo } from '../Logo'
import { sitio } from '../sitio'
import { fondoElegido, fondos } from './config'
import type { Fondo } from './fondos/comun'
import Senal from './senal/Senal'
import { textos, type Idioma, type Textos } from './textos'
import './landing.css'

const SECCIONES = ['senal', 'red', 'recompensa', 'privacidad', 'equipo'] as const

function idiomaInicial(): Idioma {
  const url = new URLSearchParams(location.search).get('lang')
  if (url === 'es' || url === 'en') return url
  const guardado = localStorage.getItem('dendra.idioma')
  if (guardado === 'es' || guardado === 'en') return guardado
  return navigator.language.toLowerCase().startsWith('es') ? 'es' : 'en'
}

export default function Landing() {
  const [idioma, setIdioma] = useState<Idioma>(idiomaInicial)
  const t = textos[idioma]

  useEffect(() => {
    document.documentElement.lang = idioma
    document.title = t.meta.titulo
    document.querySelector('meta[name="description"]')?.setAttribute('content', t.meta.descripcion)
    localStorage.setItem('dendra.idioma', idioma)
  }, [idioma, t])

  useEffect(() => {
    document.body.classList.add('cuerpo-landing')
    return () => document.body.classList.remove('cuerpo-landing')
  }, [])

  return (
    <div className="landing">
      <a className="saltar" href="#contenido">
        {idioma === 'es' ? 'Saltar al contenido' : 'Skip to content'}
      </a>
      <Navbar t={t} cambiarIdioma={() => setIdioma(idioma === 'es' ? 'en' : 'es')} />
      <main id="contenido">
        <Hero t={t} />
        <Senal t={t} />
        <Red t={t} />
        <Recompensa t={t} />
        <Privacidad t={t} />
        <Equipo t={t} idioma={idioma} />
        <Unete t={t} />
      </main>
      <Pie t={t} />
    </div>
  )
}

// --- Navbar ------------------------------------------------------------------------

function Navbar({ t, cambiarIdioma }: { t: Textos; cambiarIdioma: () => void }) {
  const [solida, setSolida] = useState(false)
  const [abierto, setAbierto] = useState(false)
  const [actual, setActual] = useState<string | null>(null)

  useEffect(() => {
    const alMover = () => setSolida(scrollY > 40)
    alMover()
    addEventListener('scroll', alMover, { passive: true })
    // La sección activa es la que cruza la franja central de la pantalla.
    const io = new IntersectionObserver(
      (entradas) => {
        for (const e of entradas) if (e.isIntersecting) setActual(e.target.id)
      },
      { rootMargin: '-45% 0px -50% 0px' },
    )
    for (const id of [...SECCIONES, 'unete']) {
      const el = document.getElementById(id)
      if (el) io.observe(el)
    }
    const arriba = document.getElementById('arriba')
    const ioArriba = new IntersectionObserver(([e]) => e.isIntersecting && setActual(null), { rootMargin: '-45% 0px -50% 0px' })
    if (arriba) ioArriba.observe(arriba)
    return () => {
      removeEventListener('scroll', alMover)
      io.disconnect()
      ioArriba.disconnect()
    }
  }, [])

  useEffect(() => {
    if (!abierto) return
    const cerrar = (e: KeyboardEvent) => e.key === 'Escape' && setAbierto(false)
    addEventListener('keydown', cerrar)
    return () => removeEventListener('keydown', cerrar)
  }, [abierto])

  const nombres: Record<(typeof SECCIONES)[number], string> = {
    senal: t.nav.senal,
    red: t.nav.red,
    recompensa: t.nav.recompensa,
    privacidad: t.nav.privacidad,
    equipo: t.nav.equipo,
  }

  return (
    <header className={`nav${solida || abierto ? ' solida' : ''}${abierto ? ' abierta' : ''}`}>
      <nav className="nav-barra" aria-label={t.nav.principal}>
        <a className="nav-marca" href="#arriba" onClick={() => setAbierto(false)}>
          <Logo /> Dendra
        </a>
        <button
          className="nav-menu"
          aria-expanded={abierto}
          aria-controls="nav-enlaces"
          aria-label={abierto ? t.nav.cerrar : t.nav.menu}
          onClick={() => setAbierto(!abierto)}
        >
          <span />
          <span />
        </button>
        <div className="nav-enlaces" id="nav-enlaces">
          <ul>
            {SECCIONES.map((id) => (
              <li key={id}>
                <a href={`#${id}`} aria-current={actual === id ? 'location' : undefined} onClick={() => setAbierto(false)}>
                  {nombres[id]}
                </a>
              </li>
            ))}
          </ul>
          <div className="nav-acciones">
            <button className="nav-idioma" onClick={cambiarIdioma} aria-label={t.nav.idiomaEtiqueta} title={t.nav.idioma}>
              {t.nav.idiomaCorto}
            </button>
            <a className="btn btn-luz" href="#unete" onClick={() => setAbierto(false)}>
              {t.nav.unete}
            </a>
          </div>
        </div>
      </nav>
    </header>
  )
}

// --- Hero --------------------------------------------------------------------------

function Hero({ t }: { t: Textos }) {
  const contenedor = useRef<HTMLDivElement>(null)
  const fondo = useRef<Fondo | null>(null)
  const [pausado, setPausado] = useState(false)
  const pausadoRef = useRef(false)

  useEffect(() => {
    let vivo = true
    let visible = true
    const el = contenedor.current!
    fondos[fondoElegido()]().then(({ montar }) => {
      if (!vivo) return
      fondo.current = montar(el)
      if (pausadoRef.current || !visible) fondo.current.pausar()
    })
    // Fuera de pantalla, el fondo se detiene: no gasta batería mientras se lee el resto.
    const io = new IntersectionObserver(([e]) => {
      visible = e.isIntersecting
      if (!fondo.current) return
      if (visible && !pausadoRef.current) fondo.current.seguir()
      else fondo.current.pausar()
    })
    io.observe(el)
    return () => {
      vivo = false
      io.disconnect()
      fondo.current?.destruir()
      fondo.current = null
    }
  }, [])

  const alternar = () => {
    const nuevo = !pausado
    pausadoRef.current = nuevo
    setPausado(nuevo)
    if (nuevo) fondo.current?.pausar()
    else fondo.current?.seguir()
  }

  return (
    <section className="hero" id="arriba">
      <div className="hero-fondo" ref={contenedor} aria-hidden="true" />
      <div className="hero-texto">
        <div className="hero-principal">
          <h1>{t.hero.titulo}</h1>
          <p className="hero-frase">{t.hero.frase}</p>
          <p className="hero-bajada">{t.hero.bajada}</p>
          <div className="acciones">
            <a className="btn btn-luz" href="#senal">
              {t.hero.principal}
            </a>
            <a className="btn btn-linea" href="#unete">
              {t.hero.secundaria}
            </a>
          </div>
        </div>
        <div className="hero-usos">
          <p>{t.hero.usosTitulo}</p>
          <ul>
            {t.hero.usos.map((u) => (
              <li key={u}>{u}</li>
            ))}
          </ul>
        </div>
      </div>
      <button className="hero-pausa" onClick={alternar} aria-pressed={pausado}>
        {pausado ? t.hero.seguir : t.hero.pausar}
      </button>
    </section>
  )
}

// --- Secciones ---------------------------------------------------------------------

function Cabeza({ eyebrow, titulo, bajada, id }: { eyebrow: string; titulo: string; bajada?: string; id: string }) {
  return (
    <div className="cabeza">
      <p className="eyebrow">{eyebrow}</p>
      <h2 id={id}>{titulo}</h2>
      {bajada && <p className="bajada">{bajada}</p>}
    </div>
  )
}

function Red({ t }: { t: Textos }) {
  return (
    <section className="seccion red" id="red" aria-labelledby="red-titulo">
      <div className="wrap">
        <Cabeza id="red-titulo" eyebrow={t.red.eyebrow} titulo={t.red.titulo} bajada={t.red.bajada} />
        <ol className="trayecto">
          {t.red.puntos.map((p, i) => (
            <li key={i}>
              <span className="nodo" aria-hidden="true" />
              <h3>{p.titulo}</h3>
              <p>{p.texto}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}

function Recompensa({ t }: { t: Textos }) {
  return (
    <section className="seccion recompensa" id="recompensa" aria-labelledby="recompensa-titulo">
      <div className="wrap recompensa-grid">
        <div>
          <Cabeza id="recompensa-titulo" eyebrow={t.recompensa.eyebrow} titulo={t.recompensa.titulo} bajada={t.recompensa.bajada} />
          <p className="recompensa-nota">{t.recompensa.nota}</p>
          <p className="chip-prueba">{t.recompensa.prueba}</p>
        </div>
        <ol className="estados">
          {t.recompensa.pasos.map((p, i) => (
            <li key={i}>
              <span className="estado-num">{String(i + 1).padStart(2, '0')}</span>
              <div>
                <h3>{p.titulo}</h3>
                <p>{p.texto}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}

function Privacidad({ t }: { t: Textos }) {
  return (
    <section className="seccion privacidad" id="privacidad" aria-labelledby="privacidad-titulo">
      <div className="wrap">
        <Cabeza id="privacidad-titulo" eyebrow={t.privacidad.eyebrow} titulo={t.privacidad.titulo} />
        <div className="principios">
          {t.privacidad.puntos.map((p, i) => (
            <article key={i}>
              <h3>{p.titulo}</h3>
              <p>{p.texto}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}

const tecnologia = [
  { nombre: 'Stellar', url: 'https://stellar.org', es: 'Red de pagos donde vive el dinero', en: 'The payment network that holds the money' },
  { nombre: 'Trustless Work', url: 'https://www.trustlesswork.com', es: 'Escrow que bloquea la recompensa', en: 'Escrow that locks the reward' },
  { nombre: 'Pollar', url: 'https://pollar.xyz', es: 'Billetera con tu cuenta de Google', en: 'A wallet with your Google account' },
]

function Equipo({ t, idioma }: { t: Textos; idioma: Idioma }) {
  return (
    <section className="seccion equipo" id="equipo" aria-labelledby="equipo-titulo">
      <div className="wrap equipo-grid">
        <div>
          <Cabeza id="equipo-titulo" eyebrow={t.equipo.eyebrow} titulo={t.equipo.titulo} bajada={t.equipo.bajada} />
          <ul className="personas">
            {sitio.equipo.map((p) => (
              <li key={p.nombre}>
                <span className="inicial" aria-hidden="true">
                  {p.nombre[0]}
                </span>
                {p.nombre}
                {p.rol && <small>{p.rol}</small>}
              </li>
            ))}
          </ul>
        </div>
        <div>
          <p className="eyebrow">{t.equipo.tecnologia}</p>
          <ul className="tecnologia">
            {tecnologia.map((x) => (
              <li key={x.nombre}>
                <a href={x.url} target="_blank" rel="noreferrer">
                  <strong>{x.nombre}</strong>
                  <span>{x[idioma]}</span>
                </a>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  )
}

function Unete({ t }: { t: Textos }) {
  return (
    <section className="seccion unete" id="unete" aria-labelledby="unete-titulo">
      <div className="wrap unete-caja">
        <Cabeza id="unete-titulo" eyebrow={t.unete.eyebrow} titulo={t.unete.titulo} bajada={t.unete.bajada} />
        {sitio.contacto && (
          <a className="btn btn-luz" href={`mailto:${sitio.contacto}`}>
            {t.unete.accion}
          </a>
        )}
        {sitio.contacto && <p className="unete-correo">{sitio.contacto}</p>}
      </div>
    </section>
  )
}

function Pie({ t }: { t: Textos }) {
  return (
    <footer className="pie-landing">
      <div className="wrap pie-fila">
        <span className="nav-marca">
          <Logo /> {t.pie.derechos}
        </span>
        <span>{sitio.hackathon}</span>
        <span className="chip-prueba">{t.pie.prueba}</span>
      </div>
    </footer>
  )
}
