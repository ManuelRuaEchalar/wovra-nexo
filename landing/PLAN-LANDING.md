# Red Centinela · Plan de landing y primeros borradores

Estudio local · 11 de noviembre de 2026 · Propuesta para discusión, sin publicación.

## Decisión de partida

Recomiendo una landing que entre por **Escudo** y se abra hacia **la red de ciudad**. La persona debe comprender primero qué puede aportar a su vida; después, cómo su participación ayuda a otros. Titular recomendado: **«Lo tuyo, cerca. Tu ciudad, conectada.»**

La audiencia provisional son ciudadanos de Sucre con Android. Aliados, jurado y negocios tienen un recorrido secundario. La primera acción es entender Escudo mediante una demostración; la conversión futura sería solicitar participar en un piloto. Hasta confirmar que existe una convocatoria, la landing no debe ofrecer una descarga pública ni una inscripción operativa.

La pregunta que guía la página: **¿por qué querría llevar esta app y confiar en ella?** La respuesta debe unir utilidad concreta, cooperación y control de los datos.

## Lo que extraigo de las referencias

- **Propuesta definitiva:** un sistema de tres capas; Escudo como entrada; cinco servicios que amplían la utilidad; financiación mediante datos agregados, convenios y accesorios; principios de privacidad y restricciones técnicas.
- **Líneas de diseño:** sistema Halo + Nocturno; fondos claros para el día a día, verde profundo en momentos importantes; anillos inclinados −14°; títulos ligeros Sora y texto Hanken Grotesk; ámbar reservado para alertas.
- **Anuncios 3D:** dos relatos complementarios. Uno sigue un celular desde el registro hasta su localización; otro recorre lugares de Sucre a través de los módulos. Conviene traducirlos a dos momentos de la landing, en vez de reproducir todos los anuncios de principio a fin.
- **Concepts:** luz suave, superficies fluidas, profundidad y horizontes. Su aporte más fuerte es una atmósfera viva y tranquila. Las formas deben expresar alcance, señal o cooperación, para que el producto siga siendo reconocible.

Los HTML originales contienen documentos anidados empaquetados; se revisó el contenido interno del sistema, las pantallas y ambos anuncios, además de la propuesta escrita y todas las imágenes de concepts.

## Estructura propuesta, en orden

| Parte | Qué debe comprender el visitante | Contenido y presentación | Acción |
| --- | --- | --- | --- |
| Cabecera | Qué es y dónde encontrar lo esencial | Marca, Escudo, ciudad, privacidad; navegación breve | Ser parte |
| Portada | Qué propone Red Centinela | Titular, explicación de dos frases, teléfono con Escudo y halo; origen en Sucre y estado de desarrollo | Descubrir Escudo |
| Escudo en una situación concreta | Qué ocurre si su celular se separa | Tres momentos: guardián, registro, entrega del rastro; maqueta identificada como simulación | Explorar los pasos |
| Un mensaje, varios caminos | Qué significa «sin internet» | Internet → SMS → Bluetooth → almacenamiento; requisitos de cada canal visibles | Comprender límites |
| Una red, más usos | Por qué tendría valor todos los días | Un módulo a la vez: Fila, Micros, Señal, Llegué, Huellita; estado junto al contenido | Cambiar de módulo |
| Confianza | Quién puede ver qué | Círculo autorizado, datos agregados, permisos y participación consciente | Consultar principios |
| Estado del proyecto | Qué existe y qué viene después | Escudo como primer enfoque; separar prototipo, piloto y visión | Explorar una alianza |
| Preguntas | Resolver las dudas que impiden confiar | Sin internet, apagado real, requisitos Android, financiación | Desplegar respuestas |
| Cierre | Cómo dar el siguiente paso | Una invitación sencilla desde Sucre | Probar / colaborar, cuando haya convocatoria |
| Pie | Quién responde y dónde consultar detalles | Contacto real, responsable, privacidad y condiciones cuando estén definidos | Contactar |

Esta secuencia cabe en una página de extensión media. Si crece demasiado, mover la arquitectura completa y el modelo institucional a páginas secundarias. No asignar una sección extensa a cada módulo ni convertir el inicio en un catálogo de seis productos equivalentes.

## Contenido y tono

**Portada recomendada**

> Lo tuyo, cerca. Tu ciudad, conectada.
>
> Una red de celulares que se ayudan entre sí. Empezamos por proteger tu teléfono y conectamos esa misma idea con la vida de la ciudad.
>
> Descubrir Escudo · Cómo funciona la red

**Escudo:** «Si tu celular se aleja, tu historia no se pierde.» Explicar el beneficio antes que GPS, BLE o protocolos. Mostrar un ejemplo cotidiano de separación y aviso; evitar escenas agresivas y estetizar el robo.

**Red:** «Un mensaje. Más de un camino.» Aclarar que sin internet no significa comunicación garantizada, que SMS requiere servicio y que Bluetooth necesita participantes cercanos.

**Ciudad:** «Cuidar lo tuyo. Conectar tu ciudad.» El servicio diario atrae personas; esas personas hacen posible una red más útil. No representar cobertura real ni números de participantes sin evidencia.

