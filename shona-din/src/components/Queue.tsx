import { usePlayer } from '@/context/PlayerContext'
import { trackById } from '@/data/tracks'

export function Queue() {
  const { queue, queueIndex, playQueue, activeMemoryId, side } = usePlayer()

  const sideA = queue.filter((_, i) => i % 2 === 0)
  const sideB = queue.filter((_, i) => i % 2 === 1)
  const list = side === 'A' ? sideA : sideB

  return (
    <section className="border border-[rgba(42,36,32,0.14)] bg-[rgba(247,241,227,0.72)] p-4">
      <div className="mb-3 flex items-end justify-between gap-3">
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-[var(--color-ink)]/45">
            Ajker Cassette
          </p>
          <h3 className="font-display text-2xl text-[var(--color-brown)]">
            SIDE {side}
          </h3>
        </div>
        <p className="font-mono text-[10px] text-[var(--color-ink)]/45">
          {list.length} tracks
        </p>
      </div>
      <ul className="max-h-72 space-y-1 overflow-y-auto pr-1">
        {queue.map((id, index) => {
          const track = trackById[id]
          if (!track) return null
          const active = index === queueIndex
          const onThisSide = side === 'A' ? index % 2 === 0 : index % 2 === 1
          return (
            <li key={`${id}-${index}`}>
              <button
                type="button"
                onClick={() => playQueue(queue, index, activeMemoryId)}
                className={`flex w-full items-start justify-between gap-3 border-b border-[rgba(42,36,32,0.08)] px-2 py-2 text-left transition ${
                  active
                    ? 'bg-[rgba(196,163,90,0.25)]'
                    : onThisSide
                      ? 'hover:bg-[rgba(196,163,90,0.12)]'
                      : 'opacity-40'
                }`}
              >
                <span>
                  <span className="font-mono text-[10px] text-[var(--color-ink)]/40">
                    {(index + 1).toString().padStart(2, '0')}
                  </span>{' '}
                  <span className="font-display text-base text-[var(--color-ink)]">
                    {track.title}
                  </span>
                  <span className="mt-0.5 block font-display text-sm text-[var(--color-ink)]/55">
                    {track.artist}
                  </span>
                </span>
              </button>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
