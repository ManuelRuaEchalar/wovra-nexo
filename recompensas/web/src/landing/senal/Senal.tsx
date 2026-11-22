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
  tuyo: ['inicio', 'guardian', 'perdido', null, null, 'devuelto'],
  nodo: ['nodo', null, null, null, null, null],
  circulo: ['reportar', null, null, null, 'seguimiento', 'recompensa'],
}
const pantallas = Object.fromEntries(
  Object.entries(pantallasDeseadas).map(([rol, lista]) => [rol, lista.map((n, i) => (i > 0 && n && !capturas[n] ? null : n))]),
) as Pantallas

const nombres: Record<string, string> = {
  inicio: 'Inicio',
  guardian: 'Guardián',
  perdido: 'Modo perdido',
  nodo: 'La red',
  reportar: 'Reportar',
  seguimiento: 'Seguimiento',
  recompensa: 'Recompensa',
  devuelto: 'Recompensa',
}

/** Cuánto se queda la escena en cada paso antes de pasar al siguiente. */
const DURACION_MS = 10000

export default function Senal({ t }: { t: Textos }) {
  const seccion = useRef<HTMLElement>(null)
  const lienzo = useRef<HTMLCanvasElement>(null)
  const escena = useRef<Escena | null>(null)
  const [paso, setPaso] = useState(0)
  const [visible, setVisible] = useState(false)
  const [reinicio, setReinicio] = useState(false)
  const [sin3d, setSin3d] = useState(false)
  const pendiente = t.senal.pendiente
  const total = t.senal.pasos.length

  useEffect(() => {
    try {
      escena.current = crearEscena(lienzo.current!, { capturas, pantallas, pendiente, nombres })
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
      <div className="wrap senal-texto">
        <p className="eyebrow">{t.senal.eyebrow}</p>
        <h2 id="senal-titulo">{t.senal.titulo}</h2>
        {sin3d && <p className="nota">{t.senal.sin3d}</p>}
        <ol className="senal-pasos">
          {t.senal.pasos.map((p, i) => (
            <li key={i} className={i === paso ? 'activo' : undefined} aria-current={i === paso ? 'step' : undefined}>
              <h3>{p.titulo}</h3>
              <p>{p.texto}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}
