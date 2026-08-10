import { useState } from 'react'
import { pujoDays } from '@/data/seasons'
import { usePlayer } from '@/context/PlayerContext'
import { CassettePlayer } from '@/components/CassettePlayer'
import { Queue } from '@/components/Queue'
import { AtmosphereOverlay } from '@/components/AtmosphereOverlay'
import { AmbientToggle } from '@/components/AmbientAudio'

export function PujoPage() {
  const { playQueue, setAmbientKind } = usePlayer()
  const [dayId, setDayId] = useState(pujoDays[0].id)
  const day = pujoDays.find((d) => d.id === dayId) ?? pujoDays[0]

  const enter = (id: string) => {
    const d = pujoDays.find((x) => x.id === id)
    if (!d) return
    setDayId(id)
    setAmbientKind('pujo')
    if (d.tracks.length) playQueue(d.tracks, 0, 'pujo-pandel')
  }

  return (
    <div
      className="relative min-h-[calc(100vh-88px)]"
      style={{
        background: 'linear-gradient(160deg, #f0d0a0 0%, #d4885a 40%, #8a3a2a 100%)',
      }}
    >
      <AtmosphereOverlay kind="pujo" />
      <div className="relative z-10 mx-auto max-w-6xl px-4 py-10">
        <p className="font-bengali text-2xl text-[#3d2914]/80">পূজোর সন্ধ্যা</p>
        <h1 className="font-display text-5xl text-[var(--color-brown)]">Pujor Shondhya</h1>
        <p className="mt-3 max-w-2xl font-display text-lg text-[var(--color-ink)]/75">
          Pandal lights, distant dhak, street-food air — pick the tithi.
        </p>
        <div className="mt-4">
          <AmbientToggle label="Pujo ambient" />
        </div>

        <div className="mt-8 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {pujoDays.map((d) => (
            <button
              key={d.id}
              type="button"
              onClick={() => enter(d.id)}
              className={`border p-4 text-left ${
                day.id === d.id
                  ? 'border-[#3d2914] bg-[rgba(61,41,20,0.9)] text-[var(--color-cream)]'
                  : 'border-[rgba(42,36,32,0.15)] bg-[rgba(247,241,227,0.75)] text-[var(--color-brown)]'
              }`}
            >
              <p className="font-bengali text-xl">{d.bengaliTitle}</p>
              <p className="font-display text-2xl">{d.title}</p>
              <p className="mt-2 text-sm opacity-80">{d.description}</p>
            </button>
          ))}
        </div>

        <div className="mt-6 border border-[rgba(42,36,32,0.12)] bg-[rgba(247,241,227,0.7)] p-4">
          <p className="font-mono text-[10px] uppercase tracking-[0.16em] text-[var(--color-ink)]/45">
            Atmosphere
          </p>
          <p className="font-display text-xl text-[var(--color-brown)]">{day.atmosphere}</p>
        </div>

        <div className="mt-8 grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
          <CassettePlayer />
          <Queue />
        </div>
      </div>
    </div>
  )
}
