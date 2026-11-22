/** Marca de Dendra: dos anillos (neuronas) a −14° y la chispa que pasa entre ellos. */
export function Logo() {
  return (
    <svg viewBox="0 0 64 40" fill="none" aria-hidden="true">
      <ellipse cx="21" cy="20" rx="15" ry="8.5" transform="rotate(-14 21 20)" stroke="currentColor" strokeWidth="2" />
      <ellipse cx="43" cy="20" rx="15" ry="8.5" transform="rotate(-14 43 20)" stroke="currentColor" strokeWidth="2" opacity="0.45" />
      <circle cx="32" cy="20" r="3.4" fill="#2bd07e" />
    </svg>
  )
}
