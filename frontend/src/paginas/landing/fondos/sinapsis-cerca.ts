/**
 * Fondo "La sinapsis, de cerca".
 *
 * Primer plano extremo del punto donde dos dendritas casi se tocan: nubes de
 * luz granulada con puntas redondeadas y bifurcadas. La emisora se carga de
 * luz, la descarga salta como hilos eléctricos y la receptora se enciende y
 * lleva la luz hacia adentro. Profundidad de campo mínima: solo el espacio
 * entre ellas está nítido.
 */
import {
  AURORA, BLANCO, DESTELLO, VH, VW, alRedimensionar, capa, chispas, gauss, lerp, luz, medir,
  movimientoReducido, moverGrano, semilla, vinetaYGrano,
  type Azar, type MontarFondo, type Punto,
} from './comun'

type Ramas = Punto[][]
type P = { x: number; y: number; w?: number }

// --- Forma: dos dendritas que casi se tocan -------------------------------------------

/** Eje que avanza con ruido hacia un destino; el grosor lo define `perfil(t)` (t de 0 a 1). */
function eje(r: Azar, desde: P, hasta: P, perfil: (t: number) => number, ondula = 0.25, angInicial: number | null = null) {
  const pts: Punto[] = []
  const n = Math.ceil(Math.hypot(hasta.x - desde.x, hasta.y - desde.y) / 4)
  let x = desde.x
  let y = desde.y
  let a = angInicial ?? Math.atan2(hasta.y - y, hasta.x - x)
  for (let i = 0; i <= n; i++) {
    if (i > 2 && Math.hypot(hasta.x - x, hasta.y - y) < 5) break // llegó: no dar vueltas en el destino
    const t = i / n
    const faltan = n - i
    let dif = Math.atan2(hasta.y - y, hasta.x - x) - a
    while (dif > Math.PI) dif -= 2 * Math.PI
    while (dif < -Math.PI) dif += 2 * Math.PI
    a += dif * Math.min(1, 0.05 + 3 / Math.max(1, faltan)) + (r() - 0.5) * ondula * (1 - t)
    pts.push({ x, y, w: perfil(t) })
    x += Math.cos(a) * 4
    y += Math.sin(a) * 4
  }
  return pts
}

/** Ramas laterales finas que salen de un eje hacia los bordes. */
function ramitas(r: Azar, base: Punto[], cantidad: number, ramas: Ramas) {
  for (let i = 0; i < cantidad; i++) {
    const j = Math.floor(lerp(0.05, 0.7, r()) * base.length)
    const p = base[j]
    const q = base[Math.min(j + 1, base.length - 1)]
    const ang = Math.atan2(q.y - p.y, q.x - p.x) + (r() < 0.5 ? -1 : 1) * (0.6 + r() * 0.8)
    const largo = 260 + r() * 520
    const w0 = p.w * (0.18 + r() * 0.16)
    const fin = { x: p.x + Math.cos(ang) * largo, y: p.y + Math.sin(ang) * largo }
    ramas.push(eje(r, p, fin, (t) => w0 * (1 - t * 0.85), 0.5))
  }
}

const GAP = { x: 800, y: 452 }

/** Ruido suave 1D determinista: grosores irregulares, como en una célula real. */
function ruido1(r: Azar) {
  const v = Array.from({ length: 64 }, r)
  return (x: number) => {
    const i = Math.floor(x) & 63
    const f = x - Math.floor(x)
    const s = f * f * (3 - 2 * f)
    return lerp(v[i], v[(i + 1) & 63], s) * 2 - 1
  }
}

