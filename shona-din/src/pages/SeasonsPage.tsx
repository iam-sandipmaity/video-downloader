import { seasons } from '@/data/seasons'
import { usePlayer } from '@/context/PlayerContext'

export function SeasonsPage() {
  const { playQueue } = usePlayer()

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <p className="font-bengali text-2xl text-[var(--color-faded-red)]">ঋতু</p>
      <h1 className="font-display text-5xl text-[var(--color-brown)]">Seasons</h1>
      <p className="mt-3 max-w-2xl font-display text-lg text-[var(--color-ink)]/65">
        Bengali seasons as listening weather — Boishakh heat to Sheet blankets.
      </p>
      <div className="mt-8 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {seasons.map((season) => (
          <button
            key={season.id}
            type="button"
            onClick={() => season.tracks.length && playQueue(season.tracks, 0, null)}
            className="border border-[rgba(42,36,32,0.12)] p-5 text-left transition hover:-translate-y-0.5"
            style={{
              background: `linear-gradient(165deg, #f7f1e3, ${season.accent}40)`,
            }}
          >
            <p className="font-bengali text-2xl text-[var(--color-ink)]/80">
              {season.bengaliTitle}
            </p>
            <h2 className="font-display text-3xl text-[var(--color-brown)]">{season.title}</h2>
            <p className="mt-2 font-display text-[var(--color-ink)]/65">{season.description}</p>
            <p className="mt-3 font-mono text-[10px] uppercase tracking-[0.14em] text-[var(--color-ink)]/45">
              {season.atmosphere}
            </p>
          </button>
        ))}
      </div>
    </div>
  )
}
