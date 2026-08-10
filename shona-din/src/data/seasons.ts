import type { Season, KolkataLocation, PujoDay, MoodCombo } from '@/types'
import { tracks } from './tracks'

const bySeason = (id: string) =>
  tracks.filter((t) => t.seasons.includes(id)).map((t) => t.id)

const byLocation = (id: string) =>
  tracks.filter((t) => t.locations.includes(id)).map((t) => t.id)

const byMemoryish = (...keys: string[]) =>
  tracks
    .filter((t) =>
      keys.some(
        (k) =>
          t.memories.includes(k) ||
          t.moods.includes(k) ||
          t.seasons.includes(k),
      ),
    )
    .map((t) => t.id)

export const seasons: Season[] = [
  {
    id: 'boishakh',
    title: 'Boishakh',
    bengaliTitle: 'বৈশাখ',
    description: 'Warm afternoon, noboborsho streets, marigold and drums.',
    atmosphere: 'Warm · Celebratory · Bright dust',
    tracks: bySeason('boishakh'),
    accent: '#c47a3a',
  },
  {
    id: 'borsha',
    title: 'Borsha',
    bengaliTitle: 'বর্ষা',
    description: 'Rain on glass, cloudy rooms, slow cassette evenings.',
    atmosphere: 'Wet · Quiet · Melancholy',
    tracks: bySeason('borsha'),
    accent: '#6b7c85',
  },
  {
    id: 'sharat',
    title: 'Sharat',
    bengaliTitle: 'শরৎ',
    description: 'Clear skies, white clouds, Pujo approaching.',
    atmosphere: 'Clear · Anticipating · Soft light',
    tracks: bySeason('sharat'),
    accent: '#c4a35a',
  },
  {
    id: 'pujo',
    title: 'Pujo',
    bengaliTitle: 'পূজো',
    description: 'Dhak, pandal hopping, para culture, childhood nights.',
    atmosphere: 'Festive · Crowded · Golden',
    tracks: bySeason('pujo'),
    accent: '#b85c38',
  },
  {
    id: 'hemanta',
    title: 'Hemanta',
    bengaliTitle: 'হেমন্ত',
    description: 'Cool evenings and softer music after dusk.',
    atmosphere: 'Cool · Soft · Intimate',
    tracks: bySeason('hemanta'),
    accent: '#8a7a68',
  },
  {
    id: 'sheet',
    title: 'Sheet',
    bengaliTitle: 'শীত',
    description: 'Winter afternoon, tea, blankets, old songs.',
    atmosphere: 'Muted · Warm indoors · Slow',
    tracks: bySeason('sheet'),
    accent: '#7a6a58',
  },
]

export const kolkataLocations: KolkataLocation[] = [
  {
    id: 'north-kolkata',
    title: 'North Kolkata',
    bengaliTitle: 'উত্তর কলকাতা',
    timeOfDay: '5:45 PM',
    vignette: 'Narrow lanes, green shutters, radio from a first-floor window.',
    tracks: byLocation('north-kolkata'),
    accent: '#8a6a4a',
  },
  {
    id: 'south-kolkata',
    title: 'South Kolkata',
    bengaliTitle: 'দক্ষিণ কলকাতা',
    timeOfDay: '7:10 PM',
    vignette: 'Quieter verandahs, lamp light, rain just finished.',
    tracks: byLocation('south-kolkata'),
    accent: '#6b7c85',
  },
  {
    id: 'college-street',
    title: 'College Street',
    bengaliTitle: 'কলেজ স্ট্রিট',
    timeOfDay: '6:30 PM',
    vignette: 'Rain has just started. Bookstalls pull plastic sheets over spines.',
    tracks: byLocation('college-street'),
    accent: '#3d5a45',
  },
  {
    id: 'gariahat',
    title: 'Gariahat',
    bengaliTitle: 'গড়িয়াহাট',
    timeOfDay: '8:00 PM',
    vignette: 'Crossing lights, evening shopping, songs from a shop radio.',
    tracks: byLocation('gariahat'),
    accent: '#a65d4e',
  },
  {
    id: 'shyambazar',
    title: 'Shyambazar',
    bengaliTitle: 'শ্যামবাজার',
    timeOfDay: '4:20 PM',
    vignette: 'Five-point crossing hum, tea glasses, afternoon adda.',
    tracks: byLocation('shyambazar'),
    accent: '#c4a35a',
  },
  {
    id: 'esplanade',
    title: 'Esplanade',
    bengaliTitle: 'এসপ্ল্যানেড',
    timeOfDay: '6:00 PM',
    vignette: 'Trams turning, colonial facades catching gold.',
    tracks: byLocation('esplanade'),
    accent: '#7a5a3a',
  },
  {
    id: 'park-street',
    title: 'Park Street',
    bengaliTitle: 'পার্ক স্ট্রিট',
    timeOfDay: '10:15 PM',
    vignette: 'Christmas lights of memory, slow taxis, old film glow.',
    tracks: byLocation('park-street'),
    accent: '#e8b86d',
  },
  {
    id: 'howrah',
    title: 'Howrah',
    bengaliTitle: 'হাওড়া',
    timeOfDay: '5:05 PM',
    vignette: 'Bridge traffic, river wind, trains announcing themselves.',
    tracks: byLocation('howrah'),
    accent: '#5a4a3a',
  },
  {
    id: 'tram-ride',
    title: 'Tram Ride',
    bengaliTitle: 'ট্রাম যাত্রা',
    timeOfDay: '3:40 PM',
    vignette: 'Wooden seats, bell ring, city sliding in frames.',
    tracks: byLocation('tram-ride'),
    accent: '#a65d4e',
  },
  {
    id: 'yellow-taxi',
    title: 'Yellow Taxi',
    bengaliTitle: 'হলুদ ট্যাক্সি',
    timeOfDay: '9:30 PM',
    vignette: 'Meter ticking, window half open, one song on the radio.',
    tracks: byLocation('yellow-taxi'),
    accent: '#c4a35a',
  },
  {
    id: 'para-adda',
    title: 'Para Adda',
    bengaliTitle: 'পাড়ার আড্ডা',
    timeOfDay: '7:45 PM',
    vignette: 'Yuva Sangha benches, club posters, someone tuning a radio.',
    tracks: byLocation('para-adda'),
    accent: '#3d5a45',
  },
  {
    id: 'coffee-house',
    title: 'Coffee House',
    bengaliTitle: 'কফি হাউস',
    timeOfDay: '5:15 PM',
    vignette: 'Cups, politics, poetry, and the same table for years.',
    tracks: byLocation('coffee-house'),
    accent: '#6b4a3a',
  },
  {
    id: 'maidan',
    title: 'Maidan',
    bengaliTitle: 'ময়দান',
    timeOfDay: '5:50 PM',
    vignette: 'Open sky, football dust, evening stretching long.',
    tracks: byLocation('maidan'),
    accent: '#5a7a4a',
  },
]

