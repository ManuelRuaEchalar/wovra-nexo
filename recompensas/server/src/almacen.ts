import { mkdirSync, readFileSync, renameSync, writeFileSync, existsSync } from "node:fs";
import { join } from "node:path";
import { config } from "./config.ts";

/**
 * Almacén mínimo en un archivo JSON. Suficiente para la demo de la hackathon;
 * en producción se reemplaza por una base de datos.
 */
export class Almacen<T> {
  private readonly ruta: string;
  private datos: Record<string, T>;

  constructor(nombre: string) {
    mkdirSync(config.directorioDatos, { recursive: true });
    this.ruta = join(config.directorioDatos, `${nombre}.json`);
    this.datos = existsSync(this.ruta) ? JSON.parse(readFileSync(this.ruta, "utf8")) : {};
  }

  obtener(id: string): T | undefined {
    return this.datos[id];
  }

  todos(): T[] {
    return Object.values(this.datos);
  }

  guardar(id: string, valor: T): T {
    this.datos[id] = valor;
    const temporal = `${this.ruta}.tmp`;
    writeFileSync(temporal, JSON.stringify(this.datos, null, 2), { mode: 0o600 });
    renameSync(temporal, this.ruta);
    return valor;
  }
}
