import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { AudioEngine, AmbientEngine, type AmbientKind } from '@/audio/engines'
import { cassetteLabels } from '@/data/seasons'
import { trackById, tracks } from '@/data/tracks'
import type { Track } from '@/types'

type PlayerContextValue = {
  currentTrack: Track | null
  isPlaying: boolean
  queue: string[]
  queueIndex: number
  volume: number
  muted: boolean
  shuffle: boolean
  repeat: 'off' | 'all' | 'one'
  progress: number
  duration: number
  activeMemoryId: string | null
  cassetteLabel: string
  side: 'A' | 'B'
  favorites: string[]
  recentlyPlayed: string[]
  ambientEnabled: boolean
  filmCountdown: boolean
  playQueue: (ids: string[], startIndex?: number, memoryId?: string | null) => void
  togglePlay: () => void
  play: () => void
  pause: () => void
  next: () => void
  prev: () => void
  seek: (t: number) => void
  setVolume: (v: number) => void
  toggleMute: () => void
  toggleShuffle: () => void
  cycleRepeat: () => void
  toggleFavorite: (id?: string) => void
  setCassetteLabel: (label: string) => void
  cycleCassetteLabel: () => void
  toggleSide: () => void
  setAmbientEnabled: (on: boolean) => void
  setAmbientKind: (kind: AmbientKind) => void
  setActiveMemoryId: (id: string | null) => void
  triggerCountdownThenPlay: () => void
}

const PlayerContext = createContext<PlayerContextValue | null>(null)

const FAV_KEY = 'shona-din-favorites'
const RECENT_KEY = 'shona-din-recent'
const LABEL_KEY = 'shona-din-cassette-label'

function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

