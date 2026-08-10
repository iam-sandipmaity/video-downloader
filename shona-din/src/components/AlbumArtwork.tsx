type Props = {
  src: string
  alt?: string
  className?: string
}

export function AlbumArtwork({ src, alt = '', className = '' }: Props) {
  return (
    <div
      className={`overflow-hidden border border-[rgba(42,36,32,0.15)] bg-[var(--color-aged)] ${className}`}
    >
      <img src={src} alt={alt} className="h-full w-full object-cover" loading="lazy" />
    </div>
  )
}
