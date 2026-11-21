/**
 * La misma criptografía que la app (data/crypto/Crypto.kt, data/ble/BleCodec.kt
 * y data/rescue/Rescue.kt), hecha con WebCrypto. Todo ocurre en el navegador:
 * el servidor nunca ve la contraseña ni el secreto del equipo.
 */

const texto = new TextEncoder()
const sutil = crypto.subtle

/** Copia a un ArrayBuffer propio (lo que espera WebCrypto). */
const buf = (b: Uint8Array): ArrayBuffer => b.slice().buffer as ArrayBuffer

export const hex = (b: Uint8Array) => Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('')
export const unhex = (h: string) => new Uint8Array((h.match(/../g) ?? []).map((x) => parseInt(x, 16)))

export function b64(b: Uint8Array): string {
  let s = ''
  b.forEach((x) => (s += String.fromCharCode(x)))
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

export function unb64(t: string): Uint8Array {
  const s = t.trim().replace(/-/g, '+').replace(/_/g, '/')
  const bin = atob(s + '='.repeat((4 - (s.length % 4)) % 4))
  return Uint8Array.from(bin, (c) => c.charCodeAt(0))
}

const unir = (...partes: Uint8Array[]) => {
  const r = new Uint8Array(partes.reduce((n, p) => n + p.length, 0))
  let i = 0
  for (const p of partes) {
    r.set(p, i)
    i += p.length
  }
  return r
}

const sha256 = async (d: Uint8Array) => new Uint8Array(await sutil.digest('SHA-256', buf(d)))

/** derive(secreto, propósito) = sha256(secreto | propósito), igual que en la app. */
const derivar = (secreto: string, proposito: string) => sha256(unir(unhex(secreto), texto.encode(proposito)))

// --- Cuenta de rescate ------------------------------------------------------------

/** PBKDF2-SHA256 de 64 bytes: [0..32) clave de cifrado, [32..64) verificador. */
export async function derivarContrasena(contrasena: string, salHex: string, iteraciones: number) {
  const base = await sutil.importKey('raw', texto.encode(contrasena), 'PBKDF2', false, ['deriveBits'])
  const bits = await sutil.deriveBits(
    { name: 'PBKDF2', hash: 'SHA-256', salt: buf(unhex(salHex)), iterations: iteraciones },
    base,
    512,
  )
  const todo = new Uint8Array(bits)
  return { clave: todo.slice(0, 32), verificador: hex(todo.slice(32, 64)) }
}

async function abrirAes(clave: Uint8Array, sellado: string): Promise<string | null> {
  try {
    const raw = unb64(sellado)
    const k = await sutil.importKey('raw', buf(clave), 'AES-GCM', false, ['decrypt'])
    const plano = await sutil.decrypt({ name: 'AES-GCM', iv: buf(raw.slice(0, 12)) }, k, buf(raw.slice(12)))
    return new TextDecoder().decode(plano)
  } catch {
    return null
  }
}

async function sellarAes(clave: Uint8Array, plano: string): Promise<string> {
  const nonce = crypto.getRandomValues(new Uint8Array(12))
  const k = await sutil.importKey('raw', buf(clave), 'AES-GCM', false, ['encrypt'])
  const c = new Uint8Array(await sutil.encrypt({ name: 'AES-GCM', iv: buf(nonce) }, k, texto.encode(plano)))
  return b64(unir(nonce, c))
}

export type Identidad = { id: string; secreto: string; nombre: string; relevo: string }

export async function abrirBloque(clave: Uint8Array, bloque: string): Promise<Identidad | null> {
  const t = await abrirAes(clave, bloque)
  if (!t) return null
  const j = JSON.parse(t)
  return { id: j.id, secreto: j.s, nombre: j.n || 'tu celular', relevo: (j.relay || 'https://ntfy.sh').replace(/\/$/, '') }
}

// --- Protocolo del círculo ------------------------------------------------------------

/** Nombre del tema en el relevo (Crypto.topic). */
export const tema = async (secreto: string, tipo: string) =>
  'rc' + hex(await derivar(secreto, `topic-${tipo}`)).slice(0, 24)

export const abrir = async (secreto: string, sellado: string) => abrirAes(await derivar(secreto, 'key'), sellado)
export const sellar = async (secreto: string, plano: string) => sellarAes(await derivar(secreto, 'key'), plano)

async function hmac(secreto: string, datos: Uint8Array): Promise<Uint8Array> {
  const k = await sutil.importKey('raw', buf(await derivar(secreto, 'mac')), { name: 'HMAC', hash: 'SHA-256' }, false, [
    'sign',
  ])
  return new Uint8Array(await sutil.sign('HMAC', k, buf(datos)))
}

const SIN_UBICACION = -2147483648

/** Ubicación de un aviso Bluetooth reenviado por la red (BleCodec.locate). */
export async function ubicarAviso(secreto: string, raw: Uint8Array) {
  if (raw.length < 18 || raw[0] !== 0xc7) return null
  const contador = (raw[6] << 8) | raw[7]
  const mascara = await hmac(secreto, texto.encode(`ble-${contador}`))
  const cuerpo = new Uint8Array(9)
  for (let i = 0; i < 9; i++) cuerpo[i] = raw[8 + i] ^ mascara[i]
  const chk = await hmac(secreto, unir(texto.encode(`chk-${contador}`), cuerpo))
  if (raw[17] !== chk[0]) return null
  const v = new DataView(cuerpo.buffer)
  const lat = v.getInt32(0)
  const lng = v.getInt32(4)
  if (lat === SIN_UBICACION) return { estado: raw[1], punto: null, bateria: cuerpo[8] }
  return { estado: raw[1], punto: { lat: lat / 1e6, lng: lng / 1e6 }, bateria: cuerpo[8] }
}