/** Brotes cortos que nacen cerca del final del tronco y se abren hacia el espacio central. Puntas romas. */
function brotes(r: Azar, tronco: Punto[], cantidad: number, ramas: Ramas, puntas: Punto[]) {
  const nr = ruido1(r)
  for (let i = 0; i < cantidad; i++) {
    const j = Math.floor(tronco.length * (0.88 + r() * 0.07))
    const p = tronco[j]
    const q = tronco[Math.min(j + 1, tronco.length - 1)]
    const dir = Math.atan2(q.y - p.y, q.x - p.x)
    const lado2 = i % 2 === 0 ? -1 : 1 // alterna arriba y abajo
    const ang = dir + lado2 * (0.4 + r() * 0.22)
    const largo = 105 + r() * 45
    const destino = { x: p.x + Math.cos(ang - lado2 * 0.12) * largo, y: p.y + Math.sin(ang - lado2 * 0.12) * largo }
    const w0 = p.w * (0.4 + r() * 0.12)
    const b = eje(r, p, destino, (t) => w0 * (1 - t * 0.38) * (1 + 0.2 * nr(t * 4 + i * 5)), 0.45, ang)
    ramas.push(b)
    puntas.push(b[b.length - 1])
    if (r() < 0.4) {
      // a veces, una bifurcación pequeña
      const m = b[Math.floor(b.length * 0.65)]
      const a2 = ang - lado2 * (0.45 + r() * 0.3)
      const l2 = 30 + r() * 26
      const sub = eje(r, m, { x: m.x + Math.cos(a2) * l2, y: m.y + Math.sin(a2) * l2 }, (t) => m.w * 0.6 * (1 - t * 0.5), 0.4, a2)
      ramas.push(sub)
      puntas.push(sub[sub.length - 1])
    }
  }
}

function construir() {
  const r = semilla(4242)
  const izq: Ramas = []
  const der: Ramas = []
  const puntasI: Punto[] = []
  const puntasD: Punto[] = []
  const nI = ruido1(r)
  const nD = ruido1(r)
  // Emisora: entra por la izquierda y termina en una punta redondeada con brotes cortos.
  const troncoI = eje(r, { x: -200, y: 640 }, { x: GAP.x - 92, y: GAP.y + 8 },
    (t) => 112 * (1 - t * 0.45) * (1 + 0.14 * nI(t * 6)) + 30 * Math.exp(-Math.pow((t - 0.94) / 0.06, 2)))
  izq.push(troncoI)
  ramitas(r, troncoI, 6, izq)
  puntasI.push(troncoI[troncoI.length - 1])
  brotes(r, troncoI, 3, izq, puntasI)
  // Receptora: igual, desde la derecha.
  const troncoD = eje(r, { x: 1820, y: 260 }, { x: GAP.x + 92, y: GAP.y - 6 },
    (t) => 128 * (1 - t * 0.5) * (1 + 0.14 * nD(t * 6)) + 28 * Math.exp(-Math.pow((t - 0.94) / 0.06, 2)))
  der.push(troncoD)
  ramitas(r, troncoD, 7, der)
  puntasD.push(troncoD[troncoD.length - 1])
  brotes(r, troncoD, 2, der, puntasD)
  return { izq, der, troncoI, troncoD, puntasI, puntasD }
}

// --- Pintura de luz granulada (adaptada a grosores grandes) ---------------------------

