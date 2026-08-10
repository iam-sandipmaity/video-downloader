import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import type { Artist } from '@/types'

export function ArtistCollection({ artist, index = 0 }: { artist: Artist; index?: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay: index * 0.04 }}
    >
      <Link
        to={`/voices/${artist.id}`}
        className="group grid grid-cols-[100px_1fr] gap-4 border border-[rgba(42,36,32,0.12)] bg-[rgba(247,241,227,0.65)] p-3 no-underline transition hover:-translate-y-0.5 sm:grid-cols-[140px_1fr]"
      >
        <div className="relative overflow-hidden bg-[var(--color-aged)]">
          <img
            src={artist.portrait}
            alt=""
            className="h-full min-h-[120px] w-full object-cover grayscale contrast-110 transition group-hover:grayscale-0"
          />
        </div>
        <div className="flex flex-col justify-center py-1">
          {artist.nameBn && (
            <p className="font-bengali text-base text-[var(--color-ink)]/70">{artist.nameBn}</p>
          )}
          <h3 className="font-display text-2xl text-[var(--color-brown)]">{artist.name}</h3>
          <p className="mt-1 font-display italic text-[var(--color-faded-red)]">
            {artist.epithet}
          </p>
          <p className="mt-2 line-clamp-2 font-display text-sm text-[var(--color-ink)]/60">
            {artist.bio}
          </p>
        </div>
      </Link>
    </motion.div>
  )
}
