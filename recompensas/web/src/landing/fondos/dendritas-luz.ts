/**
 * Fondo "Dendritas de luz" (concepto: landing/conceptos/fondo-dendritas-luz.html).
 *
 * Neuronas hechas de luz granulada sobre la noche verde de la marca. Cada rama
 * es una nube de granos luminosos con un resplandor difuso. Naturalidad de
 * microscopio: iluminación despareja, polvo, foco que cambia y partículas con
 * movimiento browniano. Una señal recorre una neurona, cruza la sinapsis y
 * sigue por la otra.
 */
import {
  AURORA, BLANCO, DESTELLO, VH, VW, alRedimensionar, capa, chispas, gauss, lerp, luz, medir,
  movimientoReducido, moverGrano, semilla, vinetaYGrano,
  type Azar, type MontarFondo, type Punto,
} from './comun'

type Ramas = Punto[][]
interface Neurona {
  cx: number
  cy: number
  tam: number
  arriba: number
  ramas: Ramas
  camino: Punto[] | null
  sinSoma?: boolean
}
interface Neuropilo {
  ramas: Ramas
  sinSoma: true
}
type Cuerpo = Neurona | Neuropilo

interface OpcionesCrecer {
  hacia?: { x: number; y: number }
  fuerza?: number
  subir?: number
  afinar?: number
  dividir?: number
}

// --- Morfología: neuronas piramidales y neuropilo ---------------------------------

function crecer(r: Azar, x: number, y: number, ang: number, w: number, largo: number, prof: number, ramas: Ramas, opts: OpcionesCrecer = {}) {
  const paso = 4
  const pts: Punto[] = [{ x, y, w }]
  let a = ang
  const n = Math.floor(largo / paso)
  const curva = (r() - 0.5) * 0.025
  for (let i = 0; i < n; i++) {
    let ruidoA = 0.36
    if (opts.hacia) {
      const dist = Math.hypot(opts.hacia.x - x, opts.hacia.y - y)
      const cerca = 1 - Math.min(1, dist / 220)
      ruidoA *= 0.3 + 0.7 * (1 - cerca)
      let dif = Math.atan2(opts.hacia.y - y, opts.hacia.x - x) - a
      while (dif > Math.PI) dif -= 2 * Math.PI
      while (dif < -Math.PI) dif += 2 * Math.PI
      a += dif * ((opts.fuerza ?? 0.05) + cerca * 0.45)
    }
    if (opts.subir) a += (opts.subir - a) * 0.02
    a += (r() - 0.5) * ruidoA + curva
    x += Math.cos(a) * paso
    y += Math.sin(a) * paso
    w = Math.max(w * (opts.afinar ?? 0.978), 0.5)
    pts.push({ x, y, w })
    if (opts.hacia && Math.hypot(opts.hacia.x - x, opts.hacia.y - y) < 8) break
    if (prof > 0 && i > 4 && w > 0.8 && r() < (opts.dividir ?? 0.07)) {
      const lado = r() < 0.5 ? -1 : 1
      crecer(r, x, y, a + lado * (0.5 + r() * 0.6), w * (0.5 + r() * 0.2), largo * (0.3 + r() * 0.35), prof - 1, ramas)
      a -= lado * (0.1 + r() * 0.15)
    }
  }
  ramas.push(pts)
  return pts
}

function piramidal(r: Azar, cx: number, cy: number, tam: number, inclin: number, opts: { hacia?: { x: number; y: number } } = {}): Neurona {
  const ramas: Ramas = []
  const arriba = -Math.PI / 2 + inclin
  crecer(r, cx + Math.cos(arriba) * tam, cy + Math.sin(arriba) * tam, arriba, tam * 0.45, tam * 17, 6, ramas, {
    subir: arriba, afinar: 0.99, dividir: 0.075,
  })
  const nb = 5 + Math.floor(r() * 3)
  for (let i = 0; i < nb; i++) {
    const a = arriba + Math.PI + (i / (nb - 1) - 0.5) * 2.6 + (r() - 0.5) * 0.3
    crecer(r, cx + Math.cos(a) * tam * 0.7, cy + Math.sin(a) * tam * 0.7, a, tam * (0.24 + r() * 0.12), tam * (6 + r() * 5), 5, ramas)
  }
  let camino: Punto[] | null = null
  if (opts.hacia) {
    const a = Math.atan2(opts.hacia.y - cy, opts.hacia.x - cx)
    camino = crecer(r, cx + Math.cos(a) * tam * 0.7, cy + Math.sin(a) * tam * 0.7, a, tam * 0.17, 1500, 0, ramas, {
      hacia: opts.hacia, afinar: 0.997,
    })
  }
  return { cx, cy, tam, arriba, ramas, camino }
}

