# Red Centinela · Escudo (APK Android)

Módulo **Escudo antirrobo** de la propuesta *Red Centinela* (National Tech
Hackathon Bolivia 2026), en Android nativo con Kotlin y Jetpack Compose,
siguiendo las líneas de diseño **Halo + Nocturno**.

## Qué hace hoy

| Parte de la propuesta | Estado |
| --- | --- |
| Registro de ubicación sin internet, cifrado en el teléfono, con frecuencia adaptativa | Funciona |
| Círculo: vincular personas por QR o invitación | Funciona |
| Reportar el celular de alguien del círculo (robo / pérdida) desde el propio | Funciona |
| Cascada internet → SMS → Bluetooth → memoria local | Funciona (ver notas) |
| Seguimiento en vivo con mapa y ruta completa | Funciona |
| Búsqueda de cerca por Bluetooth ("caliente/frío") y hacer sonar | Funciona |
| Búsqueda con dirección (girar con la brújula), sonar + linterna y órdenes por Bluetooth directo | Programado en la 0.3, **sin probar en teléfonos** |
| Alerta sola al quedar aislado; apagado falso con prioridad sobre la pantalla de perdido | Programado en la 0.3, **sin probar en teléfonos** |
| Guardián: celular de quien va contigo | Funciona |
| Guardián: llavero Bluetooth tipo iTag | Programado, **sin probar con un llavero real** |
| Apagado falso | Funciona con el permiso de accesibilidad (ver notas) |
| Modo encontrado (pantalla sobre el bloqueo) | Funciona |
| Fotos con la cámara frontal, desbloqueo fallido | No incluido |
| Reporte por web / número de servicio, mapa de flasheo | No incluido (rol Servidor) |
| Ciudad (Fila, Micros, Señal), Llegué, Huellita | Solo aparecen como "Próximamente" |

## Cómo funciona de verdad

- **Círculo.** Cada teléfono genera un identificador y un secreto. Al escanear
  el QR de otra persona recibes los suyos; con internet, tu teléfono le manda
  los tuyos y quedan vinculados en ambos sentidos. Con el secreto de alguien
  puedes leer la ubicación de su celular *cuando está reportado* y darle
  órdenes (reportar, hacer sonar, terminar la alerta).
- **Internet.** No hay servidor propio todavía: los teléfonos se hablan por un
  servidor de relevo con protocolo [ntfy](https://ntfy.sh) (por defecto
  `https://ntfy.sh`, cambiable en Ajustes → Conexión). Todo viaja cifrado
  (AES-GCM) con el secreto del equipo; el relevo solo ve texto cifrado y guarda
  los mensajes ~12 h, así quien estuvo sin conexión recibe la ruta completa.
  El servicio público limita a unos 250 mensajes por día por conexión.
- **SMS.** Sin datos pero con chip, el celular reportado manda su ubicación
  por SMS a un máximo de 2 personas del círculo (cada 3 min al inicio, luego
  cada 10). El SMS trae un enlace al mapa y una cola firmada que la app del
  destinatario lee sola. Las órdenes también viajan por SMS si no hay internet.
  Requiere guardar los números (el propio en el perfil, y el de cada contacto).
- **Bluetooth.** Con el Escudo activo, cada teléfono emite un aviso BLE. Sirve
  para el guardián, para la búsqueda de cerca y como relevo: en alerta el aviso
  lleva la ubicación cifrada, y cualquier teléfono con la app que pase cerca
  la reenvía por internet sin poder leerla.
- **La ubicación no sale del teléfono en el día a día.** Solo se comparte al
  estar reportado.

## Compilar e instalar

Requiere JDK 17+ y el SDK de Android (plataforma 34). Con el teléfono
conectado por USB con depuración activada:

```bash
./gradlew installDebug        # compila e instala
./gradlew assembleDebug       # solo el APK: app/build/outputs/apk/debug/app-debug.apk
```

Instalando por USB (adb) Android permite activar los permisos de SMS y de
accesibilidad sin restricciones. Si pasas el APK por WhatsApp o Drive, en
Android 13+ quizá debas abrir *Información de la app → ⋮ → Permitir ajustes
restringidos*.

## Probar (2 teléfonos con la app; llamémoslos A = protegido, B = amigo)

**0. Preparación (en ambos).** Completa el inicio: nombre y número, bloqueo de
pantalla, permisos (ubicación "todo el tiempo", Bluetooth, SMS,
notificaciones, sin ahorro de batería) y *Activar Escudo*. Ten encendidos
Bluetooth y ubicación.

**1. Agregar al amigo.** En A: *Círculo → +*; se muestra su QR. En B: *Círculo
→ + → Escanear su código* y apunta al QR de A. Con internet, A recibe solo a B
en unos segundos (notificación "… se unió a tu círculo"). Sin internet, A
escanea también el QR de B.

**2. Reportar y seguir (con internet).** En B: *Círculo → Reportar* junto a A
→ *Se lo robaron* → *Reportar robo…*. A pasa a "Reportado como robado" y B ve
el mapa con la ruta, que avanza cada ~20 s. Camina con A para ver el trazo.

**3. Sin internet (SMS).** Apaga datos y WiFi en A (A necesita chip con
saldo, y B debe tener guardado el número de A y viceversa). Reporta desde B:
la orden sale por internet y, si A no responde en 25 s, B insiste por SMS. A
responde con un SMS con su ubicación; en B el punto aparece hueco y "Llegó
por: SMS". Al devolverle internet a A, B recibe la ruta completa que A
guardó mientras tanto.

