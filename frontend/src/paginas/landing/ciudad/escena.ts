/**
 * La ciudad del hero: vista satelital del centro de Sucre con su color real y un
 * granulado suave; encima, la red de celulares y una superposición por aplicación.
 *
 * Las calles se detectaron en la imagen: dos familias de rectas (−50,5° y 41,25°)
 * a ~133 px. Todo se ubica con índices de esa grilla, así cae sobre calles y
 * manzanas reales.
 *
 * El módulo solo dibuja y mueve la cámara; los tiempos los lleva `Ciudad.tsx`.
 */
import imagen from './centro-de-sucre.webp'

type Punto = [number, number]

/** Textos que van dibujados sobre el mapa. */
export interface TextosMapa {
  tu: string
  celular: [Nota, Nota]
  combustible: Nota
  micro: Nota
  apagon: Nota
  casa: Nota
  mascota: Nota
}
interface Nota {
  cifra: string
  unidad?: string
  rotulo: string
}

export interface EscenaCiudad {
  /** Ciudad entera, sin la red densa ni aplicaciones (mientras se funde). */
  reposo(): void
  /** Ciudad entera con la red transmitiendo. */
  intro(): void
  /** Viaja a la aplicación i. `desdeIntro`: descenso lento con la superposición al llegar. */
  app(i: number, desdeIntro: boolean): void
  pausar(): void
  seguir(): void
  destruir(): void
}

export const CANTIDAD_APPS = 7
/** Duración del descenso desde la introducción hasta la primera aplicación. */
export const SALIDA_MS = 4600

/* --- Grilla de calles detectada en la imagen ------------------------------------- */
// Familia A (−50,5°): 0,7716·x + 0,6361·y = a   ·   Familia B (41,25°): −0,6593·x + 0,7518·y = b
const A = [466, 598, 730, 859, 990, 1123, 1256, 1390, 1528]
const B = [-652, -515, -374, -244, -107, 23]
const W = 1974
const H = 797
const MEDIA_CALLE = 7

function desplazamiento(lista: number[], i: number) {
  const k = Math.max(0, Math.min(lista.length - 1.001, i))
  const j = Math.floor(k)
  return lista[j] + (lista[j + 1] - lista[j]) * (k - j)
}
/** Punto a partir de los desplazamientos de las dos calles. */
const cruce = (a: number, b: number): Punto => [a * 0.7518 - 0.6361 * b, 0.7716 * b + 0.6593 * a]
/** Punto en la grilla: índices de calle (con fracción = parte de la cuadra). */
const P = (ia: number, ib: number) => cruce(desplazamiento(A, ia), desplazamiento(B, ib))
/** Manzana entre las calles A[i]–A[i+1] y B[j]–B[j+1], recortada al borde de la vereda. */
function manzana(i: number, j: number): Punto[] {
  const a0 = A[i] + MEDIA_CALLE, a1 = A[i + 1] - MEDIA_CALLE, b0 = B[j] + MEDIA_CALLE, b1 = B[j + 1] - MEDIA_CALLE
  return [cruce(a0, b0), cruce(a1, b0), cruce(a1, b1), cruce(a0, b1)]
}
const dentro = ([x, y]: Punto, m = 30) => x > m && x < W - m && y > m && y < H - m
const ruta = (puntos: Punto[]) => 'M' + puntos.map((p) => p.map((v) => v.toFixed(1)).join(' ')).join(' L')

const SVG = 'http://www.w3.org/2000/svg'
function el(tag: string, attrs: Record<string, string | number>, padre?: Element) {
  const e = document.createElementNS(SVG, tag) as SVGElement
  for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, String(v))
  padre?.appendChild(e)
  return e
}

