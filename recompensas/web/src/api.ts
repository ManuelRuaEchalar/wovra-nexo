export type Estado =
  | 'creando'
  | 'activa'
  | 'reclamada'
  | 'pagando'
  | 'pagada'
  | 'devolviendo'
  | 'devuelta'
  | 'error'

export interface Movimiento {
  tipo: 'despliegue' | 'fondeo' | 'disputa' | 'pago' | 'devolucion'
  hash: string
  fecha: string
  enlace: string
}

export interface Recompensa {
  id: string
  nombreDueno: string
  mensaje: string
  montoBs: number
  montoUsdc: number
  estado: Estado
  contractId?: string
  enlaces?: { escrow: string; contrato: string }
  reclamada: boolean
  reclamante?: { nombre: string; direccion: string }
  movimientos: Movimiento[]
  error?: string
}

export interface ConfigPublica {
  red: 'testnet'
  pollarClavePublica: string | null
  usdc: { codigo: string; emisor: string }
  tasaBsPorUsdc: number
}

async function pedir<T>(ruta: string, init?: RequestInit): Promise<T> {
  const r = await fetch(`/api${ruta}`, {
    ...init,
    headers: { 'content-type': 'application/json', ...init?.headers },
  })
  const datos = await r.json().catch(() => ({}))
  if (!r.ok) throw new Error(datos.error ?? `Error ${r.status}`)
  return datos as T
}

export const api = {
  config: () => pedir<ConfigPublica>('/config'),
  recompensa: (id: string) => pedir<Recompensa>(`/recompensas/${encodeURIComponent(id)}`),
  reclamar: (id: string, cuerpo: { codigo: string; direccion: string; nombre: string; contacto: string }) =>
    pedir<Recompensa>(`/recompensas/${encodeURIComponent(id)}/reclamo`, {
      method: 'POST',
      body: JSON.stringify(cuerpo),
    }),
}
