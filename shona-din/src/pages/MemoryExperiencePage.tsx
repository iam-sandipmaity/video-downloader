import { useEffect, useRef } from 'react'
import { Link, useParams } from 'react-router-dom'
import { memoryById } from '@/data/memories'
import { usePlayer } from '@/context/PlayerContext'
import { CassettePlayer } from '@/components/CassettePlayer'
import { RadioPlayer } from '@/components/RadioPlayer'
import { Queue } from '@/components/Queue'
import { AtmosphereOverlay } from '@/components/AtmosphereOverlay'
import { AmbientToggle } from '@/components/AmbientAudio'
import { motion } from 'framer-motion'

export function MemoryExperiencePage() {
  const { memoryId = '' } = useParams()
  const memory = memoryById[memoryId]
  const { playQueue, setAmbientKind, setActiveMemoryId } = usePlayer()
  const loadedRef = useRef<string | null>(null)

  useEffect(() => {
    if (!memory) return
    setActiveMemoryId(memory.id)
    if (memory.ambience) setAmbientKind(memory.ambience)
    if (loadedRef.current !== memory.id) {
      loadedRef.current = memory.id
      playQueue(memory.tracks, 0, memory.id)
    }
  }, [memory, playQueue, setActiveMemoryId, setAmbientKind])

  if (!memory) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16">
        <p className="font-display text-2xl">Ei smriti pawa gelo na.</p>
        <Link to="/">Home</Link>
      </div>
    )
  }

  return (
    <div
      className="relative min-h-[calc(100vh-88px)]"
      style={{ background: memory.atmosphere.gradient }}
    >
      <AtmosphereOverlay kind={memory.atmosphere.overlay} />
      <div className="relative z-10 mx-auto max-w-6xl px-4 py-8">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8 max-w-2xl"
        >
          <p className="text-3xl" aria-hidden>
            {memory.icon}
          </p>
          {memory.bengaliTitle && (
            <p className="font-bengali text-3xl text-[var(--color-ink)]/85">
              {memory.bengaliTitle}
            </p>
          )}
          <h1 className="font-display text-4xl font-semibold text-[var(--color-brown)] sm:text-5xl">
            {memory.title}
          </h1>
          <p className="mt-3 font-display text-lg text-[var(--color-ink)]/70">
            {memory.description}
          </p>
          <div className="mt-4 flex flex-wrap items-center gap-3">
            {memory.mood.map((m) => (
              <span
                key={m}
                className="font-mono text-[10px] uppercase tracking-[0.16em] text-[var(--color-ink)]/55"
              >
                {m}
              </span>
            ))}
            <AmbientToggle label={`Ambient · ${memory.ambience || 'none'}`} />
          </div>
          {memory.tags && (
            <div className="mt-4 flex flex-wrap gap-2">
              {memory.tags.map((tag) => (
                <span
                  key={tag}
                  className="border border-[rgba(42,36,32,0.15)] bg-[rgba(247,241,227,0.55)] px-2 py-1 font-mono text-[10px] uppercase tracking-[0.12em]"
                >
                  {tag.replace(/-/g, ' ')}
                </span>
              ))}
            </div>
          )}
        </motion.div>

        <div className="grid gap-6 lg:grid-cols-[1.15fr_0.85fr]">
          {memory.playerStyle === 'radio' ? (
            <RadioPlayer />
          ) : (
            <CassettePlayer variant={memory.playerStyle === 'compact' ? 'compact' : 'full'} />
          )}
          <Queue />
        </div>
      </div>
    </div>
  )
}
