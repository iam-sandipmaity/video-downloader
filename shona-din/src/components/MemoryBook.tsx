import { useState } from 'react'
import { useMemoryBook } from '@/context/MemoryBookContext'
import { usePlayer } from '@/context/PlayerContext'
import { trackById } from '@/data/tracks'

export function MemoryBook() {
  const { collections, createCollection, addTrack, removeTrack, deleteCollection } =
    useMemoryBook()
  const { currentTrack, playQueue } = usePlayer()
  const [name, setName] = useState('')
  const [activeId, setActiveId] = useState(collections[0]?.id ?? '')

  const active = collections.find((c) => c.id === activeId) ?? collections[0]

  return (
    <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
      <aside className="space-y-3">
        <form
          onSubmit={(e) => {
            e.preventDefault()
            createCollection(name)
            setName('')
          }}
          className="flex gap-2"
        >
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Notun smriti..."
            className="w-full border border-[rgba(42,36,32,0.2)] bg-[rgba(247,241,227,0.8)] px-3 py-2 font-display"
          />
          <button
            type="submit"
            className="border border-[rgba(42,36,32,0.25)] bg-[var(--color-brown)] px-3 py-2 font-mono text-[10px] uppercase tracking-[0.12em] text-[var(--color-cream)]"
          >
            Add
          </button>
        </form>
        <ul className="space-y-1">
          {collections.map((c) => (
            <li key={c.id}>
              <button
                type="button"
                onClick={() => setActiveId(c.id)}
                className={`flex w-full items-center justify-between px-3 py-2 text-left font-display ${
                  active?.id === c.id
                    ? 'bg-[rgba(61,41,20,0.9)] text-[var(--color-cream)]'
                    : 'bg-[rgba(247,241,227,0.55)] text-[var(--color-brown)]'
                }`}
              >
                <span>{c.name}</span>
                <span className="font-mono text-[10px] opacity-60">{c.trackIds.length}</span>
              </button>
            </li>
          ))}
        </ul>
      </aside>

      <section className="border border-[rgba(42,36,32,0.12)] bg-[rgba(247,241,227,0.7)] p-5">
        {active ? (
          <>
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink)]/45">
                  Amar Smriti
                </p>
                <h2 className="font-display text-3xl text-[var(--color-brown)]">
                  {active.name}
                </h2>
              </div>
              <div className="flex gap-2">
                {currentTrack && (
                  <button
                    type="button"
                    onClick={() => addTrack(active.id, currentTrack.id)}
                    className="border border-[rgba(42,36,32,0.2)] px-3 py-2 font-mono text-[10px] uppercase tracking-[0.12em]"
                  >
                    Save current song
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => active.trackIds.length && playQueue(active.trackIds, 0, null)}
                  className="border border-[rgba(42,36,32,0.2)] bg-[var(--color-faded-red)] px-3 py-2 font-mono text-[10px] uppercase tracking-[0.12em] text-[var(--color-cream)]"
                >
                  Play
                </button>
                <button
                  type="button"
                  onClick={() => deleteCollection(active.id)}
                  className="border border-[rgba(42,36,32,0.2)] px-3 py-2 font-mono text-[10px] uppercase tracking-[0.12em]"
                >
                  Delete
                </button>
              </div>
            </div>
            <ul className="space-y-2">
              {active.trackIds.length === 0 && (
                <li className="font-display text-[var(--color-ink)]/55">
                  Ekhono khali — save a song while listening.
                </li>
              )}
              {active.trackIds.map((id) => {
                const track = trackById[id]
                if (!track) return null
                return (
                  <li
                    key={id}
                    className="flex items-center justify-between gap-3 border-b border-[rgba(42,36,32,0.08)] py-2"
                  >
                    <div>
                      <p className="font-display text-lg">{track.title}</p>
                      <p className="font-display text-sm text-[var(--color-ink)]/55">
                        {track.artist}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeTrack(active.id, id)}
                      className="font-mono text-[10px] uppercase tracking-[0.12em] text-[var(--color-faded-red)]"
                    >
                      Remove
                    </button>
                  </li>
                )
              })}
            </ul>
          </>
        ) : (
          <p>Create a memory collection to begin.</p>
        )}
      </section>
    </div>
  )
}
