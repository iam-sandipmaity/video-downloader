import { useState } from 'react'
import { kolkataLocations } from '@/data/seasons'
import { usePlayer } from '@/context/PlayerContext'
import { CassettePlayer } from '@/components/CassettePlayer'
import { Queue } from '@/components/Queue'
import { motion, AnimatePresence } from 'framer-motion'

export function KolkataPage() {
  const { playQueue } = usePlayer()
  const [activeId, setActiveId] = useState(kolkataLocations[2]?.id)
  const active = kolkataLocations.find((l) => l.id === activeId) ?? kolkataLocations[0]

  const enter = (id: string) => {
    const loc = kolkataLocations.find((l) => l.id === id)
    if (!loc) return
    setActiveId(id)
    if (loc.tracks.length) playQueue(loc.tracks, 0, null)
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <p className="font-bengali text-2xl text-[var(--color-faded-red)]">কলকাতা</p>
      <h1 className="font-display text-5xl text-[var(--color-brown)]">Kolkata Mode</h1>
      <p className="mt-3 max-w-2xl font-display text-lg text-[var(--color-ink)]/65">
        Not a playlist — a walk through the city with music as weather.
      </p>

      <div className="no-scrollbar mt-8 flex gap-3 overflow-x-auto pb-2">
        {kolkataLocations.map((loc) => (
          <button
            key={loc.id}
            type="button"
            onClick={() => enter(loc.id)}
            className={`min-w-[160px] border px-4 py-3 text-left transition ${
              active.id === loc.id
                ? 'border-[var(--color-brown)] bg-[var(--color-brown)] text-[var(--color-cream)]'
                : 'border-[rgba(42,36,32,0.15)] bg-[rgba(247,241,227,0.7)] text-[var(--color-brown)]'
            }`}
          >
            <p className="font-mono text-[10px] uppercase tracking-[0.14em] opacity-70">
              {loc.timeOfDay}
            </p>
            <p className="font-display text-lg">{loc.title}</p>
            {loc.bengaliTitle && (
              <p className="font-bengali text-sm opacity-80">{loc.bengaliTitle}</p>
            )}
          </button>
        ))}
      </div>

      <AnimatePresence mode="wait">
        <motion.div
          key={active.id}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          className="mt-8 overflow-hidden border border-[rgba(42,36,32,0.12)]"
          style={{
            background: `linear-gradient(160deg, #f3e6cf, ${active.accent}55)`,
          }}
        >
          <div className="p-6">
            <p className="font-mono text-[11px] uppercase tracking-[0.18em] text-[var(--color-ink)]/50">
              {active.title} · {active.timeOfDay}
            </p>
            <h2 className="mt-2 font-display text-3xl text-[var(--color-brown)]">
              {active.vignette}
            </h2>
            <p className="mt-2 font-display italic text-[var(--color-ink)]/65">
              Now playing this neighbourhood&rsquo;s atmosphere…
            </p>
          </div>
        </motion.div>
      </AnimatePresence>

      <div className="mt-8 grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <CassettePlayer />
        <Queue />
      </div>
    </div>
  )
}
