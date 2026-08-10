import { Link } from 'react-router-dom'
import { featuredMemories, memories } from '@/data/memories'
import { MemoryCard } from '@/components/MemoryCard'
import { SurpriseMe } from '@/components/SurpriseMe'
import { CassettePlayer } from '@/components/CassettePlayer'
import { motion } from 'framer-motion'

export function HomePage() {
  return (
    <div className="relative">
      <section className="relative mx-auto max-w-6xl overflow-hidden px-4 pb-10 pt-10 sm:pt-14">
        <div
          className="pointer-events-none absolute -right-10 top-0 h-72 w-72 opacity-30"
          style={{
            background:
              'radial-gradient(circle, rgba(232,184,109,0.55), transparent 70%)',
          }}
        />
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
          className="max-w-3xl"
        >
          <p className="font-bengali text-2xl text-[var(--color-faded-red)] sm:text-3xl">
            সেই দিন..
          </p>
          <h1 className="mt-2 font-display text-6xl font-semibold tracking-[0.04em] text-[var(--color-brown)] sm:text-7xl md:text-8xl">
            SHONA DIN
          </h1>
          <p className="mt-4 font-display text-2xl italic text-[var(--color-ink)]/75 sm:text-3xl">
            &ldquo;Gaan noy, smriti shuni.&rdquo;
          </p>
          <p className="mt-6 font-bengali text-xl text-[var(--color-brown)] sm:text-2xl">
            আজ কোন স্মৃতিতা শুনতে ইচ্ছে করছে?
          </p>
          <p className="mt-2 max-w-xl font-display text-lg text-[var(--color-ink)]/65">
            Aaj kon smritita shunte ichhe korche? Open a memory — not a genre.
          </p>
        </motion.div>

        <div className="mt-10 flex flex-wrap gap-3">
          <Link
            to="/memory/brishtir-shondhya"
            className="border border-[rgba(61,41,20,0.3)] bg-[var(--color-brown)] px-5 py-3 font-display text-lg text-[var(--color-cream)] no-underline"
          >
            Open the cupboard
          </Link>
          <Link
            to="/voices"
            className="border border-[rgba(61,41,20,0.2)] bg-[rgba(247,241,227,0.7)] px-5 py-3 font-display text-lg text-[var(--color-brown)] no-underline"
          >
            Voices
          </Link>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-8">
        <div className="mb-4 flex items-end justify-between gap-3">
          <div>
            <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-[var(--color-ink)]/45">
              Memory machine
            </p>
            <h2 className="font-display text-3xl text-[var(--color-brown)]">
              Kon gaan ta mone porche?
            </h2>
          </div>
        </div>
        <div className="no-scrollbar flex gap-4 overflow-x-auto pb-3 sm:grid sm:grid-cols-2 sm:overflow-visible lg:grid-cols-4">
          {featuredMemories.map((memory, i) => (
            <MemoryCard key={memory.id} memory={memory} index={i} large />
          ))}
        </div>
      </section>

      <section className="mx-auto grid max-w-6xl gap-8 px-4 py-8 lg:grid-cols-[1.1fr_0.9fr]">
        <CassettePlayer />
        <div className="space-y-6">
          <SurpriseMe />
          <div className="newspaper-clip p-5">
            <p className="font-mono text-[10px] uppercase tracking-[0.18em] text-[var(--color-ink)]/45">
              Newspaper clipping
            </p>
            <h3 className="mt-1 font-display text-2xl text-[var(--color-brown)]">
              Kishore Kumar Remembrance Evening
            </h3>
            <p className="mt-2 font-display text-[var(--color-ink)]/65">
              Para club notice board energy — click through to the Voices shelf.
            </p>
            <Link
              to="/voices/kishore-kumar"
              className="mt-3 inline-block font-mono text-[11px] uppercase tracking-[0.14em] text-[var(--color-faded-red)]"
            >
              Open collection →
            </Link>
          </div>
          <div className="ticket-stub p-4" style={{ transform: 'rotate(-1deg)' }}>
            <p className="font-mono text-[10px] uppercase tracking-[0.16em]">
              Bus ticket · Route 12C
            </p>
            <Link
              to="/memory/bus-journey"
              className="mt-1 block font-display text-xl text-[var(--color-brown)]"
            >
              Kolkatar bus journey — window seat
            </Link>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-16 pt-4">
        <h2 className="mb-4 font-display text-3xl text-[var(--color-brown)]">
          Aro smriti
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {memories
            .filter((m) => !featuredMemories.find((f) => f.id === m.id))
            .map((memory, i) => (
              <MemoryCard key={memory.id} memory={memory} index={i} />
            ))}
        </div>
      </section>
    </div>
  )
}
