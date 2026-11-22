/**
 * Temporizadores que se pueden pausar: al pausar guardan lo que les faltaba y al
 * seguir continúan desde ahí. Los usa el hero para que "Pausar" y salir de pantalla
 * congelen la secuencia donde está.
 */
export class Reloj {
  private tareas = new Map<number, { fn: () => void; restante: number; inicio: number; id: number }>()
  private siguiente = 1
  private pausado = false

  programar(fn: () => void, ms: number) {
    const clave = this.siguiente++
    const tarea = { fn, restante: ms, inicio: performance.now(), id: 0 }
    this.tareas.set(clave, tarea)
    if (!this.pausado) this.arrancar(clave)
    return clave
  }

  private arrancar(clave: number) {
    const tarea = this.tareas.get(clave)!
    tarea.inicio = performance.now()
    tarea.id = window.setTimeout(() => {
      this.tareas.delete(clave)
      tarea.fn()
    }, tarea.restante)
  }

  pausar() {
    if (this.pausado) return
    this.pausado = true
    const ahora = performance.now()
    for (const t of this.tareas.values()) {
      clearTimeout(t.id)
      t.restante = Math.max(0, t.restante - (ahora - t.inicio))
    }
  }

  seguir() {
    if (!this.pausado) return
    this.pausado = false
    for (const clave of this.tareas.keys()) this.arrancar(clave)
  }

  limpiar() {
    for (const t of this.tareas.values()) clearTimeout(t.id)
    this.tareas.clear()
  }
}
