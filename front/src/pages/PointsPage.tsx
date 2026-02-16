import { useQuery } from '@tanstack/react-query'
import { EmptyState } from '../components/common/EmptyState'
import { ErrorState } from '../components/common/ErrorState'
import { LoadingState } from '../components/common/LoadingState'
import { getErrorMessage } from '../lib/api-client'
import { formatDateTime, formatPoints, isExpired } from '../lib/format'
import { queryKeys } from '../services/query-keys'
import { getMyExpiringPoints, getMyPointBalance, getMyPointUnits } from '../services/user-api'
import type { PointUnitResponse } from '../types/api'

interface PointsPageProps {
  userId: number
}

function getPointStatusLabel(pointUnit: PointUnitResponse): string {
  if (pointUnit.status === 'EXPIRED' || isExpired(pointUnit.expiresAt)) {
    return '만료됨'
  }

  if (pointUnit.status === 'USED') {
    return '사용됨'
  }

  if (pointUnit.status === 'CANCELED') {
    return '취소됨'
  }

  return '사용 가능'
}

export function PointsPage({ userId }: PointsPageProps) {
  const pointListQuery = useQuery({
    queryKey: queryKeys.pointList,
    queryFn: () => getMyPointUnits(userId),
  })

  const pointBalanceQuery = useQuery({
    queryKey: queryKeys.pointBalance,
    queryFn: () => getMyPointBalance(userId),
  })

  const expiringPointsQuery = useQuery({
    queryKey: queryKeys.expiringPoints(7),
    queryFn: () => getMyExpiringPoints(userId, 7),
  })

  if (pointListQuery.isLoading || pointBalanceQuery.isLoading || expiringPointsQuery.isLoading) {
    return <LoadingState label="포인트 정보를 불러오는 중입니다..." />
  }

  if (pointListQuery.isError) {
    return (
      <ErrorState
        message={getErrorMessage(pointListQuery.error, '포인트 목록을 불러오지 못했습니다.')}
        onRetry={() => {
          void pointListQuery.refetch()
        }}
      />
    )
  }

  if (pointBalanceQuery.isError) {
    return (
      <ErrorState
        message={getErrorMessage(pointBalanceQuery.error, '포인트 잔액을 불러오지 못했습니다.')}
        onRetry={() => {
          void pointBalanceQuery.refetch()
        }}
      />
    )
  }

  if (expiringPointsQuery.isError) {
    return (
      <ErrorState
        message={getErrorMessage(expiringPointsQuery.error, '만료 예정 포인트를 불러오지 못했습니다.')}
        onRetry={() => {
          void expiringPointsQuery.refetch()
        }}
      />
    )
  }

  if (!pointListQuery.data || !pointBalanceQuery.data || !expiringPointsQuery.data) {
    return <LoadingState label="포인트 정보를 불러오는 중입니다..." />
  }

  const pointList = pointListQuery.data
  const pointBalance = pointBalanceQuery.data
  const expiringPoints = expiringPointsQuery.data

  return (
    <section className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-medium text-slate-600">사용 가능 포인트</h2>
          <p className="mt-2 text-2xl font-bold text-slate-900">{formatPoints(pointBalance.availableBalance)}</p>
        </article>
        <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-medium text-slate-600">7일 내 만료 예정</h2>
          <p className="mt-2 text-2xl font-bold text-amber-600">
            {formatPoints(pointBalance.expiringWithin7Days)}
          </p>
        </article>
      </div>

      {expiringPoints.totalExpiringAmount > 0 ? (
        <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {expiringPoints.withinDays}일 내 만료 예정 포인트가 {formatPoints(expiringPoints.totalExpiringAmount)} 있습니다.
        </div>
      ) : null}

      <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">포인트 목록</h2>

        {pointList.items.length === 0 ? (
          <div className="mt-4">
            <EmptyState label="보유한 포인트가 없습니다." />
          </div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-600">
                  <th className="px-2 py-2">이벤트</th>
                  <th className="px-2 py-2">원금액</th>
                  <th className="px-2 py-2">잔여</th>
                  <th className="px-2 py-2">만료일</th>
                  <th className="px-2 py-2">상태</th>
                </tr>
              </thead>
              <tbody>
                {pointList.items.map((pointUnit) => (
                  <tr key={pointUnit.pointUnitId} className="border-b border-slate-100 text-slate-800">
                    <td className="px-2 py-2">{pointUnit.eventType}</td>
                    <td className="px-2 py-2">{formatPoints(pointUnit.originalAmount)}</td>
                    <td className="px-2 py-2">{formatPoints(pointUnit.remainingAmount)}</td>
                    <td className="px-2 py-2">{formatDateTime(pointUnit.expiresAt)}</td>
                    <td className="px-2 py-2">{getPointStatusLabel(pointUnit)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </article>
    </section>
  )
}
