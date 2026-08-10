import { artists } from '@/data/artists'
import { ArtistCollection } from '@/components/ArtistCollection'

export function VoicesPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <p className="font-bengali text-2xl text-[var(--color-faded-red)]">কণ্ঠস্বর</p>
      <h1 className="font-display text-5xl text-[var(--color-brown)]">Voices</h1>
      <p className="mt-3 max-w-2xl font-display text-lg text-[var(--color-ink)]/65">
        Not artist cards — nostalgic collections. Each voice opens a shelf of evenings.
      </p>
      <div className="mt-8 grid gap-4 lg:grid-cols-2">
        {artists.map((artist, i) => (
          <ArtistCollection key={artist.id} artist={artist} index={i} />
        ))}
      </div>
    </div>
  )
}
