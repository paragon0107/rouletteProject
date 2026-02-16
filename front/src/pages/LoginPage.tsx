import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { getErrorMessage } from '../lib/api-client'
import { mockLogin } from '../services/user-api'
import type { MockLoginResponse } from '../types/api'

interface LoginPageProps {
  onLoginSuccess: (session: MockLoginResponse) => void
}

const nicknamePattern = /^[a-zA-Z0-9가-힣_-]+$/

function validateNickname(nickname: string): string | null {
  if (nickname.length < 2) {
    return '닉네임은 2자 이상 입력해 주세요.'
  }

  if (nickname.length > 30) {
    return '닉네임은 30자 이하로 입력해 주세요.'
  }

  if (!nicknamePattern.test(nickname)) {
    return '닉네임은 한글/영문/숫자/언더스코어/하이픈만 사용할 수 있습니다.'
  }

  return null
}

export function LoginPage({ onLoginSuccess }: LoginPageProps) {
  const [nickname, setNickname] = useState('')
  const [validationMessage, setValidationMessage] = useState<string | null>(null)

  const loginMutation = useMutation({
    mutationFn: mockLogin,
    onSuccess: onLoginSuccess,
  })

  const apiErrorMessage = loginMutation.error
    ? getErrorMessage(loginMutation.error, '로그인에 실패했습니다.')
    : null

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const normalizedNickname = nickname.trim()
    const message = validateNickname(normalizedNickname)

    if (message) {
      setValidationMessage(message)
      return
    }

    setValidationMessage(null)
    loginMutation.mutate({ nickname: normalizedNickname })
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-8">
      <section className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-bold text-slate-900">포인트 룰렛 로그인</h1>
        <p className="mt-2 text-sm text-slate-600">닉네임으로 간단하게 로그인할 수 있습니다.</p>

        <form className="mt-6 space-y-3" onSubmit={handleSubmit}>
          <label className="block text-sm font-medium text-slate-800" htmlFor="nickname">
            닉네임
          </label>
          <input
            id="nickname"
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            placeholder="예: userA ~ userD"
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none ring-slate-900 focus:ring"
            autoComplete="off"
          />

          {validationMessage ? <p className="text-xs text-red-600">{validationMessage}</p> : null}
          {apiErrorMessage ? <p className="text-xs text-red-600">{apiErrorMessage}</p> : null}

          <button
            type="submit"
            disabled={loginMutation.isPending}
            className="w-full rounded-md bg-slate-900 px-3 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loginMutation.isPending ? '로그인 중...' : '로그인'}
          </button>
        </form>
      </section>
    </main>
  )
}