export function PlayerProvider({ children }: { children: ReactNode }) {
  const engine = useRef(new AudioEngine())
  const ambient = useRef(new AmbientEngine())

  const [queue, setQueue] = useState<string[]>(tracks.slice(0, 8).map((t) => t.id))
  const [queueIndex, setQueueIndex] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [volume, setVolumeState] = useState(0.8)
  const [muted, setMuted] = useState(false)
  const [shuffle, setShuffle] = useState(false)
  const [repeat, setRepeat] = useState<'off' | 'all' | 'one'>('off')
  const [progress, setProgress] = useState(0)
  const [duration, setDuration] = useState(0)
  const [activeMemoryId, setActiveMemoryId] = useState<string | null>(null)
  const [cassetteLabel, setCassetteLabelState] = useState(
    () => loadJson(LABEL_KEY, cassetteLabels[0]),
  )
  const [side, setSide] = useState<'A' | 'B'>('A')
  const [favorites, setFavorites] = useState<string[]>(() => loadJson(FAV_KEY, []))
  const [recentlyPlayed, setRecentlyPlayed] = useState<string[]>(() =>
    loadJson(RECENT_KEY, []),
  )
  const [ambientEnabled, setAmbientEnabledState] = useState(false)
  const [filmCountdown, setFilmCountdown] = useState(false)

  const currentTrack = trackById[queue[queueIndex]] ?? null
  const queueRef = useRef(queue)
  const indexRef = useRef(queueIndex)
  const shuffleRef = useRef(shuffle)
  const repeatRef = useRef(repeat)
  const isPlayingRef = useRef(isPlaying)

  queueRef.current = queue
  indexRef.current = queueIndex
  shuffleRef.current = shuffle
  repeatRef.current = repeat
  isPlayingRef.current = isPlaying

  const pushRecent = useCallback((id: string) => {
    setRecentlyPlayed((prev) => {
      const next = [id, ...prev.filter((x) => x !== id)].slice(0, 24)
      localStorage.setItem(RECENT_KEY, JSON.stringify(next))
      return next
    })
  }, [])

  const loadAndMaybePlay = useCallback(
    async (track: Track | null, shouldPlay: boolean) => {
      if (!track) return
      await engine.current.load(track.audioUrl)
      engine.current.setVolume(volume)
      engine.current.setMuted(muted)
      if (shouldPlay) {
        const ok = await engine.current.play()
        setIsPlaying(ok)
        if (ok) pushRecent(track.id)
      } else {
        engine.current.pause()
        setIsPlaying(false)
      }
    },
    [muted, pushRecent, volume],
  )

  const goToIndex = useCallback(
    async (nextIndex: number, shouldPlay = true) => {
      const q = queueRef.current
      if (!q.length) return
      const wrapped = ((nextIndex % q.length) + q.length) % q.length
      setQueueIndex(wrapped)
      const track = trackById[q[wrapped]]
      await loadAndMaybePlay(track, shouldPlay)
    },
    [loadAndMaybePlay],
  )

  const next = useCallback(() => {
    const q = queueRef.current
    const idx = indexRef.current
    if (!q.length) return
    if (shuffleRef.current && q.length > 1) {
      let n = Math.floor(Math.random() * q.length)
      if (n === idx) n = (n + 1) % q.length
      void goToIndex(n, true)
      return
    }
    if (idx >= q.length - 1) {
      if (repeatRef.current === 'all') void goToIndex(0, true)
      else {
        engine.current.pause()
        setIsPlaying(false)
      }
      return
    }
    void goToIndex(idx + 1, true)
  }, [goToIndex])

  const prev = useCallback(() => {
    if (engine.current.currentTime > 3) {
      engine.current.seek(0)
      return
    }
    void goToIndex(indexRef.current - 1, true)
  }, [goToIndex])

  useEffect(() => {
    const eng = engine.current
    eng.setHandlers(
      (p, d) => {
        setProgress(p)
        setDuration(d)
      },
      () => {
        if (repeatRef.current === 'one') {
          eng.seek(0)
          void eng.play()
          return
        }
        next()
      },
    )
    return () => {
      eng.destroy()
      ambient.current.destroy()
    }
  }, [next])

  const playQueue = useCallback(
    (ids: string[], startIndex = 0, memoryId: string | null = null) => {
      if (!ids.length) return
      setQueue(ids)
      setQueueIndex(startIndex)
      setActiveMemoryId(memoryId)
      const track = trackById[ids[startIndex]]
      void loadAndMaybePlay(track, true)
    },
    [loadAndMaybePlay],
  )

  const play = useCallback(() => {
    void (async () => {
      if (!currentTrack) return
      const ok = await engine.current.play()
      setIsPlaying(ok)
      if (ok) pushRecent(currentTrack.id)
    })()
  }, [currentTrack, pushRecent])

  const pause = useCallback(() => {
    engine.current.pause()
    setIsPlaying(false)
  }, [])

  const togglePlay = useCallback(() => {
    if (isPlayingRef.current) pause()
    else play()
  }, [pause, play])

  const seek = useCallback((t: number) => {
    engine.current.seek(t)
    setProgress(t)
  }, [])

  const setVolume = useCallback((v: number) => {
    setVolumeState(v)
    engine.current.setVolume(v)
    if (v > 0) setMuted(false)
  }, [])

  const toggleMute = useCallback(() => {
    setMuted((m) => {
      const nextMuted = !m
      engine.current.setMuted(nextMuted)
      return nextMuted
    })
  }, [])

  const toggleShuffle = useCallback(() => setShuffle((s) => !s), [])
  const cycleRepeat = useCallback(() => {
    setRepeat((r) => (r === 'off' ? 'all' : r === 'all' ? 'one' : 'off'))
  }, [])

  const toggleFavorite = useCallback((id?: string) => {
    const target = id ?? queueRef.current[indexRef.current]
    if (!target) return
    setFavorites((prev) => {
      const next = prev.includes(target)
        ? prev.filter((x) => x !== target)
        : [...prev, target]
      localStorage.setItem(FAV_KEY, JSON.stringify(next))
      return next
    })
  }, [])

  const setCassetteLabel = useCallback((label: string) => {
    setCassetteLabelState(label)
    localStorage.setItem(LABEL_KEY, JSON.stringify(label))
  }, [])

  const cycleCassetteLabel = useCallback(() => {
    setCassetteLabelState((prev) => {
      const idx = cassetteLabels.indexOf(prev)
      const next = cassetteLabels[(idx + 1) % cassetteLabels.length]
      localStorage.setItem(LABEL_KEY, JSON.stringify(next))
      return next
    })
  }, [])

  const toggleSide = useCallback(() => {
    setSide((s) => (s === 'A' ? 'B' : 'A'))
  }, [])

  const setAmbientEnabled = useCallback((on: boolean) => {
    setAmbientEnabledState(on)
    void ambient.current.setEnabled(on)
  }, [])

  const setAmbientKind = useCallback((kind: AmbientKind) => {
    void ambient.current.setKind(kind)
  }, [])

  const triggerCountdownThenPlay = useCallback(() => {
    setFilmCountdown(true)
    window.setTimeout(() => {
      setFilmCountdown(false)
      play()
    }, 2400)
  }, [play])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA') return
      if (e.code === 'Space') {
        e.preventDefault()
        togglePlay()
      } else if (e.code === 'ArrowRight') next()
      else if (e.code === 'ArrowLeft') prev()
      else if (e.key === 'm' || e.key === 'M') toggleMute()
      else if (e.key === 's' || e.key === 'S') toggleShuffle()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [next, prev, toggleMute, togglePlay, toggleShuffle])

  const value = useMemo<PlayerContextValue>(
    () => ({
      currentTrack,
      isPlaying,
      queue,
      queueIndex,
      volume,
      muted,
      shuffle,
      repeat,
      progress,
      duration,
      activeMemoryId,
      cassetteLabel,
      side,
      favorites,
      recentlyPlayed,
      ambientEnabled,
      filmCountdown,
      playQueue,
      togglePlay,
      play,
      pause,
      next,
      prev,
      seek,
      setVolume,
      toggleMute,
      toggleShuffle,
      cycleRepeat,
      toggleFavorite,
      setCassetteLabel,
      cycleCassetteLabel,
      toggleSide,
      setAmbientEnabled,
      setAmbientKind,
      setActiveMemoryId,
      triggerCountdownThenPlay,
    }),
    [
      activeMemoryId,
      ambientEnabled,
      cassetteLabel,
      currentTrack,
      cycleCassetteLabel,
      cycleRepeat,
      duration,
      favorites,
      filmCountdown,
      isPlaying,
      muted,
      next,
      pause,
      play,
      playQueue,
      prev,
      progress,
      queue,
      queueIndex,
      recentlyPlayed,
      repeat,
      seek,
      setAmbientEnabled,
      setAmbientKind,
      setCassetteLabel,
      setVolume,
      shuffle,
      side,
      toggleFavorite,
      toggleMute,
      togglePlay,
      toggleShuffle,
      toggleSide,
      triggerCountdownThenPlay,
      volume,
    ],
  )

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>
}

export function usePlayer() {
  const ctx = useContext(PlayerContext)
  if (!ctx) throw new Error('usePlayer must be used within PlayerProvider')
  return ctx
}
