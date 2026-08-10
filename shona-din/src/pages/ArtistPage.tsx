import { Link, useParams } from 'react-router-dom'
import { artistById } from '@/data/artists'
import { memoryById } from '@/data/memories'
import { trackById } from '@/data/tracks'
import { usePlayer } from '@/context/PlayerContext'

export function ArtistPage() {
  const { artistId = '' } = useParams()
  const artist = artistById[artistId]
  const { playQueue } = usePlayer()

  if (!artist) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16">
        <p>Artist missing.</p>
        <Link to="/voices">Back</Link>
      </div>
    )
  }

  const essential = artist.essentialTracks
    .map((id) => trackById[id])
    .filter(Boolean)

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <div className="grid gap-8 lg:grid-cols-[280px_1fr]">
        <div className="overflow-hidden border border-[rgba(42,36,32,0.12)] bg-[var(--color-aged)]">
          <img src={artist.portrait} alt="" className="aspect-[4/5] w-full object-cover grayscale" />
        </div>
        <div>
          {artist.nameBn && (
            <p className="font-bengali text-2xl text-[var(--color-ink)]/70">{artist.nameBn}</p>
          )}
          <h1 className="font-display text-5xl text-[var(--color-brown)]">{artist.name}</h1>
          <p className="mt-2 font-display text-xl italic text-[var(--color-faded-red)]">
            {artist.epithet}
          </p>
          <p className="mt-4 max-w-2xl font-display text-lg text-[var(--color-ink)]/70">
            {artist.bio}
          </p>
          <button
            type="button"
            onClick={() => playQueue(artist.essentialTracks, 0, null)}
            className="mt-6 border border-[rgba(61,41,20,0.3)] bg-[var(--color-brown)] px-5 py-3 font-display text-lg text-[var(--color-cream)]"
          >
            Start listening
          </button>

          <div className="mt-8 grid gap-6 sm:grid-cols-2">
            <div>
              <h2 className="font-display text-2xl text-[var(--color-brown)]">Essential songs</h2>
              <ul className="mt-3 space-y-2">
                {essential.map((track) => (
                  <li key={track.id} className="border-b border-[rgba(42,36,32,0.08)] py-2">
                    <button
                      type="button"
                      className="text-left"
                      onClick={() =>
                        playQueue(
                          artist.essentialTracks,
                          artist.essentialTracks.indexOf(track.id),
                          null,
                        )
                      }
                    >
                      <p className="font-display text-lg">{track.title}</p>
                      <p className="font-mono text-[10px] uppercase tracking-[0.12em] text-[var(--color-ink)]/45">
                        {track.movie || track.album || track.year}
                      </p>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
            <div className="space-y-6">
              <div>
                <h2 className="font-display text-2xl text-[var(--color-brown)]">Timeline</h2>
                <ul className="mt-3 space-y-3">
                  {artist.timeline.map((item) => (
                    <li key={item.year + item.note}>
                      <p className="font-mono text-[11px] text-[var(--color-faded-red)]">
                        {item.year}
                      </p>
                      <p className="font-display text-[var(--color-ink)]/75">{item.note}</p>
                    </li>
                  ))}
                </ul>
              </div>
              <div>
                <h2 className="font-display text-2xl text-[var(--color-brown)]">
                  Related memories
                </h2>
                <div className="mt-3 flex flex-wrap gap-2">
                  {artist.relatedMemories.map((id) => {
                    const m = memoryById[id]
                    if (!m) return null
                    return (
                      <Link
                        key={id}
                        to={`/memory/${id}`}
                        className="border border-[rgba(42,36,32,0.15)] bg-[rgba(247,241,227,0.7)] px-3 py-2 font-display text-sm text-[var(--color-brown)] no-underline"
                      >
                        {m.icon} {m.title}
                      </Link>
                    )
                  })}
                </div>
              </div>
              <div>
                <h2 className="font-display text-2xl text-[var(--color-brown)]">Moods</h2>
                <p className="mt-2 font-display text-[var(--color-ink)]/65">
                  {artist.relatedMoods.join(' · ')}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
