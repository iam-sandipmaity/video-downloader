# SHONA DIN

**Gaan noy, smriti shuni.**

A Bengali nostalgia music experience — a digital memory machine organized around rainy evenings, para culture, Pujo, bus windows, tea stalls, and old cassette shelves rather than genres.

## Stack

- React + TypeScript
- Vite
- Tailwind CSS
- Framer Motion
- HTML5 Audio + Web Audio (ambient layers)

## Run

```bash
cd shona-din
npm install
npm run dev
```

Open the printed local URL (default `http://localhost:5173`).

## Build

```bash
npm run build
npm run preview
```

## Notes

- Music metadata is culturally curated (Bengali / Indian classics).
- Track `audioUrl` values currently use royalty-free demo stand-ins so playback works without licensed masters. Swap URLs in `src/data/tracks.ts` when you attach a real catalog.
- Ambient layers (rain, bus, radio static, etc.) are procedural and optional — never autoplay music without a user gesture.
- Personal memory shelves persist in `localStorage`.

## Keyboard

- `Space` play/pause
- `←` / `→` previous / next
- `M` mute
- `S` shuffle
