import { Layout, Menu } from 'antd'
import { useState } from 'react'
import { AdminLoginPage } from './features/auth/AdminLoginPage'
import { useAdminAuth } from './features/auth/useAdminAuth'
import { BudgetPage } from './features/budget/BudgetPage'
import { DashboardPage } from './features/dashboard/DashboardPage'
import { OrderPage } from './features/orders/OrderPage'
import { ProductPage } from './features/products/ProductPage'
import type { ApiConnection } from './shared/types/adminApi'
import styles from './App.module.css'

const { Sider, Content } = Layout

type AdminMenuKey = 'dashboard' | 'budget' | 'products' | 'orders'

const MENU_ITEMS: Array<{ key: AdminMenuKey; label: string }> = [
  { key: 'dashboard', label: '대시보드' },
  { key: 'budget', label: '예산 관리' },
  { key: 'products', label: '상품 관리' },
  { key: 'orders', label: '주문 내역' },
]

function isAdminMenuKey(value: string): value is AdminMenuKey {
  return MENU_ITEMS.some((item) => item.key === value)
}

function renderPage(menuKey: AdminMenuKey, connection: ApiConnection | null) {
  if (menuKey === 'dashboard') {
    return <DashboardPage connection={connection} />
  }

  if (menuKey === 'budget') {
    return <BudgetPage connection={connection} />
  }

  if (menuKey === 'products') {
    return <ProductPage connection={connection} />
  }

  return <OrderPage connection={connection} />
}

function App() {
  const [menuKey, setMenuKey] = useState<AdminMenuKey>('dashboard')
  const adminAuth = useAdminAuth()

  if (!adminAuth.isAuthenticated) {
    return (
      <AdminLoginPage
        errorMessage={adminAuth.errorMessage}
        isLoading={adminAuth.isLoading}
        onLogin={adminAuth.loginById}
      />
    )
  }

  return (
    <Layout className={styles.root}>
      <Sider className={styles.sidebar} breakpoint="lg" collapsedWidth="0">
        <div className={styles.logo}>Roulette Admin</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[menuKey]}
          items={MENU_ITEMS}
          onClick={(event) => {
            if (!isAdminMenuKey(event.key)) {
              return
            }

            setMenuKey(event.key)
          }}
        />
      </Sider>

      <Layout className={styles.mainLayout}>
        <Content className={styles.contentWrapper}>
          <div className={styles.pageContainer}>
            {renderPage(menuKey, adminAuth.apiConnection)}
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}

export default App
