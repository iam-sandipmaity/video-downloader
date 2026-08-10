import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { PersonalMemory } from '@/types'

type MemoryBookContextValue = {
  collections: PersonalMemory[]
  createCollection: (name: string) => void
  renameCollection: (id: string, name: string) => void
  deleteCollection: (id: string) => void
  addTrack: (collectionId: string, trackId: string) => void
  removeTrack: (collectionId: string, trackId: string) => void
}

const MemoryBookContext = createContext<MemoryBookContextValue | null>(null)
const KEY = 'shona-din-memory-book'

const defaults: PersonalMemory[] = [
  { id: 'mb-ma', name: 'Ma-r Gaan', trackIds: [], createdAt: Date.now() },
  { id: 'mb-baba', name: 'Babar Cassette', trackIds: [], createdAt: Date.now() },
  { id: 'mb-college', name: 'College Days', trackIds: [], createdAt: Date.now() },
  { id: 'mb-brishti', name: 'Brishtir Din', trackIds: [], createdAt: Date.now() },
  { id: 'mb-pujo', name: 'Pujor Smriti', trackIds: [], createdAt: Date.now() },
  { id: 'mb-train', name: 'Train Journey', trackIds: [], createdAt: Date.now() },
  { id: 'mb-late', name: 'Late Night', trackIds: [], createdAt: Date.now() },
]

function load(): PersonalMemory[] {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return defaults
    return JSON.parse(raw) as PersonalMemory[]
  } catch {
    return defaults
  }
}

function save(items: PersonalMemory[]) {
  localStorage.setItem(KEY, JSON.stringify(items))
}

export function MemoryBookProvider({ children }: { children: ReactNode }) {
  const [collections, setCollections] = useState<PersonalMemory[]>(load)

  const update = useCallback((fn: (prev: PersonalMemory[]) => PersonalMemory[]) => {
    setCollections((prev) => {
      const next = fn(prev)
      save(next)
      return next
    })
  }, [])

  const createCollection = useCallback(
    (name: string) => {
      const trimmed = name.trim()
      if (!trimmed) return
      update((prev) => [
        {
          id: `mb-${Date.now()}`,
          name: trimmed,
          trackIds: [],
          createdAt: Date.now(),
        },
        ...prev,
      ])
    },
    [update],
  )

  const renameCollection = useCallback(
    (id: string, name: string) => {
      update((prev) =>
        prev.map((c) => (c.id === id ? { ...c, name: name.trim() || c.name } : c)),
      )
    },
    [update],
  )

  const deleteCollection = useCallback(
    (id: string) => update((prev) => prev.filter((c) => c.id !== id)),
    [update],
  )

  const addTrack = useCallback(
    (collectionId: string, trackId: string) => {
      update((prev) =>
        prev.map((c) =>
          c.id === collectionId && !c.trackIds.includes(trackId)
            ? { ...c, trackIds: [...c.trackIds, trackId] }
            : c,
        ),
      )
    },
    [update],
  )

  const removeTrack = useCallback(
    (collectionId: string, trackId: string) => {
      update((prev) =>
        prev.map((c) =>
          c.id === collectionId
            ? { ...c, trackIds: c.trackIds.filter((t) => t !== trackId) }
            : c,
        ),
      )
    },
    [update],
  )

  const value = useMemo(
    () => ({
      collections,
      createCollection,
      renameCollection,
      deleteCollection,
      addTrack,
      removeTrack,
    }),
    [
      addTrack,
      collections,
      createCollection,
      deleteCollection,
      removeTrack,
      renameCollection,
    ],
  )

  return (
    <MemoryBookContext.Provider value={value}>{children}</MemoryBookContext.Provider>
  )
}

export function useMemoryBook() {
  const ctx = useContext(MemoryBookContext)
  if (!ctx) throw new Error('useMemoryBook must be used within MemoryBookProvider')
  return ctx
}
