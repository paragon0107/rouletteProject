export type UserRole = 'USER' | 'ADMIN'

export type AwardedPoint = 100 | 300 | 500 | 1000

export type PointEventType =
  | 'ROULETTE_REWARD'
  | 'ORDER_USE'
  | 'ORDER_REFUND'
  | 'ROULETTE_REVOKE'

export type PointUnitStatus = 'AVAILABLE' | 'USED' | 'EXPIRED' | 'CANCELED'

export type ProductStatus = 'ACTIVE' | 'INACTIVE'

export type OrderStatus = 'PLACED' | 'CANCELED'

export interface ApiResponse<T> {
  data: T
}

export interface MockLoginRequest {
  nickname: string
}

export interface MockLoginResponse {
  userId: number
  nickname: string
  role: UserRole
}

export interface RouletteParticipationResponse {
  participationId: number
  participationDate: string
  awardedPoints: AwardedPoint
  pointExpiresAt: string
  remainingBudget: number
}

export interface TodayParticipationStatusResponse {
  participationDate: string
  participatedToday: boolean
  participationId?: number
  awardedPoints?: AwardedPoint
  participatedAt?: string
}

export interface TodayBudgetResponse {
  date: string
  totalBudget: number
  usedBudget: number
  remainingBudget: number
}

export interface PointUnitResponse {
  pointUnitId: number
  eventType: PointEventType
  originalAmount: number
  remainingAmount: number
  earnedAt: string
  expiresAt: string
  status: PointUnitStatus
}

export interface MyPointListResponse {
  totalItems: number
  items: PointUnitResponse[]
}

export interface MyPointBalanceResponse {
  availableBalance: number
  expiringWithin7Days: number
}

export interface ExpiringPointItemResponse {
  pointUnitId: number
  amount: number
  expiresAt: string
}

export interface MyExpiringPointsResponse {
  withinDays: number
  totalExpiringAmount: number
  items: ExpiringPointItemResponse[]
}

export interface ProductResponse {
  productId: number
  name: string
  description: string
  pricePoints: number
  stock: number
  status: ProductStatus
  createdAt: string
  updatedAt: string
}

export interface ProductListResponse {
  totalItems: number
  items: ProductResponse[]
}

export interface CreateOrderRequest {
  productId: number
  quantity: number
}

export interface OrderResponse {
  orderId: number
  userId: number
  productId: number
  quantity: number
  usedPoints: number
  status: OrderStatus
  orderedAt: string
}

export interface OrderListItemResponse {
  orderId: number
  userId: number
  productId: number
  quantity: number
  usedPoints: number
  status: OrderStatus
  orderedAt: string
  canceledAt?: string | null
}

export interface OrderListResponse {
  totalItems: number
  items: OrderListItemResponse[]
}

export interface ApiErrorDetail {
  field: string
  reason: string
}

export interface ApiErrorPayload {
  code: string
  message: string
  details: ApiErrorDetail[]
  trace_id: string
}

export interface ApiErrorResponse {
  error: ApiErrorPayload
}
