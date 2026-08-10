import type { Memory } from '@/types'
import { findTracksByMemory, tracks } from './tracks'

const idsFor = (memoryId: string, fallbackCount = 6) => {
  const matched = findTracksByMemory(memoryId).map((t) => t.id)
  if (matched.length >= 4) return matched
  const extras = tracks
    .filter((t) => !matched.includes(t.id))
    .slice(0, fallbackCount - matched.length)
    .map((t) => t.id)
  return [...matched, ...extras]
}

export const memories: Memory[] = [
  {
    id: 'brishtir-shondhya',
    title: 'Brishtir Shondhya',
    bengaliTitle: 'বৃষ্টির সন্ধ্যা',
    description: 'Janala-r pashe boshe, brishti-r shobdo ar purono gaan.',
    mood: ['Quiet', 'Romantic', 'Nostalgic'],
    ambience: 'rain',
    artwork: 'rain',
    tracks: idsFor('brishtir-shondhya'),
    atmosphere: {
      gradient:
        'linear-gradient(160deg, #d5dde2 0%, #c4b8a4 40%, #8a9aa3 100%)',
      accent: '#6b7c85',
      overlay: 'rain',
    },
    icon: '🌧',
    playerStyle: 'cassette',
  },
  {
    id: 'melar-shondhya',
    title: 'Melar Shondhya',
    bengaliTitle: 'মেলার সন্ধ্যা',
    description: 'Ferris wheel, dust, aloo chop-er gondho, ar dure gaan.',
    mood: ['Joyful', 'Childhood', 'Wandering'],
    ambience: 'mela',
    artwork: 'mela',
    tracks: idsFor('melar-shondhya'),
    atmosphere: {
      gradient:
        'linear-gradient(165deg, #f0d4a8 0%, #e0a86a 45%, #c47a3a 100%)',
      accent: '#c47a3a',
      overlay: 'mela',
    },
    icon: '🎡',
    playerStyle: 'cassette',
  },
  {
    id: 'biye-bari',
    title: 'Biye Bari',
    bengaliTitle: 'বিয়ে বাড়ি',
    description: 'Alpona, marigold, family photographs, ar shei gaan-gulo.',
    mood: ['Festive', 'Family', 'Celebration'],
    ambience: 'none',
    artwork: 'wedding',
    tracks: idsFor('biye-bari'),
    atmosphere: {
      gradient:
        'linear-gradient(160deg, #f5e2c4 0%, #e8c48a 40%, #c9945a 100%)',
      accent: '#a65d4e',
      overlay: 'wedding',
    },
    icon: '💍',
    playerStyle: 'cassette',
    tags: ['biye-shokal', 'gaye-holud', 'bor-jatri', 'bou-bhat'],
  },
  {
    id: 'barber-shop',
    title: 'Barber Shop',
    bengaliTitle: 'নাপিতের দোকান',
    description: 'Ceiling fan, newspaper, radio static, para-r adda.',
    mood: ['Adda', 'Neighborhood', 'Afternoon'],
    ambience: 'fan',
    artwork: 'barber',
    tracks: idsFor('barber-shop'),
    atmosphere: {
      gradient:
        'linear-gradient(170deg, #e8dcc8 0%, #cbb99a 50%, #8a7a68 100%)',
      accent: '#5a6a5a',
      overlay: 'barber',
    },
    icon: '💈',
    playerStyle: 'radio',
  },
  {
    id: 'bus-journey',
    title: 'Bus Journey',
    bengaliTitle: 'বাসের জানালা',
    description: 'Ticket stub, road lights, Kolkata sliding past the glass.',
    mood: ['Journey', 'Window', 'Sunset'],
    ambience: 'bus',
    artwork: 'bus',
    tracks: idsFor('bus-journey'),
    atmosphere: {
      gradient:
        'linear-gradient(155deg, #f0d9b0 0%, #d4a878 40%, #7a8a96 100%)',
      accent: '#7a5a3a',
      overlay: 'bus',
    },
    icon: '🚌',
    playerStyle: 'compact',
  },
  {
    id: 'train-journey',
    title: 'Train Journey',
    bengaliTitle: 'ট্রেনের পথ',
    description: 'Countryside blurring by, cassette beside the window.',
    mood: ['Journey', 'Vast', 'Evening'],
    ambience: 'train',
    artwork: 'train',
    tracks: idsFor('train-journey'),
    atmosphere: {
      gradient:
        'linear-gradient(160deg, #e6d2b0 0%, #b8a078 35%, #6b7a5a 100%)',
      accent: '#5a4a3a',
      overlay: 'train',
    },
    icon: '🚂',
    playerStyle: 'cassette',
  },
  {
    id: 'cha-er-dokan',
    title: 'Cha-er Dokan',
    bengaliTitle: 'চা-এর দোকান',
    description: 'Cutting chai, wooden bench, transistor radio, evening adda.',
    mood: ['Adda', 'Friendship', 'Evening'],
    ambience: 'tea',
    artwork: 'tea',
    tracks: idsFor('cha-er-dokan'),
    atmosphere: {
      gradient:
        'linear-gradient(165deg, #e8d4b0 0%, #c4a070 45%, #5a4a3a 100%)',
      accent: '#8a5a2a',
      overlay: 'tea',
    },
    icon: '☕',
    playerStyle: 'radio',
  },
  {
    id: 'chhotobelar-din',
    title: 'Chhotobelar Din',
    bengaliTitle: 'ছোটবেলার দিন',
    description: 'Kites, cricket, school notebooks, Sunday television.',
    mood: ['Playful', 'Childhood', 'Warm'],
    ambience: 'none',
    artwork: 'childhood',
    tracks: idsFor('chhotobelar-din'),
    atmosphere: {
      gradient:
        'linear-gradient(160deg, #f5e8c8 0%, #e0c888 40%, #88a868 100%)',
      accent: '#c47a3a',
      overlay: 'childhood',
    },
    icon: '🪁',
    playerStyle: 'cassette',
  },
  {
    id: 'dadu-radio',
    title: 'Dadu-r Radio',
    bengaliTitle: 'দাদুর রেডিও',
    description: 'Akashbani, frequency dial, warm tubes, family living room.',
    mood: ['Vintage', 'Family', 'Quiet'],
    ambience: 'radio',
    artwork: 'radio',
    tracks: idsFor('dadu-radio'),
    atmosphere: {
      gradient:
        'linear-gradient(170deg, #e8dcc4 0%, #b8a888 50%, #4a3a2a 100%)',
      accent: '#3d5a45',
      overlay: 'radio',
    },
    icon: '📻',
    playerStyle: 'radio',
  },
  {
    id: 'pujo-pandel',
    title: 'Pujor Pandel Ghora',
    bengaliTitle: 'পূজোর প্যান্ডেল ঘোরা',
    description: 'Lights, dhak rhythm in the distance, para in full bloom.',
    mood: ['Festive', 'Friends', 'Night'],
    ambience: 'pujo',
    artwork: 'pujo',
    tracks: idsFor('pujo-pandel'),
    atmosphere: {
      gradient:
        'linear-gradient(160deg, #f0d0a0 0%, #d4885a 40%, #8a3a2a 100%)',
      accent: '#b85c38',
      overlay: 'pujo',
    },
    icon: '🪔',
    playerStyle: 'cassette',
  },
  {
    id: 'college-days',
    title: 'College-er Purono Din',
    bengaliTitle: 'কলেজের পুরোনো দিন',
    description: 'Coffee House adda, books, rain on College Street.',
    mood: ['Youth', 'Adda', 'Nostalgic'],
    ambience: 'tea',
    artwork: 'college',
    tracks: idsFor('college-days'),
    atmosphere: {
      gradient:
        'linear-gradient(160deg, #e4d8c4 0%, #a8b0a0 45%, #5a6a68 100%)',
      accent: '#3d5a45',
      overlay: 'college',
    },
    icon: '📚',
    playerStyle: 'cassette',
  },
  {
    id: 'ma-cassette',
    title: "Ma-r Cassette Collection",
    bengaliTitle: 'মার ক্যাসেট',
    description: 'Handwritten labels, SIDE A worn thin from love.',
    mood: ['Family', 'Intimate', 'Nostalgic'],
    ambience: 'none',
    artwork: 'cassette',
    tracks: idsFor('ma-cassette'),
    atmosphere: {
      gradient:
        'linear-gradient(165deg, #f0e2c8 0%, #d4b888 50%, #8a6a4a 100%)',
      accent: '#a65d4e',
      overlay: 'cassette',
    },
    icon: '📼',
    playerStyle: 'cassette',
  },
  {
    id: 'ekla-janala',
    title: 'Ekla Janala-r Pashe',
    bengaliTitle: 'একলা জানালার পাশে',
    description: 'One window, one lamp, one song that knows you.',
    mood: ['Alone', 'Quiet', 'Night'],
    ambience: 'rain',
    artwork: 'window',
    tracks: idsFor('ekla-janala'),
    atmosphere: {
      gradient:
        'linear-gradient(170deg, #d8d0c0 0%, #9a9080 45%, #4a4540 100%)',
      accent: '#6b7c85',
      overlay: 'window',
    },
    icon: '🪟',
    playerStyle: 'cassette',
  },
  {
    id: 'sunday-afternoon',
    title: 'Sunday Afternoon',
    bengaliTitle: 'রবিবারের দুপুর',
    description: 'Slow ceiling fan, leftover lunch, radio humming.',
    mood: ['Lazy', 'Warm', 'Home'],
    ambience: 'fan',
    artwork: 'sunday',
    tracks: idsFor('sunday-afternoon'),
    atmosphere: {
      gradient:
        'linear-gradient(160deg, #f5ebd4 0%, #e0c8a0 50%, #b89868 100%)',
      accent: '#c4a35a',
      overlay: 'sunday',
    },
    icon: '☀️',
    playerStyle: 'radio',
  },
  {
    id: 'nostalgic-night',
    title: 'Nostalgic Kolkata Night',
    bengaliTitle: 'কলকাতার রাত',
    description: 'Yellow taxi lights, Park Street hush, old film feeling.',
    mood: ['Night', 'Cinematic', 'Nostalgic'],
    ambience: 'tea',
    artwork: 'night',
    tracks: idsFor('nostalgic-night'),
    atmosphere: {
      gradient:
        'linear-gradient(170deg, #c8b898 0%, #6a5a4a 40%, #2a2420 100%)',
      accent: '#e8b86d',
      overlay: 'night',
    },
    icon: '🌙',
    playerStyle: 'cassette',
  },
  {
    id: 'mela-ferar-poth',
    title: 'Mela Theke Ferar Poth',
    bengaliTitle: 'মেলা থেকে ফেরার পথ',
    description: 'Tired feet, sugarcane stick, songs on the way home.',
    mood: ['Wandering', 'Gentle', 'Evening'],
    ambience: 'mela',
    artwork: 'mela-return',
    tracks: idsFor('mela-ferar-poth'),
    atmosphere: {
      gradient:
        'linear-gradient(160deg, #ead4a8 0%, #c89860 45%, #6a5a48 100%)',
      accent: '#a65d4e',
      overlay: 'mela',
    },
    icon: '🛤',
    playerStyle: 'compact',
  },
]

export const memoryById = Object.fromEntries(
  memories.map((m) => [m.id, m]),
) as Record<string, Memory>

export const featuredMemories = [
  'brishtir-shondhya',
  'melar-shondhya',
  'biye-bari',
  'bus-journey',
  'cha-er-dokan',
  'dadu-radio',
  'train-journey',
  'chhotobelar-din',
].map((id) => memoryById[id])
