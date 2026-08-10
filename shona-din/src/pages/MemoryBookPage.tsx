import { MemoryBook } from '@/components/MemoryBook'

export function MemoryBookPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <p className="font-bengali text-2xl text-[var(--color-faded-red)]">আমার স্মৃতি</p>
      <h1 className="font-display text-5xl text-[var(--color-brown)]">Amar Smriti</h1>
      <p className="mt-3 max-w-2xl font-display text-lg text-[var(--color-ink)]/65">
        Personal memory shelves — Ma-r Gaan, College Days, Late Night. Private, not social.
      </p>
      <div className="mt-8">
        <MemoryBook />
      </div>
    </div>
  )
}
