/**
 * Textos del landing en español e inglés. Las dos versiones tienen la misma forma:
 * el tipo `Textos` sale del español y el inglés debe cumplirlo.
 *
 * El lema del hero es provisional.
 */

const es = {
  meta: {
    titulo: 'Dendra · Una red de celulares que se cuidan entre sí',
    descripcion:
      'Como las dendritas de una neurona, cada celular recibe la señal del otro. Si pierdes el tuyo, la red lo escucha y una recompensa en Stellar espera a quien lo devuelva.',
  },
  nav: {
    principal: 'Principal',
    senal: 'La señal',
    red: 'La red',
    recompensa: 'Recompensa',
    privacidad: 'Privacidad',
    equipo: 'Equipo',
    unete: 'Únete',
    menu: 'Menú',
    cerrar: 'Cerrar menú',
    idioma: 'English',
    idiomaCorto: 'EN',
    idiomaEtiqueta: 'Ver en inglés',
  },
  hero: {
    eyebrow: 'Sucre, Bolivia · Red de pruebas en Stellar',
    titulo: 'Dendra',
    lema: 'Como las dendritas de una neurona, cada celular recibe la señal del otro. Ninguno está solo.',
    principal: 'Ver cómo viaja la señal',
    secundaria: 'Únete',
    pausar: 'Pausar el fondo',
    seguir: 'Reanudar el fondo',
  },
  senal: {
    eyebrow: 'La señal',
    titulo: 'Así viaja la señal',
    bajada: 'Tres celulares, una misma red. Baja despacio: cada paso muestra la pantalla real de la app.',
    pasos: [
      {
        titulo: 'Tu celular entra a la red',
        texto: 'Activas Dendra y armas tu círculo: las personas que podrán ver dónde está tu celular si algún día lo pierdes.',
      },
      {
        titulo: 'Si se aleja, lo sabes',
        texto: 'Un guardián Bluetooth se queda contigo. Si tu celular se separa de ti, avisa al instante, mientras todavía está cerca.',
      },
      {
        titulo: 'Lo reportas como perdido',
        texto: 'Desde el celular de alguien de tu círculo, o desde el panel web. Tu celular muestra la recompensa y un código QR para quien lo encuentre.',
      },
      {
        titulo: 'Otro celular lo escucha',
        texto: 'Cualquier celular con Dendra que pase cerca capta su señal por Bluetooth y reenvía el aviso por internet. Está cifrado: ese celular no puede leerlo.',
      },
      {
        titulo: 'Tu círculo lo encuentra',
        texto: 'Solo tu círculo descifra el aviso y ve la última ubicación en el mapa. Ya cerca, la brújula guía los últimos metros.',
      },
      {
        titulo: 'Quien lo devuelve, cobra',
        texto: 'La recompensa espera en un escrow de Stellar. Cuando confirmas que lo recuperaste, se paga en USDC a quien lo devolvió, y una parte a la red que ayudó.',
      },
    ],
    roles: { tuyo: 'Tu celular', nodo: 'Un celular cercano', circulo: 'Tu círculo' },
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
    bajada: 'Estamos preparando las primeras pruebas en Sucre. Escríbenos si quieres llevar Dendra en tu celular o sumar a tu organización.',
    accion: 'Escríbenos',
  },
  pie: {
    derechos: 'Dendra · Sucre, Bolivia',
    prueba: 'Red de pruebas de Stellar',
  },
}

export type Textos = typeof es

const en: Textos = {
  meta: {
    titulo: 'Dendra · A network of phones that look after each other',
    descripcion:
      'Like the dendrites of a neuron, each phone picks up the signal of another. If you lose yours, the network hears it, and a reward on Stellar waits for whoever returns it.',
  },
  nav: {
    principal: 'Main',
    senal: 'The signal',
    red: 'The network',
    recompensa: 'Reward',
    privacidad: 'Privacy',
    equipo: 'Team',
    unete: 'Join',
    menu: 'Menu',
    cerrar: 'Close menu',
    idioma: 'Español',
    idiomaCorto: 'ES',
    idiomaEtiqueta: 'Ver en español',
  },
  hero: {
    eyebrow: 'Sucre, Bolivia · Stellar test network',
    titulo: 'Dendra',
    lema: 'Like the dendrites of a neuron, each phone picks up the signal of another. No one is alone.',
    principal: 'See how the signal travels',
    secundaria: 'Join',
    pausar: 'Pause the background',
    seguir: 'Resume the background',
  },
  senal: {
    eyebrow: 'The signal',
    titulo: 'How the signal travels',
    bajada: 'Three phones, one network. Scroll slowly: each step shows the real app screen.',
    pasos: [
      {
        titulo: 'Your phone joins the network',
        texto: 'You turn on Dendra and build your circle: the people who will see where your phone is if you ever lose it.',
      },
      {
        titulo: 'If it drifts away, you know',
        texto: 'A Bluetooth guardian stays with you. If your phone gets separated from you, it alerts you right away, while it is still nearby.',
      },
      {
        titulo: 'You report it lost',
        texto: 'From the phone of someone in your circle, or from the web panel. Your phone shows the reward and a QR code for whoever finds it.',
      },
      {
        titulo: 'Another phone hears it',
        texto: 'Any phone with Dendra that passes by picks up its Bluetooth signal and forwards the alert over the internet. It is encrypted: that phone cannot read it.',
      },
      {
        titulo: 'Your circle finds it',
        texto: 'Only your circle can decrypt the alert and see the last location on the map. Once nearby, the compass guides the last few meters.',
      },
      {
        titulo: 'Whoever returns it gets paid',
        texto: 'The reward waits in an escrow on Stellar. When you confirm you got it back, it pays out in USDC to the person who returned it, and a share to the network that helped.',
      },
    ],
    roles: { tuyo: 'Your phone', nodo: 'A nearby phone', circulo: 'Your circle' },
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
    bajada: 'We are preparing the first trials in Sucre. Write to us if you want Dendra on your phone or want to bring in your organization.',
    accion: 'Write to us',
  },
  pie: {
    derechos: 'Dendra · Sucre, Bolivia',
    prueba: 'Stellar test network',
  },
}

export const textos = { es, en }
export type Idioma = keyof typeof textos
