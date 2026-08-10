import { useMemo, useState } from 'react'
import { usePlayer } from '@/context/PlayerContext'
import { radioStations } from '@/data/seasons'
import { PlayerControls } from '@/components/PlayerControls'
import { TrackDisplay } from '@/components/TrackDisplay'

export function RadioPlayer() {
  const { isPlaying, togglePlay, setAmbientKind, ambientEnabled, setAmbientEnabled } =
    usePlayer()
  const [stationIndex, setStationIndex] = useState(0)
  const [tuning, setTuning] = useState(false)
  const station = radioStations[stationIndex]

  const dialRotation = useMemo(() => -60 + stationIndex * 28, [stationIndex])

  const tune = (dir: 1 | -1) => {
    setTuning(true)
    setAmbientKind('radio')
    if (!ambientEnabled) setAmbientEnabled(true)
    window.setTimeout(() => {
      setStationIndex((i) => (i + dir + radioStations.length) % radioStations.length)
      setTuning(false)
      if (!isPlaying) togglePlay()
    }, 700)
  }

  return (
    <div
      className="relative mx-auto w-full max-w-xl overflow-hidden border border-[rgba(42,36,32,0.3)] shadow-[0_24px_50px_rgba(42,36,32,0.25)]"
      style={{
        background: 'linear-gradient(165deg, #b8a078 0%, #8a6a48 45%, #4a3424 100%)',
        borderRadius: 18,
        padding: '1.25rem',
      }}
    >
      <div className="mb-3 flex items-center justify-between">
        <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-[#f3e6cf]/75">
          Dadu-r Table Radio
        </p>
        <span
          className={`h-2.5 w-2.5 rounded-full bg-[#e8b86d] ${isPlaying ? 'led-active' : 'opacity-40'}`}
        />
      </div>

      <div className="mb-4 rounded-md bg-[#1a2a1a] p-4 text-[#9dce7a] shadow-inner">
        <p className="font-mono text-[10px] uppercase tracking-[0.18em] opacity-70">
          Station
        </p>
        <p className="font-mono text-2xl tracking-[0.08em]">
          {tuning ? '···· TUNING ····' : station.name}
        </p>
        <p className="font-mono text-sm opacity-80">{station.freq} MHz</p>
      </div>

      <div className="mb-4 grid grid-cols-[1fr_auto] items-center gap-4 rounded-lg bg-[rgba(247,241,227,0.9)] p-4">
        <TrackDisplay compact />
        <div className="text-center">
          <div
            className="relative mx-auto h-24 w-24 rounded-full border-[6px] border-[#2a1f14]"
            style={{
              background:
                'radial-gradient(circle at 40% 35%, #f3e6cf, #c4a35a 55%, #5a4530)',
            }}
          >
            <div
              className="absolute left-1/2 top-1/2 h-[2px] w-10 origin-left bg-[#2a1f14]"
              style={{ transform: `rotate(${dialRotation}deg)` }}
            />
            <div className="absolute left-1/2 top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full bg-[#2a1f14]" />
          </div>
          <div className="mt-3 flex justify-center gap-2">
            <button
              type="button"
              className="border border-[rgba(42,36,32,0.25)] bg-[#f7f1e3] px-3 py-2 font-mono text-[10px] uppercase tracking-[0.14em]"
              onClick={() => tune(-1)}
            >
              ◀ Tune
            </button>
            <button
              type="button"
              className="border border-[rgba(42,36,32,0.25)] bg-[#f7f1e3] px-3 py-2 font-mono text-[10px] uppercase tracking-[0.14em]"
              onClick={() => tune(1)}
            >
              Tune ▶
            </button>
          </div>
        </div>
      </div>

      <div
        className="mb-4 h-16 rounded-md opacity-70"
        style={{
          background:
            'repeating-linear-gradient(90deg, #2a1f14 0 2px, #5a4530 2px 5px)',
        }}
        aria-hidden
      />

      <div className="rounded-lg bg-[rgba(247,241,227,0.9)] p-3">
        <PlayerControls />
      </div>
    </div>
  )
}