**Privacidad:** «Tu recorrido merece su propio círculo.» El modelo de negocio debe tener una explicación visible, porque ocultarlo hasta una política extensa dañaría la confianza.

Usar español cercano al contexto boliviano: celular, micro, surtidor, círculo. Sucre aporta origen y referencias concretas; no implica que ya exista un convenio municipal.

## Tres direcciones visuales

### A · Halo cercano — recomendada

[Ver borrador](borradores/halo.html)

Portada clara de dos columnas, texto protagonista y teléfono rodeado de anillos. Nocturno aparece en la pantalla y en la sección ciudad. La calma y la legibilidad favorecen entender y confiar. Es la continuidad más directa con el diseño de la app.

**Materiales:** superficies mate, verde muy claro, líneas finas, luz difusa. Una sola escena 3D o imagen propia puede sustituir después la maqueta CSS.

**Movimiento:** respiración lenta del halo, señal breve al cambiar de paso. El contenido no depende de la animación. El primer borrador mantiene los anillos estáticos para valorar primero la composición.

### B · Aurora viva

[Ver borrador](borradores/aurora.html)

Portada nocturna, título centrado, aurora amplia y teléfono que emerge desde abajo. Pensada para una presentación más emocional y una pieza de lanzamiento con movimiento.

**Materiales:** luz volumétrica menta, base verde profunda, gradaciones amplias. El prototipo ya incorpora una deriva suave de luz en CSS, con respeto a la preferencia de movimiento reducido.

**Compromiso:** puede parecer una campaña tecnológica genérica si la aurora desplaza al teléfono y a la explicación. Conservar la especificidad de Escudo y Sucre.

### C · Horizonte conectado

[Ver borrador](borradores/horizonte.html)

Composición editorial nocturna; el gráfico principal muestra personas y dispositivos como enlaces de una misma red. Favorece explicar la visión a aliados y a un jurado.

**Materiales:** horizonte luminoso, nodos discretos, conexiones finas. Esquema abstracto y rotulado, no mapa de cobertura real. Una futura escena de Sucre debería basarse en material o geometría del proyecto, sin inventar monumentos.

**Compromiso:** la infraestructura puede sentirse distante de la necesidad individual. Por eso Escudo permanece inmediatamente después.

Los tres borradores comparten contenido inferior para comparar con claridad las portadas. Son exploraciones de dirección, no tres arquitecturas distintas ni diseños finales de producción.

## Cómo aprovechar las imágenes de concepts

| Archivo | Qué tomar | Aplicación sugerida |
| --- | --- | --- |
| Screenshot From 2026-11-11 12-06-15.png | Flujo suave, transiciones amplias, pocas aristas | Aurora en menta y bruma para portada o cierre |
| Screenshot From 2026-11-11 12-06-57.png | Aro energético y profundidad sobre fondo oscuro | Halo de Escudo o aparición de una conexión; reducir su brillo dorado a destello verde |
| sea_abstract.jpeg | Horizonte bajo y ondas serenas | Separador entre protección individual y red de ciudad |
| 0472b91305c72c4502dcd9efda6e1f4f.jpg | Luz que atraviesa un campo oscuro | Una señal que encuentra camino, con curvas menos dramáticas |
| images_abstract.jpeg | Relieve fino y vetas oscuras | Detalle de una pieza 3D, con intensidad muy reducida |
| images.jpeg | Volumen fluido y reflejos | Prueba secundaria para una campaña; moderar brillo y saturación |
| colorful-abstract-768x1024-25884.jpg | Capas líquidas y profundidad | Exploración de material para objeto; su complejidad compite con textos largos |

Las referencias permanecen en `concepts`; el tablero las enlaza para compararlas. Los fondos de los borradores se construyen con CSS y SVG originales, sin alterar los archivos descargados. No se han generado aún imágenes con IA. Para publicación, conviene producir materiales propios o confirmar las licencias de los descargados.

### Piezas visuales que producir después de elegir dirección

1. **Escudo / portada:** teléfono Android genérico y guardián pequeño, con halo inclinado −14°, fondo verde profundo o transparente según la composición. Reservar una zona de baja complejidad para el texto. Componer los textos de interfaz en HTML para mantenerlos legibles.
2. **Señal / cascada:** una misma luz atraviesa cuatro estados —internet, SMS, Bluetooth y espera local—. Si no hay salida, queda detenida; evitar que sugiera conexión permanente.
3. **Ciudad / cooperación:** nodos que se suman de forma gradual sobre un esquema de calles inspirado en Sucre. Si se muestra geografía real, usar una base verificable.
4. **Cierre:** ondas o aurora con el mismo material de la portada, más suave. No añadir una segunda estética.

**Brief visual para A:** «Teléfono Android genérico y pequeño llavero Bluetooth, composición de estudio, materiales mate, fondo verde niebla #F4F9F6, anillo elíptico fino inclinado −14 grados, luz menta #9FF0C4, sombras suaves, zona izquierda despejada para titular, sin letras ni logotipos generados».

