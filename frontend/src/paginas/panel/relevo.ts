import { abrir, sellar, tema, ubicarAviso, unb64, type Identidad } from './cripto'

/**
 * Lee y escribe en el servidor de relevo (protocolo ntfy) igual que un
 * teléfono del círculo: el panel es "un miembro más" que eres tú mismo.
 */

export type Estado = 'INACTIVO' | 'ARMADO' | 'ALERTA_ROBO' | 'ALERTA_PERDIDA'

export type Punto = { lat: number; lng: number; precision: number; en: number; via: 'internet' | 'bluetooth'; tarde?: boolean }
export type Evento = { en: number; titulo: string; detalle: string; alerta: boolean }

export type Situacion = {
  estado: Estado | null
  motivo: string | null
  desde: number | null
  estadoEn: number
  bateria: number | null
  puntos: Punto[]
  eventos: Evento[]
  encontradoEn: number | null
}

export const vacia = (): Situacion => ({
  estado: null,
  motivo: null,
  desde: null,
  estadoEn: 0,
  bateria: null,
  puntos: [],
  eventos: [],
  encontradoEn: null,
})

type Mensaje = { id: string; time: number; event: string; topic: string; message?: string }

export class Relevo {
  private ultimo: string | null = null
  private temas: { feed: string; aviso: string; buzon: string } | null = null
  private readonly yo: Identidad

  constructor(yo: Identidad) {
    this.yo = yo
  }

  private async preparar() {
    this.temas ??= {
      feed: await tema(this.yo.secreto, 'feed'),
      aviso: 'rcs' + this.yo.id,
      buzon: await tema(this.yo.secreto, 'inbox'),
    }
    return this.temas
  }

  /** Trae lo nuevo desde la última consulta (la primera vez, las últimas 12 h). */
  async leer(s: Situacion): Promise<Situacion> {
    const t = await this.preparar()
    const desde = this.ultimo ?? '12h'
    const r = await fetch(`${this.yo.relevo}/${t.feed},${t.aviso}/json?poll=1&since=${encodeURIComponent(desde)}`)
    if (!r.ok) throw new Error(`El relevo respondió ${r.status}`)
    const lineas = (await r.text()).split('\n').filter(Boolean)
    let n = { ...s, puntos: [...s.puntos], eventos: [...s.eventos] }
    for (const l of lineas) {
      const m = JSON.parse(l) as Mensaje
      if (m.event !== 'message' || !m.message) continue
      this.ultimo = m.id
      n = m.topic === t.aviso ? await this.aviso(n, m) : await this.feed(n, m)
    }
    n.puntos.sort((a, b) => a.en - b.en)
    n.eventos.sort((a, b) => a.en - b.en)
    return n
  }

  private async feed(s: Situacion, m: Mensaje): Promise<Situacion> {
    const plano = await abrir(this.yo.secreto, m.message!)
    if (!plano) return s
    const j = JSON.parse(plano)
    switch (j.t) {
      case 'st':
        if (j.at < s.estadoEn) return s
        return {
          ...s,
          estado: j.s,
          motivo: j.r ?? null,
          desde: j.since ?? (String(j.s).startsWith('ALERTA') ? s.desde : null),
          estadoEn: j.at,
          bateria: j.bat >= 0 ? j.bat : s.bateria,
          encontradoEn: String(j.s).startsWith('ALERTA') ? s.encontradoEn : null,
        }
      case 'pts':
        for (const p of j.p as [number, number, number, number, string][]) {
          agregar(s.puntos, { lat: p[0], lng: p[1], precision: p[2], en: p[3], via: 'internet' })
        }
        return s
      case 'ev':
        if (!s.eventos.some((e) => e.en === j.at && e.titulo === j.ti)) {
          s.eventos.push({ en: j.at, titulo: j.ti, detalle: j.d, alerta: !!j.a })
        }
        return s
      case 'found':
        if (typeof j.lat === 'number') agregar(s.puntos, { lat: j.lat, lng: j.lng, precision: 30, en: j.at, via: 'internet' })
        return { ...s, encontradoEn: j.at }
    }
    return s
  }

  /** Aviso Bluetooth que otro teléfono de la red captó y reenvió. */
  private async aviso(s: Situacion, m: Mensaje): Promise<Situacion> {
    const j = JSON.parse(m.message!)
    const visto = await ubicarAviso(this.yo.secreto, unb64(j.a))
    if (!visto?.punto) return s
    const en = Number(j.at) || m.time * 1000
    agregar(s.puntos, { ...visto.punto, precision: 30, en, via: 'bluetooth', tarde: m.time * 1000 - en > 5 * 60_000 })
    return s
  }

  /** Da una orden al celular, firmada como el propio equipo. */
  async ordenar(codigo: 'ROBO' | 'PERD' | 'OK' | 'RING' | 'SEEK'): Promise<void> {
    const t = await this.preparar()
    const cuerpo = JSON.stringify({ t: 'cmd', c: codigo, from: this.yo.id, at: Date.now() })
    const r = await fetch(`${this.yo.relevo}/${t.buzon}`, { method: 'POST', body: await sellar(this.yo.secreto, cuerpo) })
    if (!r.ok) throw new Error(`No se pudo enviar la orden (${r.status})`)
  }
}

/** Si el mismo punto llega dos veces (por internet y por Bluetooth), se queda el primero. */
function agregar(lista: Punto[], p: Punto) {
  const seg = Math.floor(p.en / 1000)
  if (!lista.some((x) => Math.floor(x.en / 1000) === seg)) lista.push(p)
}
