import { ROULETTE_SEGMENTS } from '../../constants/roulette'

interface RouletteWheelProps {
  rotation: number
  isSpinning: boolean
}

const WHEEL_SIZE = 280
const CENTER = WHEEL_SIZE / 2
const OUTER_RADIUS = 124
const INNER_RADIUS = 38

interface Point {
  x: number
  y: number
}

function toRadian(angleFromTop: number): number {
  return ((angleFromTop - 90) * Math.PI) / 180
}

function polarToCartesian(radius: number, angleFromTop: number): Point {
  const radian = toRadian(angleFromTop)

  return {
    x: CENTER + radius * Math.cos(radian),
    y: CENTER + radius * Math.sin(radian),
  }
}

function describeDonutSector(startAngle: number, endAngle: number): string {
  const outerStart = polarToCartesian(OUTER_RADIUS, startAngle)
  const outerEnd = polarToCartesian(OUTER_RADIUS, endAngle)
  const innerStart = polarToCartesian(INNER_RADIUS, endAngle)
  const innerEnd = polarToCartesian(INNER_RADIUS, startAngle)
  const sweepAngle = endAngle - startAngle
  const largeArcFlag = sweepAngle > 180 ? 1 : 0

  return [
    `M ${outerStart.x} ${outerStart.y}`,
    `A ${OUTER_RADIUS} ${OUTER_RADIUS} 0 ${largeArcFlag} 1 ${outerEnd.x} ${outerEnd.y}`,
    `L ${innerStart.x} ${innerStart.y}`,
    `A ${INNER_RADIUS} ${INNER_RADIUS} 0 ${largeArcFlag} 0 ${innerEnd.x} ${innerEnd.y}`,
    'Z',
  ].join(' ')
}

function getLabelFontSize(angleSize: number): number {
  if (angleSize <= 42) {
    return 9
  }

  if (angleSize <= 72) {
    return 10
  }

  return 11
}

function getLabelPosition(angle: number): Point {
  const labelRadius = (OUTER_RADIUS + INNER_RADIUS) / 2
  return polarToCartesian(labelRadius, angle)
}

export function RouletteWheel({ rotation, isSpinning }: RouletteWheelProps) {
  return (
    <div className="relative mx-auto h-[280px] w-[280px]">
      <div
        className="absolute left-1/2 top-0 z-20 h-0 w-0 -translate-x-1/2 border-x-[12px] border-b-0 border-t-[18px] border-x-transparent border-t-slate-900"
        aria-hidden
      />

      <div
        className="relative h-full w-full"
        style={{
          transform: `rotate(${rotation}deg)`,
          transition: isSpinning ? 'transform 2.6s cubic-bezier(0.2, 0.95, 0.3, 1)' : 'none',
        }}
      >
        <svg viewBox={`0 0 ${WHEEL_SIZE} ${WHEEL_SIZE}`} className="h-full w-full" aria-label="룰렛판">
          <circle cx={CENTER} cy={CENTER} r={OUTER_RADIUS + 6} fill="#0f172a" />

          {ROULETTE_SEGMENTS.map((segment) => (
            <path
              key={segment.points}
              d={describeDonutSector(segment.startAngle, segment.endAngle)}
              fill={segment.colorClassName}
              stroke="#0f172a"
              strokeWidth={1.5}
            />
          ))}

          {ROULETTE_SEGMENTS.map((segment) => {
            const angleSize = segment.endAngle - segment.startAngle
            const fontSize = getLabelFontSize(angleSize)
            const position = getLabelPosition(segment.centerAngle)

            return (
              <text
                key={`${segment.points}-label`}
                x={position.x}
                y={position.y}
                textAnchor="middle"
                dominantBaseline="middle"
                fill="#0f172a"
                fontSize={fontSize}
                fontWeight={700}
              >
                <tspan x={position.x} dy="-0.45em">
                  {segment.points}p
                </tspan>
                <tspan x={position.x} dy="1.1em">
                  {segment.weightPercent}%
                </tspan>
              </text>
            )
          })}
        </svg>
      </div>

      <div className="absolute left-1/2 top-1/2 z-10 flex h-16 w-16 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full border-4 border-white bg-slate-900 text-xs font-bold text-white">
        SPIN
      </div>
    </div>
  )
}
