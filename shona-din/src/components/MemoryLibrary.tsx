import { memories } from '@/data/memories'
import { MemoryCard } from '@/components/MemoryCard'

export function MemoryLibrary() {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {memories.map((memory, i) => (
        <MemoryCard key={memory.id} memory={memory} index={i} />
      ))}
    </div>
  )
}
