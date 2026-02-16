import { useState } from 'react'
import { Alert, Button, Card, Input, Typography } from 'antd'
import styles from './AdminLoginPage.module.css'

interface AdminLoginPageProps {
  errorMessage: string | null
  isLoading: boolean
  onLogin: (inputValue: string) => Promise<boolean>
}

export function AdminLoginPage({ errorMessage, isLoading, onLogin }: AdminLoginPageProps) {
  const [adminInput, setAdminInput] = useState('')

  const handleLogin = async (): Promise<void> => {
    await onLogin(adminInput)
  }

  return (
    <div className={styles.wrapper}>
      <Card className={styles.card}>
        <Typography.Title level={3} className={styles.title}>
          Admin Login
        </Typography.Title>
        <Typography.Text type="secondary" className={styles.description}>
          어드민 인증 코드를 입력해 주세요.
        </Typography.Text>

        <div className={styles.inputArea}>
          <Input
            value={adminInput}
            onChange={(event) => setAdminInput(event.target.value)}
            placeholder="어드민 인증 코드를 입력하세요"
            onPressEnter={() => {
              void handleLogin()
            }}
          />
          <Button type="primary" loading={isLoading} onClick={() => void handleLogin()}>
            로그인
          </Button>
        </div>

        {errorMessage ? <Alert className={styles.error} type="error" showIcon message={errorMessage} /> : null}
      </Card>
    </div>
  )
}
