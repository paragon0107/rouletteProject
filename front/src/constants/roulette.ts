import type { AwardedPoint } from '../types/api'

interface RouletteSegmentConfig {
  points: AwardedPoint
  weightPercent: number
  colorClassName: string
}

export interface RouletteSegment extends RouletteSegmentConfig {
  startAngle: number
  endAngle: number
  centerAngle: number
}

const ROULETTE_SEGMENT_CONFIGS: RouletteSegmentConfig[] = [
  {
    points: 100,
    weightPercent: 40,
    colorClassName: '#38bdf8',
  },
  {
    points: 300,
    weightPercent: 30,
    colorClassName: '#34d399',
  },
  {
    points: 500,
    weightPercent: 20,
    colorClassName: '#f59e0b',
  },
  {
    points: 1000,
    weightPercent: 10,
    colorClassName: '#f97316',
  },
]

function buildRouletteSegments(configs: RouletteSegmentConfig[]): RouletteSegment[] {
  const totalWeight = configs.reduce((sum, segment) => sum + segment.weightPercent, 0)

  if (totalWeight <= 0) {
    return []
  }

  let currentStartAngle = 0

  return configs.map((segment, index) => {
    const isLast = index === configs.length - 1
    const sectionAngle = (segment.weightPercent / totalWeight) * 360
    const startAngle = currentStartAngle
    const endAngle = isLast ? 360 : startAngle + sectionAngle
    const centerAngle = startAngle + (endAngle - startAngle) / 2

    currentStartAngle = endAngle

    return {
      ...segment,
      startAngle,
      endAngle,
      centerAngle,
    }
  })
}

export const ROULETTE_SEGMENTS = buildRouletteSegments(ROULETTE_SEGMENT_CONFIGS)

export const ROULETTE_ROTATION_BASE_TURNS = 6
