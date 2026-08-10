import { usePlayer } from '@/context/PlayerContext'
import { TapeReels } from '@/components/TapeReels'
import { TrackDisplay } from '@/components/TrackDisplay'
import { PlayerControls } from '@/components/PlayerControls'

type Props = {
  variant?: 'full' | 'compact'
}

export function CassettePlayer({ variant = 'full' }: Props) {
  const {
    isPlaying,
    cassetteLabel,
    setCassetteLabel,
    cycleCassetteLabel,
    volume,
    setVolume,
  } = usePlayer()

  const compact = variant === 'compact'

  return (
    <div
      className={`relative mx-auto w-full overflow-hidden border border-[rgba(42,36,32,0.25)] shadow-[0_24px_60px_rgba(42,36,32,0.22)] ${
        compact ? 'max-w-md' : 'max-w-2xl'
      }`}
      style={{
        background:
          'linear-gradient(160deg, #6a5642 0%, #3d2914 42%, #2a1f14 100%)',
        borderRadius: compact ? 18 : 22,
        padding: compact ? '1rem' : '1.35rem',
      }}
    >
      <div className="mb-3 flex items-center justify-between gap-3 px-1">
        <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-[#e8d6b5]/70">
          Shona Din · Stereo Cassette
        </p>
        <div className="flex items-center gap-2">
          <span
            className={`h-2.5 w-2.5 rounded-full bg-[#9dce7a] ${isPlaying ? 'led-active' : 'opacity-30'}`}
            aria-hidden
          />
          <div className="flex h-5 items-end gap-[2px]" aria-hidden>
            {[0, 1, 2, 3, 4].map((i) => (
              <span
                key={i}
                className={`w-[3px] rounded-sm bg-[#9dce7a] ${isPlaying ? 'eq-bar' : 'opacity-30'}`}
                style={{
                  height: 8 + (i % 3) * 4,
                  animationDelay: isPlaying ? `${i * 0.12}s` : undefined,
                }}
              />
            ))}
          </div>
        </div>
      </div>

      <div
        className="relative mb-4 overflow-hidden border border-[#2a1f14]/60"
        style={{
          background: 'linear-gradient(180deg, #d8c4a0, #c4a878)',
          borderRadius: 10,
          padding: compact ? '0.9rem' : '1.1rem',
        }}
      >
        <div className="mb-3 flex items-start justify-between gap-3">
          <button
            type="button"
            onClick={cycleCassetteLabel}
            onDoubleClick={() => {
              const next = window.prompt('Cassette label', cassetteLabel)
              if (next) setCassetteLabel(next)
            }}
            className="handwritten max-w-[70%] text-left text-lg text-[#3d2914] underline decoration-dotted underline-offset-4"
            title="Click to cycle · double-click to edit"
          >
            {cassetteLabel}
          </button>
          <div
            className="ticket-stub px-2 py-1 font-mono text-[9px] uppercase tracking-[0.14em] text-[#3d2914]/70"
            style={{ transform: 'rotate(2deg)' }}
          >
            Side A/B
          </div>
        </div>
        <TapeReels spinning={isPlaying} size={compact ? 48 : 64} />
        <div
          className="mt-3 h-2 w-full rounded-sm"
          style={{
            background:
              'repeating-linear-gradient(90deg, #2a1f14 0 2px, #5a4530 2px 4px)',
            opacity: 0.35,
          }}
        />
      </div>

      <div className="mb-4 grid gap-4 rounded-lg bg-[rgba(247,241,227,0.92)] p-4 sm:grid-cols-[1.2fr_0.8fr]">
        <TrackDisplay compact={compact} />
        <div className="space-y-3">
          <div className="lcd-glow rounded px-3 py-2 text-[11px]">
            {isPlaying ? '▶ PLAYING' : '❚❚ PAUSED'} · TAPE IN
          </div>
          <label className="block">
            <span className="mb-1 block font-mono text-[9px] uppercase tracking-[0.16em] text-[var(--color-ink)]/50">
              Volume knob
            </span>
            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={volume}
              onChange={(e) => setVolume(Number(e.target.value))}
              className="w-full accent-[var(--color-mustard)]"
            />
          </label>
          <div
            className="mx-auto h-16 w-16 rounded-full border-4 border-[#2a1f14]"
            style={{
              background:
                'conic-gradient(from 210deg, #5a4530, #c4a35a, #5a4530)',
              boxShadow: 'inset 0 0 0 10px #3d2914',
            }}
            aria-hidden
          />
        </div>
      </div>

      <div className="rounded-lg bg-[rgba(247,241,227,0.88)] p-3">
        <PlayerControls />
      </div>

      <div
        className="pointer-events-none absolute inset-x-6 bottom-3 h-8 opacity-40"
        style={{
          background:
            'repeating-linear-gradient(90deg, #1a120c 0 3px, transparent 3px 7px)',
          maskImage: 'linear-gradient(90deg, transparent, black 20%, black 80%, transparent)',
        }}
        aria-hidden
      />
    </div>
  )
}