function neuropilo(r: Azar, n: number, largoMin: number, largoMax: number, grosor: number): Neuropilo {
  const ramas: Ramas = []
  for (let i = 0; i < n; i++) {
    crecer(r, r() * VW, r() * VH, r() * Math.PI * 2, grosor * (0.6 + r() * 0.8), largoMin + r() * (largoMax - largoMin), 1, ramas, {
      afinar: 0.997, dividir: 0.015,
    })
  }
  return { ramas, sinSoma: true }
}

// --- Dibujo con luz granulada ------------------------------------------------------

/** Resplandor difuso (el trazo, desenfocado) y luego miles de granos alrededor del eje de cada rama. */
function pintarNeurona(ctx: CanvasRenderingContext2D, n: Cuerpo, r: Azar, D: number, brillo = 1) {
  const W = ctx.canvas.width
  const H = ctx.canvas.height
  const m = ctx.getTransform()

  const tmp = document.createElement('canvas')
  tmp.width = W
  tmp.height = H
  const t = tmp.getContext('2d')!
  t.setTransform(m)
  t.lineCap = 'round'
  t.lineJoin = 'round'
  t.strokeStyle = `rgb(${AURORA})`
  for (const pts of n.ramas)
    for (let i = 1; i < pts.length; i++) {
      t.lineWidth = pts[i].w * 2.6
      t.beginPath()
      t.moveTo(pts[i - 1].x, pts[i - 1].y)
      t.lineTo(pts[i].x, pts[i].y)
      t.stroke()
    }
  if (!n.sinSoma) {
    t.fillStyle = `rgb(${AURORA})`
    t.beginPath()
    t.arc(n.cx, n.cy, n.tam * 1.3, 0, Math.PI * 2)
    t.fill()
  }
  ctx.save()
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.globalCompositeOperation = 'lighter'
  ctx.filter = `blur(${14 * D}px)`
  ctx.globalAlpha = 0.11 * brillo
  ctx.drawImage(tmp, 0, 0)
  ctx.filter = `blur(${4 * D}px)`
  ctx.globalAlpha = 0.14 * brillo
  ctx.drawImage(tmp, 0, 0)
  ctx.restore()

  ctx.save()
  ctx.globalCompositeOperation = 'lighter'
  const grano = (x: number, y: number, rad: number, color: string, alfa: number) => {
    ctx.fillStyle = `rgba(${color},${alfa})`
    ctx.beginPath()
    ctx.arc(x, y, rad, 0, Math.PI * 2)
    ctx.fill()
  }
  for (const pts of n.ramas) {
    for (let i = 1; i < pts.length; i++) {
      const a = pts[i - 1]
      const b = pts[i]
      const ang = Math.atan2(b.y - a.y, b.x - a.x) + Math.PI / 2
      const densidad = Math.min(14, 2.2 + b.w * 2.4) // más granos donde la rama es gruesa
      for (let k = 0; k < densidad; k++) {
        const f = r()
        const d = gauss(r) * b.w * 0.7
        const x = lerp(a.x, b.x, f) + Math.cos(ang) * d
        const y = lerp(a.y, b.y, f) + Math.sin(ang) * d
        const centro = 1 - Math.min(1, Math.abs(d) / (b.w * 0.9 + 0.5)) // más brillante en el eje
        const color = r() < 0.18 + centro * 0.3 ? BLANCO : r() < 0.55 ? DESTELLO : AURORA
        grano(x, y, 0.35 + r() * 0.75, color, (0.08 + 0.32 * centro * r()) * brillo)
      }
      // Granos sueltos alrededor: la luz se dispersa.
      if (r() < 0.5)
        grano(lerp(a.x, b.x, r()) + gauss(r) * (b.w * 2 + 4), lerp(a.y, b.y, r()) + gauss(r) * (b.w * 2 + 4), 0.3 + r() * 0.5, DESTELLO, 0.06 * brillo)
      // Espinas: brotes de pocos granos.
      if (b.w < 3 && r() < 0.28) {
        const s = r() < 0.5 ? -1 : 1
        const l = 1.5 + r() * 4
        for (let k = 0; k < 4; k++) {
          const q = (k / 3) * l
          grano(b.x + Math.cos(ang) * s * q + gauss(r) * 0.4, b.y + Math.sin(ang) * s * q + gauss(r) * 0.4, 0.3 + r() * 0.4, DESTELLO, (0.12 + 0.12 * Number(k === 3)) * brillo)
        }
      }
    }
  }
  // Soma: nube densa de granos, más luminosa al centro.
  if (!n.sinSoma) {
    for (let k = 0; k < 900; k++) {
      const ang = r() * Math.PI * 2
      const rad = Math.abs(gauss(r)) * n.tam * 0.75
      const tri = 1 + 0.3 * Math.cos(3 * (ang - n.arriba))
      const x = n.cx + Math.cos(ang) * rad * tri
      const y = n.cy + Math.sin(ang) * rad * tri * 0.92
      const centro = 1 - Math.min(1, rad / n.tam)
      grano(x, y, 0.4 + r() * 0.8, centro > 0.55 ? BLANCO : DESTELLO, (0.06 + 0.22 * centro) * brillo)
    }
  }
  ctx.restore()
}

