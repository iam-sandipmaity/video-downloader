import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { memories } from '@/data/memories'
import { artists } from '@/data/artists'
import { tracks } from '@/data/tracks'
import { usePlayer } from '@/context/PlayerContext'

export function SurpriseMe() {
  const navigate = useNavigate()
  const { playQueue, triggerCountdownThenPlay } = usePlayer()
  const [drawing, setDrawing] = useState(false)
  const [result, setResult] = useState<string | null>(null)

  const draw = () => {
    setDrawing(true)
    const memory = memories[Math.floor(Math.random() * memories.length)]
    const artist = artists[Math.floor(Math.random() * artists.length)]
    const track = tracks[Math.floor(Math.random() * tracks.length)]
    const decade = ['1960s', '1970s', '1980s', '1990s'][Math.floor(Math.random() * 4)]
    const line = `${memory.title} · ${artist.name} · ${decade} · ${track.title}`
    setResult(line)

    window.setTimeout(() => {
      playQueue(memory.tracks, 0, memory.id)
      triggerCountdownThenPlay()
      setDrawing(false)
      navigate(`/memory/${memory.id}`)
    }, 1200)
  }

  const label = useMemo(
    () => (drawing ? 'Cassette berocor...' : 'Ajke Ki Shunbo?'),
    [drawing],
  )

  return (
    <div className="border border-dashed border-[rgba(42,36,32,0.28)] bg-[rgba(247,241,227,0.55)] p-5 text-center">
      <p className="font-bengali text-lg text-[var(--color-ink)]/70">আজকে কি শুনবো?</p>
      <button
        type="button"
        onClick={draw}
        disabled={drawing}
        className="mt-3 w-full max-w-md border border-[rgba(61,41,20,0.35)] bg-[var(--color-brown)] px-6 py-4 font-display text-xl tracking-[0.06em] text-[var(--color-cream)] transition hover:bg-[var(--color-charcoal)] disabled:opacity-70"
      >
        {label}
      </button>
      {result && (
        <p className="mt-3 font-display text-sm text-[var(--color-ink)]/65">{result}</p>
      )}
      <p className="mt-2 font-mono text-[10px] uppercase tracking-[0.16em] text-[var(--color-ink)]/40">
        Pull a random cassette from the cupboard
      </p>
    </div>
  )
}
