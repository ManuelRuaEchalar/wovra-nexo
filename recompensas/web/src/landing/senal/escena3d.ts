/**
 * Escena 3D de "La señal": tres celulares (el tuyo, uno cercano y el de tu
 * círculo) y la señal que viaja entre ellos como partículas de luz granulada.
 *
 * Los celulares son objetos mate y oscuros, casi siluetas iluminadas por la luz
 * verde: lo único que brilla es la señal (ver la estética "luz granulada").
 * El avance lo marca la sección (pasa sola de un paso al siguiente): `fijarProgreso(p)`
 * con p entre 0 y el número de pasos − 1; la escena se desliza hasta ahí interpolando
 * las poses. `saltar(p)` llega sin transición (al volver a empezar).
 */
import * as THREE from 'three'
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js'

export type Rol = 'tuyo' | 'nodo' | 'circulo'

/** Captura que muestra cada celular en cada paso (null: conserva la anterior). */
export type Pantallas = Record<Rol, (string | null)[]>

export interface OpcionesEscena {
  /** Archivo de captura → URL. */
  capturas: Record<string, string>
  pantallas: Pantallas
  /** Rótulo para las pantallas cuya captura todavía no existe. */
  pendiente: string
  /** Nombre legible de cada captura, para el rótulo provisional. */
  nombres: Record<string, string>
}

export interface Escena {
  fijarProgreso(p: number): void
  saltar(p: number): void
  activar(activa: boolean): void
  destruir(): void
}

const AURORA = new THREE.Color('#2bd07e')
const DESTELLO = new THREE.Color('#9ff0c4')
const BLANCO = new THREE.Color('#e8fff3')

// Medidas del celular (unidades de la escena; la cámara ve unos 3,2 de alto).
const ANCHO = 0.78
const ALTO = 1.64
const GROSOR = 0.05
const BISEL = 0.016
const FRENTE = GROSOR + BISEL // z de la cara frontal
const PANT_ANCHO = 0.72
const PANT_ALTO = 1.58

// --- Poses por paso ----------------------------------------------------------------

interface Pose {
  x: number
  y: number
  z: number
  ry: number
  rx: number
  /** 0: oculto, 1: presente. */
  v: number
  /** Resplandor detrás del celular. */
  halo: number
}

const P = (x: number, y: number, z: number, ry: number, rx: number, v: number, halo = 0): Pose => ({ x, y, z, ry, rx, v, halo })

/** Fuera de cuadro, a cada lado. */
const FUERA_IZQ = P(-2.2, 0.05, -1, 0.4, 0, 0)
const FUERA_DER = P(2.2, 0.05, -1, -0.4, 0, 0)

/**
 * Una pose por escena del guion (ver `textos.ts`): 0 apertura, 1 prepáralo,
 * 2 reportar, 3 recompensa o apagado falso, 4 mapa, 5 brújula, 6 hacer sonar,
 * 7 recompensa en Stellar, 8 devuelto y pagado.
 * "tuyo" es el celular perdido, "circulo" el de tu gente y "nodo" un tercero
 * (el celular que pasa cerca y, al final, quien lo devuelve).
 */
const POSES: Record<Rol, Pose[]> = {
  tuyo: [
    P(0, 0, 0.35, -0.1, 0.03, 1, 0.6),
    P(0.1, 0, 0.4, -0.12, 0.03, 1, 0.5),
    P(0.75, 0, -0.6, -0.2, 0.03, 0.85, 0.4),
    P(0.1, 0, 0.4, -0.12, 0.03, 1, 0.8),
    P(-0.95, -0.08, -1, 0.2, 0.03, 0.75, 0.3),
    P(0.75, -0.05, -0.5, -0.2, 0.03, 0.9, 0.5),
    P(0.55, 0, 0.35, -0.14, 0.03, 1, 0.9),
    P(0.7, 0, -0.3, -0.16, 0.03, 0.9, 0.6),
    P(-0.62, 0, 0.3, 0.14, 0.03, 1, 0.7),
  ],
  nodo: [
    P(1, 0.05, -0.9, -0.25, 0.03, 0.8, 0.3),
    FUERA_DER,
    FUERA_DER,
    FUERA_DER,
    P(-0.15, 0.05, -1.15, -0.05, 0.03, 0.8, 0.3),
    FUERA_DER,
    FUERA_DER,
    P(-0.55, 0.03, 0.35, 0.14, 0.03, 1, 0.6),
    P(0.66, 0.03, 0.3, -0.14, 0.03, 1, 0.8),
  ],
  circulo: [
    P(-1, 0.05, -0.9, 0.25, 0.03, 0.8, 0.3),
    FUERA_IZQ,
    P(-0.55, 0.03, 0.35, 0.14, 0.03, 1, 0.5),
    P(-1, 0.05, -1, 0.25, 0.03, 0.7, 0.2),
    P(0.62, 0, 0.35, -0.12, 0.03, 1, 0.75),
    P(-0.5, 0, 0.35, 0.14, 0.03, 1, 0.7),
    P(-0.7, 0.03, -0.3, 0.18, 0.03, 0.9, 0.4),
    FUERA_IZQ,
    FUERA_IZQ,
  ],
}

