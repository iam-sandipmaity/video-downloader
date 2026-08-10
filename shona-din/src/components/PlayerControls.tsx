import { usePlayer } from '@/context/PlayerContext'

function IconBtn({
  label,
  onClick,
  active,
  children,
}: {
  label: string
  onClick: () => void
  active?: boolean
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className={`flex h-11 min-w-11 items-center justify-center rounded-sm border border-[rgba(42,36,32,0.2)] bg-[rgba(247,241,227,0.55)] px-3 font-mono text-[11px] uppercase tracking-[0.12em] transition hover:bg-[rgba(247,241,227,0.9)] ${
        active ? 'text-[var(--color-faded-red)]' : 'text-[var(--color-brown)]'
      }`}
    >
      {children}
    </button>
  )
}

export function PlayerControls() {
  const {
    isPlaying,
    togglePlay,
    next,
    prev,
    progress,
    duration,
    seek,
    volume,
    setVolume,
    muted,
    toggleMute,
    shuffle,
    toggleShuffle,
    repeat,
    cycleRepeat,
    toggleFavorite,
    favorites,
    currentTrack,
    side,
    toggleSide,
  } = usePlayer()

  const liked = currentTrack ? favorites.includes(currentTrack.id) : false

  return (
    <div className="space-y-4">
      <input
        type="range"
        min={0}
        max={duration || 0}
        step={0.1}
        value={progress}
        onChange={(e) => seek(Number(e.target.value))}
        className="w-full accent-[var(--color-faded-red)]"
        aria-label="Seek"
      />
      <div className="flex flex-wrap items-center gap-2">
        <IconBtn label="Previous" onClick={prev}>
          Prev
        </IconBtn>
        <IconBtn label={isPlaying ? 'Pause' : 'Play'} onClick={togglePlay}>
          {isPlaying ? 'Pause' : 'Play'}
        </IconBtn>
        <IconBtn label="Next" onClick={next}>
          Next
        </IconBtn>
        <IconBtn label="Shuffle" onClick={toggleShuffle} active={shuffle}>
          Shuffle
        </IconBtn>
        <IconBtn label="Repeat" onClick={cycleRepeat} active={repeat !== 'off'}>
          {repeat === 'one' ? 'Rep 1' : repeat === 'all' ? 'Rep A' : 'Repeat'}
        </IconBtn>
        <IconBtn label="Favorite" onClick={() => toggleFavorite()} active={liked}>
          {liked ? '♥ Saved' : '♡ Save'}
        </IconBtn>
        <IconBtn label="Flip side" onClick={toggleSide}>
          Side {side}
        </IconBtn>
      </div>
      <div className="flex items-center gap-3">
        <IconBtn label="Mute" onClick={toggleMute}>
          {muted ? 'Unmute' : 'Mute'}
        </IconBtn>
        <input
          type="range"
          min={0}
          max={1}
          step={0.01}
          value={muted ? 0 : volume}
          onChange={(e) => setVolume(Number(e.target.value))}
          className="w-32 accent-[var(--color-mustard)]"
          aria-label="Volume"
        />
      </div>
    </div>
  )
}