export const pujoDays: PujoDay[] = [
  {
    id: 'saptami',
    title: 'Saptami',
    bengaliTitle: 'সপ্তমী',
    description: 'First lights, first dhak, the para wakes early.',
    tracks: byMemoryish('pujo-pandel', 'saptami', 'pujo'),
    atmosphere: 'Beginning · Soft festive · Morning gold',
  },
  {
    id: 'ashtami',
    title: 'Ashtami',
    bengaliTitle: 'অষ্টমী',
    description: 'Crowds peak. Pushpanjali mornings, pandel nights.',
    tracks: byMemoryish('ashtami', 'pujo', 'aguner'),
    atmosphere: 'Full · Crowded · Bright',
  },
  {
    id: 'nabami',
    title: 'Nabami',
    bengaliTitle: 'নবমী',
    description: 'One more night of lights before goodbye begins.',
    tracks: byMemoryish('nabami', 'pujo-pandel'),
    atmosphere: 'Lingering · Emotional · Night',
  },
  {
    id: 'dashami',
    title: 'Dashami',
    bengaliTitle: 'দশমী',
    description: 'Sindur khela, immersion, and songs that feel like farewell.',
    tracks: byMemoryish('dashami', 'pujo'),
    atmosphere: 'Farewell · Tender · Bittersweet',
  },
]

export const moodCombos: MoodCombo[] = [
  {
    id: 'rain-night-alone',
    tags: ['rain', 'night', 'alone'],
    label: 'Rain + Night + Alone',
    labelBn: 'বৃষ্টি · রাত · একলা',
    tracks: byMemoryish('rain', 'night', 'alone', 'brishtir-shondhya', 'ekla-janala'),
  },
  {
    id: 'mela-evening-childhood',
    tags: ['mela', 'evening', 'childhood'],
    label: 'Mela + Evening + Childhood',
    labelBn: 'মেলা · সন্ধ্যা · ছোটবেলা',
    tracks: byMemoryish('mela', 'childhood', 'joyful', 'melar-shondhya'),
  },
  {
    id: 'wedding-family',
    tags: ['wedding', 'family', 'celebration'],
    label: 'Wedding + Family + Celebration',
    labelBn: 'বিয়ে · পরিবার · উৎসব',
    tracks: byMemoryish('biye-bari', 'celebration', 'family'),
  },
  {
    id: 'bus-window-sunset',
    tags: ['bus', 'window', 'sunset'],
    label: 'Bus + Window + Sunset',
    labelBn: 'বাস · জানালা · সূর্যাস্ত',
    tracks: byMemoryish('bus-journey', 'journey', 'window'),
  },
  {
    id: 'tea-adda-evening',
    tags: ['tea', 'adda', 'evening'],
    label: 'Tea Stall + Adda + Evening',
    labelBn: 'চা · আড্ডা · সন্ধ্যা',
    tracks: byMemoryish('cha-er-dokan', 'adda', 'evening', 'friendship'),
  },
  {
    id: 'winter-blanket',
    tags: ['winter', 'blanket', 'old'],
    label: 'Winter + Blanket + Old Songs',
    labelBn: 'শীত · কম্বল · পুরোনো গান',
    tracks: byMemoryish('sheet', 'quiet', 'family', 'dadu-radio'),
  },
  {
    id: 'pujo-friends-night',
    tags: ['pujo', 'friends', 'night'],
    label: 'Pujo + Friends + Night',
    labelBn: 'পূজো · বন্ধু · রাত',
    tracks: byMemoryish('pujo', 'pujo-pandel', 'festive'),
  },
]

export const cassetteLabels = [
  'Ma-r Collection',
  'Brishtir Din',
  'College Mix',
  'SIDE A — Kishore',
  'Pujor Smriti',
  'Train Window',
  'Late Night',
  'Dadu-r Favourites',
  'Cha-er Adda',
  'Shei Din..',
]

export const radioStations = [
  { name: 'AKASHBANI', freq: '100.1' },
  { name: 'YUVA SANGHA FM', freq: '91.8' },
  { name: 'PARA RADIO', freq: '88.4' },
  { name: 'NOBORSHEI', freq: '102.3' },
  { name: 'CHA DOKAN AIR', freq: '94.7' },
]
