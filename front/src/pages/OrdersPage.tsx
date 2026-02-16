import { useQuery } from '@tanstack/react-query'
import { EmptyState } from '../components/common/EmptyState'
import { ErrorState } from '../components/common/ErrorState'
import { LoadingState } from '../components/common/LoadingState'
import { getErrorMessage } from '../lib/api-client'
import { formatDateTime, formatPoints } from '../lib/format'
import { queryKeys } from '../services/query-keys'
import { getMyOrders } from '../services/user-api'

interface OrdersPageProps {
  userId: number
}

export function OrdersPage({ userId }: OrdersPageProps) {
  const ordersQuery = useQuery({
    queryKey: queryKeys.orders,
    queryFn: () => getMyOrders(userId),
  })

  if (ordersQuery.isLoading) {
    return <LoadingState label="주문 내역을 불러오는 중입니다..." />
  }

  if (ordersQuery.isError) {
    return (
      <ErrorState
        message={getErrorMessage(ordersQuery.error, '주문 내역을 불러오지 못했습니다.')}
        onRetry={() => {
          void ordersQuery.refetch()
        }}
      />
    )
  }

  if (!ordersQuery.data) {
    return <LoadingState label="주문 내역을 불러오는 중입니다..." />
  }

  const orders = ordersQuery.data.items

  return (
    <section>
      <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">내 주문 내역</h2>

        {orders.length === 0 ? (
          <div className="mt-4">
            <EmptyState label="주문 내역이 없습니다." />
          </div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-600">
                  <th className="px-2 py-2">상품 ID</th>
                  <th className="px-2 py-2">수량</th>
                  <th className="px-2 py-2">사용 포인트</th>
                  <th className="px-2 py-2">주문 상태</th>
                  <th className="px-2 py-2">주문 일시</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.orderId} className="border-b border-slate-100 text-slate-800">
                    <td className="px-2 py-2">{order.productId}</td>
                    <td className="px-2 py-2">{order.quantity}</td>
                    <td className="px-2 py-2">{formatPoints(order.usedPoints)}</td>
                    <td className="px-2 py-2">{order.status}</td>
                    <td className="px-2 py-2">{formatDateTime(order.orderedAt)}</td>
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
