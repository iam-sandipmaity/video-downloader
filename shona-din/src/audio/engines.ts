type ProgressCallback = (progress: number, duration: number) => void
type EndedCallback = () => void

export class AudioEngine {
  private audio: HTMLAudioElement
  private onProgress: ProgressCallback | null = null
  private onEnded: EndedCallback | null = null
  private raf = 0

  constructor() {
    this.audio = new Audio()
    this.audio.preload = 'metadata'
    this.audio.addEventListener('ended', () => this.onEnded?.())
    this.audio.addEventListener('timeupdate', () => {
      this.onProgress?.(this.audio.currentTime, this.audio.duration || 0)
    })
  }

  setHandlers(progress: ProgressCallback, ended: EndedCallback) {
    this.onProgress = progress
    this.onEnded = ended
  }

  async load(url: string) {
    if (this.audio.src === url) return
    this.audio.src = url
    this.audio.load()
  }

  async play() {
    try {
      await this.audio.play()
      return true
    } catch {
      return false
    }
  }

  pause() {
    this.audio.pause()
  }

  seek(time: number) {
    if (Number.isFinite(time)) this.audio.currentTime = time
  }

  setVolume(v: number) {
    this.audio.volume = Math.min(1, Math.max(0, v))
  }

  setMuted(muted: boolean) {
    this.audio.muted = muted
  }

  get currentTime() {
    return this.audio.currentTime
  }

  get duration() {
    return this.audio.duration || 0
  }

  get paused() {
    return this.audio.paused
  }

  destroy() {
    cancelAnimationFrame(this.raf)
    this.audio.pause()
    this.audio.src = ''
  }
}

export type AmbientKind =
  | 'rain'
  | 'bus'
  | 'train'
  | 'mela'
  | 'tea'
  | 'radio'
  | 'fan'
  | 'pujo'
  | 'none'

/** Subtle procedural ambience via Web Audio API — never autoplays music. */
export class AmbientEngine {
  private ctx: AudioContext | null = null
  private nodes: AudioNode[] = []
  private master: GainNode | null = null
  private kind: AmbientKind = 'none'
  private enabled = false

  private ensure() {
    if (!this.ctx) {
      this.ctx = new AudioContext()
      this.master = this.ctx.createGain()
      this.master.gain.value = 0.07
      this.master.connect(this.ctx.destination)
    }
    return this.ctx
  }

  private clear() {
    for (const n of this.nodes) {
      try {
        n.disconnect()
      } catch {
        /* ignore */
      }
    }
    this.nodes = []
  }

  private noiseBuffer(ctx: AudioContext, seconds = 2) {
    const buffer = ctx.createBuffer(1, ctx.sampleRate * seconds, ctx.sampleRate)
    const data = buffer.getChannelData(0)
    for (let i = 0; i < data.length; i++) data[i] = Math.random() * 2 - 1
    return buffer
  }

  private startNoise(
    ctx: AudioContext,
    filterType: BiquadFilterType,
    frequency: number,
    gainValue: number,
  ) {
    const src = ctx.createBufferSource()
    src.buffer = this.noiseBuffer(ctx)
    src.loop = true
    const filter = ctx.createBiquadFilter()
    filter.type = filterType
    filter.frequency.value = frequency
    const gain = ctx.createGain()
    gain.gain.value = gainValue
    src.connect(filter)
    filter.connect(gain)
    gain.connect(this.master!)
    src.start()
    this.nodes.push(src, filter, gain)
  }

  private startTone(ctx: AudioContext, freq: number, type: OscillatorType, gainValue: number) {
    const osc = ctx.createOscillator()
    osc.type = type
    osc.frequency.value = freq
    const gain = ctx.createGain()
    gain.gain.value = gainValue
    osc.connect(gain)
    gain.connect(this.master!)
    osc.start()
    this.nodes.push(osc, gain)
  }

  async setKind(kind: AmbientKind) {
    this.kind = kind
    if (!this.enabled || kind === 'none') {
      this.clear()
      return
    }
    const ctx = this.ensure()
    if (ctx.state === 'suspended') await ctx.resume()
    this.clear()

    switch (kind) {
      case 'rain':
        this.startNoise(ctx, 'lowpass', 900, 0.35)
        break
      case 'bus':
        this.startNoise(ctx, 'lowpass', 220, 0.25)
        this.startTone(ctx, 55, 'sine', 0.02)
        break
      case 'train':
        this.startNoise(ctx, 'bandpass', 180, 0.2)
        this.startTone(ctx, 70, 'triangle', 0.015)
        break
      case 'mela':
        this.startNoise(ctx, 'highpass', 400, 0.12)
        break
      case 'tea':
        this.startNoise(ctx, 'lowpass', 500, 0.1)
        break
      case 'radio':
        this.startNoise(ctx, 'bandpass', 1200, 0.18)
        break
      case 'fan':
        this.startTone(ctx, 90, 'sine', 0.025)
        this.startNoise(ctx, 'lowpass', 300, 0.08)
        break
      case 'pujo':
        this.startNoise(ctx, 'bandpass', 600, 0.1)
        this.startTone(ctx, 110, 'triangle', 0.01)
        break
      default:
        break
    }
  }

  async setEnabled(on: boolean) {
    this.enabled = on
    if (!on) {
      this.clear()
      return
    }
    await this.setKind(this.kind)
  }

  setVolume(v: number) {
    if (this.master) this.master.gain.value = Math.min(0.2, Math.max(0, v))
  }

  destroy() {
    this.clear()
    void this.ctx?.close()
    this.ctx = null
  }
}
