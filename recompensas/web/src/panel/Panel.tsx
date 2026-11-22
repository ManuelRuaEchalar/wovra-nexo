import { useEffect, useMemo, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { Pie } from '../App'
import { Logo } from '../Logo'
import { sitio } from '../sitio'
import { abrirBloque, derivarContrasena, type Identidad } from './cripto'
import { Relevo, vacia, type Punto, type Situacion } from './relevo'

/**
 * Panel web: el dueño sigue y controla su celular desde cualquier navegador,
 * sin depender de nadie del círculo. Todo se descifra aquí, en el navegador.
 */
export default function Panel() {
  const [yo, setYo] = useState<Identidad | null>(null)
  return (
    <>
      <header className="wrap">
        <nav aria-label="Principal">
          <a className="marca" href="/panel">
            <Logo /> {sitio.nombre}
          </a>
          <div className="enlaces">
            <span>Panel web</span>
            {yo && (
              <button className="enlace" onClick={() => setYo(null)}>
                Salir
              </button>
            )}
            <span className="chip-prueba">Prototipo</span>
          </div>
        </nav>
      </header>
      <main className="panel-web">{yo ? <Tablero yo={yo} /> : <Entrar onEntrar={setYo} />}</main>
      <Pie r={null} />
    </>
  )
}

// --- Entrar --------------------------------------------------------------------------

async function pedir<T>(ruta: string, cuerpo?: unknown): Promise<T> {
  const r = await fetch(ruta, cuerpo ? { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(cuerpo) } : {})
  const j = await r.json().catch(() => ({}))
  if (!r.ok) throw new Error(j.error ?? `Error ${r.status}`)
  return j as T
}

function Entrar({ onEntrar }: { onEntrar: (yo: Identidad) => void }) {
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState('')

  const enviar = async (e: React.FormEvent) => {
    e.preventDefault()
    setTrabajando(true)
    setError('')
    try {
      const c = correo.trim().toLowerCase()
      const { sal, iteraciones } = await pedir<{ sal: string; iteraciones: number }>(
        `/api/rescate/sal?correo=${encodeURIComponent(c)}`,
      )
      const { clave, verificador } = await derivarContrasena(contrasena, sal, iteraciones)
      const { bloque } = await pedir<{ bloque: string }>('/api/rescate/entrar', { correo: c, verificador })
      const yo = await abrirBloque(clave, bloque)
      if (!yo) throw new Error('No se pudo abrir tu cuenta. Vuelve a crearla desde la app.')
      setContrasena('')
      onEntrar(yo)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setTrabajando(false)
    }
  }

  return (
    <section className="wrap entrar-grid">
      <div>
        <p className="eyebrow">Panel web · desde cualquier computadora</p>
        <h1>¿Dónde está tu celular?</h1>
        <p className="bajada">
          Entra con el correo y la contraseña que creaste en la app, en <em>Ajustes → Panel web</em>. Puedes ver su
          ubicación, reportarlo, hacerlo sonar o terminar la alerta, aunque no tengas a nadie en tu círculo.
        </p>
        <ul className="garantias">
          <li>Tu contraseña no sale de este navegador.</li>
          <li>Nuestro servidor guarda tu cuenta cifrada y no puede leerla.</li>
          <li>La ubicación solo existe si el celular está reportado.</li>
        </ul>
      </div>
      <form className="panel pila" onSubmit={enviar}>
        <h3>Entrar</h3>
        <label>
          Correo
          <input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} required autoComplete="username" />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
            required
            minLength={8}
            autoComplete="current-password"
          />
        </label>
        <button className="boton" type="submit" disabled={trabajando}>
          {trabajando ? 'Abriendo tu cuenta…' : 'Entrar'}
        </button>
        {error && <p className="error">{error}</p>}
        <p className="nota">¿Aún no tienes cuenta? Créala en la app, en Ajustes → Panel web.</p>
      </form>
    </section>
  )
}

// --- Tablero -------------------------------------------------------------------------

const hora = (ms: number) => new Date(ms).toLocaleString('es-BO', { dateStyle: 'short', timeStyle: 'short' })

function hace(ms: number, ahora = Date.now()) {
  const s = Math.max(0, Math.round((ahora - ms) / 1000))
  if (s < 60) return 'hace un momento'
  if (s < 3600) return `hace ${Math.round(s / 60)} min`
  if (s < 86400) return `hace ${Math.round(s / 3600)} h`
  return `hace ${Math.round(s / 86400)} d`
}

