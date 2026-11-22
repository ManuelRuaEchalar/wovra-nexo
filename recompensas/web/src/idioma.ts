import { useEffect, useState } from 'react'

/**
 * Idioma del sitio, compartido por la landing, el panel y la página de reclamo:
 * se elige una vez y se recuerda al pasar de una página a otra.
 */
export type Idioma = 'es' | 'en'

const CLAVE = 'dendra.idioma'

export function idiomaInicial(): Idioma {
  const url = new URLSearchParams(location.search).get('lang')
  if (url === 'es' || url === 'en') return url
  const guardado = localStorage.getItem(CLAVE)
  if (guardado === 'es' || guardado === 'en') return guardado
  return navigator.language.toLowerCase().startsWith('es') ? 'es' : 'en'
}

/** Idioma actual y una función para alternarlo; ajusta `<html lang>` y lo recuerda. */
export function useIdioma(): [Idioma, () => void] {
  const [idioma, setIdioma] = useState<Idioma>(idiomaInicial)
  useEffect(() => {
    document.documentElement.lang = idioma
    localStorage.setItem(CLAVE, idioma)
  }, [idioma])
  return [idioma, () => setIdioma((i) => (i === 'es' ? 'en' : 'es'))]
}

/** Formatos de números y fechas del idioma. */
export const regional = (idioma: Idioma) => (idioma === 'es' ? 'es-BO' : 'en-US')
