/**
 * Datos del sitio que define el equipo (pie de página, "Quiénes somos").
 * Lo que quede vacío no se muestra: no publicar datos inventados.
 */
export const sitio = {
  nombre: 'Dendra',
  ciudad: 'Sucre, Bolivia',
  /** Personas del equipo: { nombre, rol? }. */
  equipo: [
    { nombre: 'Manuel Rua' },
    { nombre: 'Freddy Valencia Medina' },
    { nombre: 'Andre Claros' },
  ] as { nombre: string; rol?: string }[],
  /** Correo de contacto público del equipo. */
  contacto: 'asd.solucionesdigitales@gmail.com',
  hackathon: 'National Startups Hackathon by Stellar · 2026',
}
