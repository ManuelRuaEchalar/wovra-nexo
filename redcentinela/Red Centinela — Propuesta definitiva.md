# Red Centinela — Propuesta definitiva

National Tech Hackathon Bolivia 2026 · Sede Sucre · Oct 9, 2026 · @Manuel

## Resumen

Red Centinela convierte los celulares de una ciudad en una red que se cuida a sí misma: cada teléfono es a la vez un sensor (ubicación, movimiento, señal) y un enlace de comunicación que funciona con o sin internet.

La misma red que localiza tu celular robado, aunque no tenga datos o lo intenten apagar, mide la fila de gasolina, muestra dónde viene tu micro, avisa si tu amigo no llegó a casa y encuentra a tu perro. Los datos agregados y anónimos que genera tienen valor para negocios y para el gobierno, y eso financia el proyecto.

**Pitch en una frase:** "Los celulares de Sucre ya están en todas partes. Los conectamos para que se cuiden entre ellos y para que la ciudad, por fin, sepa lo que pasa en sus calles."

## Arquitectura en 3 capas

&#91;embedded content: arquitectura · 3 capas, 6 módulos\]

Los módulos de uso diario dan razones para tener la app; cada usuario se vuelve un nodo de la infraestructura compartida, y los datos agregados que esta produce se venden a negocios e instituciones.

## Capa 1 · Infraestructura

Se construye una sola vez y la reutilizan todos los módulos. Tiene tres partes.

**1. Sensado (qué sabe cada teléfono)**

| Fuente | Qué aporta | Funciona sin internet |
| --- | --- | --- |
| GPS | Ubicación, velocidad y rumbo | Sí (la primera ubicación tarda más) |
| Antena celular conectada | Ubicación aproximada, tipo de red, intensidad de señal | Sí |
| Redes WiFi visibles | Ubicación en interiores | Sí (no necesita conectarse) |
| Acelerómetro | Tipo de movimiento: quieto, caminando, en vehículo, paradas | Sí |
| Bluetooth (BLE) | Proximidad a otros teléfonos y accesorios de la red | Sí |
| Cámara frontal | Foto de quien tiene el teléfono robado | Sí |
| Estado del sistema | Batería, chip, encendido, modo avión | Sí |

**2. Comunicación en cascada (cómo salen los datos)**

Cada mensaje usa el mejor canal disponible y baja al siguiente si falla:

1. **Internet** (datos o cualquier WiFi): envía todo, incluidas fotos y registros acumulados.
2. **SMS**: sin datos, pero con chip. Envía mensajes cortos: coordenadas, hora y antena.
3. **Bluetooth entre teléfonos**: sin datos ni chip. Un paquete mínimo pasa al teléfono de la red más cercano, que lo guarda y lo reenvía cuando tenga conexión ("guardar y reenviar").
4. **Memoria local**: si no hay nada, el teléfono guarda todo cifrado y lo envía apenas se conecte.

**3. Servidor**

Recibe los datos, los anonimiza, los agrega por zona y hora, y genera los mapas en vivo. Ningún dato individual de ubicación se muestra a terceros.

## Módulo 1 · Escudo (robo y pérdida del celular)

El Escudo registra dónde está el celular y quién lo tiene desde el primer segundo, aunque no haya internet y aunque intenten apagarlo.

### El problema

