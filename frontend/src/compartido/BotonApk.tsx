import { useEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import type { TextosSitio } from './textos'

const APK = '/dendra.apk'
/** Lo que el botón muestra "Descargando…" tras el clic. */
const BOTON_MS = 4000
/** Lo que queda el aviso en pantalla. */
const AVISO_MS = 14000

/**
 * Enlace que descarga el APK y deja una señal inmediata de que se pulsó: el botón
 * cambia a "Descargando…" con un indicador y aparece un aviso abajo con el nombre,
 * el tamaño y qué hacer al terminar (el navegador no informa del progreso).
 */
export function BotonApk({
  className,
  t,
  onClick,
  children,
}: {
  className: string
  t: TextosSitio['nav']['descarga']
  onClick?: () => void
  children: ReactNode
}) {
  const [descargando, setDescargando] = useState(false)
  const [aviso, setAviso] = useState(false)
  const relojes = useRef<number[]>([])

  useEffect(() => () => relojes.current.forEach(clearTimeout), [])

  const pulsar = () => {
    onClick?.()
    relojes.current.forEach(clearTimeout)
    setDescargando(true)
    setAviso(true)
    relojes.current = [
      window.setTimeout(() => setDescargando(false), BOTON_MS),
      window.setTimeout(() => setAviso(false), AVISO_MS),
    ]
  }

  return (
    <>
      <a
        className={`${className} boton-apk${descargando ? ' descargando' : ''}`}
        href={APK}
        download
        onClick={pulsar}
        aria-busy={descargando}
      >
        {descargando && <span className="boton-apk-giro" aria-hidden="true" />}
        {descargando ? t.descargando : children}
      </a>
      {aviso &&
        createPortal(
          <div className="aviso-apk" role="status">
            <span className="aviso-apk-icono" aria-hidden="true">
              ↓
            </span>
            <div>
              <strong>{t.titulo}</strong>
              <p>{t.detalle}</p>
              <a className="aviso-apk-ayuda" href="/#instalar" onClick={() => setAviso(false)}>
                {t.ayuda}
              </a>
            </div>
            <button type="button" onClick={() => setAviso(false)} aria-label={t.cerrar}>
              ×
            </button>
            <span className="aviso-apk-barra" aria-hidden="true" />
          </div>,
          document.body,
        )}
    </>
  )
}
