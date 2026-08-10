import { useState } from 'react'
import { moodCombos } from '@/data/seasons'
import { findTracksByMoodTags } from '@/data/tracks'
import { usePlayer } from '@/context/PlayerContext'

const TAG_OPTIONS = [
  'Rain',
  'Night',
  'Alone',
  'Mela',
  'Evening',
  'Childhood',
  'Wedding',
  'Family',
  'Celebration',
  'Bus',
  'Window',
  'Sunset',
  'Tea',
  'Adda',
  'Winter',
  'Pujo',
  'Friends',
  'Journey',
]

export function MoodSelector() {
  const { playQueue } = usePlayer()
  const [selected, setSelected] = useState<string[]>(['Rain', 'Night'])

  const toggle = (tag: string) => {
    setSelected((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag].slice(0, 4),
    )
  }

  const recommend = () => {
    const preset = moodCombos.find((m) =>
      m.tags.every((t) =>
        selected.some((s) => s.toLowerCase().includes(t) || t.includes(s.toLowerCase())),
      ),
    )
    const ids =
      preset?.tracks?.length
        ? preset.tracks
        : findTracksByMoodTags(selected.map((s) => s.toLowerCase())).map((t) => t.id)
    if (ids.length) playQueue(ids, 0, null)
  }

  return (
    <div className="space-y-5">
      <div>
        <p className="font-bengali text-xl text-[var(--color-brown)]">
          কেমন লাগছে আজ?
        </p>
        <p className="font-display text-[var(--color-ink)]/65">
          Combine moods the way memories actually feel.
        </p>
      </div>
      <div className="flex flex-wrap gap-2">
        {TAG_OPTIONS.map((tag) => {
          const on = selected.includes(tag)
          return (
            <button
              key={tag}
              type="button"
              onClick={() => toggle(tag)}
              className={`border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.12em] transition ${
                on
                  ? 'border-[var(--color-brown)] bg-[var(--color-brown)] text-[var(--color-cream)]'
                  : 'border-[rgba(42,36,32,0.2)] bg-[rgba(247,241,227,0.7)] text-[var(--color-brown)]'
              }`}
            >
              {tag}
            </button>
          )
        })}
      </div>
      <p className="font-display text-lg text-[var(--color-faded-red)]">
        {selected.join(' + ') || 'Pick a feeling'}
      </p>
      <button
        type="button"
        onClick={recommend}
        className="border border-[rgba(61,41,20,0.3)] bg-[var(--color-faded-red)] px-5 py-3 font-display text-lg text-[var(--color-cream)]"
      >
        Make me a cassette
      </button>
      <div className="grid gap-3 sm:grid-cols-2">
        {moodCombos.map((combo) => (
          <button
            key={combo.id}
            type="button"
            onClick={() => playQueue(combo.tracks, 0, null)}
            className="newspaper-clip p-4 text-left transition hover:-translate-y-0.5"
          >
            <p className="font-mono text-[10px] uppercase tracking-[0.14em] text-[var(--color-ink)]/45">
              Mood recipe
            </p>
            <p className="font-display text-lg text-[var(--color-brown)]">{combo.label}</p>
            {combo.labelBn && (
              <p className="font-bengali text-sm text-[var(--color-ink)]/65">{combo.labelBn}</p>
            )}
          </button>
        ))}
      </div>
    </div>
  )
}
