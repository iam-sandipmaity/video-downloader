import { usePlayer } from '@/context/PlayerContext'
import { memoryById } from '@/data/memories'

function formatTime(s: number) {
  if (!Number.isFinite(s) || s < 0) return '0:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${m}:${sec.toString().padStart(2, '0')}`
}

export function TrackDisplay({ compact }: { compact?: boolean }) {
  const { currentTrack, queueIndex, side, activeMemoryId, progress, duration } =
    usePlayer()
  const memory = activeMemoryId ? memoryById[activeMemoryId] : null

  if (!currentTrack) {
    return (
      <div className="lcd-glow rounded px-3 py-2 text-xs">
        Waiting for cassette…
      </div>
    )
  }

  return (
    <div className={compact ? 'space-y-1' : 'space-y-2'}>
      <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-[var(--color-ink)]/50">
        Now Playing
      </p>
      <h2 className="font-display text-2xl font-semibold leading-tight text-[var(--color-brown)] sm:text-3xl">
        {currentTrack.titleBn || currentTrack.title}
      </h2>
      {!currentTrack.titleBn && (
        <p className="font-display text-lg text-[var(--color-ink)]/80">
          {currentTrack.title}
        </p>
      )}
      {currentTrack.titleBn && (
        <p className="font-display text-base text-[var(--color-ink)]/65">
          {currentTrack.title}
        </p>
      )}
      <p className="font-display text-base text-[var(--color-faded-red)]">
        {currentTrack.artist}
      </p>
      <div className="space-y-0.5 font-mono text-[11px] uppercase tracking-[0.12em] text-[var(--color-ink)]/55">
        {(currentTrack.movie || currentTrack.album) && (
          <p>From: {currentTrack.movie || currentTrack.album}</p>
        )}
        {currentTrack.year && (
          <p>
            {Math.floor(currentTrack.year / 10) * 10}s · {currentTrack.year}
          </p>
        )}
        {memory && <p>Memory: {memory.title}</p>}
        <p>
          Side {side} · Track {(queueIndex + 1).toString().padStart(2, '0')}
        </p>
        <p>
          {formatTime(progress)} / {formatTime(duration)}
        </p>
      </div>
    </div>
  )
}