**4. Buscar de cerca.** En B, en el seguimiento de A: *Buscar de cerca*. B le
pide a A que emita su aviso Bluetooth al máximo (dura 3 min y se renueva).
Los pitidos se aceleran de forma continua al acercarte. Para saber **hacia
dónde**: sostén B frente al pecho y gira despacio una vuelta completa; el
anillo se va llenando por direcciones y aparece una flecha hacia donde la
señal llega más fuerte (tu cuerpo la tapa cuando le das la espalda a A).
Camina unos pasos y vuelve a girar. *Hacer sonar* hace sonar a A a todo
volumen, con la linterna parpadeando, aunque esté en silencio.

**4b. Sin internet en ninguno de los dos.** Apaga datos y WiFi en A y en B y
repite el paso 4: con A a la vista por Bluetooth, *Hacer sonar* y *Reportar*
llegan por conexión Bluetooth directa (el mensaje dice "Recibido por
Bluetooth"). Si reportaste a A cuando estaba lejos y sin red, el reporte
queda pendiente 12 h y B se lo entrega solo en cuanto capte su Bluetooth.

**5. Guardián.** En A: *Círculo → B → Es mi guardián* (con internet, B queda
como guardián solo; sin internet, en B activa *Soy su guardián* en la ficha de
A). Separa los teléfonos unos 20–30 m: a los ~25 s B suena y ofrece *Reportar
robo*. En la pantalla de A el guardián pasa a "fuera de alcance".

**6. Apagado falso.** En A: *Ajustes → Activar* en el aviso de accesibilidad y
enciende "Red Centinela · Apagado falso". Luego **bloquea A** y mantén
presionado el botón de encendido: en vez del menú de apagado aparece
"Apagando…" y la pantalla queda en negro, pero A sigue encendido, pasa a
"robado" y B recibe la alerta y la ubicación. Para salir: toca la pantalla
negra **5 veces seguidas** y desbloquea. Con A desbloqueado el botón funciona
normal (el dueño sí puede apagarlo). Para ver solo el efecto visual: *Ajustes
→ Probar apagado falso*.

**6b. Apagado falso contra pantalla de perdido.** Con A en apagado falso,
repórtalo desde B como *Lo perdió*: A debe seguir en negro (mientras haya
internet, SMS o un Bluetooth del círculo cerca, conviene que parezca
apagado). Al revés: con la pantalla de perdido visible, mantén el botón de
encendido en A: pasa a apagado falso y a "robado". La pantalla de perdido
solo reemplaza al apagado falso cuando A lleva sin ningún canal el tiempo de
*Ajustes → Alerta sola si queda aislado* (ponlo en 2 min para probar: A en
apagado falso, modo avión y B lejos o con Bluetooth apagado).

**6c. Alerta sola.** En A: B como guardián y *Alerta sola* en 2 min. Pon A en
modo avión con Bluetooth encendido, bloquéalo y aleja B (o apágale el
Bluetooth). A los ~25 s A anota "Se separó del guardián" y a los 2 min entra
en "robado" por su cuenta: su aviso Bluetooth pasa a llevar la ubicación
cifrada y al recuperar red envía toda la ruta.

**7. Pérdida.** En B: *Reportar → Lo perdió*. A muestra sobre el bloqueo
"Este celular está perdido"; al tocar *avisar*, B recibe la notificación con
la ubicación. Termina con *Ya lo recuperó* (en B) o *Lo recuperé* (en A).

**Con un solo teléfono:** *Ajustes → Probar el Escudo* permite simular robo,
pérdida, retiro de chip y el apagado falso; *Escudo → Ruta guardada* muestra el
registro propio.

## Límites conocidos

- El apagado falso depende de reconocer el menú de apagado de cada marca; se
  programó para Android puro y MIUI/HyperOS, pero **no se pudo comprobar en un
  equipo bloqueado** durante el desarrollo. Un apagado forzado (botón ~10 s)
  no se puede impedir: al encender de nuevo y desbloquear, el Escudo retoma la
  alerta.
- Bluetooth no puede confirmar que alguien lo escucha: para decidir si "queda
  comunicación" cuentan un envío entregado por internet o SMS, una orden
  recibida, o haber captado el Bluetooth de alguien del círculo.
- La alerta sola solo actúa si hay un guardián configurado; sin guardián, un
  equipo aislado solo entra en alerta por apagado o retiro de chip.
- La flecha de dirección depende de que el cuerpo tape la señal: en cuartos
  pequeños con muchos rebotes puede no aparecer (hace falta 6 dB de contraste).
- Los reportes pendientes de entregar por Bluetooth se pierden si se cierra la
  app de quien reportó.
- Tras reiniciar, Android no deja correr la app hasta el primer desbloqueo.
- El identificador Bluetooth todavía no rota (la propuesta lo pide por
  privacidad).
- El relevo público es para el prototipo; el servidor propio del equipo puede
  reemplazarlo implementando los mismos dos endpoints.

## Estructura

```
domain/   modelos, puertos y lógica: EscudoCoordinator (estados y reacción a
          señales), CommCascade (cadena de canales), ReportingPolicy,
          ProximityEstimator
data/     ble/ (aviso y escaneo BLE, llavero), circle/ (círculo y
          sincronización), comm/ (eslabones de la cascada y SMS), crypto/,
          net/ (relevo), shutdown/ (señales del sistema y apagado falso),
          storage/ (bitácora cifrada, registro de eventos)
service/  EscudoService (primer plano), GuardianWatch, Notifier, ServiceLocator
ui/       Compose: theme/ (Halo + Nocturno), component/, screen/
```

minSdk 26 (Android 8.0) · targetSdk 34 · Kotlin 1.9 · Compose · Material 3.
