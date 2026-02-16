import type { ReactNode } from 'react'

export type AppPage = 'roulette' | 'points' | 'products' | 'orders'

interface AppLayoutProps {
  nickname: string
  activePage: AppPage
  onChangePage: (page: AppPage) => void
  onLogout: () => void
  children: ReactNode
}

const navItems: { key: AppPage; label: string }[] = [
  { key: 'roulette', label: '홈(룰렛)' },
  { key: 'points', label: '내 포인트' },
  { key: 'products', label: '상품 목록' },
  { key: 'orders', label: '주문 내역' },
]

export function AppLayout({
  nickname,
  activePage,
  onChangePage,
  onLogout,
  children,
}: AppLayoutProps) {
  return (
    <main className="min-h-screen bg-slate-100 px-4 py-6 text-slate-900 sm:px-6">
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-5">
        <header className="rounded-xl border border-slate-200 bg-white px-4 py-4 shadow-sm sm:px-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h1 className="text-xl font-bold">Point Roulette</h1>
              <p className="text-sm text-slate-600">{nickname}님 환영합니다.</p>
            </div>
            <button
              type="button"
              onClick={onLogout}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
            >
              로그아웃
            </button>
          </div>
          <nav className="mt-4 flex flex-wrap gap-2">
            {navItems.map((item) => {
              const isActive = item.key === activePage

              return (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => onChangePage(item.key)}
                  className={`rounded-md px-3 py-2 text-sm font-medium ${
                    isActive
                      ? 'bg-slate-900 text-white'
                      : 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  {item.label}
                </button>
              )
            })}
          </nav>
        </header>
        {children}
      </div>
    </main>
  )
}
