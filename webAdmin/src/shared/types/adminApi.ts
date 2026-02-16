export type UserRole = 'USER' | 'ADMIN'
export type ProductStatus = 'ACTIVE' | 'INACTIVE'
export type OrderStatus = 'PLACED' | 'COMPLETED' | 'CANCELED'

export interface ApiConnection {
  baseUrl: string
  userId?: number
  adminHeaderName?: string
  adminToken?: string
}

export interface PageQuery {
  page: number
  size: number
}

export interface ApiErrorDetail {
  field: string
  reason: string
}

export interface ApiErrorBody {
  code: string
  message: string
  details: ApiErrorDetail[]
  trace_id: string
}

export interface ApiErrorResponse {
  error: ApiErrorBody
}

export interface MockLoginResponse {
  userId: number
  nickname: string
  role: UserRole
}

export interface AdminLoginRequest {
  adminCode: string
}

export interface AdminLoginResponse {
  headerName: string
  adminToken: string
}

export interface AdminBudgetResponse {
  date: string
  totalBudget: number
  usedBudget: number
  remainingBudget: number
}

export interface UpdateDailyBudgetRequest {
  totalBudget: number
}

export interface AdminRouletteParticipantCountResponse {
  date: string
  participantCount: number
  totalAwardedPoints: number
}

export interface AdminRouletteParticipantItemResponse {
  participationId: number
  userId: number
  awardedPoints: number
  awardedAt: string
  canceled: boolean
}

export interface PagedResponse<T> {
  totalItems: number
  items: T[]
}

export type AdminRouletteParticipantListResponse =
  PagedResponse<AdminRouletteParticipantItemResponse> & { date: string }

export interface CancelRouletteParticipationResponse {
  participationId: number
  userId: number
  recoveredPoints: number
  canceledAt: string
}

export interface AdminProductResponse {
  productId: number
  name: string
  description: string
  pricePoints: number
  stock: number
  status: ProductStatus
  createdAt: string
  updatedAt: string
}

export type AdminProductListResponse = PagedResponse<AdminProductResponse>

export interface CreateProductRequest {
  name: string
  description: string
  pricePoints: number
  stock: number
  status: ProductStatus
}

export interface UpdateProductRequest {
  name?: string
  description?: string
  pricePoints?: number
  stock?: number
  status?: ProductStatus
}

export interface OrderResponse {
  orderId: number
  userId: number
  productId: number
  quantity: number
  usedPoints: number
  status: OrderStatus
  orderedAt: string
  canceledAt?: string | null
}

export type OrderListResponse = PagedResponse<OrderResponse>

export interface CancelOrderResponse {
  orderId: number
  userId: number
  refundedPoints: number
  refundedAt: string
}

export interface DatePageQuery {
  date?: string
}

export interface OrderListQuery {
  status?: OrderStatus
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus
}
