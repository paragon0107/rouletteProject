import { useCallback, useState } from 'react'
import type { MockLoginResponse } from '../types/api'

const AUTH_SESSION_STORAGE_KEY = 'point-roulette-auth-session'

function isAuthSession(value: unknown): value is MockLoginResponse {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Partial<MockLoginResponse>

  return (
    typeof candidate.userId === 'number' &&
    typeof candidate.nickname === 'string' &&
    typeof candidate.role === 'string'
  )
}

function readSession(): MockLoginResponse | null {
  const raw = localStorage.getItem(AUTH_SESSION_STORAGE_KEY)

  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as unknown

    if (isAuthSession(parsed)) {
      return parsed
    }
  } catch {
    // storage 값이 깨진 경우 초기화
  }

  localStorage.removeItem(AUTH_SESSION_STORAGE_KEY)
  return null
}

export function useAuthSession() {
  const [session, setSession] = useState<MockLoginResponse | null>(() => readSession())

  const saveSession = useCallback((nextSession: MockLoginResponse) => {
    localStorage.setItem(AUTH_SESSION_STORAGE_KEY, JSON.stringify(nextSession))
    setSession(nextSession)
  }, [])

  const clearSession = useCallback(() => {
    localStorage.removeItem(AUTH_SESSION_STORAGE_KEY)
    setSession(null)
  }, [])

  return {
    session,
    isLoggedIn: session !== null,
    saveSession,
    clearSession,
  }
}