const DEFS = `
  <filter id="cdd-granulado" color-interpolation-filters="sRGB" x="0" y="0" width="100%" height="100%">
    <feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="2" seed="4" result="ruido" />
    <feDisplacementMap in="SourceGraphic" in2="ruido" scale="1.2" xChannelSelector="R" yChannelSelector="G" result="disperso" />
    <feColorMatrix in="ruido" type="saturate" values="0" result="gris" />
    <feComposite in="disperso" in2="gris" operator="arithmetic" k1="0" k2="1" k3="0.12" k4="-0.06" />
  </filter>
  <radialGradient id="cdd-luz">
    <stop offset="0" stop-color="#ffffff" />
    <stop offset="0.18" stop-color="#c8f7de" />
    <stop offset="0.45" stop-color="#2bd07e" stop-opacity="0.45" />
    <stop offset="1" stop-color="#2bd07e" stop-opacity="0" />
  </radialGradient>
  <linearGradient id="cdd-estela" gradientUnits="objectBoundingBox">
    <stop offset="0" stop-color="#9ff0c4" />
    <stop offset="1" stop-color="#ffffff" />
  </linearGradient>
  <filter id="cdd-brillo" x="-50%" y="-50%" width="200%" height="200%">
    <feGaussianBlur stdDeviation="1.6" result="b" />
    <feMerge><feMergeNode in="b" /><feMergeNode in="SourceGraphic" /></feMerge>
  </filter>`

