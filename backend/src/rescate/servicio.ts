import { createHash, createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import { Almacen } from "../compartido/almacen.ts";
import { ErrorNegocio } from "../compartido/errores.ts";

/**
 * Cuentas de rescate del panel web.
 *
 * El celular cifra su identidad (id y secreto del equipo) con una clave que
 * deriva de la contraseña del dueño (PBKDF2) y sube solo el resultado. El
 * servidor guarda:
 *   - el bloque cifrado, que no puede abrir;
 *   - la sal y las iteraciones, para que el navegador derive la misma clave;
 *   - el hash de un "verificador" (la otra mitad de la derivación), que
 *     prueba que quien entra conoce la contraseña sin que la contraseña
 *     ni la clave de cifrado lleguen nunca aquí.
 */
type Cuenta = {
  correo: string;
  idEquipo: string;
  sal: string;
  iteraciones: number;
  bloque: string;
  verificadorHash: string;
  tokenEquipoHash: string;
  creada: string;
  actualizada: string;
};

const cuentas = new Almacen<Cuenta>("rescate");
const sistema = new Almacen<string>("rescate-sistema");

const ITERACIONES_MIN = 100_000;
const INTENTOS_MAX = 8;
const VENTANA_MS = 15 * 60_000;
const fallos = new Map<string, { n: number; desde: number }>();

const sha256 = (texto: string) => createHash("sha256").update(texto).digest("hex");

function claveSistema(): string {
  return sistema.obtener("sal-falsa") ?? sistema.guardar("sal-falsa", randomBytes(32).toString("hex"));
}

function normalizar(correo: unknown): string {
  const c = String(correo ?? "").trim().toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(c) || c.length > 200) throw new ErrorNegocio("Correo inválido");
  return c;
}

function hex(valor: unknown, nombre: string, largo?: number): string {
  const v = String(valor ?? "");
  if (!/^[0-9a-f]+$/.test(v) || (largo !== undefined && v.length !== largo)) {
    throw new ErrorNegocio(`${nombre} inválido`);
  }
  return v;
}

function iguales(a: string, b: string): boolean {
  const x = Buffer.from(a);
  const y = Buffer.from(b);
  return x.length === y.length && timingSafeEqual(x, y);
}

/**
 * Sal para derivar la clave. Para un correo sin cuenta se inventa una sal
 * estable: así no se puede averiguar qué correos están registrados.
 */
export function sal(correo: unknown): { sal: string; iteraciones: number } {
  const c = normalizar(correo);
  const cuenta = cuentas.obtener(c);
  if (cuenta) return { sal: cuenta.sal, iteraciones: cuenta.iteraciones };
  return {
    sal: createHmac("sha256", claveSistema()).update(c).digest("hex").slice(0, 32),
    iteraciones: 210_000,
  };
}

/** Crea o actualiza la cuenta. Solo el mismo celular (con su token) puede cambiarla. */
export function guardarCuenta(cuerpo: Record<string, unknown>): { correo: string } {
  const correo = normalizar(cuerpo.correo);
  const idEquipo = hex(cuerpo.idEquipo, "Id del equipo", 8);
  const salHex = hex(cuerpo.sal, "Sal", 32);
  const iteraciones = Number(cuerpo.iteraciones);
  if (!Number.isInteger(iteraciones) || iteraciones < ITERACIONES_MIN || iteraciones > 2_000_000) {
    throw new ErrorNegocio("Iteraciones inválidas");
  }
  const verificador = hex(cuerpo.verificador, "Verificador", 64);
  const tokenEquipo = hex(cuerpo.tokenEquipo, "Token del equipo", 64);
  const bloque = String(cuerpo.bloque ?? "");
  if (!/^[A-Za-z0-9_-]{40,4000}$/.test(bloque)) throw new ErrorNegocio("Bloque inválido");

  const previa = cuentas.obtener(correo);
  if (previa && !iguales(previa.tokenEquipoHash, sha256(tokenEquipo))) {
    throw new ErrorNegocio("Ese correo ya está en uso por otro celular", 409);
  }
  // Un celular tiene una sola cuenta: si cambió de correo, se borra la anterior.
  for (const otra of cuentas.todos()) {
    if (otra.correo !== correo && iguales(otra.tokenEquipoHash, sha256(tokenEquipo))) cuentas.borrar(otra.correo);
  }
  const ahora = new Date().toISOString();
  cuentas.guardar(correo, {
    correo,
    idEquipo,
    sal: salHex,
    iteraciones,
    bloque,
    verificadorHash: sha256(verificador),
    tokenEquipoHash: sha256(tokenEquipo),
    creada: previa?.creada ?? ahora,
    actualizada: ahora,
  });
  return { correo };
}

/** El celular quita su cuenta de rescate. */
export function borrarCuenta(cuerpo: Record<string, unknown>): void {
  const tokenEquipo = hex(cuerpo.tokenEquipo, "Token del equipo", 64);
  for (const c of cuentas.todos()) {
    if (iguales(c.tokenEquipoHash, sha256(tokenEquipo))) cuentas.borrar(c.correo);
  }
}

/** Entrar al panel: devuelve el bloque cifrado si el verificador coincide. */
export function entrar(cuerpo: Record<string, unknown>): { bloque: string; idEquipo: string } {
  const correo = normalizar(cuerpo.correo);
  const verificador = hex(cuerpo.verificador, "Verificador", 64);
  const ahora = Date.now();
  const f = fallos.get(correo);
  if (f && ahora - f.desde < VENTANA_MS && f.n >= INTENTOS_MAX) {
    throw new ErrorNegocio("Demasiados intentos. Espera 15 minutos y vuelve a probar.", 429);
  }
  const cuenta = cuentas.obtener(correo);
  if (!cuenta || !iguales(cuenta.verificadorHash, sha256(verificador))) {
    const actual = f && ahora - f.desde < VENTANA_MS ? f : { n: 0, desde: ahora };
    fallos.set(correo, { n: actual.n + 1, desde: actual.desde });
    throw new ErrorNegocio("Correo o contraseña incorrectos", 401);
  }
  fallos.delete(correo);
  return { bloque: cuenta.bloque, idEquipo: cuenta.idEquipo };
}