**Brief visual para B:** «Aurora abstracta suave sobre verde profundo #0E3B36, flujo lento de luz #2BD07E y bruma #DDF5E7, profundidad atmosférica, sin neón violeta, sin estrellas ni elementos de ciencia ficción, centro superior de baja complejidad para texto».

**Brief visual para C:** «Horizonte bajo de ondas suaves en verde profundo, puntos de luz menta conectados con trazos finos que sugieren cooperación entre celulares, composición horizontal, espacio izquierdo para texto, escala humana, sin mapa de cobertura ni símbolos institucionales».

## Animación y formatos

Para halos, nodos y luz: CSS o SVG antes que vídeo. Para una escena 3D compleja: un bucle de vídeo mudo WebM/MP4 de unos 6–10 segundos, con imagen de portada y alternativa estática. GIF queda para compartir avances: suele ser pesado y pierde gradaciones en este tipo de fondos.

Máximo una escena en movimiento visible a la vez. Evitar scroll forzado, audio, parallax fuerte y ciclos rápidos. Pausar los vídeos fuera de pantalla y ofrecer control de pausa si la reproducción decorativa continua lo requiere. Respetar `prefers-reduced-motion`; en móvil, simplificar la escena y priorizar la imagen estática.

Objetivos iniciales, pendientes de medir con los archivos finales: imagen principal de unos 150–300 KB, vídeo opcional de 1–2 MB, carga diferida del resto y recursos responsive. Mantener la primera pantalla útil sin depender del vídeo o de JavaScript.

## Sistema visual y UX

- Paleta heredada: fondo `#F4F9F6`, tinta `#0B2A26`, noche `#0E3B36`, aurora `#2BD07E`, destello `#9FF0C4`, línea `#D3E5DB`. Ámbar `#E08A2E` solo para alertas explicadas, nunca como color de campaña general.
- Sora ligera en titulares y Hanken Grotesk en párrafos e interfaz. Los borradores usan las fuentes locales de la app y abren sin conexión.
- Anchos de lectura contenidos; espacio en blanco generoso; pocas superficies encerradas; botones con verbos concretos; estado del proyecto cerca de la promesa.
- En móvil: texto primero, escena después; navegación abreviada, secciones en una columna, módulos con controles que admiten salto de línea.
- Demostraciones manuales, sin avance automático. Controles nativos, estados accesibles y regiones de actualización anunciadas. Preguntas con `details` y `summary`.
- En la versión final, colocar un enlace secundario de reporte o acceso solo si ese servicio existe y está preparado para atenderlo. No simular una herramienta de emergencia operativa en una página promocional.

## Afirmaciones pendientes de resolver

1. **Disponibilidad:** los anuncios dicen «el antirrobo ya funciona en Android»; la propuesta describe un alcance previsto para la hackathon. Hace falta una matriz de funciones probadas, modelos y condiciones antes de publicar esa afirmación.
2. **Cuándo sale el registro:** el anuncio dice que la ruta no sale del teléfono hasta reportarlo; la propuesta habla de registros, servidor y usos agregados. Acordar el comportamiento real y explicarlo con precisión en producto y privacidad.
3. **Apagado real:** evitar «sigue funcionando apagado». Distinguir registro previo, apagado simulado y reanudación si el sistema lo permite. No garantizar recuperación ni funcionamiento tras borrado del sistema.
4. **Cámara, SMS y segundo plano:** presentar como capacidades sujetas a validación y permisos hasta confirmar su disponibilidad real en los dispositivos de destino.
5. **Datos de ciudad:** son una visión dependiente de participación, calidad y densidad. Los ejemplos de espera, cobertura y llegada deben marcarse como simulados hasta contar con datos reales.
6. **Convocatoria:** definir fecha, capacidad, contacto responsable, requisitos y tratamiento de datos. Los botones de interés de los borradores solo explican el recorrido propuesto y no recogen información.
7. **Modelo gratuito:** la propuesta dice que el ciudadano no paga por la red; confirmar qué incluye antes de convertirlo en una promesa comercial.

No incorporé las cifras históricas de robos ni afirmaciones comparativas de la propuesta: no hacen falta para esta primera narrativa y requerirían una revisión de fuentes y vigencia si se publican. Este estudio analiza el material entregado; no es una auditoría de las capacidades técnicas de la app.

## Próxima iteración recomendada

Elegir una portada, confirmar audiencia y estado real del prototipo, y sustituir la maqueta por capturas o renders propios. Después ajustar el texto público y preparar una única landing con convocatoria real. El detalle institucional podría vivir en una página complementaria para aliados, con el caso de la cafetería y ejemplos de información agregada.

## Archivos y revisión

Abrir `borradores/index.html` para ver el tablero, las tres portadas y las imágenes de referencia. Cada borrador contiene la misma página completa y permite explorar Escudo, cambiar módulos y abrir preguntas. No hay instalación, llamadas externas, formularios conectados ni publicación. El contenido y la geometría de las escenas son editables en HTML/CSS/JS.