function pintarLuz(ctx: CanvasRenderingContext2D, ramas: Ramas, r: Azar, D: number, brillo = 1) {
  const W = ctx.canvas.width
  const H = ctx.canvas.height
  const m = ctx.getTransform()
  const tmp = document.createElement('canvas')
  tmp.width = W
  tmp.height = H
  const t = tmp.getContext('2d')!
  t.setTransform(m)
  t.lineCap = 'round'
  t.strokeStyle = `rgb(${AURORA})`
  for (const pts of ramas)
    for (let i = 1; i < pts.length; i++) {
      t.lineWidth = pts[i].w * 1.8
      t.beginPath()
      t.moveTo(pts[i - 1].x, pts[i - 1].y)
      t.lineTo(pts[i].x, pts[i].y)
      t.stroke()
    }
  ctx.save()
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.globalCompositeOperation = 'lighter'
  ctx.filter = `blur(${30 * D}px)`
  ctx.globalAlpha = 0.1 * brillo
  ctx.drawImage(tmp, 0, 0)
  ctx.filter = `blur(${8 * D}px)`
  ctx.globalAlpha = 0.1 * brillo
  ctx.drawImage(tmp, 0, 0)
  ctx.restore()

  ctx.save()
  ctx.globalCompositeOperation = 'lighter'
  for (const pts of ramas)
    for (let i = 1; i < pts.length; i++) {
      const a = pts[i - 1]
      const b = pts[i]
      const ang = Math.atan2(b.y - a.y, b.x - a.x) + Math.PI / 2
      const n = Math.min(420, 3 + b.w * 3)
      // Donde hay muchos granos, cada uno brilla menos: se conserva la textura sin quemar a blanco.
      const atenua = Math.min(1, 26 / n)
      for (let k = 0; k < n; k++) {
        const f = r()
        const d = gauss(r) * b.w * 0.42
        const centro = 1 - Math.min(1, Math.abs(d) / (b.w * 0.5 + 0.5))
        const color = r() < 0.06 + centro * 0.12 ? BLANCO : r() < 0.55 ? DESTELLO : AURORA
        const rad = 0.45 + r() * (0.7 + Math.min(1.4, b.w * 0.018))
        ctx.fillStyle = `rgba(${color},${(0.05 + 0.2 * centro * r()) * brillo * (0.35 + 0.65 * atenua)})`
        ctx.beginPath()
        ctx.arc(lerp(a.x, b.x, f) + Math.cos(ang) * d, lerp(a.y, b.y, f) + Math.sin(ang) * d, rad, 0, 6.283)
        ctx.fill()
      }
      // Remate redondeado: en la punta, los granos se reparten en un disco.
      if (i === pts.length - 1) {
        const nCap = Math.PI * b.w * b.w * 0.25 * 0.7
        for (let k = 0; k < nCap; k++) {
          const a2 = r() * 6.283
          const d2 = Math.abs(gauss(r)) * b.w * 0.36
          const cen = 1 - Math.min(1, d2 / (b.w * 0.5 + 0.5))
          ctx.fillStyle = `rgba(${r() < 0.5 ? DESTELLO : AURORA},${(0.04 + 0.16 * cen * r()) * brillo * (0.35 + 0.65 * Math.min(1, 26 / (3 + b.w * 3)))})`
          ctx.beginPath()
          ctx.arc(b.x + Math.cos(a2) * d2, b.y + Math.sin(a2) * d2, 0.45 + r() * 0.9, 0, 6.283)
          ctx.fill()
        }
      }
      // Granos sueltos: la luz se dispersa alrededor.
      for (let k = 0; k < 2; k++) {
        ctx.fillStyle = `rgba(${DESTELLO},${0.05 * brillo})`
        ctx.beginPath()
        ctx.arc(lerp(a.x, b.x, r()) + gauss(r) * (b.w + 8), lerp(a.y, b.y, r()) + gauss(r) * (b.w + 8), 0.4 + r() * 0.6, 0, 6.283)
        ctx.fill()
      }
    }
  ctx.restore()
}

function pintarFondo(canvas: HTMLCanvasElement, D: number) {
  const ctx = canvas.getContext('2d')!
  const W = canvas.width
  const H = canvas.height
  const r = semilla(12)
  const g = ctx.createRadialGradient(W * 0.5, H * 0.5, 0, W * 0.5, H * 0.5, Math.max(W, H) * 0.7)
  g.addColorStop(0, '#0c312c')
  g.addColorStop(0.6, '#061b18')
  g.addColorStop(1, '#020a09')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, W, H)
  ctx.globalCompositeOperation = 'screen'
  for (let i = 0; i < 40; i++) {
    // bokeh: vesículas lejanas fuera de foco
    const x = r() * W
    const y = r() * H
    const rad = (20 + r() * 90) * D
    const b = ctx.createRadialGradient(x, y, 0, x, y, rad)
    b.addColorStop(0, `rgba(${AURORA},${0.015 + r() * 0.03})`)
    b.addColorStop(0.7, `rgba(${DESTELLO},${0.01 + r() * 0.02})`)
    b.addColorStop(1, `rgba(${AURORA},0)`)
    ctx.fillStyle = b
    ctx.beginPath()
    ctx.arc(x, y, rad, 0, 6.283)
    ctx.fill()
  }
  for (let i = 0; i < 500; i++) {
    ctx.fillStyle = `rgba(${DESTELLO},${0.02 + r() * 0.06})`
    ctx.beginPath()
    ctx.arc(r() * W, r() * H, (0.3 + r()) * D, 0, 6.283)
    ctx.fill()
  }
  ctx.globalCompositeOperation = 'source-over'
}

