/** Error esperado de una regla de negocio: se responde al cliente con su estado HTTP y su mensaje. */
export class ErrorNegocio extends Error {
  readonly estado: number;
  constructor(mensaje: string, estado = 400) {
    super(mensaje);
    this.estado = estado;
  }
}
