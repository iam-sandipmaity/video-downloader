import { usePlayer } from '@/context/PlayerContext'

export function Settings() {
  const {
    ambientEnabled,
    setAmbientEnabled,
    volume,
    setVolume,
    shuffle,
    toggleShuffle,
    repeat,
    cycleRepeat,
  } = usePlayer()

  return (
    <section className="border border-[rgba(42,36,32,0.12)] bg-[rgba(247,241,227,0.7)] p-4">
      <h3 className="font-display text-2xl text-[var(--color-brown)]">Settings</h3>
      <div className="mt-4 space-y-3">
        <label className="flex items-center justify-between gap-3 font-display">
          <span>Ambient layer</span>
          <input
            type="checkbox"
            checked={ambientEnabled}
            onChange={(e) => setAmbientEnabled(e.target.checked)}
          />
        </label>
        <label className="block font-display">
          <span className="mb-1 block">Master volume</span>
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
        <button
          type="button"
          onClick={toggleShuffle}
          className="mr-2 border border-[rgba(42,36,32,0.2)] px-3 py-2 font-mono text-[10px] uppercase tracking-[0.12em]"
        >
          Shuffle: {shuffle ? 'on' : 'off'}
        </button>
        <button
          type="button"
          onClick={cycleRepeat}
          className="border border-[rgba(42,36,32,0.2)] px-3 py-2 font-mono text-[10px] uppercase tracking-[0.12em]"
        >
          Repeat: {repeat}
        </button>
      </div>
    </section>
  )
}
