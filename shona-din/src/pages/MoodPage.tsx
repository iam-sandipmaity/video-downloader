import { MoodSelector } from '@/components/MoodSelector'
import { CassettePlayer } from '@/components/CassettePlayer'
import { Queue } from '@/components/Queue'

export function MoodPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <p className="font-bengali text-2xl text-[var(--color-faded-red)]">মুড ইঞ্জিন</p>
      <h1 className="font-display text-5xl text-[var(--color-brown)]">Mood Engine</h1>
      <p className="mt-3 max-w-2xl font-display text-lg text-[var(--color-ink)]/65">
        Rain + Night + Alone. Mela + Evening + Childhood. Build the feeling, get the cassette.
      </p>
      <div className="mt-8 grid gap-8 lg:grid-cols-[0.95fr_1.05fr]">
        <MoodSelector />
        <div className="space-y-6">
          <CassettePlayer variant="compact" />
          <Queue />
        </div>
      </div>
    </div>
  )
}
