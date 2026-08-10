type Props = {
  kind?: string
}

export function AtmosphereOverlay({ kind }: Props) {
  if (!kind || kind === 'none') return null

  if (kind === 'rain' || kind === 'window') {
    return (
      <div className="pointer-events-none absolute inset-0 overflow-hidden opacity-40">
        {Array.from({ length: 28 }).map((_, i) => (
          <span
            key={i}
            className="absolute top-[-10%] w-px bg-[rgba(42,36,32,0.35)]"
            style={{
              left: `${(i * 37) % 100}%`,
              height: `${12 + (i % 5) * 8}px`,
              animation: `rain-fall ${1.4 + (i % 5) * 0.25}s linear infinite`,
              animationDelay: `${(i % 10) * 0.12}s`,
            }}
          />
        ))}
      </div>
    )
  }

  if (kind === 'bus' || kind === 'train') {
    return (
      <div
        className="pointer-events-none absolute inset-x-0 bottom-0 h-24 opacity-30"
        style={{
          backgroundImage:
            'repeating-linear-gradient(90deg, transparent 0 40px, rgba(42,36,32,0.25) 40px 42px)',
          backgroundSize: '200% 100%',
          animation: 'road-scroll 8s linear infinite',
        }}
      />
    )
  }

  if (kind === 'mela' || kind === 'pujo') {
    return (
      <div className="pointer-events-none absolute inset-0 opacity-25">
        {Array.from({ length: 12 }).map((_, i) => (
          <span
            key={i}
            className="absolute rounded-full bg-[#e8b86d]"
            style={{
              width: 6 + (i % 3) * 4,
              height: 6 + (i % 3) * 4,
              left: `${8 + i * 7}%`,
              top: `${12 + (i % 4) * 10}%`,
              animation: 'soft-pulse 2.4s ease-in-out infinite',
              animationDelay: `${i * 0.15}s`,
            }}
          />
        ))}
      </div>
    )
  }

  return null
}
