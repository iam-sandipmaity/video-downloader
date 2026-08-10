import { AnimatePresence, motion } from 'framer-motion'
import { usePlayer } from '@/context/PlayerContext'

export function FilmCountdown() {
  const { filmCountdown } = usePlayer()
  return (
    <AnimatePresence>
      {filmCountdown && (
        <motion.div
          className="fixed inset-0 z-[80] flex items-center justify-center bg-[#1a120c]/88"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            className="font-mono text-7xl text-[#f3e6cf]"
            initial={{ scale: 0.7, opacity: 0 }}
            animate={{ scale: [0.7, 1.1, 1], opacity: 1 }}
            transition={{ duration: 0.8 }}
          >
            3
          </motion.div>
          <motion.div
            className="absolute font-mono text-7xl text-[#f3e6cf]"
            initial={{ opacity: 0 }}
            animate={{ opacity: [0, 1, 0] }}
            transition={{ delay: 0.7, duration: 0.7 }}
          >
            2
          </motion.div>
          <motion.div
            className="absolute font-mono text-7xl text-[#f3e6cf]"
            initial={{ opacity: 0 }}
            animate={{ opacity: [0, 1, 0] }}
            transition={{ delay: 1.4, duration: 0.7 }}
          >
            1
          </motion.div>
          <p className="absolute bottom-16 font-bengali text-xl text-[#e8d6b5]/80">
            সেই দিন..
          </p>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
