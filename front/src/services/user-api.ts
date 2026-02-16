import { apiRequest } from '../lib/api-client'
import type {
  CreateOrderRequest,
  MockLoginRequest,
  MockLoginResponse,
  MyExpiringPointsResponse,
  MyPointBalanceResponse,
  MyPointListResponse,
  OrderListResponse,
  OrderStatus,
  OrderResponse,
  ProductListResponse,
  RouletteParticipationResponse,
  TodayBudgetResponse,
  TodayParticipationStatusResponse,
} from '../types/api'

export function mockLogin(payload: MockLoginRequest): Promise<MockLoginResponse> {
  return apiRequest<MockLoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: payload,
  })
}

export function getTodayRouletteParticipationStatus(
  userId: number,
): Promise<TodayParticipationStatusResponse> {
  return apiRequest<TodayParticipationStatusResponse>('/api/v1/roulette/participations/today', {
    userId,
  })
}

export function createRouletteParticipation(userId: number): Promise<RouletteParticipationResponse> {
  return apiRequest<RouletteParticipationResponse>('/api/v1/roulette/participations', {
    method: 'POST',
    userId,
  })
}

export function getTodayBudget(userId: number): Promise<TodayBudgetResponse> {
  return apiRequest<TodayBudgetResponse>('/api/v1/budgets/today', {
    userId,
  })
}

export function getMyPointUnits(userId: number): Promise<MyPointListResponse> {
  return apiRequest<MyPointListResponse>('/api/v1/points/me', {
    userId,
  })
}

export function getMyPointBalance(userId: number): Promise<MyPointBalanceResponse> {
  return apiRequest<MyPointBalanceResponse>('/api/v1/points/me/balance', {
    userId,
  })
}

export function getMyExpiringPoints(
  userId: number,
  withinDays = 7,
): Promise<MyExpiringPointsResponse> {
  return apiRequest<MyExpiringPointsResponse>('/api/v1/points/me/expiring', {
    userId,
    query: {
      withinDays,
    },
  })
}

export function getProducts(userId: number, activeOnly = true): Promise<ProductListResponse> {
  return apiRequest<ProductListResponse>('/api/v1/products', {
    userId,
    query: {
      activeOnly,
    },
  })
}

export function createOrder(userId: number, payload: CreateOrderRequest): Promise<OrderResponse> {
  return apiRequest<OrderResponse>('/api/v1/orders', {
    method: 'POST',
    userId,
    body: payload,
  })
}

export function getMyOrders(userId: number, status?: OrderStatus): Promise<OrderListResponse> {
  return apiRequest<OrderListResponse>('/api/v1/orders/me', {
    userId,
    query: {
      status,
    },
  })
}