// --- Rayos ----------------------------------------------------------------------

/** Camino quebrado de un rayo entre dos puntos (desplazamiento del punto medio). */
function rayo(a: P, b: P, desvio: number, prof: number): P[] {
  if (prof === 0) return [a, b]
  const m = { x: (a.x + b.x) / 2 + (Math.random() - 0.5) * desvio, y: (a.y + b.y) / 2 + (Math.random() - 0.5) * desvio }
  return rayo(a, m, desvio / 2, prof - 1).concat(rayo(m, b, desvio / 2, prof - 1).slice(1))
}

/** Dibuja un hilo eléctrico: resplandor, núcleo fino y granos de luz a lo largo. */
function hilo(ctx: CanvasRenderingContext2D, pts: P[], fuerza: number) {
  const trazo = (ancho: number, color: string, alfa: number) => {
    ctx.strokeStyle = `rgba(${color},${alfa})`
    ctx.lineWidth = ancho
    ctx.lineJoin = 'round'
    ctx.lineCap = 'round'
    ctx.beginPath()
    pts.forEach((p, i) => (i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)))
    ctx.stroke()
  }
  trazo(14, AURORA, 0.05 * fuerza)
  trazo(4, DESTELLO, 0.22 * fuerza)
  trazo(1.1, BLANCO, 0.75 * fuerza)
  for (let i = 1; i < pts.length; i++) {
    const a = pts[i - 1]
    const b = pts[i]
    for (let k = 0; k < 3; k++) {
      const f = Math.random()
      chispas(ctx, lerp(a.x, b.x, f), lerp(a.y, b.y, f), 3.5, 1, 0.9 * fuerza, 1)
    }
  }
}

// --- Escena y animación ----------------------------------------------------------

const CICLO = 10
const T_CARGA = 3
const T_DESCARGA = 2.6
const T_PROPAGA = 3.4

