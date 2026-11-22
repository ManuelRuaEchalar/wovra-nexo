/**
 * Piezas comunes de los fondos animados del hero.
 *
 * Cada fondo es un módulo con la misma interfaz: `montar(contenedor)` dibuja la
 * escena dentro del contenedor y devuelve cómo pausarla, seguirla y apagarla.
 * Así el hero puede usar cualquiera de los dos cambiando un solo nombre
 * (ver `../config.ts`).
 */

export interface Fondo {
  pausar(): void
  seguir(): void
  destruir(): void
}

export type MontarFondo = (contenedor: HTMLElement) => Fondo

/** Punto de una rama: posición y grosor. */
export interface Punto {
  x: number
  y: number
  w: number
}

/** Generador pseudoaleatorio con semilla (mulberry32): la escena sale igual cada vez. */
export function semilla(a: number) {
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}
export type Azar = ReturnType<typeof semilla>

export const lerp = (a: number, b: number, t: number) => a + (b - a) * t
/** Normal aproximada (suma de uniformes): los granos se agrupan cerca del eje de la rama. */
export const gauss = (r: Azar) => (r() + r() + r() + r() - 2) / 1.15

export const movimientoReducido = () => matchMedia('(prefers-reduced-motion: reduce)').matches
export const dpr = () => Math.min(devicePixelRatio || 1, 1.5)

/** Escena virtual de 1600 × 900 que se escala para cubrir el contenedor. */
export const VW = 1600
export const VH = 900

// Colores de la marca (Halo + Nocturno), como "r,g,b" para armar rgba().
export const AURORA = '43,208,126'
export const DESTELLO = '159,240,196'
export const BLANCO = '232,255,243'

export function luz(ctx: CanvasRenderingContext2D, x: number, y: number, rad: number, color: string, alfa: number) {
  const g = ctx.createRadialGradient(x, y, 0, x, y, rad)
  g.addColorStop(0, `rgba(${color},${alfa})`)
  g.addColorStop(1, `rgba(${color},0)`)
  ctx.fillStyle = g
  ctx.beginPath()
  ctx.arc(x, y, rad, 0, Math.PI * 2)
  ctx.fill()
}

/** Chispeo de granos alrededor de un punto: la señal también es luz granulada. */
export function chispas(ctx: CanvasRenderingContext2D, x: number, y: number, disp: number, n: number, alfa: number, tam = 0.9) {
  for (let i = 0; i < n; i++) {
    const a = Math.random() * Math.PI * 2
    const d = Math.abs(Math.random() + Math.random() - 1) * disp
    ctx.fillStyle = `rgba(${Math.random() < 0.5 ? BLANCO : DESTELLO},${alfa * Math.random()})`
    ctx.beginPath()
    ctx.arc(x + Math.cos(a) * d, y + Math.sin(a) * d, 0.4 + Math.random() * tam, 0, Math.PI * 2)
    ctx.fill()
  }
}

/** Crea una capa (div con un canvas) dentro de la escena. */
export function capa(escena: HTMLElement, clase: string) {
  const div = document.createElement('div')
  div.className = clase
  const canvas = document.createElement('canvas')
  div.appendChild(canvas)
  escena.appendChild(div)
  return { div, canvas }
}

/** Viñeta y grano de película encima de todo. Devuelve el elemento del grano para moverlo cada cuadro. */
export function vinetaYGrano(escena: HTMLElement, vineta: string, opacidadGrano: number) {
  const v = document.createElement('div')
  v.className = 'fondo-vineta'
  v.style.background = vineta
  escena.appendChild(v)

  const grano = document.createElement('div')
  grano.className = 'fondo-grano'
  grano.style.opacity = String(opacidadGrano)
  const c = document.createElement('canvas')
  c.width = c.height = 220
  const x = c.getContext('2d')!
  const img = x.createImageData(220, 220)
  for (let i = 0; i < img.data.length; i += 4) {
    const val = Math.random() * 255
    img.data[i] = img.data[i + 1] = img.data[i + 2] = val
    img.data[i + 3] = 255
  }
  x.putImageData(img, 0, 0)
  grano.style.backgroundImage = `url(${c.toDataURL()})`
  escena.appendChild(grano)
  return grano
}

export const moverGrano = (grano: HTMLElement) => {
  grano.style.transform = `translate(${(Math.random() * 100 - 50) | 0}px,${(Math.random() * 100 - 50) | 0}px)`
}

/** Ajusta el tamaño de los canvas a su caja y calcula cómo cubrir la escena virtual. */
export function medir(referencia: HTMLElement, canvases: HTMLCanvasElement[]) {
  const D = dpr()
  const box = referencia.getBoundingClientRect()
  for (const c of canvases) {
    const b = c.getBoundingClientRect()
    c.width = Math.max(1, Math.round(b.width * D))
    c.height = Math.max(1, Math.round(b.height * D))
  }
  const escala = Math.max(box.width / VW, box.height / VH)
  return { D, escala, ox: (box.width - VW * escala) / 2, oy: (box.height - VH * escala) / 2 }
}

/** Repinta la escena cuando cambia el tamaño del contenedor (con espera, para no repintar en cada píxel). */
export function alRedimensionar(el: HTMLElement, fn: () => void) {
  let espera = 0
  let ancho = el.clientWidth
  let alto = el.clientHeight
  const obs = new ResizeObserver(() => {
    if (el.clientWidth === ancho && Math.abs(el.clientHeight - alto) < 120) return // la barra del navegador móvil no repinta
    ancho = el.clientWidth
    alto = el.clientHeight
    clearTimeout(espera)
    espera = window.setTimeout(fn, 200)
  })
  obs.observe(el)
  return () => {
    clearTimeout(espera)
    obs.disconnect()
  }
}
