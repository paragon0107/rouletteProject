import { useState, type ChangeEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { EmptyState } from '../components/common/EmptyState'
import { ErrorState } from '../components/common/ErrorState'
import { LoadingState } from '../components/common/LoadingState'
import { getErrorMessage } from '../lib/api-client'
import { formatPoints } from '../lib/format'
import { queryKeys } from '../services/query-keys'
import { createOrder, getMyPointBalance, getProducts } from '../services/user-api'
import type { ProductResponse } from '../types/api'

interface ProductsPageProps {
  userId: number
}

interface OrderMutationVariables {
  productId: number
  quantity: number
}

function clampQuantity(quantity: number): number {
  if (quantity < 1) {
    return 1
  }

  if (quantity > 20) {
    return 20
  }

  return quantity
}

function canPurchaseProduct(product: ProductResponse, balance: number, quantity: number): boolean {
  if (product.status !== 'ACTIVE') {
    return false
  }

  if (quantity < 1 || quantity > 20) {
    return false
  }

  if (product.stock < quantity) {
    return false
  }

  return balance >= product.pricePoints * quantity
}

function getUnavailableReason(product: ProductResponse, balance: number, quantity: number): string {
  if (product.status !== 'ACTIVE') {
    return '비활성 상품'
  }

  if (quantity < 1 || quantity > 20) {
    return '수량은 1~20개만 가능합니다.'
  }

  if (product.stock < quantity) {
    return product.stock <= 0 ? '재고 없음' : '재고 부족'
  }

  if (balance < product.pricePoints * quantity) {
    return '포인트 부족'
  }

  return ''
}

export function ProductsPage({ userId }: ProductsPageProps) {
  const queryClient = useQueryClient()
  const [orderMessage, setOrderMessage] = useState<string | null>(null)
  const [orderingProductId, setOrderingProductId] = useState<number | null>(null)
  const [quantities, setQuantities] = useState<Record<number, number>>({})

  const productsQuery = useQuery({
    queryKey: queryKeys.products,
    queryFn: () => getProducts(userId, true),
  })

  const pointBalanceQuery = useQuery({
    queryKey: queryKeys.pointBalance,
    queryFn: () => getMyPointBalance(userId),
  })

  const orderMutation = useMutation({
    mutationFn: ({ productId, quantity }: OrderMutationVariables) =>
      createOrder(userId, { productId, quantity }),
    onMutate: ({ productId }) => {
      setOrderMessage(null)
      setOrderingProductId(productId)
    },
    onSuccess: async (response) => {
      setOrderMessage(`${response.productId}번 상품 ${response.quantity}개 주문이 완료되었습니다.`)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.orders }),
        queryClient.invalidateQueries({ queryKey: queryKeys.pointBalance }),
        queryClient.invalidateQueries({ queryKey: queryKeys.pointList }),
        queryClient.invalidateQueries({ queryKey: queryKeys.expiringPoints(7) }),
      ])
    },
    onError: (error) => {
      setOrderMessage(getErrorMessage(error, '상품 주문에 실패했습니다.'))
    },
    onSettled: () => {
      setOrderingProductId(null)
    },
  })

  if (productsQuery.isLoading || pointBalanceQuery.isLoading) {
    return <LoadingState label="상품 정보를 불러오는 중입니다..." />
  }

  if (productsQuery.isError) {
    return (
      <ErrorState
        message={getErrorMessage(productsQuery.error, '상품 목록을 불러오지 못했습니다.')}
        onRetry={() => {
          void productsQuery.refetch()
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

  if (!productsQuery.data || !pointBalanceQuery.data) {
    return <LoadingState label="상품 정보를 불러오는 중입니다..." />
  }

  const products = productsQuery.data.items
  const availableBalance = pointBalanceQuery.data.availableBalance

  function handleQuantityChange(productId: number, event: ChangeEvent<HTMLInputElement>) {
    const parsed = Number.parseInt(event.target.value, 10)
    const nextQuantity = Number.isNaN(parsed) ? 1 : clampQuantity(parsed)

    setQuantities((prev) => ({
      ...prev,
      [productId]: nextQuantity,
    }))
  }

  return (
    <section className="space-y-4">
      <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">내 사용 가능 포인트</h2>
        <p className="mt-2 text-2xl font-bold text-slate-900">{formatPoints(availableBalance)}</p>
      </article>

      {orderMessage ? (
        <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
          {orderMessage}
        </div>
      ) : null}

      {products.length === 0 ? (
        <EmptyState label="구매 가능한 상품이 없습니다." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {products.map((product) => {
            const selectedQuantity = clampQuantity(quantities[product.productId] ?? 1)
            const totalCost = product.pricePoints * selectedQuantity
            const canPurchase = canPurchaseProduct(product, availableBalance, selectedQuantity)
            const isOrdering = orderingProductId === product.productId

            return (
              <article
                key={product.productId}
                className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
              >
                <h3 className="text-base font-semibold text-slate-900">{product.name}</h3>
                <p className="mt-2 text-sm text-slate-600">{product.description}</p>

                <div className="mt-4 space-y-1 text-sm text-slate-700">
                  <p>가격: {formatPoints(product.pricePoints)}</p>
                  <p>재고: {product.stock}</p>
                </div>

                <div className="mt-4">
                  <label
                    htmlFor={`quantity-${product.productId}`}
                    className="block text-sm font-medium text-slate-700"
                  >
                    수량 (1~20)
                  </label>
                  <input
                    id={`quantity-${product.productId}`}
                    type="number"
                    min={1}
                    max={20}
                    value={selectedQuantity}
                    onChange={(event) => handleQuantityChange(product.productId, event)}
                    className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none ring-slate-900 focus:ring"
                    disabled={orderMutation.isPending}
                  />
                  <p className="mt-1 text-xs text-slate-600">
                    총 사용 포인트: {formatPoints(totalCost)}
                  </p>
                </div>

                <button
                  type="button"
                  disabled={!canPurchase || orderMutation.isPending}
                  onClick={() =>
                    orderMutation.mutate({
                      productId: product.productId,
                      quantity: selectedQuantity,
                    })
                  }
                  className="mt-4 w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isOrdering ? '주문 처리 중...' : '구매'}
                </button>

                {!canPurchase ? (
                  <p className="mt-2 text-xs text-rose-600">
                    구매 불가: {getUnavailableReason(product, availableBalance, selectedQuantity)}
                  </p>
                ) : null}
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}