/** Intensidad de cada flujo de señal en cada escena. */
const FLUJOS = {
  onda: [0.6, 0, 0, 1, 0, 0, 1, 0.4, 0], // el celular perdido emite
  bluetooth: [0.8, 0, 0, 0, 1, 0, 0, 0, 0], // perdido → celular cercano
  internet: [0.8, 0, 0, 0, 1, 0, 0, 0, 0], // celular cercano → círculo
  orden: [0, 0, 1, 0, 0, 0, 0.7, 0, 0], // círculo → perdido: reportar, hacer sonar
  cerca: [0, 0, 0, 0, 0, 1, 0, 0, 0], // perdido → círculo, de cerca (brújula)
  reclamo: [0, 0, 0, 0, 0, 0, 0, 0.8, 0], // quien lo encontró → escrow
  pago: [0, 0, 0, 0, 0, 0, 0, 0, 1], // el escrow paga a quien lo devolvió
}

/** Intensidad de un flujo en el progreso p, interpolada entre escenas. */
function intensidad(valores: number[], p: number) {
  const i = Math.max(0, Math.min(valores.length - 1, Math.floor(p)))
  const j = Math.min(valores.length - 1, i + 1)
  const k = Math.max(0, Math.min(1, p - i))
  return valores[i] + (valores[j] - valores[i]) * k
}

const suave = (t: number) => t * t * (3 - 2 * t)

function poseEn(poses: Pose[], p: number): Pose {
  const i = Math.max(0, Math.min(poses.length - 1, Math.floor(p)))
  const j = Math.min(poses.length - 1, i + 1)
  const k = suave(Math.max(0, Math.min(1, p - i)))
  const a = poses[i]
  const b = poses[j]
  const m = (x: number, y: number) => x + (y - x) * k
  return { x: m(a.x, b.x), y: m(a.y, b.y), z: m(a.z, b.z), ry: m(a.ry, b.ry), rx: m(a.rx, b.rx), v: m(a.v, b.v), halo: m(a.halo, b.halo) }
}


// --- Geometría ---------------------------------------------------------------------

function rectRedondeado(w: number, h: number, r: number) {
  const s = new THREE.Shape()
  const x = -w / 2
  const y = -h / 2
  s.moveTo(x + r, y)
  s.lineTo(x + w - r, y)
  s.quadraticCurveTo(x + w, y, x + w, y + r)
  s.lineTo(x + w, y + h - r)
  s.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  s.lineTo(x + r, y + h)
  s.quadraticCurveTo(x, y + h, x, y + h - r)
  s.lineTo(x, y + r)
  s.quadraticCurveTo(x, y, x + r, y)
  return s
}

/** ShapeGeometry con UV de 0 a 1, para poner una textura completa encima. */
function placaRedondeada(w: number, h: number, r: number) {
  const g = new THREE.ShapeGeometry(rectRedondeado(w, h, r), 12)
  const pos = g.attributes.position
  const uv = g.attributes.uv
  for (let i = 0; i < pos.count; i++) uv.setXY(i, pos.getX(i) / w + 0.5, pos.getY(i) / h + 0.5)
  return g
}

