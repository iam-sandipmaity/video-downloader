export type Language = 'Bengali' | 'Hindi' | 'Other'

export type Track = {
  id: string
  title: string
  titleBn?: string
  artist: string
  album?: string
  movie?: string
  year?: number
  language: Language
  artwork: string
  audioUrl: string
  moods: string[]
  memories: string[]
  seasons: string[]
  locations: string[]
  decades: string[]
  featuredArtists?: string[]
  side?: 'A' | 'B'
  durationHint?: string
}

export type Memory = {
  id: string
  title: string
  bengaliTitle?: string
  description: string
  mood: string[]
  ambience?: 'rain' | 'bus' | 'train' | 'mela' | 'tea' | 'radio' | 'fan' | 'pujo' | 'none'
  artwork: string
  tracks: string[]
  atmosphere: {
    gradient: string
    accent: string
    overlay?: string
  }
  icon: string
  playerStyle?: 'cassette' | 'radio' | 'compact'
  tags?: string[]
}

export type Artist = {
  id: string
  name: string
  nameBn?: string
  epithet: string
  epithetBn?: string
  bio: string
  portrait: string
  eras: string[]
  relatedMemories: string[]
  relatedMoods: string[]
  essentialTracks: string[]
  timeline: { year: string; note: string }[]
}

export type Season = {
  id: string
  title: string
  bengaliTitle: string
  description: string
  atmosphere: string
  tracks: string[]
  accent: string
}

export type KolkataLocation = {
  id: string
  title: string
  bengaliTitle?: string
  timeOfDay: string
  vignette: string
  tracks: string[]
  accent: string
}

export type PujoDay = {
  id: string
  title: string
  bengaliTitle: string
  description: string
  tracks: string[]
  atmosphere: string
}

export type MoodCombo = {
  id: string
  tags: string[]
  label: string
  labelBn?: string
  tracks: string[]
}

export type PersonalMemory = {
  id: string
  name: string
  trackIds: string[]
  createdAt: number
}

export type PlayerState = {
  currentTrackId: string | null
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
}

export type AppView =
  | 'home'
  | 'memory'
  | 'voices'
  | 'artist'
  | 'kolkata'
  | 'seasons'
  | 'pujo'
  | 'mood'
  | 'memory-book'
  | 'radio'