/** La noche verde con iluminación despareja y polvo. */
function pintarFondo(canvas: HTMLCanvasElement, r: Azar, D: number) {
  const ctx = canvas.getContext('2d')!
  const W = canvas.width
  const H = canvas.height
  const g = ctx.createRadialGradient(W * 0.55, H * 0.44, 0, W * 0.55, H * 0.44, Math.max(W, H) * 0.78)
  g.addColorStop(0, '#0f3a34')
  g.addColorStop(0.5, '#09241f')
  g.addColorStop(1, '#030e0c')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, W, H)
  ctx.globalCompositeOperation = 'screen'
  for (let i = 0; i < 70; i++) {
    const x = r() * W
    const y = r() * H
    const rad = (60 + r() * 300) * D
    const m = ctx.createRadialGradient(x, y, 0, x, y, rad)
    m.addColorStop(0, `rgba(${AURORA},${0.012 + r() * 0.025})`)
    m.addColorStop(1, `rgba(${AURORA},0)`)
    ctx.fillStyle = m
    ctx.fillRect(x - rad, y - rad, rad * 2, rad * 2)
  }
  for (let i = 0; i < 700; i++) {
    ctx.fillStyle = `rgba(${DESTELLO},${0.02 + r() * 0.07})`
    ctx.beginPath()
    ctx.arc(r() * W, r() * H, (0.3 + r() * 1) * D, 0, Math.PI * 2)
    ctx.fill()
  }
  ctx.globalCompositeOperation = 'source-over'
}

// --- Escena y animación ------------------------------------------------------------

const CICLO = 11
const T_A = 4.2
const T_SALTO = 0.9
const T_B = 3.4

function puntoEn(c: Punto[], f: number): Punto {
  const i = Math.min(c.length - 1, Math.max(0, f * (c.length - 1)))
  const a = c[Math.floor(i)]
  const b = c[Math.ceil(i)]
  const k = i - Math.floor(i)
  return { x: lerp(a.x, b.x, k), y: lerp(a.y, b.y, k), w: lerp(a.w, b.w, k) }
}

/** Recorre el tramo detrás de la señal y lo hace brillar con granos. */
function estela(ctx: CanvasRenderingContext2D, camino: Punto[], f: number, largo: number) {
  const n = camino.length - 1
  const fin = Math.floor(f * n)
  for (let i = Math.max(1, fin - largo); i <= fin; i++) {
    const k = 1 - (fin - i) / largo
    const p = camino[i]
    chispas(ctx, p.x, p.y, p.w + 1.5, 3, 0.5 * k * k)
  }
}

const enfoque = (t: number) => 0.45 + 0.3 * Math.sin(t * 0.09) * Math.sin(t * 0.031 + 1)

