import { requestDataJson } from './httpClient'
import type {
  AdminLoginRequest,
  AdminLoginResponse,
  AdminBudgetResponse,
  AdminProductListResponse,
  AdminProductResponse,
  AdminRouletteParticipantCountResponse,
  AdminRouletteParticipantListResponse,
  ApiConnection,
  CancelOrderResponse,
  CancelRouletteParticipationResponse,
  CreateProductRequest,
  DatePageQuery,
  MockLoginResponse,
  OrderListQuery,
  OrderListResponse,
  OrderResponse,
  UpdateOrderStatusRequest,
  UpdateDailyBudgetRequest,
  UpdateProductRequest,
} from '../types/adminApi'

export async function requestMockLogin(
  baseUrl: string,
  nickname: string,
): Promise<MockLoginResponse> {
  return requestDataJson<MockLoginResponse>({ baseUrl }, '/api/v1/auth/login', {
    method: 'POST',
    useUserHeader: false,
    useAdminHeader: false,
    body: { nickname },
  })
}

export async function requestAdminLogin(
  baseUrl: string,
  body: AdminLoginRequest,
): Promise<AdminLoginResponse> {
  return requestDataJson<AdminLoginResponse>({ baseUrl }, '/api/v1/auth/admin-login', {
    method: 'POST',
    useUserHeader: false,
    useAdminHeader: false,
    body,
  })
}

export async function getAdminBudgetByDate(
  connection: ApiConnection,
  date: string,
): Promise<AdminBudgetResponse> {
  return requestDataJson<AdminBudgetResponse>(
    connection,
    `/api/v1/admin/budgets/${encodeURIComponent(date)}`,
    {
      useUserHeader: false,
    },
  )
}

export async function updateAdminBudgetByDate(
  connection: ApiConnection,
  date: string,
  body: UpdateDailyBudgetRequest,
): Promise<AdminBudgetResponse> {
  return requestDataJson<AdminBudgetResponse>(
    connection,
    `/api/v1/admin/budgets/${encodeURIComponent(date)}`,
    {
      method: 'PUT',
      useUserHeader: false,
      body,
    },
  )
}

export async function getAdminParticipantCount(
  connection: ApiConnection,
  date: string,
): Promise<AdminRouletteParticipantCountResponse> {
  return requestDataJson<AdminRouletteParticipantCountResponse>(
    connection,
    '/api/v1/admin/roulette/participants/count',
    {
      useUserHeader: false,
      query: date ? { date } : undefined,
    },
  )
}

export async function getAdminParticipants(
  connection: ApiConnection,
  query: DatePageQuery,
): Promise<AdminRouletteParticipantListResponse> {
  return requestDataJson<AdminRouletteParticipantListResponse>(
    connection,
    '/api/v1/admin/roulette/participants',
    {
      useUserHeader: false,
      query: query.date ? { date: query.date } : undefined,
    },
  )
}

export async function cancelAdminParticipation(
  connection: ApiConnection,
  participationId: number,
): Promise<CancelRouletteParticipationResponse> {
  return requestDataJson<CancelRouletteParticipationResponse>(
    connection,
    `/api/v1/admin/roulette/participations/${participationId}/cancel`,
    {
      method: 'POST',
      useUserHeader: false,
    },
  )
}

export async function getAdminProducts(
  connection: ApiConnection,
): Promise<AdminProductListResponse> {
  return requestDataJson<AdminProductListResponse>(connection, '/api/v1/admin/products', {
    useUserHeader: false,
  })
}

export async function createAdminProduct(
  connection: ApiConnection,
  body: CreateProductRequest,
): Promise<AdminProductResponse> {
  return requestDataJson<AdminProductResponse>(connection, '/api/v1/admin/products', {
    method: 'POST',
    useUserHeader: false,
    body,
  })
}

export async function updateAdminProduct(
  connection: ApiConnection,
  productId: number,
  body: UpdateProductRequest,
): Promise<AdminProductResponse> {
  return requestDataJson<AdminProductResponse>(
    connection,
    `/api/v1/admin/products/${productId}`,
    {
      method: 'PATCH',
      useUserHeader: false,
      body,
    },
  )
}

export async function deleteAdminProduct(
  connection: ApiConnection,
  productId: number,
): Promise<void> {
  await requestDataJson<Record<string, never>>(
    connection,
    `/api/v1/admin/products/${productId}`,
    {
      method: 'DELETE',
      useUserHeader: false,
    },
  )
}

export async function getAdminOrders(
  connection: ApiConnection,
  query: OrderListQuery,
): Promise<OrderListResponse> {
  return requestDataJson<OrderListResponse>(connection, '/api/v1/admin/orders', {
    query: {
      status: query.status,
    },
    useUserHeader: false,
  })
}

export async function updateAdminOrderStatus(
  connection: ApiConnection,
  orderId: number,
  body: UpdateOrderStatusRequest,
): Promise<OrderResponse> {
  return requestDataJson<OrderResponse>(
    connection,
    `/api/v1/admin/orders/${orderId}/status`,
    {
      method: 'PATCH',
      useUserHeader: false,
      body,
    },
  )
}

export async function cancelAdminOrder(
  connection: ApiConnection,
  orderId: number,
): Promise<CancelOrderResponse> {
  return requestDataJson<CancelOrderResponse>(
    connection,
    `/api/v1/admin/orders/${orderId}/cancel`,
    {
      method: 'POST',
      useUserHeader: false,
    },
  )
}
