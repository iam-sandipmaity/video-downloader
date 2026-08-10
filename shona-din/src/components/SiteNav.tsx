import { NavLink } from 'react-router-dom'

const links = [
  { to: '/', label: 'Smriti', end: true },
  { to: '/voices', label: 'Voices' },
  { to: '/kolkata', label: 'Kolkata' },
  { to: '/seasons', label: 'Seasons' },
  { to: '/pujo', label: 'Pujo' },
  { to: '/mood', label: 'Mood' },
  { to: '/memory-book', label: 'Amar Smriti' },
]

export function SiteNav() {
  return (
    <header className="relative z-20 border-b border-[rgba(42,36,32,0.12)] bg-[rgba(243,230,207,0.82)] backdrop-blur-sm">
      <div className="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
        <NavLink to="/" className="group no-underline">
          <p className="font-bengali text-sm text-[var(--color-faded-red)]">সেই দিন..</p>
          <h1 className="font-display text-3xl font-semibold tracking-[0.04em] text-[var(--color-brown)]">
            SHONA DIN
          </h1>
        </NavLink>
        <nav className="no-scrollbar flex gap-1 overflow-x-auto pb-1">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) =>
                `whitespace-nowrap px-3 py-2 font-mono text-[11px] uppercase tracking-[0.14em] no-underline transition ${
                  isActive
                    ? 'bg-[rgba(61,41,20,0.9)] text-[var(--color-cream)]'
                    : 'text-[var(--color-brown)]/75 hover:bg-[rgba(61,41,20,0.08)]'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  )
}
