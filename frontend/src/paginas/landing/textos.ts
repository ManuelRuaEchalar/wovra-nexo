import type { TextosMapa } from './ciudad/escena'

/**
 * Textos del landing en español e inglés. Las dos versiones tienen la misma forma:
 * el tipo `Textos` sale del español y el inglés debe cumplirlo.
 *
 * La frase del hero es provisional.
 */

const es = {
  meta: {
    titulo: 'Dendra · Una red de celulares que se cuidan entre sí',
    descripcion:
      'Encuentra tu celular robado o perdido: los celulares de la red lo detectan por internet, wifi, SMS o Bluetooth y te avisan dónde está.',
  },
  hero: {
    titulo: 'Dendra',
    frase: 'Lo que pierdes, la red lo encuentra.',
    bajada: 'Cada celular con Dendra cuida a los demás. Por internet, wifi, SMS o Bluetooth, el tuyo siempre encuentra cómo avisarte dónde está.',
    /** "Stellar" se resalta con la luz de la marca. */
    stellar: ['Y quien te lo devuelva cobra una recompensa garantizada en ', 'Stellar', '.'],
    principal: 'Mira cómo lo encuentra',
    secundaria: 'Únete',
    pausar: 'Pausar el fondo',
    seguir: 'Reanudar el fondo',
  },
  ciudad: {
    frase: 'La misma red cuida tu ciudad.',
    intro: 'Cada celular es un punto de la red.',
    disponible: 'Disponible',
    pronto: 'Pronto',
    apps: [
      { nombre: 'Tu celular, robado o perdido', que: 'La red lo ubica por internet, wifi, SMS o Bluetooth. Quien lo devuelve cobra una recompensa en Stellar.' },
      { nombre: 'La fila del combustible', que: 'Cuántos autos esperan en cada surtidor, antes de salir de casa.' },
      { nombre: 'Tu micro', que: 'Dónde viene y qué tan lleno, medido por los celulares que van en él.' },
      { nombre: 'La señal', que: 'Qué operadora funciona mejor en cada cuadra, con datos de la gente.' },
      { nombre: 'Los apagones', que: 'Dónde se cortó la luz y desde cuándo, avisado por los propios vecinos.' },
      { nombre: 'Llegar a casa', que: 'Tu gente sabe que llegaste bien, sin que tengas que escribir.' },
      { nombre: 'Tu mascota', que: 'Un llavero en su collar y toda la red ayuda a encontrarla.' },
    ],
    mapa: {
      tu: 'Tú',
      celular: [
        { cifra: '±25', unidad: 'm', rotulo: 'Tu celular · visto hace 1 min' },
        { cifra: '50', unidad: 'Bs', rotulo: 'Recompensa garantizada en Stellar' },
      ],
      combustible: { cifra: '14', unidad: 'autos', rotulo: 'En fila · unos 25 min' },
      micro: { cifra: '4', unidad: 'min', rotulo: 'Línea A · va por la mitad' },
      apagon: { cifra: '19:40', rotulo: 'Sin luz · 32 vecinos avisaron' },
      casa: { cifra: '22:14', rotulo: 'Llegó a casa · tu círculo ya sabe' },
      mascota: { cifra: '2', unidad: 'min', rotulo: 'Huellita · la vio un celular cerca' },
    } as TextosMapa,
  },
  senal: {
    titulo: 'Así viaja la señal',
    pasos: [
      {
        titulo: 'Por datos móviles, wifi, SMS o Bluetooth, tu celular siempre encuentra la forma de decirte dónde está.',
        texto: '',
      },
      {
        titulo: 'Prepáralo en un minuto',
        texto: 'Instala Dendra, activa el Escudo y suma a tu círculo: las personas de confianza que podrán ayudarte a buscarlo.',
      },
      {
        titulo: 'Si lo pierdes o te lo roban',
        texto: 'Avísalo desde el celular de alguien de tu círculo o desde la página web. Solo eliges qué pasó.',
      },
      {
        titulo: 'Ofrece recompensa o finge apagarse',
        texto: 'Si lo perdiste, muestra cómo devolverlo y la recompensa que espera a quien lo encuentre. Si te lo robaron, finge apagarse y sigue enviando su ubicación.',
      },
      {
        titulo: 'Síguelo en el mapa',
        texto: 'Tu círculo ve su ubicación en vivo, cifrada. Aunque no tenga internet, cualquier celular con Dendra que pase cerca la capta por Bluetooth y la reenvía.',
      },
      {
        titulo: 'Ya estás cerca: la brújula te guía',
        texto: 'Gira una vuelta con tu celular: Dendra mide la señal y te indica la dirección y los metros que faltan.',
      },
      {
        titulo: 'Hazlo sonar',
        texto: 'Aunque esté en silencio, suena a todo volumen, parpadea la linterna y la pantalla se enciende en blanco. Lo encuentras dentro de un bolso, bajo un cojín o en la oscuridad.',
      },
      {
        titulo: 'Una recompensa garantizada en Stellar',
        texto: 'Al reportarlo como perdido, la recompensa queda bloqueada en un contrato de Stellar: solo puede ir a quien lo devuelva o volver a ti. Quien lo encuentra escanea el QR, entra con su cuenta de Google y la reclama, sin saber de criptomonedas.',
      },
      {
        titulo: 'Devuelto y pagado',
        texto: 'Cuando lo recuperas, lo confirmas y Stellar le paga en USDC al instante. Si quieres, una parte va a los celulares que ayudaron a encontrarlo.',
      },
    ],
    roles: { tuyo: 'Tu celular', nodo: 'Un celular cercano', circulo: 'Tu círculo' },
    anterior: 'Escena anterior',
    siguiente: 'Escena siguiente',
    pendiente: 'Captura pendiente',
    sin3d: 'Tu navegador no puede mostrar la escena 3D. Los pasos se leen igual.',
  },
  red: {
    eyebrow: 'La red',
    titulo: 'Cada celular protege y es protegido.',
    bajada:
      'Dendra no depende de antenas propias. La red son los celulares de las personas que la usan: cuantos más son, más lejos llega la señal.',
    puntos: [
      {
        titulo: 'Escucha por Bluetooth',
        texto: 'Un celular perdido emite una señal breve. Los celulares con Dendra que pasan cerca la captan sin que nadie haga nada.',
      },
      {
        titulo: 'Reenvía por internet',
        texto: 'El aviso viaja cifrado. Si no hay conexión en ese momento, se guarda y se envía después: siempre gana la ubicación más reciente.',
      },
      {
        titulo: 'Cobra por ayudar',
        texto: 'Cada aviso útil, confirmado por el círculo del dueño, paga 0,01 USDC al celular que lo reenvió. Los pagos salen en lotes por Stellar desde un fondo de la red.',
      },
    ],
  },
  recompensa: {
    eyebrow: 'Recompensa',
    titulo: 'Una recompensa que ya está ahí.',
    bajada:
      'Cuando reportas tu celular como perdido, eliges un monto. Ese dinero queda bloqueado en un contrato de escrow sobre Stellar: no lo tiene nadie, y cualquiera puede verificarlo.',
    pasos: [
      { titulo: 'Bloqueada', texto: 'El monto queda en un escrow de Trustless Work, en USDC.' },
      { titulo: 'Reclamada', texto: 'Quien lo encuentra escanea el QR y crea su billetera con Pollar, con su cuenta de Google.' },
      { titulo: 'Pagada', texto: 'Al confirmar que lo recuperaste, el contrato paga. Si cancelas, el dinero vuelve a ti.' },
    ],
    nota: 'Solo para celulares perdidos, nunca robados: un robo se denuncia, no se recompensa.',
    prueba: 'Hoy funciona en la red de pruebas de Stellar: los montos todavía no son dinero real.',
  },
  privacidad: {
    eyebrow: 'Privacidad',
    titulo: 'Tu recorrido es de tu círculo.',
    puntos: [
      {
        titulo: 'Cifrado de punta a punta',
        texto: 'La ubicación de tu celular solo la leen las personas de tu círculo. Los celulares que reenvían el aviso no pueden abrirlo.',
      },
      {
        titulo: 'Ayudar es anónimo',
        texto: 'Quien reenvía un aviso usa un número al azar, distinto del de su celular. Ayudar no revela quién eres ni dónde estuviste.',
      },
      {
        titulo: 'El panel no ve tu clave',
        texto: 'Si entras desde la web, tu contraseña no sale del navegador. Nuestro servidor guarda tu cuenta cifrada y no puede leerla.',
      },
    ],
  },
  equipo: {
    eyebrow: 'Equipo',
    titulo: 'Hecho en Sucre.',
    bajada: 'Somos un equipo boliviano que compite en la National Startups Hackathon by Stellar 2026.',
    tecnologia: 'Con la tecnología de',
  },
  unete: {
    eyebrow: 'Únete',
    titulo: 'La red crece con cada celular.',
    bajada: 'Descarga la app para Android e instálala en tu celular. Si quieres sumar a tu organización a las primeras pruebas en Sucre, escríbenos.',
    accion: 'Descargar la app (APK)',
    correo: 'Organizaciones:',
    instalar: {
      titulo: 'Si Android bloquea la instalación',
      aviso: 'Play Protect puede mostrar «Se bloqueó la app para proteger tu dispositivo». Pasa porque Dendra todavía no está en Play Store y usa dos permisos que Google bloquea en apps descargadas de internet: recibir SMS, para avisarte aunque no haya internet, y accesibilidad, para simular que el celular se apaga si te lo roban.',
      pasosTitulo: 'Cómo instalarla en 1 minuto',
      pasos: [
        'Abre Play Store, toca tu foto de perfil y entra a Play Protect.',
        'Toca el engranaje ⚙ y desactiva «Analizar apps con Play Protect».',
        'Abre dendra.apk desde tus Descargas y toca Instalar. Si Android lo pide, permite instalar apps de esa fuente.',
        'Vuelve a Play Protect y activa otra vez «Analizar apps con Play Protect».',
      ],
    },
  },
}

