import { useState } from 'react'
import { requestAdminLogin } from '../../shared/api/adminApi'
import { getErrorMessage } from '../../shared/api/httpClient'
import type { ApiConnection } from '../../shared/types/adminApi'

const AUTH_STORAGE_KEY = 'roulette_web_admin_auth'
const DEFAULT_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

interface StoredAdminAuth {
  adminHeaderName: string
  adminToken: string
}

interface UseAdminAuthResult {
  isAuthenticated: boolean
  apiConnection: ApiConnection | null
  isLoading: boolean
  errorMessage: string | null
  loginById: (inputValue: string) => Promise<boolean>
  logout: () => void
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function readStoredAuth(): StoredAdminAuth | null {
  const storedValue = sessionStorage.getItem(AUTH_STORAGE_KEY)

  if (!storedValue) {
    return null
  }

  try {
    const parsedValue: unknown = JSON.parse(storedValue)

    if (
      isRecord(parsedValue) &&
      typeof parsedValue.adminHeaderName === 'string' &&
      typeof parsedValue.adminToken === 'string'
    ) {
      return {
        adminHeaderName: parsedValue.adminHeaderName,
        adminToken: parsedValue.adminToken,
      }
    }
  } catch {
    return null
  }

  return null
}

function writeStoredAuth(auth: StoredAdminAuth | null): void {
  if (!auth) {
    sessionStorage.removeItem(AUTH_STORAGE_KEY)
  } else {
    sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth))
  }
}

export function useAdminAuth(): UseAdminAuthResult {
  const storedAuth = readStoredAuth()
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(Boolean(storedAuth))
  const [apiConnection, setApiConnection] = useState<ApiConnection | null>(
    storedAuth
      ? {
          baseUrl: DEFAULT_BASE_URL,
          adminHeaderName: storedAuth.adminHeaderName,
          adminToken: storedAuth.adminToken,
        }
      : null,
  )
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const loginById = async (inputValue: string): Promise<boolean> => {
    const normalizedValue = inputValue.trim()

    if (!normalizedValue) {
      setErrorMessage('어드민 인증 코드를 입력해 주세요.')
      setIsAuthenticated(false)
      setApiConnection(null)
      writeStoredAuth(null)
      return false
    }

    try {
      setIsLoading(true)
      setErrorMessage(null)

      const loginResponse = await requestAdminLogin(DEFAULT_BASE_URL, {
        adminCode: normalizedValue,
      })

      if (!loginResponse.headerName || !loginResponse.adminToken) {
        setErrorMessage('어드민 인증 응답이 올바르지 않습니다.')
        setIsAuthenticated(false)
        setApiConnection(null)
        writeStoredAuth(null)
        return false
      }

      setIsAuthenticated(true)
      setApiConnection({
        baseUrl: DEFAULT_BASE_URL,
        adminHeaderName: loginResponse.headerName,
        adminToken: loginResponse.adminToken,
      })
      writeStoredAuth({
        adminHeaderName: loginResponse.headerName,
        adminToken: loginResponse.adminToken,
      })
      return true
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
      setIsAuthenticated(false)
      setApiConnection(null)
      writeStoredAuth(null)
      return false
    } finally {
      setIsLoading(false)
    }
  }

  const logout = (): void => {
    setErrorMessage(null)
    setIsAuthenticated(false)
    setApiConnection(null)
    writeStoredAuth(null)
  }

  return {
    isAuthenticated,
    apiConnection,
    isLoading,
    errorMessage,
    loginById,
    logout,
  }
}
