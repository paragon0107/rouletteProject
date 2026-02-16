import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AppLayout, type AppPage } from './components/layout/AppLayout'
import { useAuthSession } from './hooks/use-auth'
import { LoginPage } from './pages/LoginPage'
import { OrdersPage } from './pages/OrdersPage'
import { PointsPage } from './pages/PointsPage'
import { ProductsPage } from './pages/ProductsPage'
import { RoulettePage } from './pages/RoulettePage'
import type { MockLoginResponse } from './types/api'

function renderPage(page: AppPage, userId: number) {
  switch (page) {
    case 'roulette':
      return <RoulettePage userId={userId} />
    case 'points':
      return <PointsPage userId={userId} />
    case 'products':
      return <ProductsPage userId={userId} />
    case 'orders':
      return <OrdersPage userId={userId} />
    default:
      return null
  }
}

function App() {
  const queryClient = useQueryClient()
  const { session, isLoggedIn, saveSession, clearSession } = useAuthSession()
  const [activePage, setActivePage] = useState<AppPage>('roulette')

  function handleLoginSuccess(nextSession: MockLoginResponse) {
    saveSession(nextSession)
    setActivePage('roulette')
  }

  function handleLogout() {
    clearSession()
    queryClient.clear()
    setActivePage('roulette')
  }

  if (!isLoggedIn || !session) {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />
  }

  return (
    <AppLayout
      nickname={session.nickname}
      activePage={activePage}
      onChangePage={setActivePage}
      onLogout={handleLogout}
    >
      {renderPage(activePage, session.userId)}
    </AppLayout>
  )
}

export default App
