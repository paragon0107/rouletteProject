import {
  ROULETTE_ROTATION_BASE_TURNS,
  ROULETTE_SEGMENTS,
} from '../constants/roulette'
import type { AwardedPoint } from '../types/api'

function normalizeAngle(angle: number): number {
  return ((angle % 360) + 360) % 360
}

export function calculateNextRouletteRotation(
  currentRotation: number,
  awardedPoints: AwardedPoint,
): number {
  const targetSegment = ROULETTE_SEGMENTS.find((segment) => segment.points === awardedPoints)

  if (!targetSegment) {
    return currentRotation
  }

  const normalizedCurrent = normalizeAngle(currentRotation)
  const alignToTopAngle = 360 - targetSegment.centerAngle
  const delta = ROULETTE_ROTATION_BASE_TURNS * 360 + alignToTopAngle - normalizedCurrent

  return currentRotation + delta
}

export function buildRouletteGradient(): string {
  const segmentStyles = ROULETTE_SEGMENTS.map(
    (segment) => `${segment.colorClassName} ${segment.startAngle}deg ${segment.endAngle}deg`,
  )

  return `conic-gradient(from -90deg, ${segmentStyles.join(', ')})`
}