function texturaPunto() {
  const c = document.createElement('canvas')
  c.width = c.height = 64
  const x = c.getContext('2d')!
  const g = x.createRadialGradient(32, 32, 0, 32, 32, 32)
  g.addColorStop(0, 'rgba(255,255,255,1)')
  g.addColorStop(0.25, 'rgba(255,255,255,.55)')
  g.addColorStop(1, 'rgba(255,255,255,0)')
  x.fillStyle = g
  x.fillRect(0, 0, 64, 64)
  return new THREE.CanvasTexture(c)
}

// --- Pantallas ----------------------------------------------------------------------

const TEX_W = 720
const TEX_H = Math.round((TEX_W * PANT_ALTO) / PANT_ANCHO)

/** Pantalla provisional mientras falta la captura real: noche, anillos de la marca y el rótulo. */
function pintarProvisional(ctx: CanvasRenderingContext2D, nombre: string, pendiente: string) {
  const g = ctx.createLinearGradient(0, 0, 0, TEX_H)
  g.addColorStop(0, '#0e3b36')
  g.addColorStop(1, '#06201c')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, TEX_W, TEX_H)
  ctx.save()
  ctx.translate(TEX_W / 2, TEX_H * 0.4)
  ctx.rotate((-14 * Math.PI) / 180)
  ctx.lineWidth = 3
  ctx.strokeStyle = 'rgba(159,240,196,.55)'
  ctx.beginPath()
  ctx.ellipse(-48, 0, 110, 62, 0, 0, Math.PI * 2)
  ctx.stroke()
  ctx.strokeStyle = 'rgba(159,240,196,.25)'
  ctx.beginPath()
  ctx.ellipse(48, 0, 110, 62, 0, 0, Math.PI * 2)
  ctx.stroke()
  ctx.fillStyle = '#2bd07e'
  ctx.beginPath()
  ctx.arc(0, 0, 12, 0, Math.PI * 2)
  ctx.fill()
  ctx.restore()
  ctx.textAlign = 'center'
  ctx.fillStyle = '#f4f9f6'
  ctx.font = '300 44px Sora, sans-serif'
  ctx.fillText(nombre, TEX_W / 2, TEX_H * 0.62)
  ctx.fillStyle = 'rgba(201,214,210,.8)'
  ctx.font = '600 24px Hanken, sans-serif'
  ctx.fillText(pendiente.toUpperCase(), TEX_W / 2, TEX_H * 0.62 + 52)
}

function pintarCaptura(ctx: CanvasRenderingContext2D, img: HTMLImageElement) {
  // Cubrir la pantalla manteniendo la proporción (recorta lo que sobre, centrado arriba).
  const s = Math.max(TEX_W / img.width, TEX_H / img.height)
  const w = img.width * s
  const h = img.height * s
  ctx.fillStyle = '#000'
  ctx.fillRect(0, 0, TEX_W, TEX_H)
  ctx.drawImage(img, (TEX_W - w) / 2, 0, w, h)
}

// --- Escena ------------------------------------------------------------------------