const textoEstado = {
  ALERTA_ROBO: ['Reportado como robado', 'alerta'],
  ALERTA_PERDIDA: ['Reportado como perdido', 'alerta'],
  ARMADO: ['Protegido · sin alerta', 'ok'],
  INACTIVO: ['Escudo apagado', 'espera'],
} as const

function Tablero({ yo }: { yo: Identidad }) {
  const relevo = useMemo(() => new Relevo(yo), [yo])
  const [s, setS] = useState<Situacion>(vacia)
  const [cargado, setCargado] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [enviando, setEnviando] = useState(false)
  const actual = useRef(s)
  actual.current = s

  useEffect(() => {
    let vivo = true
    const leer = async () => {
      try {
        const n = await relevo.leer(actual.current)
        if (!vivo) return
        setS(n)
        setError('')
      } catch (e) {
        if (vivo) setError(e instanceof Error ? e.message : String(e))
      } finally {
        if (vivo) setCargado(true)
      }
    }
    void leer()
    const t = setInterval(leer, 10_000)
    return () => {
      vivo = false
      clearInterval(t)
    }
  }, [relevo])

  const ordenar = async (codigo: 'ROBO' | 'PERD' | 'OK' | 'RING', texto: string) => {
    setEnviando(true)
    setAviso('')
    try {
      await relevo.ordenar(codigo)
      setAviso(texto)
    } catch (e) {
      setAviso(e instanceof Error ? e.message : String(e))
    } finally {
      setEnviando(false)
    }
  }

  const enAlerta = s.estado === 'ALERTA_ROBO' || s.estado === 'ALERTA_PERDIDA'
  const ultimo = s.puntos.at(-1)
  const [titulo, tono] = s.estado ? textoEstado[s.estado] : (['Sin noticias en las últimas 12 horas', 'espera'] as const)

  return (
    <section className="wrap tablero">
      <div className="tablero-cabeza">
        <div>
          <p className="eyebrow">Tu celular</p>
          <h2>{yo.nombre ? `El celular de ${yo.nombre}` : 'Tu celular'}</h2>
        </div>
        <p className={`estado-grande ${tono}`}>
          <i aria-hidden /> {cargado ? titulo : 'Conectando…'}
        </p>
      </div>

      <div className="tablero-grid">
        <div className="mapa-caja">
          {s.puntos.length > 0 ? (
            <Mapa puntos={s.puntos} />
          ) : (
            <div className="mapa-vacio">
              <span className="orbita o1" aria-hidden />
              <span className="orbita o2" aria-hidden />
              <p>
                {enAlerta
                  ? 'Esperando la primera ubicación. Llega por internet, por SMS a tu círculo o por cualquier teléfono de la red que pase cerca.'
                  : 'Tu ubicación no se comparte en el día a día. Aparecerá aquí en cuanto reportes el celular.'}
              </p>
            </div>
          )}
        </div>

        <aside className="tablero-lado">
          <div className="tarjeta-noche recta">
            <span className="horizonte" aria-hidden />
            <p className="eyebrow">Última ubicación</p>
            <p className="dato-grande">{ultimo ? hace(ultimo.en) : '—'}</p>
            <dl>
              <dt>Llegó por</dt>
              <dd>
                {!ultimo
                  ? '—'
                  : ultimo.via === 'bluetooth'
                    ? ultimo.tarde
                      ? 'Bluetooth · guardado por un teléfono de la red, llegó tarde'
                      : 'Bluetooth · un teléfono de la red pasó cerca'
                    : 'Internet'}
              </dd>
              <dt>Puntos de la ruta</dt>
              <dd>{s.puntos.length}</dd>
              {s.bateria !== null && (
                <>
                  <dt>Batería</dt>
                  <dd>{s.bateria} %</dd>
                </>
              )}
              {s.desde && enAlerta && (
                <>
                  <dt>En alerta desde</dt>
                  <dd>{hora(s.desde)}</dd>
                </>
              )}
            </dl>
            {ultimo && (
              <a
                className="verificar"
                href={`https://www.google.com/maps?q=${ultimo.lat},${ultimo.lng}`}
                target="_blank"
                rel="noreferrer"
              >
                Abrir en Google Maps ↗
              </a>
            )}
          </div>

          {s.encontradoEn && enAlerta && (
            <p className="aviso-encontrado">Alguien avisó que lo encontró ({hace(s.encontradoEn)}).</p>
          )}

          <div className="ordenes">
            {enAlerta ? (
              <>
                <button className="boton" disabled={enviando} onClick={() => ordenar('RING', 'Orden enviada: sonará a todo volumen 40 s.')}>
                  Hacer sonar
                </button>
                <button
                  className="boton secundario"
                  disabled={enviando}
                  onClick={() => confirm('¿Ya tienes tu celular de vuelta?') && ordenar('OK', 'Listo: la alerta terminará en cuanto el celular reciba la orden.')}
                >
                  Ya lo recuperé
                </button>
              </>
            ) : (
              <>
                <button
                  className="boton alerta"
                  disabled={enviando}
                  onClick={() => confirm('¿Reportar tu celular como robado?') && ordenar('ROBO', 'Reporte enviado. En cuanto lo reciba, empezará a mandar su ubicación.')}
                >
                  Me lo robaron
                </button>
                <button
                  className="boton secundario"
                  disabled={enviando}
                  onClick={() => ordenar('PERD', 'Reporte enviado. Mostrará "Este celular está perdido" y, si la configuraste, tu recompensa.')}
                >
                  Lo perdí
                </button>
                <button className="enlace" disabled={enviando} onClick={() => ordenar('RING', 'Orden enviada: sonará a todo volumen 40 s.')}>
                  Hacerlo sonar
                </button>
              </>
            )}
            {aviso && <p className="nota">{aviso}</p>}
            {error && <p className="error">{error}</p>}
            <p className="nota">
              Las órdenes llegan en cuanto el celular tiene internet. Se actualiza solo cada 10 segundos.
            </p>
          </div>
        </aside>
      </div>

      {s.eventos.length > 0 && (
        <div className="registro">
          <h3>Registro</h3>
          <ol>
            {s.eventos
              .slice(-12)
              .reverse()
              .map((e) => (
                <li key={`${e.en}-${e.titulo}`} className={e.alerta ? 'alerta' : ''}>
                  <span>{hora(e.en)}</span>
                  <strong>{e.titulo}</strong>
                  <em>{e.detalle}</em>
                </li>
              ))}
          </ol>
        </div>
      )}
    </section>
  )
}

