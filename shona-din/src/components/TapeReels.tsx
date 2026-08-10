import { motion } from 'framer-motion'

type Props = {
  spinning: boolean
  size?: number
}

function Reel({ spinning, size = 56 }: Props) {
  return (
    <div
      className={`relative rounded-full border-2 border-[#2a1f14] ${spinning ? 'reel-spinning' : ''}`}
      style={{
        width: size,
        height: size,
        background:
          'repeating-conic-gradient(from 0deg, #3d2914 0deg 12deg, #5a4530 12deg 24deg)',
        boxShadow: 'inset 0 0 0 8px #c4a35a, inset 0 0 0 12px #2a1f14',
      }}
    >
      <div
        className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-full bg-[#ede0c8]"
        style={{ width: size * 0.18, height: size * 0.18 }}
      />
    </div>
  )
}

export function TapeReels({ spinning, size = 56 }: Props) {
  return (
    <div className="flex items-center justify-center gap-8">
      <Reel spinning={spinning} size={size} />
      <motion.div
        className="h-[2px] w-10 bg-[#2a1f14]/40"
        animate={spinning ? { opacity: [0.3, 0.7, 0.3] } : { opacity: 0.35 }}
        transition={{ repeat: Infinity, duration: 1.2 }}
      />
      <Reel spinning={spinning} size={size} />
    </div>
  )
}
