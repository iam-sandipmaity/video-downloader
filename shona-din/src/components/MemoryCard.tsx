import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import type { Memory } from '@/types'
import { usePlayer } from '@/context/PlayerContext'

type Props = {
  memory: Memory
  index?: number
  large?: boolean
}

export function MemoryCard({ memory, index = 0, large }: Props) {
  const { playQueue, setAmbientKind, setActiveMemoryId } = usePlayer()

  const onSelect = () => {
    setActiveMemoryId(memory.id)
    if (memory.ambience) setAmbientKind(memory.ambience)
    playQueue(memory.tracks, 0, memory.id)
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05, duration: 0.45 }}
      className={large ? 'min-w-[240px] sm:min-w-0' : 'min-w-[200px] sm:min-w-0'}
    >
      <Link
        to={`/memory/${memory.id}`}
        onClick={onSelect}
        className="group block focus:outline-none"
        style={{ textDecoration: 'none', color: 'inherit' }}
      >
        <div
          className="relative overflow-hidden border border-[rgba(42,36,32,0.14)] transition-transform duration-300 group-hover:-translate-y-1 group-focus-visible:ring-2 group-focus-visible:ring-[var(--color-mustard)]"
          style={{
            background: memory.atmosphere.gradient,
            minHeight: large ? 180 : 150,
            padding: '1.25rem 1.1rem',
          }}
        >
          <div
            className="pointer-events-none absolute inset-0 opacity-30"
            style={{
              backgroundImage:
                'radial-gradient(circle at 80% 20%, rgba(255,255,255,0.35), transparent 40%)',
            }}
          />
          <div className="relative z-[1]">
            <div className="mb-3 text-2xl" aria-hidden>
              {memory.icon}
            </div>
            {memory.bengaliTitle && (
              <p className="font-bengali text-lg leading-tight text-[var(--color-ink)]/90">
                {memory.bengaliTitle}
              </p>
            )}
            <h3 className="font-display text-xl font-semibold tracking-wide text-[var(--color-brown)]">
              {memory.title}
            </h3>
            <p className="mt-2 max-w-[28ch] font-display text-sm leading-snug text-[var(--color-ink)]/70">
              {memory.description}
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              {memory.mood.slice(0, 3).map((m) => (
                <span
                  key={m}
                  className="font-mono text-[10px] uppercase tracking-[0.14em] text-[var(--color-ink)]/55"
                >
                  {m}
                </span>
              ))}
            </div>
          </div>
        </div>
      </Link>
    </motion.div>
  )
}
