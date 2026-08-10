import { usePlayer } from '@/context/PlayerContext'

export function AmbientToggle({ label }: { label?: string }) {
  const { ambientEnabled, setAmbientEnabled } = usePlayer()
  return (
    <button
      type="button"
      onClick={() => setAmbientEnabled(!ambientEnabled)}
      className={`border px-3 py-2 font-mono text-[10px] uppercase tracking-[0.14em] ${
        ambientEnabled
          ? 'border-[var(--color-deep-green)] bg-[var(--color-deep-green)] text-[var(--color-cream)]'
          : 'border-[rgba(42,36,32,0.2)] bg-[rgba(247,241,227,0.7)] text-[var(--color-brown)]'
      }`}
    >
      {ambientEnabled ? 'Ambient on' : label || 'Ambient off'}
    </button>
  )
}