export function crearEscena(canvas: HTMLCanvasElement, op: OpcionesEscena): Escena {
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true, powerPreference: 'high-performance' })
  renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 1.75))
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.05

  const scene = new THREE.Scene()
  const pmrem = new THREE.PMREMGenerator(renderer)
  const entorno = pmrem.fromScene(new RoomEnvironment(), 0.04).texture
  scene.environment = entorno

  const camara = new THREE.PerspectiveCamera(30, 1, 0.1, 50)
  camara.position.set(0, 0.05, 6.2)

  // Luz: clave fría desde arriba a la izquierda, contraluz verde de la marca.
  scene.add(new THREE.AmbientLight(0x9ff0c4, 0.12))
  const clave = new THREE.DirectionalLight(0xe8fff3, 1.1)
  clave.position.set(-3, 4, 5)
  scene.add(clave)
  const contra = new THREE.PointLight(0x2bd07e, 18, 9, 1.6)
  contra.position.set(1.5, 1.2, -2.2)
  scene.add(contra)
  const relleno = new THREE.PointLight(0x2bd07e, 6, 8, 1.8)
  relleno.position.set(-2.5, -1.4, 2)
  scene.add(relleno)

  const mundo = new THREE.Group()
  scene.add(mundo)

  const punto = texturaPunto()
  const aDesechar: { dispose(): void }[] = [punto, entorno, pmrem]

  // --- Celulares
  const geoCuerpo = new THREE.ExtrudeGeometry(rectRedondeado(ANCHO - BISEL * 2, ALTO - BISEL * 2, 0.1), {
    depth: GROSOR * 2, bevelEnabled: true, bevelThickness: BISEL, bevelSize: BISEL, bevelSegments: 5, curveSegments: 16,
  })
  geoCuerpo.translate(0, 0, -GROSOR)
  const geoPantalla = placaRedondeada(PANT_ANCHO, PANT_ALTO, 0.085)
  const geoCamara = new THREE.ExtrudeGeometry(rectRedondeado(0.26, 0.3, 0.07), { depth: 0.02, bevelEnabled: true, bevelThickness: 0.006, bevelSize: 0.006, bevelSegments: 3 })
  const geoLente = new THREE.CylinderGeometry(0.045, 0.045, 0.012, 32)
  const geoBoton = new THREE.BoxGeometry(0.012, 0.16, 0.03)
  aDesechar.push(geoCuerpo, geoPantalla, geoCamara, geoLente, geoBoton)

  const geoHalo = new THREE.PlaneGeometry(1, 1)
  aDesechar.push(geoHalo)

  interface Telefono {
    grupo: THREE.Group
    materiales: THREE.Material[]
    capas: { mat: THREE.MeshBasicMaterial; tex: THREE.CanvasTexture; ctx: CanvasRenderingContext2D; archivo: string | null }[]
    actual: string | null
    mezcla: number
    halo: THREE.Mesh
    haloMat: THREE.MeshBasicMaterial
  }

  const imagenes = new Map<string, HTMLImageElement | 'falta' | 'cargando'>()
  const pedirImagen = (archivo: string) => {
    if (imagenes.has(archivo)) return
    const url = op.capturas[archivo]
    if (!url) {
      imagenes.set(archivo, 'falta')
      return
    }
    imagenes.set(archivo, 'cargando')
    const img = new Image()
    img.onload = () => {
      imagenes.set(archivo, img)
      for (const t of Object.values(telefonos)) for (const c of t.capas) if (c.archivo === archivo) pintarCapa(c, archivo)
    }
    img.onerror = () => imagenes.set(archivo, 'falta')
    img.src = url
  }

  function pintarCapa(c: Telefono['capas'][number], archivo: string | null) {
    c.archivo = archivo
    if (!archivo) return
    const img = imagenes.get(archivo)
    if (img instanceof HTMLImageElement) pintarCaptura(c.ctx, img)
    else pintarProvisional(c.ctx, op.nombres[archivo] ?? archivo, op.pendiente)
    c.tex.needsUpdate = true
  }

  function crearTelefono(): Telefono {
    const grupo = new THREE.Group()
    const matCuerpo = new THREE.MeshPhysicalMaterial({
      color: 0x0b1916, roughness: 0.42, metalness: 0.55, clearcoat: 0.6, clearcoatRoughness: 0.35, envMapIntensity: 0.9,
    })
    const matVidrio = new THREE.MeshPhysicalMaterial({
      color: 0x020605, roughness: 0.06, metalness: 0.1, clearcoat: 1, clearcoatRoughness: 0.04, envMapIntensity: 1.2,
    })
    const matLente = new THREE.MeshPhysicalMaterial({ color: 0x030807, roughness: 0.1, metalness: 0.8, envMapIntensity: 1.4 })
    const cuerpo = new THREE.Mesh(geoCuerpo, matCuerpo)
    grupo.add(cuerpo)
    // Cara frontal de vidrio negro (marco fino alrededor de la pantalla).
    const frente = new THREE.Mesh(placaRedondeada(ANCHO - 0.02, ALTO - 0.02, 0.105), matVidrio)
    frente.position.z = FRENTE + 0.001
    grupo.add(frente)
    // Módulo de cámara atrás y botones al costado.
    const modulo = new THREE.Mesh(geoCamara, matCuerpo)
    modulo.position.set(-0.17, 0.56, -FRENTE - 0.02)
    grupo.add(modulo)
    for (const dy of [-0.06, 0.06]) {
      const lente = new THREE.Mesh(geoLente, matLente)
      lente.rotation.x = Math.PI / 2
      lente.position.set(-0.17, 0.56 + dy, -FRENTE - 0.03)
      grupo.add(lente)
    }
    const boton = new THREE.Mesh(geoBoton, matCuerpo)
    boton.position.set(ANCHO / 2 + 0.003, 0.25, 0)
    grupo.add(boton)

    // Dos capas de pantalla para fundir una captura con la siguiente.
    const capas: Telefono['capas'] = []
    for (let i = 0; i < 2; i++) {
      const c = document.createElement('canvas')
      c.width = TEX_W
      c.height = TEX_H
      const tex = new THREE.CanvasTexture(c)
      tex.colorSpace = THREE.SRGBColorSpace
      tex.anisotropy = renderer.capabilities.getMaxAnisotropy()
      const mat = new THREE.MeshBasicMaterial({ map: tex, transparent: true, toneMapped: false, depthWrite: i === 0 })
      const m = new THREE.Mesh(geoPantalla, mat)
      m.position.z = FRENTE + 0.003 + i * 0.001
      // Orden fijo: cuando el celular se atenúa, todo es transparente y el vidrio no debe tapar la pantalla.
      m.renderOrder = 1 + i
      grupo.add(m)
      capas.push({ mat, tex, ctx: c.getContext('2d')!, archivo: null })
      aDesechar.push(tex, mat)
    }
    // Reflejo sobre el vidrio de la pantalla.
    const reflejo = new THREE.Mesh(geoPantalla, new THREE.MeshPhysicalMaterial({
      color: 0x000000, transparent: true, opacity: 0.07, roughness: 0.05, metalness: 0, clearcoat: 1, envMapIntensity: 2, depthWrite: false,
    }))
    reflejo.position.z = FRENTE + 0.006
    reflejo.renderOrder = 3
    grupo.add(reflejo)

    const haloMat = new THREE.MeshBasicMaterial({
      map: punto, color: AURORA, transparent: true, opacity: 0, blending: THREE.AdditiveBlending, depthWrite: false,
    })
    const halo = new THREE.Mesh(geoHalo, haloMat)
    halo.scale.set(2.4, 3.4, 1)
    mundo.add(halo)
    mundo.add(grupo)

    const materiales = [matCuerpo, matVidrio, matLente, frente.material as THREE.Material, reflejo.material as THREE.Material]
    // Opacidad propia de cada material: al atenuar el celular se multiplica, no se reemplaza.
    for (const m of materiales) m.userData.base = m.transparent ? m.opacity : 1
    aDesechar.push(...materiales, frente.geometry, haloMat)
    return { grupo, materiales, capas, actual: null, mezcla: 1, halo, haloMat }
  }

  const telefonos: Record<Rol, Telefono> = { tuyo: crearTelefono(), nodo: crearTelefono(), circulo: crearTelefono() }
  for (const lista of Object.values(op.pantallas)) for (const a of lista) if (a) pedirImagen(a)

  // --- Señal: flujos de partículas de luz
  interface Flujo {
    puntos: THREE.Points
    pos: Float32Array
    col: Float32Array
    fase: Float32Array
    desv: Float32Array
    tipo: 'arco' | 'onda'
    alto: number
    velocidad: number
  }
  const flujos: Flujo[] = []
  function crearFlujo(n: number, tipo: Flujo['tipo'], alto = 0.4, velocidad = 0.35, tam = 0.06): Flujo {
    const geo = new THREE.BufferGeometry()
    const pos = new Float32Array(n * 3)
    const col = new Float32Array(n * 3)
    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
    geo.setAttribute('color', new THREE.BufferAttribute(col, 3))
    const mat = new THREE.PointsMaterial({
      size: tam, map: punto, vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, sizeAttenuation: true, toneMapped: false,
    })
    const puntos = new THREE.Points(geo, mat)
    puntos.frustumCulled = false
    mundo.add(puntos)
    const fase = new Float32Array(n).map(() => Math.random())
    const desv = new Float32Array(n * 3).map(() => (Math.random() + Math.random() + Math.random() - 1.5) * 0.9)
    const f = { puntos, pos, col, fase, desv, tipo, alto, velocidad }
    flujos.push(f)
    aDesechar.push(geo, mat)
    return f
  }
  const onda = crearFlujo(320, 'onda', 0, 0.22, 0.04)
  const bluetooth = crearFlujo(700, 'arco', 0.25, 0.32)
  const internet = crearFlujo(700, 'arco', 0.9, 0.4)
  const orden = crearFlujo(520, 'arco', 0.6, 0.36)
  const cerca = crearFlujo(520, 'arco', 0.2, 0.3)
  const reclamo = crearFlujo(420, 'arco', 0.5, 0.3)
  const pago = crearFlujo(560, 'arco', 0.5, 0.3)

  const centroPantalla = (t: Telefono, z = FRENTE + 0.05) => mundo.worldToLocal(t.grupo.localToWorld(new THREE.Vector3(0, 0, z)))

  const a = new THREE.Vector3()
  const b = new THREE.Vector3()
  const c = new THREE.Vector3()
  const tmp = new THREE.Vector3()
  const colorTmp = new THREE.Color()

  /** Partículas de un flujo en arco: viajan de `desde` a `hasta`, dispersas alrededor de la curva. */
  function moverArco(f: Flujo, desde: THREE.Vector3, hasta: THREE.Vector3, fuerza: number, t: number) {
    const n = f.fase.length
    f.puntos.visible = fuerza > 0.01
    if (!f.puntos.visible) return
    a.copy(desde)
    b.copy(hasta)
    c.copy(a).add(b).multiplyScalar(0.5)
    c.y += f.alto
    c.z += 0.4
    for (let i = 0; i < n; i++) {
      const u = (f.fase[i] + t * f.velocidad) % 1
      const v = 1 - u
      // Bézier cuadrática.
      tmp.set(v * v * a.x + 2 * v * u * c.x + u * u * b.x, v * v * a.y + 2 * v * u * c.y + u * u * b.y, v * v * a.z + 2 * v * u * c.z + u * u * b.z)
      const ancho = 0.05 + 0.1 * Math.sin(u * Math.PI)
      f.pos[i * 3] = tmp.x + f.desv[i * 3] * ancho
      f.pos[i * 3 + 1] = tmp.y + f.desv[i * 3 + 1] * ancho
      f.pos[i * 3 + 2] = tmp.z + f.desv[i * 3 + 2] * ancho
      // Más brillo al centro de la curva y en la cabeza de cada pulso; titilan.
      const pulso = 0.35 + 0.65 * Math.pow(Math.sin(((u * 3 - t * 0.9) % 1) * Math.PI), 4)
      const brillo = 1.5 * fuerza * Math.sin(u * Math.PI) * pulso * (0.45 + Math.random() * 0.55)
      colorTmp.copy(i % 5 === 0 ? BLANCO : i % 2 ? DESTELLO : AURORA).multiplyScalar(brillo)
      f.col[i * 3] = colorTmp.r
      f.col[i * 3 + 1] = colorTmp.g
      f.col[i * 3 + 2] = colorTmp.b
    }
    f.puntos.geometry.attributes.position.needsUpdate = true
    f.puntos.geometry.attributes.color.needsUpdate = true
  }

  /** Partículas en ondas que se abren alrededor de un punto, en el plano del celular. */
  function moverOnda(f: Flujo, centro: THREE.Vector3, eje: THREE.Object3D, fuerza: number, t: number, radioMax: number) {
    const n = f.fase.length
    f.puntos.visible = fuerza > 0.01
    if (!f.puntos.visible) return
    const q = eje.getWorldQuaternion(new THREE.Quaternion())
    const ex = new THREE.Vector3(1, 0, 0).applyQuaternion(q)
    const ey = new THREE.Vector3(0, 1, 0).applyQuaternion(q)
    const ez = new THREE.Vector3(0, 0, 1).applyQuaternion(q)
    for (let i = 0; i < n; i++) {
      const u = (f.fase[i] * 0.33 + t * f.velocidad + (i % 3) / 3) % 1
      const ang = f.desv[i * 3] * 7 + i
      const r = 0.25 + u * radioMax + f.desv[i * 3 + 1] * 0.03
      tmp.copy(centro).addScaledVector(ex, Math.cos(ang) * r).addScaledVector(ey, Math.sin(ang) * r * 1.15).addScaledVector(ez, f.desv[i * 3 + 2] * 0.06)
      f.pos[i * 3] = tmp.x
      f.pos[i * 3 + 1] = tmp.y
      f.pos[i * 3 + 2] = tmp.z
      const brillo = fuerza * (1 - u) * (1 - u) * (0.4 + Math.random() * 0.6)
      colorTmp.copy(i % 4 === 0 ? BLANCO : DESTELLO).multiplyScalar(brillo)
      f.col[i * 3] = colorTmp.r
      f.col[i * 3 + 1] = colorTmp.g
      f.col[i * 3 + 2] = colorTmp.b
    }
    f.puntos.geometry.attributes.position.needsUpdate = true
    f.puntos.geometry.attributes.color.needsUpdate = true
  }

  // --- Polvo de fondo (browniano, como en el hero)
  const nPolvo = 140
  const geoPolvo = new THREE.BufferGeometry()
  const posPolvo = new Float32Array(nPolvo * 3)
  const colPolvo = new Float32Array(nPolvo * 3)
  for (let i = 0; i < nPolvo; i++) {
    posPolvo[i * 3] = (Math.random() - 0.5) * 7
    posPolvo[i * 3 + 1] = (Math.random() - 0.5) * 4
    posPolvo[i * 3 + 2] = -2.5 + Math.random() * 3.5
    colorTmp.copy(DESTELLO).multiplyScalar(0.08 + Math.random() * 0.2)
    colPolvo.set([colorTmp.r, colorTmp.g, colorTmp.b], i * 3)
  }
  geoPolvo.setAttribute('position', new THREE.BufferAttribute(posPolvo, 3))
  geoPolvo.setAttribute('color', new THREE.BufferAttribute(colPolvo, 3))
  const matPolvo = new THREE.PointsMaterial({ size: 0.06, map: punto, vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, toneMapped: false })
  const polvo = new THREE.Points(geoPolvo, matPolvo)
  scene.add(polvo)
  aDesechar.push(geoPolvo, matPolvo)

  // --- Encuadre según el tamaño
  let ancho = 1
  let alto = 1
  function encuadrar() {
    const r = canvas.getBoundingClientRect()
    ancho = Math.max(1, r.width)
    alto = Math.max(1, r.height)
    renderer.setSize(ancho, alto, false)
    camara.aspect = ancho / alto
    camara.updateProjectionMatrix()
    const visibleAlto = 2 * camara.position.z * Math.tan((camara.fov * Math.PI) / 360)
    const visibleAncho = visibleAlto * camara.aspect
    if (camara.aspect >= 1.1) {
      // Escritorio: el texto va a la izquierda, los celulares a la derecha.
      // Los celulares ocupan casi todo el alto: la app tiene que leerse.
      const s = Math.min(visibleAlto / 2.4, (visibleAncho * 0.44) / 2.4)
      mundo.scale.setScalar(s)
      // Corridos a la derecha: dejan aire entre el texto y los celulares.
      mundo.position.set(visibleAncho * 0.22, -visibleAlto * 0.01, 0)
    } else {
      // Celular o tableta vertical: escena arriba, texto abajo.
      const s = Math.min(visibleAncho / 2.15, (visibleAlto * 0.6) / 1.75)
      mundo.scale.setScalar(s)
      mundo.position.set(0, visibleAlto * 0.15, 0)
    }
  }
  const obs = new ResizeObserver(encuadrar)
  obs.observe(canvas)
  encuadrar()

  // --- Animación
  const reducido = matchMedia('(prefers-reduced-motion: reduce)').matches
  let objetivo = 0
  let progreso = 0
  let activa = false
  let raf = 0
  let previo = performance.now()
  let tiempo = 0

  function actualizarPantalla(t: Telefono, rol: Rol, p: number, dt: number) {
    const lista = op.pantallas[rol]
    // Captura del paso más cercano (o la última definida antes).
    let i = Math.max(0, Math.min(lista.length - 1, Math.round(p)))
    while (i > 0 && !lista[i]) i--
    const archivo = lista[i]
    if (archivo !== t.actual) {
      // La capa de atrás toma la captura vieja y la de adelante la nueva, que se funde encima.
      if (t.actual) pintarCapa(t.capas[0], t.actual)
      pintarCapa(t.capas[1], archivo)
      t.actual = archivo
      t.mezcla = t.capas[0].archivo ? 0 : 1
    }
    t.mezcla = Math.min(1, t.mezcla + dt * 3.5)
  }

  function cuadro(ahora: number) {
    const dt = Math.min(0.05, (ahora - previo) / 1000)
    previo = ahora
    tiempo += reducido ? dt * 0.3 : dt
    progreso += (objetivo - progreso) * Math.min(1, dt * 1.4)
    const p = progreso

    for (const rol of ['tuyo', 'nodo', 'circulo'] as Rol[]) {
      const t = telefonos[rol]
      const pose = poseEn(POSES[rol], p)
      const flota = reducido ? 0 : Math.sin(tiempo * 0.7 + rol.length) * 0.025
      t.grupo.position.set(pose.x, pose.y + flota, pose.z)
      t.grupo.rotation.set(pose.rx + (reducido ? 0 : Math.sin(tiempo * 0.5 + rol.length) * 0.015), pose.ry, 0)
      const s = 0.7 + 0.3 * pose.v
      t.grupo.scale.setScalar(s)
      t.grupo.visible = pose.v > 0.02
      for (const m of t.materiales) {
        const base = m.userData.base as number
        m.transparent = base < 1 || pose.v < 0.999
        m.opacity = base * pose.v
      }
      actualizarPantalla(t, rol, p, dt)
      t.capas[0].mat.opacity = pose.v
      t.capas[1].mat.opacity = pose.v * t.mezcla
      t.capas[0].mat.visible = !!t.capas[0].archivo && t.mezcla < 1
      t.capas[1].mat.visible = !!t.capas[1].archivo
      t.halo.position.set(pose.x, pose.y, pose.z - 0.5)
      t.haloMat.opacity = pose.halo * pose.v * (0.32 + 0.08 * Math.sin(tiempo * 1.3 + rol.length))
      t.halo.visible = t.haloMat.opacity > 0.01
    }

    const { tuyo, nodo, circulo } = telefonos
    moverOnda(onda, centroPantalla(tuyo, 0), tuyo.grupo, intensidad(FLUJOS.onda, p), tiempo, 1.1)
    moverArco(bluetooth, centroPantalla(tuyo), centroPantalla(nodo), intensidad(FLUJOS.bluetooth, p), tiempo)
    moverArco(internet, centroPantalla(nodo), centroPantalla(circulo), intensidad(FLUJOS.internet, p), tiempo)
    moverArco(orden, centroPantalla(circulo), centroPantalla(tuyo), intensidad(FLUJOS.orden, p), tiempo)
    moverArco(cerca, centroPantalla(tuyo), centroPantalla(circulo), intensidad(FLUJOS.cerca, p), tiempo)
    moverArco(reclamo, centroPantalla(nodo), centroPantalla(tuyo), intensidad(FLUJOS.reclamo, p), tiempo)
    moverArco(pago, centroPantalla(tuyo), centroPantalla(nodo), intensidad(FLUJOS.pago, p), tiempo)

    if (!reducido) {
      for (let i = 0; i < nPolvo; i++) {
        posPolvo[i * 3] += (Math.random() - 0.5) * 0.004
        posPolvo[i * 3 + 1] += (Math.random() - 0.5) * 0.004 + 0.0008
        if (posPolvo[i * 3 + 1] > 2) posPolvo[i * 3 + 1] = -2
      }
      geoPolvo.attributes.position.needsUpdate = true
      // La cámara deriva apenas, como un microscopio a mano.
      camara.position.x = Math.sin(tiempo * 0.11) * 0.06
      camara.position.y = 0.05 + Math.cos(tiempo * 0.09) * 0.04
      camara.lookAt(0, 0, 0)
    }

    renderer.render(scene, camara)
    if (activa) raf = requestAnimationFrame(cuadro)
  }

  return {
    fijarProgreso(p) {
      objetivo = p
    },
    saltar(p) {
      objetivo = p
      progreso = p
    },
    activar(si) {
      if (si === activa) return
      activa = si
      if (si) {
        previo = performance.now()
        raf = requestAnimationFrame(cuadro)
      } else cancelAnimationFrame(raf)
    },
    destruir() {
      activa = false
      cancelAnimationFrame(raf)
      obs.disconnect()
      for (const d of aDesechar) d.dispose()
      renderer.dispose()
    },
  }
}
