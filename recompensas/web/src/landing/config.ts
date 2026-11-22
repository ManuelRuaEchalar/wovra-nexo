import type { MontarFondo } from './fondos/comun'

/**
 * Fondo animado del hero. Para cambiarlo, cambiar solo esta línea:
 *   'dendritas-luz'  → neuronas de luz granulada (landing/conceptos/fondo-dendritas-luz.html)
 *   'sinapsis-cerca' → la sinapsis de cerca (landing/conceptos/fondo-sinapsis-cerca.html)
 *
 * Para comparar sin tocar el código: agregar ?fondo=sinapsis-cerca a la URL.
 */
export const FONDO: NombreFondo = 'dendritas-luz'

export const fondos = {
  'dendritas-luz': () => import('./fondos/dendritas-luz'),
  'sinapsis-cerca': () => import('./fondos/sinapsis-cerca'),
} satisfies Record<string, () => Promise<{ montar: MontarFondo }>>

export type NombreFondo = keyof typeof fondos

export function fondoElegido(): NombreFondo {
  const pedido = new URLSearchParams(location.search).get('fondo')
  return pedido && pedido in fondos ? (pedido as NombreFondo) : FONDO
}
