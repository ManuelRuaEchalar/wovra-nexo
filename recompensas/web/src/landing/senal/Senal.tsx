import { useEffect, useRef, useState } from 'react'
import type { Textos } from '../textos'
import { crearEscena, type Escena, type Pantallas } from './escena3d'

/**
 * Capturas reales de la app, en `./capturas/<nombre>.png`. Las que todavía no
 * existen se muestran como pantalla provisional con su nombre.
 */
const capturas = Object.fromEntries(
  Object.entries(import.meta.glob('./capturas/*.{png,jpg,webp}', { eager: true, query: '?url', import: 'default' })).map(
    ([ruta, url]) => [ruta.replace(/^.*\/|\.\w+$/g, ''), url as string],
  ),
)

/** Qué captura muestra cada celular en cada uno de los seis pasos (null: sigue la anterior). */
const pantallas: Pantallas = {
  tuyo: ['inicio', 'guardian', 'perdido', null, null, null],
  nodo: ['nodo', null, null, null, null, null],
  circulo: ['reportar', null, null, null, 'seguimiento', 'recompensa'],
}

const nombres: Record<string, string> = {
  inicio: 'Inicio',
  guardian: 'Guardián',
  perdido: 'Modo perdido',
  nodo: 'La red',
  reportar: 'Reportar',
  seguimiento: 'Seguimiento',
  recompensa: 'Recompensa',
}

/** Progreso continuo (0 … pasos − 1) según qué paso está más cerca del centro de la pantalla. */
function progresoDe(pasos: HTMLElement[]) {
  const medio = innerHeight / 2
  const centros = pasos.map((p) => {
    const r = p.getBoundingClientRect()
    return r.top + r.height / 2
  })
  if (medio <= centros[0]) return 0
  for (let i = 0; i < centros.length - 1; i++)
    if (medio <= centros[i + 1]) return i + (medio - centros[i]) / (centros[i + 1] - centros[i])
  return centros.length - 1
}

export default function Senal({ t }: { t: Textos }) {
  const escenario = useRef<HTMLDivElement>(null)
  const lienzo = useRef<HTMLCanvasElement>(null)
  const [activo, setActivo] = useState(0)
  const [sin3d, setSin3d] = useState(false)
  const pendiente = t.senal.pendiente

  useEffect(() => {
    const cont = escenario.current!
    const pasos = Array.from(cont.querySelectorAll<HTMLElement>('.senal-paso'))
    let escena: Escena | null = null
    try {
      escena = crearEscena(lienzo.current!, { capturas, pantallas, pendiente, nombres })
    } catch {
      setSin3d(true)
    }

    const alMover = () => {
      const p = progresoDe(pasos)
      escena?.fijarProgreso(p)
      setActivo(Math.round(p))
    }
    const io = new IntersectionObserver(([e]) => escena?.activar(e.isIntersecting), { rootMargin: '200px 0px' })
    io.observe(cont)
    addEventListener('scroll', alMover, { passive: true })
    addEventListener('resize', alMover)
    alMover()
    return () => {
      io.disconnect()
      removeEventListener('scroll', alMover)
      removeEventListener('resize', alMover)
      escena?.destruir()
    }
  }, [pendiente])

  return (
    <section className="senal" id="senal" aria-labelledby="senal-titulo">
      <div className="wrap senal-cabeza">
        <p className="eyebrow">{t.senal.eyebrow}</p>
        <h2 id="senal-titulo">{t.senal.titulo}</h2>
        <p className="bajada">{t.senal.bajada}</p>
      </div>
      <div className="senal-escenario" ref={escenario}>
        <div className="senal-fondo" aria-hidden="true">
          <div className="senal-lienzo">
            <canvas ref={lienzo} />
            <div className="senal-grano" />
          </div>
        </div>
        {sin3d && <p className="wrap nota senal-sin3d">{t.senal.sin3d}</p>}
        <ol className="senal-pasos wrap">
          {t.senal.pasos.map((p, i) => (
            <li key={i} className={`senal-paso${i === activo ? ' activo' : ''}`}>
              <div className="senal-tarjeta">
                <span className="senal-num">
                  {String(i + 1).padStart(2, '0')}
                  <span> / {String(t.senal.pasos.length).padStart(2, '0')}</span>
                </span>
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