export const montar: MontarFondo = (contenedor) => {
  const escenaEl = document.createElement('div')
  escenaEl.className = 'fondo-escena'
  escenaEl.style.background = '#020b0a'
  contenedor.appendChild(escenaEl)

  const cFondo = capa(escenaEl, 'fondo-capa')
  const cIzq = capa(escenaEl, 'fondo-capa fondo-luz')
  const cDer = capa(escenaEl, 'fondo-capa fondo-luz')
  const cFx = capa(escenaEl, 'fondo-capa fondo-luz')
  for (const c of [cFondo, cIzq, cDer, cFx]) c.div.style.inset = '-5%'
  const grano = vinetaYGrano(escenaEl, 'radial-gradient(110% 90% at 50% 50%, transparent 35%, #01060590 70%, #010504f2 100%)', 0.15)

  const reducido = movimientoReducido()
  let D = 1
  let escala = 1
  let ox = 0
  let oy = 0
  const aplicar = (ctx: CanvasRenderingContext2D) => ctx.setTransform(D * escala, 0, 0, D * escala, D * ox, D * oy)
  let forma: ReturnType<typeof construir>

  /** Profundidad de campo mínima: todo muy desenfocado y, encima, la versión nítida solo cerca del espacio sináptico. */
  function pintarConFoco(canvas: HTMLCanvasElement, ramas: Ramas, semillaN: number) {
    const W = canvas.width
    const H = canvas.height
    const ctx = canvas.getContext('2d')!
    ctx.setTransform(1, 0, 0, 1, 0, 0)
    ctx.clearRect(0, 0, W, H)
    const nitida = document.createElement('canvas')
    nitida.width = W
    nitida.height = H
    const n = nitida.getContext('2d')!
    aplicar(n)
    pintarLuz(n, ramas, semilla(semillaN), D)
    ctx.filter = `blur(${16 * D}px)`
    ctx.globalAlpha = 0.9
    ctx.drawImage(nitida, 0, 0)
    ctx.filter = 'none'
    ctx.globalAlpha = 1
    const m = document.createElement('canvas')
    m.width = W
    m.height = H
    const mc = m.getContext('2d')!
    mc.drawImage(nitida, 0, 0)
    const cx = (GAP.x * escala + ox) * D
    const cy = (GAP.y * escala + oy) * D
    const rad = 520 * escala * D
    const g = mc.createRadialGradient(cx, cy, rad * 0.15, cx, cy, rad)
    g.addColorStop(0, 'rgba(0,0,0,1)')
    g.addColorStop(1, 'rgba(0,0,0,0)')
    mc.globalCompositeOperation = 'destination-in'
    mc.fillStyle = g
    mc.fillRect(0, 0, W, H)
    ctx.globalCompositeOperation = 'lighter'
    ctx.drawImage(m, 0, 0)
    ctx.globalCompositeOperation = 'source-over'
  }

  function preparar() {
    ;({ D, escala, ox, oy } = medir(cIzq.div, [cFondo.canvas, cIzq.canvas, cDer.canvas, cFx.canvas]))
    forma = construir()
    pintarFondo(cFondo.canvas, D)
    pintarConFoco(cIzq.canvas, forma.izq, 7)
    pintarConFoco(cDer.canvas, forma.der, 8)
  }

  type Arco = { a: P; b: P; llega: Punto | null; vida: number; edad: number }
  let tiempo = 0
  let previo = performance.now()
  let pausa = false
  let ciclo = -1
  let arcos: Arco[] = []
  let raf = 0
  const polvo = Array.from({ length: 40 }, () => ({
    x: Math.random() * VW, y: Math.random() * VH, vx: 0, vy: 0, rad: 1 + Math.random() * 3.5, prof: Math.random(),
  }))

  /** Un arco nuevo: de una punta de la emisora a una de las puntas más cercanas de la receptora. */
  function nuevoArco(completo: boolean): Arco {
    const a = forma.puntasI[Math.floor(Math.random() * forma.puntasI.length)]
    const cerca = forma.puntasD.slice().sort((p, q) => Math.hypot(p.x - a.x, p.y - a.y) - Math.hypot(q.x - a.x, q.y - a.y))
    const b = cerca[Math.floor(Math.random() * Math.min(2, cerca.length))]
    // Un "líder" (completo = false) avanza solo una parte del camino: tantea antes de la descarga.
    const fin = completo
      ? b
      : { x: lerp(a.x, b.x, 0.25 + Math.random() * 0.35), y: lerp(a.y, b.y, 0.25 + Math.random() * 0.35) + (Math.random() - 0.5) * 30 }
    return { a, b: fin, llega: completo ? b : null, vida: 0.08 + Math.random() * 0.22, edad: 0 }
  }

  function cuadro(ahora: number) {
    const dt = Math.min(0.05, Math.max(0, (ahora - previo) / 1000))
    previo = ahora
    tiempo += dt
    const ctx = cFx.canvas.getContext('2d')!
    ctx.setTransform(1, 0, 0, 1, 0, 0)
    ctx.clearRect(0, 0, cFx.canvas.width, cFx.canvas.height)
    aplicar(ctx)
    ctx.globalCompositeOperation = 'lighter'
    const c = tiempo % CICLO
    const n = Math.floor(tiempo / CICLO)
    if (n !== ciclo) {
      ciclo = n
      arcos = []
    }

    // 1. Carga: las puntas de la emisora juntan luz y empiezan a tantear el espacio.
    const carga = c < T_CARGA ? c / T_CARGA : Math.max(0, 1 - (c - T_CARGA) / 1.2)
    for (const p of forma.puntasI) {
      luz(ctx, p.x, p.y, 40 + 30 * carga, AURORA, 0.06 + 0.22 * carga)
      chispas(ctx, p.x, p.y, 10 + 10 * carga, Math.floor(4 + 18 * carga), 0.4 + 0.5 * carga, 1)
    }

    // 2. Descarga: hilos eléctricos que saltan entre las puntas y titilan.
    const enDescarga = c >= T_CARGA && c < T_CARGA + T_DESCARGA
    const intensidad = enDescarga ? Math.sin(((c - T_CARGA) / T_DESCARGA) * Math.PI) : 0
    if (dt) {
      if (c > T_CARGA * 0.6 && c < T_CARGA && Math.random() < 0.25 * carga) arcos.push(nuevoArco(false))
      if (enDescarga && Math.random() < 0.2 + 0.6 * intensidad) arcos.push(nuevoArco(true))
      for (const a of arcos) a.edad += dt
      arcos = arcos.filter((a) => a.edad < a.vida)
    }
    const encendidas = new Set<Punto>()
    for (const a of arcos) {
      const d = Math.hypot(a.b.x - a.a.x, a.b.y - a.a.y)
      const pts = rayo(a.a, a.b, d * 0.38, 5) // se recalcula cada cuadro: el hilo tiembla
      const vida = 1 - a.edad / a.vida
      hilo(ctx, pts, a.llega ? 0.55 + 0.45 * vida : 0.45 * vida)
      if (a.llega && Math.random() < 0.5) {
        const p = pts[Math.floor(Math.random() * (pts.length - 2)) + 1]
        const ang = Math.random() * 6.283
        const l = 14 + Math.random() * 30
        hilo(ctx, rayo(p, { x: p.x + Math.cos(ang) * l, y: p.y + Math.sin(ang) * l }, l * 0.5, 3), 0.35 * vida)
      }
      if (a.llega) encendidas.add(a.llega)
    }
    luz(ctx, GAP.x, GAP.y, 160, AURORA, 0.04 + 0.14 * intensidad)

    // 3. Las puntas de la receptora se encienden donde llega el hilo, y la luz viaja hacia adentro.
    for (const p of forma.puntasD) {
      const toca = encendidas.has(p)
      luz(ctx, p.x, p.y, toca ? 70 : 40, toca ? DESTELLO : AURORA, toca ? 0.5 : 0.05 + 0.1 * intensidad)
      if (toca) chispas(ctx, p.x, p.y, 16, 22, 0.9, 1)
    }
    const tProp = c - T_CARGA - T_DESCARGA * 0.5
    if (tProp > 0 && tProp < T_PROPAGA) {
      const f = tProp / T_PROPAGA
      const e = f * f * (3 - 2 * f)
      const camino = forma.troncoD
      const idx = Math.floor((1 - e) * (camino.length - 1))
      for (let i = camino.length - 1; i >= idx; i -= 3) {
        const p = camino[i]
        const k = 1 - (i - idx) / (camino.length - idx)
        chispas(ctx, p.x, p.y, p.w * 0.45, 3, 0.45 * k * (1 - f * 0.5), 1)
      }
      const p = camino[idx]
      luz(ctx, p.x, p.y, p.w * 2, AURORA, 0.3 * (1 - f * 0.6))
      chispas(ctx, p.x, p.y, p.w * 0.5, 26, 0.9 * (1 - f * 0.5), 1)
    }

    // Polvo con movimiento browniano (algunos muy cerca y desenfocados).
    for (const p of polvo) {
      if (dt && !reducido) {
        p.vx = p.vx * 0.92 + (Math.random() - 0.5) * 0.9
        p.vy = p.vy * 0.92 + (Math.random() - 0.5) * 0.9 - 0.02
        p.x = (p.x + p.vx * dt * 20 + VW) % VW
        p.y = (p.y + p.vy * dt * 20 + VH) % VH
      }
      const des = p.prof > 0.8 ? (p.prof - 0.8) * 5 : 0
      luz(ctx, p.x, p.y, p.rad * (2 + des * 12), DESTELLO, 0.14 * (1 - des * 0.6))
    }

    // Respiración: las dos puntas palpitan, como si buscaran contacto.
    if (!reducido) {
      const s1 = Math.sin(tiempo * 0.9)
      const s2 = Math.sin(tiempo * 0.8 + 1.3)
      const dx = Math.sin(tiempo * 0.05) * 10
      const dy = Math.cos(tiempo * 0.04) * 7
      cIzq.div.style.transform = `translate(${dx + s1 * 3 + carga * 3}px,${dy}px) scale(${1 + s1 * 0.004})`
      cDer.div.style.transform = `translate(${dx - s2 * 3}px,${dy}px) scale(${1 + s2 * 0.004})`
      cFx.div.style.transform = `translate(${dx}px,${dy}px)`
      cFondo.div.style.transform = `translate(${dx * 0.4}px,${dy * 0.4}px) scale(1.02)`
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
