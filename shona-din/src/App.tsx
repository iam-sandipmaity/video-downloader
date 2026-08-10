import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { PlayerProvider } from '@/context/PlayerContext'
import { MemoryBookProvider } from '@/context/MemoryBookContext'
import { SiteNav } from '@/components/SiteNav'
import { FilmCountdown } from '@/components/FilmCountdown'
import { HomePage } from '@/pages/HomePage'
import { MemoryExperiencePage } from '@/pages/MemoryExperiencePage'
import { VoicesPage } from '@/pages/VoicesPage'
import { ArtistPage } from '@/pages/ArtistPage'
import { KolkataPage } from '@/pages/KolkataPage'
import { SeasonsPage } from '@/pages/SeasonsPage'
import { PujoPage } from '@/pages/PujoPage'
import { MoodPage } from '@/pages/MoodPage'
import { MemoryBookPage } from '@/pages/MemoryBookPage'

export default function App() {
  return (
    <PlayerProvider>
      <MemoryBookProvider>
        <BrowserRouter>
          <div className="paper-bg relative min-h-screen">
            <div className="film-grain" aria-hidden />
            <div className="vignette" aria-hidden />
            <SiteNav />
            <main className="relative z-10">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/memory/:memoryId" element={<MemoryExperiencePage />} />
                <Route path="/voices" element={<VoicesPage />} />
                <Route path="/voices/:artistId" element={<ArtistPage />} />
                <Route path="/kolkata" element={<KolkataPage />} />
                <Route path="/seasons" element={<SeasonsPage />} />
                <Route path="/pujo" element={<PujoPage />} />
                <Route path="/mood" element={<MoodPage />} />
                <Route path="/memory-book" element={<MemoryBookPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </main>
            <footer className="relative z-10 border-t border-[rgba(42,36,32,0.1)] px-4 py-8 text-center">
              <p className="font-bengali text-[var(--color-ink)]/55">গান নয়, স্মৃতি শুনি।</p>
              <p className="mt-1 font-mono text-[10px] uppercase tracking-[0.16em] text-[var(--color-ink)]/40">
                SHONA DIN · Memory machine · Demo audio stand-ins
              </p>
            </footer>
            <FilmCountdown />
          </div>
        </BrowserRouter>
      </MemoryBookProvider>
    </PlayerProvider>
  )
}
