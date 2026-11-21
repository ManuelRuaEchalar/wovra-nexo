/**
 * Datos del sitio que define el equipo (pie de página, "Quiénes somos").
 * Lo que quede vacío no se muestra: no publicar datos inventados.
 */
export const sitio = {
  nombre: 'Red Centinela',
  lema: 'Una red de celulares que se cuidan entre sí. Empezamos en Sucre, protegiendo tu teléfono.',
  ciudad: 'Sucre, Bolivia',
  /** Personas del equipo: { nombre, rol }. */
  equipo: [] as { nombre: string; rol: string }[],
  /** Correo o WhatsApp de contacto público del equipo. */
  contacto: '',
  hackathon: 'National Startups Hackathon by Stellar · 2026',
}

export const tecnologia = [
  { nombre: 'Stellar', url: 'https://stellar.org', nota: 'Red de pagos donde vive el dinero' },
  { nombre: 'Trustless Work', url: 'https://www.trustlesswork.com', nota: 'Escrow que bloquea la recompensa' },
  { nombre: 'Pollar', url: 'https://pollar.xyz', nota: 'Billetera con tu cuenta de Google' },
]
