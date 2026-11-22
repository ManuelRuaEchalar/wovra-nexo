import { useEffect, useRef, useState } from 'react'
import type { Textos } from '../textos'
import { Reloj } from '../reloj'
import { CANTIDAD_APPS, SALIDA_MS, crearCiudad, type EscenaCiudad } from './escena'

/** Cuánto dura la introducción (la red transmitiendo) y cada aplicación. */
const INTRO_MS = 3000
const APP_MS = 6000
/** Desde la introducción, la lista entra a mitad del descenso. */
const LISTA_MS = 1500

/**
 * La ciudad del hero. Mientras `activa` es false queda en reposo (la ciudad entera,
 * sin la red densa), que es lo que se ve durante el fundido. Al activarse recorre:
 * introducción → las siete aplicaciones → `alTerminar`.
 */
export default function Ciudad({
  t,
  activa,
  pausada,
  alTerminar,
}: {
  t: Textos
  activa: boolean
  pausada: boolean
  alTerminar: () => void
}) {
  const lienzo = useRef<HTMLDivElement>(null)
  const escena = useRef<EscenaCiudad | null>(null)
  const reloj = useRef(new Reloj())
  const terminar = useRef(alTerminar)
  const [paso, setPaso] = useState<number>(-1) // -1: introducción
  const [lista, setLista] = useState(false)
  const c = t.ciudad

  useEffect(() => {
    terminar.current = alTerminar
  }, [alTerminar])

  // La escena se arma una vez por idioma (los rótulos del mapa van dibujados).
  useEffect(() => {
    const e = crearCiudad(lienzo.current!, c.mapa)
    escena.current = e
    e.reposo()
    return () => {
      e.destruir()
      escena.current = null
    }
  }, [c.mapa])

  // Recorrido: introducción, luego cada aplicación; al final avisa al hero.
  useEffect(() => {
    const r = reloj.current
    const e = escena.current
    if (!activa || !e) return
    const app = (i: number, desdeIntro: boolean) => {
      if (i >= CANTIDAD_APPS) {
        terminar.current()
        return
      }
      setPaso(i)
      e.app(i, desdeIntro)
      if (desdeIntro) r.programar(() => setLista(true), LISTA_MS)
      r.programar(() => app(i + 1, false), APP_MS + (desdeIntro ? SALIDA_MS - 1000 : 0))
    }
    setPaso(-1)
    setLista(false)
    e.intro()
    r.programar(() => app(0, true), INTRO_MS)
    return () => {
      r.limpiar()
      e.reposo()
      setPaso(-1)
      setLista(false)
    }
  }, [activa, c.mapa])

  useEffect(() => {
    if (pausada) {
      reloj.current.pausar()
      escena.current?.pausar()
    } else {
      reloj.current.seguir()
      escena.current?.seguir()
    }
  }, [pausada])

  const enIntro = paso < 0 || !lista
  return (
    <div className={`cdd${activa ? ' activa' : ''}`}>
      <div className="cdd-lienzo" ref={lienzo} aria-hidden="true" />
      <div className="cdd-velo" />
      <div className={`cdd-texto${enIntro ? ' en-intro' : ''}`}>
        <p className="cdd-frase">{c.frase}</p>
        <p className={`cdd-intro${paso >= 0 ? ' saliendo' : ''}`}>{c.intro}</p>
        <ul className="cdd-apps">
          {c.apps.map((a, i) => (
            <li key={a.nombre} className={i === paso ? 'activa' : undefined}>
              <span className="nombre">{a.nombre}</span>
              <span className={`estado${i === 0 ? ' disponible' : ''}`}>{i === 0 ? c.disponible : c.pronto}</span>
              <span className="que">{a.que}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
