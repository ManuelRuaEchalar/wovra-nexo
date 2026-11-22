import { useEffect, useRef, useState } from 'react'
import type { Textos } from '../textos'
import { crearEscena, type Escena, type Pantallas } from './escena3d'

/**
 * Capturas reales de la app, en `./capturas/<nombre>.webp`. Las que todavía no
 * existen se muestran como pantalla provisional con su nombre.
 */
const capturas = Object.fromEntries(
  Object.entries(import.meta.glob('./capturas/*.{png,jpg,webp}', { eager: true, query: '?url', import: 'default' })).map(
    ([ruta, url]) => [ruta.replace(/^.*\/|\.\w+$/g, ''), url as string],
  ),
)

/**
 * Qué captura muestra cada celular en cada uno de los seis pasos (null: sigue la anterior).
 * Una captura que falta a mitad del recorrido no se muestra: el celular sigue con la anterior.
 */
const pantallasDeseadas: Pantallas = {
  //     0 apertura  1 prepáralo  2 reportar   3 recompensa  4 mapa          5 brújula   6 sonar      7 Stellar   8 pagado
  tuyo: ['inicio', 'armar', 'inicio', 'perdido', null, null, 'sonando', 'perdido', 'devuelto'],
  nodo: ['brujula', null, null, null, null, null, null, 'reclamo', 'recompensa'],
  circulo: ['seguimiento', null, 'reportar', null, 'seguimiento', 'brujula', null, null, null],
}
const pantallas = Object.fromEntries(
  Object.entries(pantallasDeseadas).map(([rol, lista]) => [rol, lista.map((n, i) => (i > 0 && n && !capturas[n] ? null : n))]),
) as Pantallas

const nombres: Record<string, string> = {
  inicio: 'Inicio',
  armar: 'Tu círculo',
  reportar: 'Reportar',
  perdido: 'Modo perdido',
  seguimiento: 'Seguimiento',
  brujula: 'Buscar de cerca',
  sonando: 'Hacer sonar',
  reclamo: 'Reclamo',
  recompensa: 'Recompensa',
  devuelto: 'Recompensa',
}

/** `?escena=N` empieza el recorrido en esa escena (para revisarlas una por una). */
function escenaInicial(total: number) {
  const n = Number(new URLSearchParams(location.search).get('escena'))
  return Number.isInteger(n) && n > 0 && n < total ? n : 0
}

/** Cuánto se queda la escena en cada paso antes de pasar al siguiente. */
const DURACION_MS = 10000

export default function Senal({ t }: { t: Textos }) {
  const seccion = useRef<HTMLElement>(null)
  const lienzo = useRef<HTMLCanvasElement>(null)
  const escena = useRef<Escena | null>(null)
  const [paso, setPaso] = useState(() => escenaInicial(t.senal.pasos.length))
  const [visible, setVisible] = useState(false)
  const [reinicio, setReinicio] = useState(false)
  const [sin3d, setSin3d] = useState(false)
  const pendiente = t.senal.pendiente
  const total = t.senal.pasos.length

  useEffect(() => {
    try {
      escena.current = crearEscena(lienzo.current!, { capturas, pantallas, pendiente, nombres })
      escena.current.saltar(escenaInicial(t.senal.pasos.length))
    } catch {
      queueMicrotask(() => setSin3d(true))
    }
    const io = new IntersectionObserver(
      ([e]) => {
        escena.current?.activar(e.isIntersecting)
        setVisible(e.intersectionRatio > 0.35)
      },
      { threshold: [0, 0.35, 0.6] },
    )
    io.observe(seccion.current!)
    return () => {
      io.disconnect()
      escena.current?.destruir()
      escena.current = null
    }
  }, [pendiente])

  // Avance solo: cada paso dura DURACION_MS; del último vuelve al primero con un fundido.
  useEffect(() => {
    if (!visible) return
    let fundido = 0
    const id = setTimeout(() => {
      if (paso < total - 1) {
        setPaso(paso + 1)
        return
      }
      setReinicio(true)
      fundido = window.setTimeout(() => {
        escena.current?.saltar(0)
        setPaso(0)
        setReinicio(false)
      }, 700)
    }, DURACION_MS)
    return () => {
      clearTimeout(id)
      clearTimeout(fundido)
    }
  }, [paso, visible, total])

  useEffect(() => {
    escena.current?.fijarProgreso(paso)
  }, [paso])

  // Flechas: escena anterior o siguiente. Al dar la vuelta (de la última a la primera
  // o al revés) salta con un fundido en vez de recorrer todas las escenas.
  const ir = (destino: number) => {
    const n = (destino + total) % total
    if (Math.abs(n - paso) <= 1) {
      setPaso(n)
      return
    }
    setReinicio(true)
    setTimeout(() => {
      escena.current?.saltar(n)
      setPaso(n)
      setReinicio(false)
    }, 500)
  }

  return (
    <section
      ref={seccion}
      className={`senal${reinicio ? ' reinicio' : ''}`}
      id="senal"
      aria-labelledby="senal-titulo"
    >
      <div className="senal-lienzo" aria-hidden="true">
        <canvas ref={lienzo} />
      </div>
      <button className="senal-flecha anterior" onClick={() => ir(paso - 1)} aria-label={t.senal.anterior}>
        <svg viewBox="0 0 64 12" aria-hidden="true"><path d="M63 6H1m0 0l5-5M1 6l5 5" /></svg>
      </button>
      <button className="senal-flecha siguiente" onClick={() => ir(paso + 1)} aria-label={t.senal.siguiente}>
        <svg viewBox="0 0 64 12" aria-hidden="true"><path d="M1 6h62m0 0l-5-5m5 5l-5 5" /></svg>
      </button>
      <div className="senal-texto">
        <p className="eyebrow">{t.senal.eyebrow}</p>
        <h2 id="senal-titulo">{t.senal.titulo}</h2>
        {sin3d && <p className="nota">{t.senal.sin3d}</p>}
        <ol className="senal-pasos">
          {t.senal.pasos.map((p, i) => (
            <li
              key={i}
              className={`${i === paso ? 'activo' : ''}${p.texto ? '' : ' apertura'}`}
              aria-current={i === paso ? 'step' : undefined}
            >
              <h3>{p.titulo}</h3>
              {p.texto && <p>{p.texto}</p>}
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}
