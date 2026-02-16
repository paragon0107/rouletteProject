import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ROULETTE_SEGMENTS } from '../constants/roulette'
import { getErrorMessage } from '../lib/api-client'
import { formatPoints, getBudgetRemainRatio } from '../lib/format'
import { calculateNextRouletteRotation } from '../lib/roulette'
import { queryKeys } from '../services/query-keys'
import {
  createRouletteParticipation,
  getTodayBudget,
  getTodayRouletteParticipationStatus,
} from '../services/user-api'
import type { RouletteParticipationResponse } from '../types/api'
import { ErrorState } from '../components/common/ErrorState'
import { LoadingState } from '../components/common/LoadingState'
import { RouletteWheel } from '../components/roulette/RouletteWheel'

interface RoulettePageProps {
  userId: number
}

const ROULETTE_ANIMATION_MS = 2600

export function RoulettePage({ userId }: RoulettePageProps) {
  const queryClient = useQueryClient()
  const rotationRef = useRef(0)
  const animationTimerRef = useRef<number | null>(null)

  const [rotation, setRotation] = useState(0)
  const [isSpinning, setIsSpinning] = useState(false)
  const [spinMessage, setSpinMessage] = useState<string | null>(null)
  const [latestResult, setLatestResult] = useState<RouletteParticipationResponse | null>(null)

  const participationQuery = useQuery({
    queryKey: queryKeys.todayParticipation,
    queryFn: () => getTodayRouletteParticipationStatus(userId),
  })

  const budgetQuery = useQuery({
    queryKey: queryKeys.todayBudget,
    queryFn: () => getTodayBudget(userId),
    refetchInterval: 10_000,
  })

  const spinMutation = useMutation({
    mutationFn: () => createRouletteParticipation(userId),
    onSuccess: async (response) => {
      const nextRotation = calculateNextRouletteRotation(rotationRef.current, response.awardedPoints)
      rotationRef.current = nextRotation
      setRotation(nextRotation)
      setLatestResult(response)

      animationTimerRef.current = window.setTimeout(() => {
        setIsSpinning(false)
        setSpinMessage(`${response.awardedPoints}p를 획득했습니다.`)
      }, ROULETTE_ANIMATION_MS)

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.todayParticipation }),
        queryClient.invalidateQueries({ queryKey: queryKeys.todayBudget }),
        queryClient.invalidateQueries({ queryKey: queryKeys.pointList }),
        queryClient.invalidateQueries({ queryKey: queryKeys.pointBalance }),
        queryClient.invalidateQueries({ queryKey: queryKeys.expiringPoints(7) }),
      ])
    },
    onError: (error) => {
      setIsSpinning(false)
      setSpinMessage(getErrorMessage(error, '룰렛 참여에 실패했습니다.'))
    },
  })

  useEffect(() => {
    return () => {
      if (animationTimerRef.current !== null) {
        window.clearTimeout(animationTimerRef.current)
      }
    }
  }, [])

  if (participationQuery.isLoading || budgetQuery.isLoading) {
    return <LoadingState label="룰렛 정보를 불러오는 중입니다..." />
  }

  if (participationQuery.isError) {
    return (
      <ErrorState
        message={getErrorMessage(participationQuery.error, '오늘 참여 상태를 불러오지 못했습니다.')}
        onRetry={() => {
          void participationQuery.refetch()
        }}
      />
    )
  }

  if (budgetQuery.isError) {
    return (
      <ErrorState
        message={getErrorMessage(budgetQuery.error, '오늘 예산을 불러오지 못했습니다.')}
        onRetry={() => {
          void budgetQuery.refetch()
        }}
      />
    )
  }

  if (!participationQuery.data || !budgetQuery.data) {
    return <LoadingState label="룰렛 정보를 불러오는 중입니다..." />
  }

  const participationStatus = participationQuery.data
  const budget = budgetQuery.data

  const remainRatio = getBudgetRemainRatio(budget.totalBudget, budget.remainingBudget)
  const shouldWarnBudget = remainRatio < 0.05
  const hasParticipatedToday = participationStatus.participatedToday
  const canSpin = !hasParticipatedToday && !isSpinning && !spinMutation.isPending

  function handleSpin() {
    if (!canSpin) {
      if (hasParticipatedToday) {
        setSpinMessage('오늘은 이미 룰렛에 참여했습니다.')
      }
      return
    }

    if (animationTimerRef.current !== null) {
      window.clearTimeout(animationTimerRef.current)
      animationTimerRef.current = null
    }

    setSpinMessage(null)
    setIsSpinning(true)
    spinMutation.mutate()
  }

  return (
    <section className="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
      <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-bold text-slate-900">오늘의 룰렛</h2>
        <p className="mt-1 text-sm text-slate-600">하루 한 번 참여할 수 있습니다.</p>

        <div className="mt-5">
          <RouletteWheel rotation={rotation} isSpinning={isSpinning} />
        </div>

        <button
          type="button"
          disabled={!canSpin}
          onClick={handleSpin}
          className="mt-6 w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSpinning ? '회전 중...' : hasParticipatedToday ? '오늘 참여 완료' : '룰렛 돌리기'}
        </button>

        {spinMessage ? <p className="mt-3 text-sm text-slate-700">{spinMessage}</p> : null}

        {hasParticipatedToday && participationStatus.awardedPoints ? (
          <p className="mt-2 text-sm font-medium text-emerald-700">
            오늘 당첨 포인트: {formatPoints(participationStatus.awardedPoints)}
          </p>
        ) : null}

        {latestResult ? (
          <p className="mt-1 text-xs text-slate-500">포인트 만료일: {latestResult.pointExpiresAt}</p>
        ) : null}
      </article>

      <aside className="space-y-4">
        <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 className="text-base font-semibold text-slate-900">오늘 잔여 예산</h3>
          <p
            className={`mt-3 text-2xl font-bold ${
              shouldWarnBudget ? 'animate-pulse text-red-600' : 'text-slate-900'
            }`}
          >
            {formatPoints(budget.remainingBudget)}
          </p>
          <p className="mt-1 text-xs text-slate-600">
            총 {formatPoints(budget.totalBudget)} / 사용 {formatPoints(budget.usedBudget)}
          </p>
          {shouldWarnBudget ? (
            <p className="mt-2 text-xs font-medium text-red-600">잔여 예산이 5% 미만입니다.</p>
          ) : null}
        </article>

        <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 className="text-base font-semibold text-slate-900">확률 안내</h3>
          <ul className="mt-3 space-y-2 text-sm text-slate-700">
            {ROULETTE_SEGMENTS.map((segment) => (
              <li key={segment.points}>
                {formatPoints(segment.points)}: {segment.weightPercent}%
              </li>
            ))}
          </ul>
        </article>
      </aside>
    </section>
  )
}