export const montar: MontarFondo = (contenedor) => {
  const escenaEl = document.createElement('div')
  escenaEl.className = 'fondo-escena'
  escenaEl.style.background = '#03100e'
  contenedor.appendChild(escenaEl)

  const luzFondo = document.createElement('canvas')
  luzFondo.style.cssText = 'position:absolute;inset:-4%;width:108%;height:108%'
  escenaEl.appendChild(luzFondo)
  const clase = 'fondo-capa fondo-luz'
  const lejos = capa(escenaEl, clase)
  const medio = capa(escenaEl, clase)
  const fx = capa(escenaEl, clase)
  const cerca = capa(escenaEl, clase)
  for (const c of [lejos, medio, fx, cerca]) c.div.style.inset = '-6%'
  const grano = vinetaYGrano(escenaEl, 'radial-gradient(125% 100% at 54% 46%, transparent 38%, #02090880 72%, #010605f0 100%)', 0.16)

  const reducido = movimientoReducido()
  let D = 1
  let escala = 1
  let ox = 0
  let oy = 0
  const aplicar = (ctx: CanvasRenderingContext2D) => ctx.setTransform(D * escala, 0, 0, D * escala, D * ox, D * oy)
  const limpiar = (c: HTMLCanvasElement) => {
    const x = c.getContext('2d')!
    x.setTransform(1, 0, 0, 1, 0, 0)
    x.clearRect(0, 0, c.width, c.height)
    return x
  }

  let escena: {
    A: Neurona
    caminoB: Punto[]
    B: Neurona
    sinapsis: { x: number; y: number }
    particulas: { x: number; y: number; rad: number; vx: number; vy: number; prof: number }[]
  }

  function preparar() {
    ;({ D, escala, ox, oy } = medir(medio.div, [luzFondo, lejos.canvas, medio.canvas, fx.canvas, cerca.canvas]))
    const r = semilla(2026)
    const sinapsis = { x: 930, y: 420 }
    const A = piramidal(r, 540, 720, 24, 0.3, { hacia: { x: sinapsis.x - 6, y: sinapsis.y + 4 } })
    const B = piramidal(r, 1250, 470, 21, -0.2, { hacia: { x: sinapsis.x + 6, y: sinapsis.y - 3 } })
    const lejanas: Cuerpo[] = [
      piramidal(r, 200, 420, 18, -0.2), piramidal(r, 1460, 860, 19, 0.3), piramidal(r, 820, 200, 15, 0.1),
      piramidal(r, 1080, 820, 16, -0.4), piramidal(r, 420, 120, 14, 0.25), piramidal(r, 1500, 220, 16, -0.1),
      piramidal(r, 700, 520, 13, 0.5), piramidal(r, 60, 820, 17, 0.1), neuropilo(r, 70, 250, 900, 1.1),
    ]
    const fibras = neuropilo(semilla(404), 26, 200, 700, 0.8)
    const cercanas = [piramidal(semilla(91), 150, 980, 30, 0.6)]
    const particulas = Array.from({ length: 50 }, () => ({ x: r() * VW, y: r() * VH, rad: 1.2 + r() * 4, vx: 0, vy: 0, prof: r() }))
    escena = { A, B, caminoB: B.camino!.slice().reverse(), sinapsis, particulas }

    pintarFondo(luzFondo, semilla(5), D)
    let ctx = limpiar(lejos.canvas)
    aplicar(ctx)
    lejanas.forEach((n, i) => pintarNeurona(ctx, n, semilla(30 + i), D, n.sinSoma ? 0.28 : 0.42))
    ctx = limpiar(medio.canvas)
    aplicar(ctx)
    pintarNeurona(ctx, fibras, semilla(9), D, 0.5)
    ;[A, B].forEach((n, i) => pintarNeurona(ctx, n, semilla(10 + i), D, 1))
    ctx = limpiar(cerca.canvas)
    aplicar(ctx)
    cercanas.forEach((n, i) => pintarNeurona(ctx, n, semilla(50 + i), D, 0.8))
  }

  let tiempo = 0
  let previo = performance.now()
  let pausa = false
  let raf = 0

  function cuadro(ahora: number) {
    const dt = Math.min(0.05, Math.max(0, (ahora - previo) / 1000))
    previo = ahora
    tiempo += dt
    const t = tiempo
    const { A, B, caminoB, sinapsis, particulas } = escena
    const ctx = limpiar(fx.canvas)
    aplicar(ctx)
    ctx.globalCompositeOperation = 'lighter'

    luz(ctx, A.cx, A.cy, A.tam * 3, AURORA, 0.07 + 0.04 * Math.sin(t * 0.8))
    luz(ctx, B.cx, B.cy, B.tam * 3, AURORA, 0.06 + 0.04 * Math.sin(t * 0.6 + 1))

    const c = t % CICLO
    if (c < T_A) {
      const f = c / T_A
      const e = f * f * (3 - 2 * f)
      const p = puntoEn(A.camino!, e)
      estela(ctx, A.camino!, e, 40)
      luz(ctx, p.x, p.y, 30, AURORA, 0.3)
      chispas(ctx, p.x, p.y, 7, 26, 0.9)
    } else if (c < T_A + T_SALTO) {
      const f = (c - T_A) / T_SALTO
      luz(ctx, sinapsis.x, sinapsis.y, 20 + 80 * f, AURORA, 0.45 * Math.sin(f * Math.PI))
      chispas(ctx, sinapsis.x, sinapsis.y, 6 + 30 * f, 60, 0.9 * (1 - f * 0.6))
    } else if (c < T_A + T_SALTO + T_B) {
      const f = (c - T_A - T_SALTO) / T_B
      const e = f * f * (3 - 2 * f)
      const p = puntoEn(caminoB, e)
      estela(ctx, caminoB, e, 36)
      luz(ctx, p.x, p.y, 26, AURORA, 0.26)
      chispas(ctx, p.x, p.y, 6, 22, 0.85)
    } else {
      const f = (c - T_A - T_SALTO - T_B) / (CICLO - T_A - T_SALTO - T_B)
      luz(ctx, B.cx, B.cy, B.tam * (2 + 3 * f), DESTELLO, 0.28 * (1 - f))
      chispas(ctx, B.cx, B.cy, B.tam * (1 + 2 * f), 30, 0.6 * (1 - f))
    }
    // La sinapsis titila apenas: la chispa del logo.
    chispas(ctx, sinapsis.x, sinapsis.y, 5, 6, 0.5)
    luz(ctx, sinapsis.x, sinapsis.y, 14, AURORA, 0.12 + 0.06 * Math.sin(t * 1.7))

    // Partículas con movimiento browniano; desenfocadas según su plano.
    const foco = enfoque(t)
    for (const p of particulas) {
      if (!reducido) {
        p.vx = p.vx * 0.9 + (Math.random() - 0.5) * 16 * dt
        p.vy = p.vy * 0.9 + (Math.random() - 0.5) * 16 * dt - 0.5 * dt
        p.x = (p.x + p.vx + VW) % VW
        p.y = (p.y + p.vy + VH) % VH
      }
      const des = Math.abs(p.prof - foco)
      luz(ctx, p.x, p.y, p.rad * (1.4 + des * 5), DESTELLO, 0.22 * (1 - des * 0.7))
    }

    if (!reducido) {
      const dx = Math.sin(t * 0.04) * 18
      const dy = Math.cos(t * 0.033) * 12
      const s = 1 + Math.sin(t * 0.025) * 0.012
      lejos.div.style.transform = `translate(${dx * 0.4}px,${dy * 0.4}px) scale(${s})`
      medio.div.style.transform = `translate(${dx}px,${dy}px) scale(${s})`
      fx.div.style.transform = `translate(${dx}px,${dy}px) scale(${s})`
      cerca.div.style.transform = `translate(${dx * 2}px,${dy * 2}px) scale(${s * 1.02})`
      // Todo un poco difuso; el foco se pasea despacio entre planos.
      lejos.div.style.filter = `blur(${(3 + foco * 6).toFixed(2)}px)`
      medio.div.style.filter = `blur(${(0.8 + Math.abs(foco - 0.45) * 2).toFixed(2)}px)`
      cerca.div.style.filter = `blur(${(10 + (1 - foco) * 14).toFixed(2)}px)`
    }
    moverGrano(grano)
    if (!pausa) raf = requestAnimationFrame(cuadro)
  }

  preparar()
  raf = requestAnimationFrame(cuadro)
  const dejarDeMedir = alRedimensionar(contenedor, preparar)

  return {
    pausar() {
      pausa = true
      cancelAnimationFrame(raf)
    },
    seguir() {
      if (!pausa) return
      pausa = false
      previo = performance.now()
      raf = requestAnimationFrame(cuadro)
    },
    destruir() {
      pausa = true
      cancelAnimationFrame(raf)
      dejarDeMedir()
      escenaEl.remove()
    },
  }
}