// --- Mapa ----------------------------------------------------------------------------

function Mapa({ puntos }: { puntos: Punto[] }) {
  const caja = useRef<HTMLDivElement>(null)
  const mapa = useRef<L.Map | null>(null)
  const capa = useRef<L.LayerGroup | null>(null)
  const encuadrado = useRef(false)

  useEffect(() => {
    if (!caja.current || mapa.current) return
    mapa.current = L.map(caja.current, { zoomControl: true, attributionControl: true })
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap',
    }).addTo(mapa.current)
    capa.current = L.layerGroup().addTo(mapa.current)
    return () => {
      mapa.current?.remove()
      mapa.current = null
      encuadrado.current = false
    }
  }, [])

  useEffect(() => {
    const m = mapa.current
    const g = capa.current
    if (!m || !g || puntos.length === 0) return
    g.clearLayers()
    const linea = puntos.map((p) => [p.lat, p.lng] as [number, number])
    L.polyline(linea, { color: '#17684f', weight: 3, opacity: 0.7 }).addTo(g)
    puntos.slice(0, -1).forEach((p) =>
      L.circleMarker([p.lat, p.lng], {
        radius: 4,
        color: '#17684f',
        weight: 1.5,
        fillColor: p.via === 'bluetooth' ? '#ffffff' : '#2bd07e',
        fillOpacity: 1,
        dashArray: p.via === 'bluetooth' ? '2 2' : undefined,
      })
        .bindTooltip(`${hora(p.en)} · ${p.via === 'bluetooth' ? 'Bluetooth' : 'Internet'}`)
        .addTo(g),
    )
    const u = puntos.at(-1)!
    L.circle([u.lat, u.lng], { radius: Math.max(u.precision, 15), color: '#2bd07e', weight: 1, fillOpacity: 0.12 }).addTo(g)
    L.circleMarker([u.lat, u.lng], { radius: 9, color: '#0e3b36', weight: 3, fillColor: '#2bd07e', fillOpacity: 1 })
      .bindTooltip(`Última: ${hora(u.en)}`, { permanent: false })
      .addTo(g)
    if (!encuadrado.current) {
      m.fitBounds(L.latLngBounds(linea).pad(0.3), { maxZoom: 17 })
      encuadrado.current = true
    }
  }, [puntos])

  return <div ref={caja} className="mapa" />
}
