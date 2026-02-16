import type { ApiErrorResponse, ApiResponse } from '../types/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  userId?: number
  body?: unknown
  query?: Record<string, string | number | boolean | undefined>
}

export class ApiRequestError extends Error {
  public readonly status: number
  public readonly code?: string
  public readonly details?: ApiErrorResponse['error']['details']

  constructor(
    status: number,
    message: string,
    code?: string,
    details?: ApiErrorResponse['error']['details'],
  ) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.code = code
    this.details = details
  }
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Partial<ApiErrorResponse>

  return (
    typeof candidate.error === 'object' &&
    candidate.error !== null &&
    typeof candidate.error.code === 'string' &&
    typeof candidate.error.message === 'string' &&
    Array.isArray(candidate.error.details)
  )
}

function isApiDataResponse<T>(value: unknown): value is ApiResponse<T> {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  return 'data' in value
}

function extractApiData<T>(status: number, payload: unknown): T {
  if (isApiDataResponse<T>(payload)) {
    return payload.data
  }

  throw new ApiRequestError(status, '서버 응답 형식이 올바르지 않습니다.')
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const normalizedBase = API_BASE_URL.endsWith('/')
    ? API_BASE_URL.slice(0, -1)
    : API_BASE_URL
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = new URL(`${normalizedBase}${normalizedPath}`)

  if (!query) {
    return url.toString()
  }

  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined) {
      return
    }

    url.searchParams.set(key, String(value))
  })

  return url.toString()
}

async function parseResponseBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type')

  if (contentType && contentType.includes('application/json')) {
    return response.json()
  }

  const text = await response.text()
  return text.length > 0 ? text : null
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', userId, body, query } = options
  const requestHeaders = new Headers()
  requestHeaders.set('Accept', 'application/json')

  if (userId !== undefined) {
    requestHeaders.set('X-USER-ID', String(userId))
  }

  let bodyPayload: string | undefined

  if (body !== undefined) {
    requestHeaders.set('Content-Type', 'application/json')
    bodyPayload = JSON.stringify(body)
  }

  let response: Response

  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers: requestHeaders,
      body: bodyPayload,
    })
  } catch {
    throw new ApiRequestError(0, '네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.')
  }

  const payload = await parseResponseBody(response)

  if (!response.ok) {
    if (isApiErrorResponse(payload)) {
      throw new ApiRequestError(
        response.status,
        payload.error.message,
        payload.error.code,
        payload.error.details,
      )
    }

    throw new ApiRequestError(response.status, '요청 처리 중 오류가 발생했습니다.')
  }

  return extractApiData<T>(response.status, payload)
}

export function getErrorMessage(error: unknown, fallback = '요청 처리 중 오류가 발생했습니다.'): string {
  if (error instanceof ApiRequestError) {
    return error.message
  }

  if (error instanceof Error && error.message.length > 0) {
    return error.message
  }

  return fallback
}
