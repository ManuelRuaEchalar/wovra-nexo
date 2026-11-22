import { lazy, Suspense, useCallback, useEffect, useRef, useState } from 'react'
import { sitio } from '../../compartido/datosSitio'
import { fondoElegido, fondos } from './config'
import type { Fondo } from './fondos/comun'
import Senal from './senal/Senal'
import type { Idioma } from '../../compartido/idioma'
import { useIdioma } from '../../compartido/idioma'
import { BotonApk } from '../../compartido/BotonApk'
import { Navbar } from '../../compartido/Navbar'
import type { TextosSitio } from '../../compartido/textos'
import { Pie } from '../../compartido/Pie'
import { textosSitio } from '../../compartido/textos'
import { textos, type Textos } from './textos'
import { Reloj } from './reloj'
import './landing.css'

/** La ciudad (foto satelital y red) se carga aparte para no demorar la primera pintura. */
const Ciudad = lazy(() => import('./ciudad/Ciudad'))

export default function Landing() {
  const [idioma, cambiarIdioma] = useIdioma()
  const t = textos[idioma]
  const ts = textosSitio[idioma]

  useEffect(() => {
    document.title = t.meta.titulo
    document.querySelector('meta[name="description"]')?.setAttribute('content', t.meta.descripcion)
  }, [t])

  useEffect(() => {
    document.body.classList.add('cuerpo-landing')
    return () => document.body.classList.remove('cuerpo-landing')
  }, [])

  return (
    <div className="landing">
      <Navbar t={ts.nav} cambiarIdioma={cambiarIdioma} pagina="landing" />
      <main id="contenido">
        <Hero t={t} descarga={ts.nav.descarga} />
        <Senal t={t} />
        <Red t={t} />
        <Recompensa t={t} />
        <Privacidad t={t} />
        <Equipo t={t} idioma={idioma} />
        <Unete t={t} descarga={ts.nav.descarga} />
      </main>
      <Pie t={ts} enLanding />
    </div>
  )
}

// --- Hero --------------------------------------------------------------------------

/**
 * El hero se transforma solo: las dendritas con el titular, luego se funden con la
 * ciudad, que recorre la introducción y las aplicaciones, y vuelve a las dendritas.
 * La introducción de la ciudad empieza recién cuando termina el fundido.
 */
type Fase = 'dendritas' | 'a-ciudad' | 'ciudad' | 'a-dendritas'
const DENDRITAS_MS = 11000
const FUNDIDO_MS = 1600

function Hero({ t, descarga }: { t: Textos; descarga: TextosSitio['nav']['descarga'] }) {
  const contenedor = useRef<HTMLDivElement>(null)
  const seccion = useRef<HTMLElement>(null)
  const fondo = useRef<Fondo | null>(null)
  const reloj = useRef(new Reloj())
  const [fase, setFase] = useState<Fase>('dendritas')
  const [pausado, setPausado] = useState(false)
  const [visible, setVisible] = useState(true)
  const detenido = pausado || !visible
  const conDendritas = fase !== 'ciudad'

  useEffect(() => {
    let vivo = true
    fondos[fondoElegido()]().then(({ montar }) => {
      if (!vivo) return
      fondo.current = montar(contenedor.current!)
    })
    // Fuera de pantalla todo se detiene: no gasta batería mientras se lee el resto.
    const io = new IntersectionObserver(([e]) => setVisible(e.isIntersecting))
    io.observe(seccion.current!)
    return () => {
      vivo = false
      io.disconnect()
      fondo.current?.destruir()
      fondo.current = null
    }
  }, [])

  // Las dendritas solo se animan mientras se ven.
  useEffect(() => {
    const f = fondo.current
    if (!f) return
    if (conDendritas && !detenido) f.seguir()
    else f.pausar()
  })

  // Secuencia: dendritas → fundido → ciudad (avisa al terminar) → fundido → dendritas.
  useEffect(() => {
    const r = reloj.current
    if (fase === 'dendritas') r.programar(() => setFase('a-ciudad'), DENDRITAS_MS)
    if (fase === 'a-ciudad') r.programar(() => setFase('ciudad'), FUNDIDO_MS)
    if (fase === 'a-dendritas') r.programar(() => setFase('dendritas'), FUNDIDO_MS)
    return () => r.limpiar()
  }, [fase])

  useEffect(() => {
    if (detenido) reloj.current.pausar()
    else reloj.current.seguir()
  }, [detenido])

  const terminarCiudad = useCallback(() => setFase('a-dendritas'), [])
  const titularVisible = fase === 'dendritas' || fase === 'a-dendritas'

  return (
    <section className="hero" id="arriba" ref={seccion} data-fase={fase}>
      <div className="hero-fondo" ref={contenedor} aria-hidden="true" />
      <div className="hero-ciudad">
        <Suspense fallback={null}>
          <Ciudad t={t} activa={fase === 'ciudad'} pausada={detenido} alTerminar={terminarCiudad} />
        </Suspense>
      </div>
      <div className="hero-texto" inert={!titularVisible}>
        <div className="hero-principal">
          <h1>{t.hero.titulo}</h1>
          <p className="hero-frase">{t.hero.frase}</p>
          <p className="hero-bajada">
            {t.hero.bajada}{' '}
            <span className="hero-stellar">
              {t.hero.stellar[0]}
              <strong>{t.hero.stellar[1]}</strong>
              {t.hero.stellar[2]}
            </span>
          </p>
          <div className="acciones">
            <a className="btn btn-luz" href="#senal">
              {t.hero.principal}
            </a>
            <BotonApk className="btn btn-linea" t={descarga}>
              {t.hero.secundaria}
            </BotonApk>
          </div>
        </div>
      </div>
      <button className="hero-pausa" onClick={() => setPausado(!pausado)} aria-pressed={pausado}>
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

function Unete({ t, descarga }: { t: Textos; descarga: TextosSitio['nav']['descarga'] }) {
  return (
    <section className="seccion unete" id="unete" aria-labelledby="unete-titulo">
      <div className="wrap unete-caja">
        <Cabeza id="unete-titulo" eyebrow={t.unete.eyebrow} titulo={t.unete.titulo} bajada={t.unete.bajada} />
        <BotonApk className="btn btn-luz" t={descarga}>
          {t.unete.accion}
        </BotonApk>
        <div className="instalar" id="instalar">
          <h3>{t.unete.instalar.titulo}</h3>
          <p className="instalar-aviso">{t.unete.instalar.aviso}</p>
          <p className="instalar-pasos-titulo">{t.unete.instalar.pasosTitulo}</p>
          <ol>
            {t.unete.instalar.pasos.map((paso) => (
              <li key={paso}>{paso}</li>
            ))}
          </ol>
        </div>
        {sitio.contacto && (
          <p className="unete-correo">
            {t.unete.correo} <a href={`mailto:${sitio.contacto}`}>{sitio.contacto}</a>
          </p>
        )}
      </div>
    </section>
  )
}