- En Bolivia se reportan alrededor de 700 celulares robados por día ([Bloomberg Línea](https://www.bloomberglinea.com/2022/08/30/robo-de-celular-a-evo-desata-peleas-politicas-y-ciudadanos-se-quejan-de-la-inseguridad/)).
- Desde 2010 se acumulan 781.173 equipos robados y hurtados; La Paz lidera con 264.608, seguida de Santa Cruz y Cochabamba. El 35% de las víctimas son menores de edad o adolescentes ([eju.tv](https://eju.tv/?p=580225)).
- Flashear y desbloquear un Android robado cuesta unos 150 bolivianos ([Bloomberg Línea](https://www.bloomberglinea.com/2022/08/30/robo-de-celular-a-evo-desata-peleas-politicas-y-ciudadanos-se-quejan-de-la-inseguridad/)). Hay muy poco tiempo para actuar.

### Tres escenarios

| Escenario | Qué pasa | ¿La víctima lo nota? |
| --- | --- | --- |
| Arrebato | Te lo quitan de la mano y huyen | Sí, al instante |
| Robo discreto | Te lo sacan del bolsillo o la mochila | No, quizás horas después |
| Pérdida | Lo olvidas en el taxi o se cae | No, hasta que lo buscas |

En los dos últimos, el celular y su dueño se separan sin que nadie lo note. Por eso el Escudo necesita un punto de referencia que se quede con el dueño: el guardián.

### Requisito de entrada

La app solo se activa si el celular tiene PIN, patrón o huella. Con el teléfono bloqueado, el ladrón no entra a las apps; el Escudo se concentra en ubicar el equipo y registrar quién lo tiene.

### El guardián

Un objeto que se queda contigo, conectado por Bluetooth al celular:

- un llavero Bluetooth tipo iTag (1 a 3 USD, ya trae botón y pitido), o
- el celular de un amigo o familiar con quien vas, o una pulsera inteligente.

| Escenario | Qué detecta el guardián | Qué pasa |
| --- | --- | --- |
| Arrebato | Se corta el Bluetooth en segundos | El llavero pita; aprietas su botón y queda reportado como robado |
| Robo discreto | El celular se aleja aunque no lo sientas | El llavero pita en el momento, con el ladrón todavía cerca |
| Pérdida | Tú te alejas y el celular queda quieto | "Olvidaste tu celular": lo recuperas antes de irte |

### Cómo se reporta sin tu celular

1. **Botón del llavero**: la vía más rápida. Sin celular, sin internet, sin pedir nada a nadie.
2. **Celular de alguien de tu círculo**: un toque en "reportar robo de Juan".
3. **SMS o llamada a un número del servicio** desde cualquier teléfono, incluso uno básico, con tu número y una palabra clave.
4. **Página web**.

Además, la app guarda un rastro de ubicación cada pocos minutos. Aunque el reporte llegue horas después, se ve la ruta completa desde que el celular se separó de tu recorrido habitual.

### Registro de ubicación sin internet (prioridad 1)

El teléfono sabe dónde está aunque no tenga conexión:

| Fuente | Precisión | Nota |
| --- | --- | --- |
| GPS | 5–20 m al aire libre | Sin datos, la primera ubicación tarda de 30 s a unos minutos; en interiores puede fallar |
| Redes WiFi visibles | 20–50 m | Ideal dentro de casas y locales; el servidor traduce los identificadores a ubicación |
| Antena celular | 200 m a varios km | Respaldo; se traduce con bases como OpenCellID |

Todo se guarda cifrado en el teléfono y sale por la cascada de la Capa 1: SMS con la última ubicación, paquete Bluetooth a teléfonos de la red que pasen cerca, y el registro completo en cuanto haya internet. Así se reconstruye la ruta entera, no solo el último punto.

| Momento | Frecuencia de registro |
| --- | --- |
| Primeros 15 minutos tras el reporte | Cada 15–30 segundos |
| Primera hora | Cada 2 minutos |
| Después | Cada 10–15 minutos |
| Batería menor al 10% | Último SMS con la ubicación y modo mínimo |

### Si intentan apagarlo

1. **Antes de que lo apaguen**, la ubicación y las primeras fotos ya salieron.
2. **Apagado falso**: la pantalla simula apagarse y el teléfono sigue registrando. Ya existe en otras apps ([Hammer Security](https://www.eluniversal.com.mx/english/mexican-app-be-safe-all-times), [Cerberus](https://www.apkmirror.com/?p=262449)); se incluye, pero no es el diferencial.
3. **Si lo apagan de verdad**: para flashearlo hay que encenderlo. En ese instante, el teléfono reporta su ubicación antes de que borren el sistema.
4. **Señales sospechosas** que se registran: modo avión o datos apagados sin haber desbloqueado, intentos fallidos de desbloqueo, retiro o cambio de chip. Si cambian el chip, el teléfono envía un SMS desde el chip nuevo, lo que revela ese número.

### El celular siempre alerta: mapa de puntos de flasheo

Si muchos celulares robados "despiertan" en el mismo lugar, el mapa revela dónde se flashean. Es información para las autoridades, nunca para que la gente vaya a enfrentarse a nadie. Con el tiempo, vuelve riesgoso el paso que hoy hace rentable el robo.

### Fotos (prioridad 2)

La cámara frontal toma una foto al fallar un desbloqueo, al encenderse la pantalla en modo robado o al cambiar el chip. Las fotos se guardan en el teléfono y se envían con internet; el SMS avisa cuántas hay pendientes.

### Pérdida no es robo: modo encontrado

Si lo reportas como perdido, la pantalla bloqueada muestra "Este celular está perdido, toca aquí para avisar al dueño" y, opcionalmente, una recompensa. Quien lo encuentra puede avisarte sin desbloquearlo y se propone un punto seguro de devolución. No toma fotos ni actúa agresivamente.

### Qué lo hace distinto

1. Detecta el **robo discreto y la pérdida en el momento**, no solo el arrebato.
2. **Denuncia con un botón físico**, sin tu celular y sin internet.
3. **Registra y entrega la ubicación sin internet**, por SMS o a través de otros teléfonos de la red, en cualquier Android con la app. La red de Google solo ubica teléfonos apagados en modelos compatibles como los Pixel 8, 9 y 10 ([Google](https://support.google.com/android/answer/3265955)).
4. **Distingue robo de pérdida** y actúa distinto en cada caso.
5. **Delata dónde se flashean** los equipos robados.

## Módulos 2 a 6 · Servicios de uso diario

Estos módulos dan razones para tener la app instalada todos los días, y cada usuario que entra por ellos se vuelve un nodo más de la red que protege celulares.

### Módulo 2 · Fila (combustible)

**Problema:** hay transportistas que esperan más de dos días por gasolina o diésel, y YPFB reportó doble carguío, placas clonadas y venta de lugares en la fila ([Correo del Sur, septiembre de 2026](https://correodelsur.com/opinion/20260927/la-fila-interminable.html)).

**Qué ve el usuario:** un mapa de surtidores con su estado (sin fila, fila corta, fila larga, sin combustible), el tiempo de espera estimado y si la fila crece o se achica.

**Cómo se detecta sin que nadie reporte:** varios teléfonos detenidos o avanzando muy despacio en las cuadras alrededor de un surtidor forman una fila. El largo sale de la distancia entre el primero y el último; la espera, de la velocidad a la que avanzan.

**Fase posterior:** turno virtual con QR, con acuerdo de surtidores o YPFB, contra el doble carguío y la venta de lugares.

**Competencia:** [Fulealo](https://mwm.ai/apps/fulealo/6738454179) muestra en qué estaciones hay combustible. Red Centinela mide la fila real, en vivo y de forma automática.

### Módulo 3 · Movilidad (micros en vivo)

**Para el pasajero:** dónde viene mi micro y en cuántos minutos llega, qué tan lleno viene y qué línea me conviene para ir de A a B.

**Cómo se detecta:** el acelerómetro y el GPS reconocen el patrón de un micro (arranques, paradas cortas, velocidad típica). Varios pasajeros que se mueven juntos por la misma ruta son el mismo micro. La cantidad de teléfonos con Bluetooth a bordo estima qué tan lleno va. Con el tiempo, la IA aprende las rutas reales, que no siempre coinciden con las oficiales.

**Uso:** diario; casi todos se mueven en micro dos o más veces al día.

### Módulo 4 · Señal (cobertura real)

**Problema:** no sabes qué operadora funciona mejor en tu barrio, tu ruta o tu trabajo, ni dónde se cortan las llamadas.

**Cómo:** cada teléfono registra en segundo plano la intensidad de señal, el tipo de red (2G a 5G) y la operadora según la ubicación. Miles de teléfonos arman un mapa real de cobertura.

**Para el usuario:** "en tu ruta diaria, otra operadora tiene mejor señal que la tuya".

### Módulo 5 · Llegué

**Problema:** volver a casa de noche y que nadie sepa si llegaste.

**Cómo:** activas "voy a casa" y la app conoce tu ruta y tu hora estimada. Si te detienes demasiado, te desvías mucho o el celular se apaga de golpe, primero te pregunta a ti. Si no respondes, avisa a tu círculo con tu última ubicación, por internet, SMS o Bluetooth.

**Diferencia:** compartir ubicación obliga a que alguien esté mirando. Aquí nadie tiene que vigilar: el sistema avisa solo cuando algo anda mal.

### Módulo 6 · Huellita (mascotas y objetos)

Un collar o llavero con chip Bluetooth de 2 a 3 USD para el perro, la mochila del niño o las llaves. Si se pierde, la misma red que busca celulares lo busca, y cualquier teléfono con la app que pase cerca reporta dónde está.

## Capa 3 · Datos para negocios y gobierno

La red produce, sin encuestas ni sensores instalados, algo que hoy casi nadie tiene en Bolivia: cuánta gente pasa por cada calle, a qué hora, cómo se mueve y cuánto espera. Eso le sirve a cualquiera que decida dónde y cuándo atender público.

### Quién compra qué

| Cliente | Pregunta que responde | Dato de la red |
| --- | --- | --- |
| Emprendedor o franquicia | ¿Dónde abro mi local? | Tránsito de peatones por cuadra y por hora |
| Comercios y restaurantes | ¿A qué hora abro, cuánto personal necesito? | Picos de movimiento por día y hora en su zona |
| Inmobiliarias | ¿Cuánto vale este local comercial? | Flujo de gente frente al inmueble comparado con la zona |
| Publicidad exterior | ¿Cuánto vale este cartel? | Personas y vehículos que pasan frente a él |
| Bancos | ¿Dónde pongo un cajero o un agente? | Zonas con mucho movimiento y poca oferta |
| Delivery y logística | ¿Cuánto tarda realmente una entrega? | Tiempos de viaje reales por hora y zona |
| Hoteles, agencias y restaurantes turísticos | ¿Por dónde se mueven los turistas? | Recorridos, tiempo de permanencia y lugares poco visitados (usuarios en roaming) |
| Organizadores de eventos | ¿Cuánta gente llegó y por dónde? | Densidad de dispositivos antes, durante y después |
| Aseguradoras | ¿Cuánto cobro por asegurar un celular? | Zonas y horarios con más robos reportados |
| Operadoras | ¿Dónde está floja mi red? | Mapa de cobertura del módulo Señal |
| Surtidores y transportistas | ¿A qué surtidor voy, cuándo cargo? | Filas y tiempos de espera del módulo Fila |
| Alcaldía | ¿Qué rutas de micro faltan o sobran? | Rutas reales, frecuencias, origen y destino de viajes |
| Gobernación y turismo municipal | ¿Cuántos turistas hay y qué visitan? | Flujos de visitantes por atractivo y temporada |
| YPFB y ANH | ¿Dónde y cuándo falta combustible? | Espera por surtidor y por día |
| ATT | ¿Cumplen las operadoras con la cobertura? | Mapa de cobertura independiente |
| Autoridades de seguridad | ¿Dónde se roba y dónde se flashea? | Zonas de robo y puntos de reactivación de equipos robados |

La necesidad es real: el secretario municipal de turismo de Sucre reconoció que el municipio no maneja números precisos y que recién iniciaba un observatorio de turismo ([Correo del Sur](https://correodelsur.com/capitales/20190820/debemos-visibilizar-a-sucre-a-nivel-internacional-en-el-rubro-del-turismo.html)).

### Cómo venderlo en la presentación

Mostrar un caso concreto, no una lista:

> "María quiere abrir una cafetería en Sucre. Duda entre dos calles. Con Red Centinela ve que la primera tiene el doble de gente entre las 7 y las 9 de la mañana, y la segunda se llena de estudiantes a mediodía. Decide dónde abrir y en qué horario, antes de firmar un alquiler."

Y cerrar con la idea fuerza: **lo que antes requería encuestas costosas o cámaras instaladas, aquí sale de los celulares que la gente ya lleva en el bolsillo.**

### Formatos de venta

- **Panel web por suscripción**: mapas de calor por zona y hora, comparación de calles.
- **Informes a medida**: estudio de ubicación para un local, un evento o una campaña.
- **API**: para empresas que integran los datos en sus sistemas (delivery, logística, aseguradoras).

Todos los datos se entregan agregados y anónimos (ver Principios de privacidad).

## Modelo de negocio y arranque de la red

El ciudadano no paga por la red; la pagan quienes usan sus datos y quienes venden accesorios.

| Fuente de ingreso | Quién paga | Qué recibe |
| --- | --- | --- |
| Datos agregados | Negocios, inmobiliarias, publicidad, bancos, logística | Panel, informes y API (Capa 3) |
| Convenios institucionales | Alcaldía, Gobernación, YPFB, ATT | Datos de movilidad, turismo, combustible y cobertura |
| Accesorios | Usuarios | Llavero guardián y collar Huellita, con margen sobre un costo de 1 a 3 USD |
| Alianzas | Aseguradoras | Usuarios con Escudo y guardián activos, de menor riesgo |

### Cómo se arranca una red vacía

Una red solo sirve si hay gente. El problema se resuelve con módulos que funcionan desde el primer usuario:

- **El guardián y Llegué** funcionan con un círculo de 2 a 5 personas; no necesitan a toda la ciudad.
- **Fila y Movilidad** dan valor diario y atraen usuarios en volumen.
- **Lanzamiento por comunidades**: universidades de Sucre, cooperativas de transporte y sindicatos de conductores, donde muchos usuarios entran juntos.

Cada usuario que entra por un módulo diario se vuelve, sin hacer nada más, un nodo que ayuda a encontrar celulares y mascotas.

## Principios de privacidad

La red solo funciona si la gente confía en ella, y el jurado lo va a preguntar.

1. **Identificadores anónimos que rotan**: la señal Bluetooth de cada teléfono cambia de identificador cada pocos minutos; nadie puede seguir a una persona por ella.
2. **Cada uno ve solo lo suyo**: la ubicación de tu celular, tu mascota o tu recorrido de Llegué solo la ven tú y tu círculo.
3. **Datos de negocio siempre agregados**: se venden conteos por zona y hora, nunca recorridos individuales, y no se publica ningún dato de zonas con muy pocas personas.
4. **Nada de rastrear a otros**: la app no puede instalarse a escondidas ni seguir el celular de otra persona sin que lo sepa. Algunas apps antirrobo fueron señaladas como herramientas de espionaje ([Certo](https://www.certosoftware.com/insights/popular-android-app-allegedly-used-for-hidden-surveillance/)); Red Centinela se diseña para no serlo.
5. **Información de robos para autoridades, no para particulares**: el mapa de flasheo no se publica para que nadie haga justicia por mano propia.

## Plan para la hackathon

Para el 28 de noviembre se construyen funcionando la infraestructura básica, el Escudo y uno de los módulos diarios; el resto se muestra como visión.

### Alcance

| Parte | Estado en la demo |
| --- | --- |
| Capa 1: registro local cifrado y cascada internet → SMS → Bluetooth | Funcional |
| Escudo: guardián con llavero, reporte por botón y SMS, registro de ubicación sin internet, apagado falso, foto | Funcional |
| Fila o Movilidad, con datos reales tomados por el equipo en Sucre | Funcional |
| Señal, Llegué y Huellita | Maqueta de pantallas |
| Panel de datos para negocios | Maqueta con datos simulados |

### Guion de la demo (3 minutos)

1. **Robo discreto**: un integrante saca el celular del bolsillo de un jurado que lleva el llavero. Al alejarse unos metros, el llavero pita.
2. **Reporte sin celular**: el jurado aprieta el botón del llavero. En la pantalla grande aparece "Celular reportado como robado".
3. **Sin internet**: el "ladrón" apaga los datos y camina. Al teléfono del círculo llega un SMS con la ubicación; cuando el robado recupera conexión, aparece en el mapa la ruta completa que hizo sin internet.
4. **Intento de apagado**: el "ladrón" intenta apagarlo; la pantalla simula apagarse y llega su foto.
5. **La misma red, para todo**: el mapa de Sucre muestra la fila real de un surtidor (o un micro en vivo) y el caso de María eligiendo dónde abrir su cafetería.

### Reparto de roles

| Integrante | Rol | Responsabilidades |
| --- | --- | --- |
| A | Móvil | App Android: registro de ubicación, cascada de envío, Bluetooth, guardián, foto, apagado falso |
| B | Servidor y datos | Servidor, mapa en vivo, recepción de SMS, detección de filas o micros, panel de negocios |
| C | Producto y negocio | Diseño de pantallas, pruebas en la calle, toma de datos reales, modelo de negocio, pitch y ensayo de la demo |

### Cronograma

&#91;embedded content: cronograma · 6 semanas, 3 hitos\]

La primera semana se dedica entera a validar los riesgos técnicos: si algo falla ahí, hay tiempo de cambiar el diseño antes de construir el resto.

## Riesgos técnicos y mitigación

Ninguno impide la demo, pero todos se validan en la primera semana, en los celulares del propio equipo.

| Riesgo | Por qué importa | Mitigación |
| --- | --- | --- |
| Ubicación en segundo plano | Android exige el permiso "Permitir todo el tiempo" y un servicio activo | Servicio en primer plano con notificación fija; explicar bien el permiso al usuario |
| Cámara en segundo plano | Desde Android 9 no se puede usar sin estar en primer plano, y Android 11 restringe más | Disparar la foto con el evento de desbloqueo fallido y una pantalla propia; probar en cada modelo |
| Envío automático de SMS | Permiso especial muy controlado por Google Play | En el prototipo, instalar el APK directamente; para publicar, justificar el uso |
| Ahorro de batería de Xiaomi, Samsung y otras marcas | Cierran apps en segundo plano | Guía en la app para excluirla de la optimización |
| Bluetooth en segundo plano y consumo de batería | Escanear todo el tiempo gasta batería | Ciclos de escaneo cortos y emisión de baja potencia |
| Modo avión activado desde la pantalla bloqueada | Corta la comunicación del celular robado | Recomendar desactivar los ajustes rápidos en la pantalla bloqueada, donde la marca lo permita; el registro local sigue con GPS |
| iPhone | Apple limita mucho el Bluetooth en segundo plano | Empezar solo con Android, que es la mayoría en Bolivia |
| Celular ya flasheado | Al borrarse el sistema, la app desaparece | Asumirlo: el valor está en los primeros minutos y en el mapa de puntos de flasheo |

## Fuentes

- [Bloomberg Línea: robo de celulares y flasheo en Bolivia (2022)](https://www.bloomberglinea.com/2022/08/30/robo-de-celular-a-evo-desata-peleas-politicas-y-ciudadanos-se-quejan-de-la-inseguridad/)
- [eju.tv: 781.173 celulares robados y hurtados desde 2010](https://eju.tv/?p=580225)
- [Correo del Sur: "La fila interminable", editorial de septiembre de 2026](https://correodelsur.com/opinion/20260927/la-fila-interminable.html)
- [Fulealo en la App Store](https://mwm.ai/apps/fulealo/6738454179)
- [Correo del Sur: entrevista al secretario municipal de turismo de Sucre](https://correodelsur.com/capitales/20190820/debemos-visibilizar-a-sucre-a-nivel-internacional-en-el-rubro-del-turismo.html)
- [Google: red Find Hub y ubicación de dispositivos apagados](https://support.google.com/android/answer/3265955)
- [El Universal: Hammer Security y su apagado falso](https://www.eluniversal.com.mx/english/mexican-app-be-safe-all-times)
- [Cerberus: apagado falso (versión 3.5)](https://www.apkmirror.com/?p=262449)
- [Certo: Cerberus señalada como herramienta de espionaje](https://www.certosoftware.com/insights/popular-android-app-allegedly-used-for-hidden-surveillance/)
