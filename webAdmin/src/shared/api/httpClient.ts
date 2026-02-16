import type {
  ApiConnection,
  ApiErrorDetail,
  ApiErrorResponse,
} from '../types/adminApi'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

type QueryValue = string | number | boolean | undefined

interface RequestOptions {
  method?: HttpMethod
  body?: unknown
  query?: Record<string, QueryValue>
  useUserHeader?: boolean
  useAdminHeader?: boolean
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isApiErrorDetail(value: unknown): value is ApiErrorDetail {
  if (!isRecord(value)) {
    return false
  }

  return typeof value.field === 'string' && typeof value.reason === 'string'
}

function parseApiErrorResponse(payload: unknown): ApiErrorResponse | null {
  if (!isRecord(payload) || !isRecord(payload.error)) {
    return null
  }

  const details = Array.isArray(payload.error.details)
    ? payload.error.details.filter(isApiErrorDetail)
    : []

  const code = typeof payload.error.code === 'string' ? payload.error.code : 'UNKNOWN_ERROR'
  const message =
    typeof payload.error.message === 'string'
      ? payload.error.message
      : '요청 처리 중 오류가 발생했습니다.'
  const traceId =
    typeof payload.error.trace_id === 'string' ? payload.error.trace_id : 'unknown'

  return {
    error: {
      code,
      message,
      details,
      trace_id: traceId,
    },
  }
}

function buildRequestUrl(baseUrl: string, path: string, query?: Record<string, QueryValue>): string {
  const normalizedBaseUrl = baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
  const normalizedPath = path.startsWith('/') ? path.slice(1) : path
  const requestUrl = new URL(normalizedPath, normalizedBaseUrl)

  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value === undefined) {
        return
      }

      requestUrl.searchParams.set(key, String(value))
    })
  }

  return requestUrl.toString()
}

export class ApiRequestError extends Error {
  readonly status: number
  readonly code: string
  readonly details: ApiErrorDetail[]
  readonly traceId: string

  constructor(params: {
    status: number
    message: string
    code?: string
    details?: ApiErrorDetail[]
    traceId?: string
  }) {
    super(params.message)
    this.name = 'ApiRequestError'
    this.status = params.status
    this.code = params.code ?? 'UNKNOWN_ERROR'
    this.details = params.details ?? []
    this.traceId = params.traceId ?? 'unknown'
  }
}

async function createRequestError(response: Response): Promise<ApiRequestError> {
  const fallbackMessage = `요청 처리 중 오류가 발생했습니다. (HTTP ${response.status})`

  try {
    const rawBody: unknown = await response.json()
    const apiError = parseApiErrorResponse(rawBody)

    if (!apiError) {
      return new ApiRequestError({
        status: response.status,
        message: fallbackMessage,
      })
    }

    return new ApiRequestError({
      status: response.status,
      message: apiError.error.message,
      code: apiError.error.code,
      details: apiError.error.details,
      traceId: apiError.error.trace_id,
    })
  } catch {
    return new ApiRequestError({
      status: response.status,
      message: fallbackMessage,
    })
  }
}

export async function requestJson<TResponse>(
  connection: ApiConnection,
  path: string,
  options: RequestOptions = {},
): Promise<TResponse> {
  const requestUrl = buildRequestUrl(connection.baseUrl, path, options.query)
  const headers = new Headers()

  headers.set('Accept', 'application/json')

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  const shouldUseUserHeader = options.useUserHeader !== false
  const shouldUseAdminHeader = options.useAdminHeader !== false

  if (shouldUseUserHeader && typeof connection.userId === 'number') {
    headers.set('X-USER-ID', String(connection.userId))
  }

  if (shouldUseAdminHeader && connection.adminHeaderName && connection.adminToken) {
    headers.set(connection.adminHeaderName, connection.adminToken)
  }

  const response = await fetch(requestUrl, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  if (!response.ok) {
    throw await createRequestError(response)
  }

  if (response.status === 204) {
    return undefined as TResponse
  }

  const textBody = await response.text()

  if (!textBody) {
    return undefined as TResponse
  }

  return JSON.parse(textBody) as TResponse
}

function isDataEnvelope<TData>(value: unknown): value is { data: TData } {
  if (!isRecord(value)) {
    return false
  }

  return 'data' in value
}

export async function requestDataJson<TData>(
  connection: ApiConnection,
  path: string,
  options: RequestOptions = {},
): Promise<TData> {
  const response = await requestJson<unknown>(connection, path, options)

  if (!isDataEnvelope<TData>(response)) {
    throw new ApiRequestError({
      status: 500,
      code: 'INVALID_API_RESPONSE',
      message: '응답 형식이 올바르지 않습니다.',
    })
  }

  return response.data
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return `${error.message} (code: ${error.code})`
  }

  if (error instanceof Error) {
    return error.message
  }

  return '알 수 없는 오류가 발생했습니다.'
}