export type Textos = typeof es

const en: Textos = {
  meta: {
    titulo: 'Dendra · A network of phones that look after each other',
    descripcion:
      'Find your stolen or lost phone: phones on the network pick it up over the internet, wifi, SMS or Bluetooth and tell you where it is.',
  },
  hero: {
    titulo: 'Dendra',
    frase: 'What you lose, the network finds.',
    bajada: 'Every phone with Dendra looks out for the others. Over the internet, wifi, SMS or Bluetooth, yours always finds a way to tell you where it is.',
    stellar: ['And whoever returns it earns a reward guaranteed on ', 'Stellar', '.'],
    principal: 'See how it finds it',
    secundaria: 'Join',
    pausar: 'Pause the background',
    seguir: 'Resume the background',
  },
  ciudad: {
    frase: 'The same network looks after your city.',
    intro: 'Every phone is a point in the network.',
    disponible: 'Available',
    pronto: 'Soon',
    apps: [
      { nombre: 'Your phone, stolen or lost', que: 'The network locates it over the internet, wifi, SMS or Bluetooth. Whoever returns it earns a reward on Stellar.' },
      { nombre: 'Fuel queues', que: 'How many cars are waiting at each station, before you leave home.' },
      { nombre: 'Your bus', que: 'Where it is and how full, measured by the phones riding it.' },
      { nombre: 'Signal', que: 'Which carrier works best on each block, with data from people.' },
      { nombre: 'Blackouts', que: 'Where the power went out and since when, reported by the neighbours themselves.' },
      { nombre: 'Getting home', que: 'Your people know you got home safe, without you having to text.' },
      { nombre: 'Your pet', que: 'A tag on its collar and the whole network helps find it.' },
    ],
    mapa: {
      tu: 'You',
      celular: [
        { cifra: '±25', unidad: 'm', rotulo: 'Your phone · seen 1 min ago' },
        { cifra: '50', unidad: 'Bs', rotulo: 'Reward guaranteed on Stellar' },
      ],
      combustible: { cifra: '14', unidad: 'cars', rotulo: 'In line · about 25 min' },
      micro: { cifra: '4', unidad: 'min', rotulo: 'Line A · halfway there' },
      apagon: { cifra: '19:40', rotulo: 'No power · 32 neighbours reported' },
      casa: { cifra: '22:14', rotulo: 'Got home · your circle knows' },
      mascota: { cifra: '2', unidad: 'min', rotulo: 'Huellita · spotted by a nearby phone' },
    } as TextosMapa,
  },
  senal: {
    titulo: 'How the signal travels',
    pasos: [
      {
        titulo: 'Over mobile data, wifi, SMS or Bluetooth, your phone always finds a way to tell you where it is.',
        texto: '',
      },
      {
        titulo: 'Set it up in a minute',
        texto: 'Install Dendra, turn on the Shield and add your circle: the people you trust to help you look for it.',
      },
      {
        titulo: 'If you lose it or it gets stolen',
        texto: 'Report it from the phone of someone in your circle or from the website. You just choose what happened.',
      },
      {
        titulo: 'It offers a reward or plays dead',
        texto: 'If it is lost, it shows how to return it and the reward waiting for whoever finds it. If it was stolen, it pretends to switch off and keeps sending its location.',
      },
      {
        titulo: 'Follow it on the map',
        texto: 'Your circle sees its location live, encrypted. Even without internet, any phone with Dendra passing by picks it up over Bluetooth and forwards it.',
      },
      {
        titulo: 'You are close: the compass guides you',
        texto: 'Turn around once with your phone: Dendra measures the signal and shows you the direction and the meters left.',
      },
      {
        titulo: 'Make it ring',
        texto: 'Even on silent, it rings at full volume, flashes the torch and lights the screen up in white. You find it in a bag, under a cushion or in the dark.',
      },
      {
        titulo: 'A reward guaranteed on Stellar',
        texto: 'When it is reported lost, the reward is locked in a Stellar contract: it can only go to whoever returns it or back to you. The finder scans the QR, signs in with Google and claims it, no crypto knowledge needed.',
      },
      {
        titulo: 'Returned and paid',
        texto: 'Once you get it back, you confirm and Stellar pays them in USDC instantly. If you want, a share goes to the phones that helped find it.',
      },
    ],
    roles: { tuyo: 'Your phone', nodo: 'A nearby phone', circulo: 'Your circle' },
    anterior: 'Previous scene',
    siguiente: 'Next scene',
    pendiente: 'Screenshot pending',
    sin3d: 'Your browser cannot show the 3D scene. The steps read the same.',
  },
  red: {
    eyebrow: 'The network',
    titulo: 'Every phone protects and is protected.',
    bajada:
      'Dendra has no antennas of its own. The network is the phones of the people who use it: the more there are, the farther the signal reaches.',
    puntos: [
      {
        titulo: 'Listens over Bluetooth',
        texto: 'A lost phone sends out a brief signal. Phones with Dendra passing nearby pick it up without anyone doing anything.',
      },
      {
        titulo: 'Forwards over the internet',
        texto: 'The alert travels encrypted. If there is no connection at that moment, it is stored and sent later: the most recent location always wins.',
      },
      {
        titulo: 'Gets paid to help',
        texto: 'Each useful alert, confirmed by the owner’s circle, pays 0.01 USDC to the phone that forwarded it. Payments go out in batches on Stellar from a network fund.',
      },
    ],
  },
  recompensa: {
    eyebrow: 'Reward',
    titulo: 'A reward that is already there.',
    bajada:
      'When you report your phone lost, you choose an amount. That money is locked in an escrow contract on Stellar: nobody holds it, and anyone can verify it.',
    pasos: [
      { titulo: 'Locked', texto: 'The amount sits in a Trustless Work escrow, in USDC.' },
      { titulo: 'Claimed', texto: 'Whoever finds it scans the QR code and creates a wallet with Pollar, using their Google account.' },
      { titulo: 'Paid', texto: 'When you confirm you got it back, the contract pays. If you cancel, the money returns to you.' },
    ],
    nota: 'Only for lost phones, never stolen ones: a theft is reported to the police, not rewarded.',
    prueba: 'Today it runs on the Stellar test network: amounts are not real money yet.',
  },
  privacidad: {
    eyebrow: 'Privacy',
    titulo: 'Your trail belongs to your circle.',
    puntos: [
      {
        titulo: 'End-to-end encrypted',
        texto: 'Only the people in your circle can read your phone’s location. The phones that forward the alert cannot open it.',
      },
      {
        titulo: 'Helping is anonymous',
        texto: 'A phone that forwards an alert uses a random number, different from its own ID. Helping does not reveal who you are or where you were.',
      },
      {
        titulo: 'The panel never sees your password',
        texto: 'If you sign in from the web, your password never leaves the browser. Our server keeps your account encrypted and cannot read it.',
      },
    ],
  },
  equipo: {
    eyebrow: 'Team',
    titulo: 'Made in Sucre.',
    bajada: 'We are a Bolivian team competing in the National Startups Hackathon by Stellar 2026.',
    tecnologia: 'Powered by',
  },
  unete: {
    eyebrow: 'Join',
    titulo: 'The network grows with every phone.',
    bajada: 'Download the Android app and install it on your phone. If you want to bring your organization into the first trials in Sucre, write to us.',
    accion: 'Download the app (APK)',
    correo: 'Organizations:',
    instalar: {
      titulo: 'If Android blocks the install',
      aviso: 'Play Protect may show “App blocked to protect your device”. This happens because Dendra is not on the Play Store yet and uses two permissions Google blocks in apps downloaded from the internet: receiving SMS, to alert you even without internet, and accessibility, to fake a shutdown if your phone is stolen.',
      pasosTitulo: 'How to install it in 1 minute',
      pasos: [
        'Open the Play Store, tap your profile picture and go to Play Protect.',
        'Tap the gear ⚙ and turn off “Scan apps with Play Protect”.',
        'Open dendra.apk from your Downloads and tap Install. If Android asks, allow installs from that source.',
        'Go back to Play Protect and turn “Scan apps with Play Protect” on again.',
      ],
    },
  },
}

export const textos = { es, en }
export type Idioma = keyof typeof textos