export function crearCiudad(contenedor: HTMLElement, textos: TextosMapa): EscenaCiudad {
  const raiz = document.createElement('div')
  raiz.className = 'cdd-escena'
  raiz.innerHTML = `<svg width="0" height="0" class="cdd-defs" aria-hidden="true"><defs>${DEFS}</defs></svg>
    <div class="cdd-mundo"><img src="${imagen}" alt="" decoding="async"><svg viewBox="0 0 ${W} ${H}"></svg></div>`
  contenedor.appendChild(raiz)
  const mundo = raiz.querySelector<HTMLDivElement>('.cdd-mundo')!
  const capas = mundo.querySelector('svg')!

  let semilla = 7
  const azar = () => (semilla = (semilla * 16807) % 2147483647) / 2147483647

  /* --- La red: celulares en las esquinas y la señal corriendo por las calles ------- */
  const red = el('g', {}, capas)
  const hay = new Map<string, Punto>()
  for (let i = 0; i < A.length; i++)
    for (let j = 0; j < B.length; j++) {
      const p = P(i, j)
      if (dentro(p) && azar() < 0.62) hay.set(i + ',' + j, p)
    }
  for (const [k, p] of hay) {
    const [i, j] = k.split(',').map(Number)
    for (const [di, dj] of [[1, 0], [0, 1]]) {
      const q = hay.get(i + di + ',' + (j + dj))
      if (!q) continue
      el('path', { class: 'cdd-enlace', d: ruta([p, q]) }, red)
      if (azar() < 0.55) {
        const pulso = el('path', { class: 'cdd-pulso', d: ruta(azar() < 0.5 ? [p, q] : [q, p]) }, red)
        pulso.style.animationDelay = (-azar() * 3.2).toFixed(2) + 's'
        pulso.style.animationDuration = (2.4 + azar() * 2).toFixed(2) + 's'
      }
    }
  }
  for (const p of hay.values()) {
    el('circle', { class: 'cdd-nodo-halo', cx: p[0], cy: p[1], r: 9 }, red)
    el('circle', { class: 'cdd-nodo', cx: p[0], cy: p[1], r: 3 }, red)
  }

  /* --- Introducción: la red completa y los avisos saltando de celular en celular ---- */
  const densa = el('g', { class: 'cdd-red-densa' }, capas)
  const puntos = new Map<string, Punto>()
  const vecinos = new Map<string, string[]>()
  const clave = (i: number, j: number) => i.toFixed(3) + ',' + j.toFixed(3)
  const agregar = (i: number, j: number) => {
    const k = clave(i, j)
    if (!puntos.has(k)) {
      const p = P(i, j)
      if (!dentro(p, 8)) return null
      puntos.set(k, p)
      vecinos.set(k, [])
    }
    return k
  }
  const unir = (k1: string | null, k2: string | null) => {
    if (!k1 || !k2) return
    vecinos.get(k1)!.push(k2)
    vecinos.get(k2)!.push(k1)
  }
  for (let i = 0; i < A.length; i++)
    for (let j = 0; j < B.length - 1; j++) for (let t = 0; t < 3; t++) unir(agregar(i, j + t / 3), agregar(i, j + (t + 1) / 3))
  for (let j = 0; j < B.length; j++)
    for (let i = 0; i < A.length - 1; i++) for (let t = 0; t < 3; t++) unir(agregar(i + t / 3, j), agregar(i + (t + 1) / 3, j))
  // Tres avisos que nacen en distintos lugares y se propagan por la red (recorrido en anchura).
  const PASO_S = 0.15
  const CICLO_S = 7.5
  const retraso = new Map<string, number[]>()
  for (const [origen, inicio] of [[clave(1, 4), 0], [clave(7, 1), 1.5], [clave(4, 0), 3.1]] as const) {
    if (!puntos.has(origen)) continue
    const dist = new Map([[origen as string, 0]])
    const cola = [origen as string]
    while (cola.length) {
      const k = cola.shift()!
      for (const v of vecinos.get(k)!) if (!dist.has(v)) (dist.set(v, dist.get(k)! + 1), cola.push(v))
    }
    for (const [k, d] of dist) {
      if (!retraso.has(k)) retraso.set(k, [])
      retraso.get(k)!.push((inicio + d * PASO_S) % CICLO_S)
    }
  }
  // Estelas: por cada onda, el aviso recorre cada tramo desde el celular que lo tenía al siguiente.
  const estelas = el('g', { class: 'cdd-estelas' }, densa)
  const hechos = new Set<string>()
  for (const [k, lista] of vecinos)
    for (const v of lista) {
      const par = k < v ? k + '|' + v : v + '|' + k
      if (hechos.has(par)) continue
      hechos.add(par)
      const tk = retraso.get(k) || []
      const tv = retraso.get(v) || []
      tk.forEach((t, n) => {
        const u = tv[n]
        if (u === undefined) return
        const [desde, hasta, t0] = t <= u ? [k, v, t] : [v, k, u]
        const tramo = el('path', { class: 'cdd-tramo', d: ruta([puntos.get(desde)!, puntos.get(hasta)!]), pathLength: 1 }, estelas)
        tramo.style.animationDelay = -(CICLO_S - t0) + 's'
      })
    }
  // Celulares dentro de las manzanas: se encienden cuando la onda pasa por la esquina más cercana.
  for (let i = 0; i < A.length - 1; i++)
    for (let j = 0; j < B.length - 1; j++)
      for (let n = 0; n < 3; n++) {
        const fi = i + 0.2 + azar() * 0.6
        const fj = j + 0.2 + azar() * 0.6
        const p = P(fi, fj)
        if (!dentro(p, 8)) continue
        const k = clave(Math.round(fi * 3) / 3, Math.round(fj))
        const cercano = puntos.has(k) ? k : clave(Math.round(fi), Math.round(fj))
        for (const t of retraso.get(cercano) || [0]) {
          const c = el('circle', { class: 'cdd-punto', cx: p[0], cy: p[1], r: 5.5 }, densa)
          c.style.animationDelay = -(CICLO_S - t - 0.1) + 's'
        }
      }
  for (const [k, p] of puntos)
    for (const t of retraso.get(k) || [0]) {
      const c = el('circle', { class: 'cdd-punto', cx: p[0], cy: p[1], r: 7 }, densa)
      c.style.animationDelay = -(CICLO_S - t) + 's'
    }

  /* --- Piezas de las superposiciones ------------------------------------------------ */
  function anotacion(padre: Element, [x, y]: Punto, nota: Nota, { lado = 1, alto = 34, largo = 26 } = {}) {
    // En pantallas verticales se achica desde su punto de origen (ver .cdd-escena.vertical).
    const g = el('g', { class: 'cdd-nota', style: `transform-origin: ${x}px ${y}px` }, padre)
    const cx = x + lado * 10, cy = y - alto, fx = cx + lado * largo
    el('path', { class: 'cdd-guia', d: `M${x} ${y} L${cx} ${cy} L${fx} ${cy}` }, g)
    el('circle', { class: 'cdd-remate', cx: fx, cy, r: 1.6 }, g)
    const ancla = lado > 0 ? 'start' : 'end'
    const tx = fx + lado * 6
    const t = el('text', { class: 'cdd-cifra', x: tx, y: cy + 9, 'text-anchor': ancla }, g)
    t.textContent = nota.cifra
    if (nota.unidad) {
      const u = el('tspan', { class: 'cdd-unidad', dx: 4 }, t)
      u.textContent = nota.unidad
    }
    const r = el('text', { class: 'cdd-rotulo', x: tx + lado, y: cy + 21, 'text-anchor': ancla }, g)
    r.textContent = nota.rotulo
  }
  const ICONOS = {
    surtidor: 'M-5 6 H3 M-4 6 V-5.5 H2 V6 M-2.6 -4 H0.6 V-1 H-2.6 Z M2 -0.5 H3.6 V3.6 a1 1 0 0 0 2 0 V-2.2 L3.8 -4.2',
    casa: 'M-5.5 0.5 L0 -5 L5.5 0.5 M-3.8 -0.8 V5.2 H3.8 V-0.8 M-1.3 5.2 V2 H1.3 V5.2',
  }
  function globo(padre: Element, [x, y]: Punto, icono: keyof typeof ICONOS, color: string) {
    const g = el('g', { transform: `translate(${x} ${y})` }, padre)
    el('ellipse', { cx: 0, cy: 0, rx: 6, ry: 2.2, fill: 'rgba(3,16,14,.55)' }, g)
    el('path', { d: 'M0 0 C -4 -7 -12 -12 -12 -22 A 12 12 0 1 1 12 -22 C 12 -12 4 -7 0 0 Z', fill: color, class: 'cdd-globo' }, g)
    el('path', { d: ICONOS[icono], transform: 'translate(0 -22)', class: 'cdd-icono' }, g)
  }
  function cobertura(padre: Element, [x, y]: Punto, filas: [string, number][]) {
    const g = el('g', { class: 'cdd-nota', style: `transform-origin: ${x}px ${y}px` }, padre)
    const cx = x + 12, cy = y - 40, fx = cx + 40
    el('path', { class: 'cdd-guia', d: `M${x} ${y} L${cx} ${cy} L${fx} ${cy}` }, g)
    el('circle', { class: 'cdd-remate', cx: fx, cy, r: 1.6 }, g)
    filas.forEach(([nombre, nivel], k) => {
      const by = cy - 14 + k * 24
      for (let b = 0; b < 4; b++) {
        const h = 5 + b * 4
        el('rect', { class: b < nivel ? 'cdd-barra-on' : 'cdd-barra-off', x: fx + 8 + b * 6.5, y: by + 18 - h, width: 4.4, height: h, rx: 0.8 }, g)
      }
      const t = el('text', { class: 'cdd-rotulo cdd-rotulo-grande', x: fx + 42, y: by + 17 }, g)
      t.textContent = nombre
    })
  }

  /* --- Las siete aplicaciones (en el mismo orden que la lista de textos) -------------- */
  /** `vertical`: otro foco para pantallas verticales, donde el encuadre es más angosto. */
  const apps: { foco: Punto; vertical?: Punto; dibujar(g: Element): void }[] = [
    {
      // Tu celular: recorrido, ondas Bluetooth a un celular cercano y aviso por la calle hasta ti.
      foco: P(5, 3.1),
      dibujar(g) {
        const cel = P(5, 3.6)
        el('path', { class: 'cdd-punteado', d: ruta([P(2.3, 4), P(3, 4), P(4, 4), P(5, 4), cel]), style: 'opacity:.7' }, g)
        const cercano = P(5, 3)
        for (const r of [0, 0.8, 1.6]) {
          const onda = el('circle', { class: 'cdd-anillo', cx: cel[0], cy: cel[1], r: 56 }, g)
          onda.style.animationDelay = r + 's'
        }
        const tu = P(5, 1.6)
        el('path', { class: 'cdd-enlace', d: ruta([cercano, tu]), style: 'opacity:.7' }, g)
        const aviso = el('path', { class: 'cdd-pulso', d: ruta([cercano, tu]) }, g)
        aviso.style.animationDuration = '1.6s'
        el('circle', { class: 'cdd-nodo', cx: cercano[0], cy: cercano[1], r: 3.6 }, g)
        el('circle', { class: 'cdd-tu', cx: tu[0], cy: tu[1], r: 6 }, g)
        const etiqueta = el('text', { class: 'cdd-rotulo', x: tu[0] + 11, y: tu[1] + 2.5 }, el('g', { class: 'cdd-nota' }, g))
        etiqueta.textContent = textos.tu
        el('ellipse', { class: 'cdd-orbita', cx: cel[0], cy: cel[1], rx: 15, ry: 6, transform: `rotate(-14 ${cel[0]} ${cel[1]})` }, g)
        el('circle', { class: 'cdd-ambar', cx: cel[0], cy: cel[1], r: 5.5 }, g)
        anotacion(g, cel, textos.celular[0], { alto: 30 })
        anotacion(g, cel, textos.celular[1], { alto: -34 })
      },
    },
    {
      foco: P(5.6, 1),
      dibujar(g) {
        const surtidor = P(6, 1)
        for (let k = 0; k < 12; k++) {
          const [x, y] = P(5.92 - k * 0.085, 1)
          el('rect', { class: 'cdd-auto', x: x - 3.2, y: y - 1.8, width: 6.4, height: 3.6, rx: 1.2, transform: `rotate(41.25 ${x} ${y})` }, g)
        }
        globo(g, surtidor, 'surtidor', '#e08a2e')
        anotacion(g, [surtidor[0] + 12, surtidor[1] - 22], textos.combustible, { alto: 18 })
      },
    },
    {
      foco: P(4.6, 2.7),
      vertical: P(4.1, 2.25),
      dibujar(g) {
        const recorrido = [P(1, 4), P(4, 4), P(4, 2), P(7, 2)]
        el('path', { class: 'cdd-trazo', d: ruta(recorrido) }, g)
        const micro = el('circle', { class: 'cdd-micro', r: 6 }, g)
        el('animateMotion', { dur: '9s', repeatCount: 'indefinite', path: ruta(recorrido) }, micro)
        for (const p of [P(1, 4), P(4, 2), P(7, 2)]) el('circle', { class: 'cdd-parada', cx: p[0], cy: p[1], r: 4 }, g)
        anotacion(g, P(4, 2), textos.micro)
      },
    },
    {
      foco: P(2, 3.3),
      dibujar(g) {
        const celdas = [[1.5, 3.5, 0.55], [2.5, 3.5, 0.3], [1.5, 2.5, 0.42], [2.5, 2.5, 0.16], [2.5, 4.5, 0.48]]
        for (const [i, j, f] of celdas) {
          const esquinas = [P(i - 0.42, j - 0.42), P(i + 0.42, j - 0.42), P(i + 0.42, j + 0.42), P(i - 0.42, j + 0.42)]
          el('path', { class: 'cdd-celda', d: ruta(esquinas) + ' Z', 'fill-opacity': f * 0.5, 'stroke-opacity': f + 0.15 }, g)
        }
        cobertura(g, P(1.5, 3.5), [['Entel', 3], ['Tigo', 2], ['Viva', 1]])
      },
    },
    {
      foco: P(6, 2.1),
      vertical: P(6.9, 2.35),
      dibujar(g) {
        for (const [i, j] of [[5, 1], [6, 1], [5, 2], [6, 2], [4, 2]]) el('path', { class: 'cdd-sin-luz', d: ruta(manzana(i, j)) + ' Z' }, g)
        for (const [i, j] of [[5.4, 1.4], [6.5, 1.6], [5.6, 2.5], [6.4, 2.4], [4.5, 2.6]]) {
          const [x, y] = P(i, j)
          el('circle', { class: 'cdd-apagado', cx: x, cy: y, r: 2.4 }, g)
        }
        anotacion(g, cruce(A[7] - MEDIA_CALLE, (B[2] + B[3]) / 2), textos.apagon, { alto: 24, largo: 18 })
      },
    },
    {
      foco: P(2.1, 4.4),
      dibujar(g) {
        const casa = P(2.5, 4.5)
        el('path', { class: 'cdd-punteado', d: ruta([P(0, 5), P(2, 5), P(2, 4.5), casa]) }, g)
        globo(g, casa, 'casa', '#1fa866')
        anotacion(g, [casa[0] + 12, casa[1] - 22], textos.casa, { alto: 18 })
      },
    },
    {
      foco: P(3.5, 2.5),
      dibujar(g) {
        const [cx, cy] = P(3.5, 2.5)
        const vuelta = `M${cx - 22} ${cy + 6} C ${cx - 18} ${cy - 22}, ${cx + 14} ${cy - 26}, ${cx + 22} ${cy - 4} S ${cx + 4} ${cy + 26}, ${cx - 22} ${cy + 6}`
        el('path', { class: 'cdd-punteado', d: vuelta }, g)
        const huella = el('circle', { class: 'cdd-huella', r: 4.5 }, g)
        el('animateMotion', { dur: '7s', repeatCount: 'indefinite', path: vuelta }, huella)
        anotacion(g, [cx + 22, cy - 4], textos.mascota)
      },
    },
  ]
  const grupos = apps.map((a) => {
    const g = el('g', { class: 'cdd-app' }, capas)
    a.dibujar(g)
    return g
  })

  /* --- Cámara ------------------------------------------------------------------------ */
  let actual = -1
  const vertical = () => contenedor.clientWidth / Math.max(1, contenedor.clientHeight) < 1.1
  const zoomApp = () => (vertical() ? 1.7 : 1.75)
  const FOCO_CIUDAD = P(4, 2.6)
  function encuadrar(foco: Punto, zoom: number) {
    raiz.classList.toggle('vertical', vertical())
    const vw = contenedor.clientWidth
    const vh = contenedor.clientHeight
    const s = Math.max(vw / W, vh / H) * zoom
    // En vertical el punto queda a la izquierda: las anotaciones salen hacia la derecha.
    const sx = vertical() ? vw * 0.3 : vw * 0.66
    const sy = vertical() ? vh * 0.34 : vh * 0.5
    const tx = Math.min(0, Math.max(vw - W * s, sx - foco[0] * s))
    // En vertical la foto es más baja que la pantalla útil: se permite pasar su borde
    // (arriba queda bajo la barra y abajo bajo el texto, ambos con velo oscuro).
    const ty = vertical() ? sy - foco[1] * s : Math.min(0, Math.max(vh - H * s, sy - foco[1] * s))
    mundo.style.transform = `translate(${tx}px, ${ty}px) scale(${s})`
  }
  const focoApp = (i: number) => (vertical() && apps[i].vertical) || apps[i].foco
  const reencuadrar = () => (actual >= 0 ? encuadrar(focoApp(actual), zoomApp()) : encuadrar(FOCO_CIUDAD, 1))
  const obs = new ResizeObserver(reencuadrar)
  obs.observe(contenedor)
  let salida = 0

  function limpiarApps() {
    grupos.forEach((g) => g.classList.remove('activa', 'tras-intro'))
  }

  return {
    reposo() {
      actual = -1
      clearTimeout(salida)
      raiz.classList.remove('en-intro')
      mundo.classList.remove('saliendo')
      limpiarApps()
      encuadrar(FOCO_CIUDAD, 1)
    },
    intro() {
      actual = -1
      raiz.classList.add('en-intro')
      limpiarApps()
      encuadrar(FOCO_CIUDAD, 1)
    },
    app(i, desdeIntro) {
      actual = i
      raiz.classList.remove('en-intro')
      grupos.forEach((g, k) => {
        g.classList.toggle('activa', k === i)
        g.classList.toggle('tras-intro', desdeIntro && k === i)
      })
      mundo.classList.toggle('saliendo', desdeIntro)
      clearTimeout(salida)
      if (desdeIntro) salida = window.setTimeout(() => mundo.classList.remove('saliendo'), SALIDA_MS)
      encuadrar(focoApp(i), zoomApp())
    },
    pausar() {
      raiz.classList.add('pausada')
      capas.pauseAnimations()
    },
    seguir() {
      raiz.classList.remove('pausada')
      capas.unpauseAnimations()
    },
    destruir() {
      clearTimeout(salida)
      obs.disconnect()
      raiz.remove()
    },
  }
}
