import { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Row,
  Skeleton,
  Statistic,
  Typography,
} from 'antd'
import {
  getAdminBudgetByDate,
  getAdminParticipantCount,
} from '../../shared/api/adminApi'
import { getErrorMessage } from '../../shared/api/httpClient'
import { getTodayDateValue } from '../../shared/date/dateValue'
import { formatPoint } from '../../shared/format/displayFormat'
import type {
  AdminBudgetResponse,
  AdminRouletteParticipantCountResponse,
  ApiConnection,
} from '../../shared/types/adminApi'
import styles from './DashboardPage.module.css'

interface DashboardPageProps {
  connection: ApiConnection | null
}

export function DashboardPage({ connection }: DashboardPageProps) {
  const [selectedDate, setSelectedDate] = useState(getTodayDateValue)
  const [budgetData, setBudgetData] = useState<AdminBudgetResponse | null>(null)
  const [participantData, setParticipantData] =
    useState<AdminRouletteParticipantCountResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const loadDashboard = useCallback(async (): Promise<void> => {
    if (!connection) {
      setBudgetData(null)
      setParticipantData(null)
      setErrorMessage(null)
      return
    }

    try {
      setIsLoading(true)
      setErrorMessage(null)

      const [budgetResponse, participantResponse] = await Promise.all([
        getAdminBudgetByDate(connection, selectedDate),
        getAdminParticipantCount(connection, selectedDate),
      ])

      setBudgetData(budgetResponse)
      setParticipantData(participantResponse)
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
      setBudgetData(null)
      setParticipantData(null)
    } finally {
      setIsLoading(false)
    }
  }, [connection, selectedDate])

  useEffect(() => {
    void loadDashboard()
  }, [loadDashboard])

  if (!connection) {
    return (
      <Card>
        <Alert
          type="warning"
          showIcon
          message="어드민 로그인 정보가 없습니다. 다시 로그인해 주세요."
        />
      </Card>
    )
  }

  return (
    <div className={styles.container}>
      <Card>
        <div className={styles.toolbar}>
          <Typography.Title level={4}>대시보드</Typography.Title>
          <div className={styles.toolbarActions}>
            <Input
              className={styles.dateInput}
              type="date"
              value={selectedDate}
              onChange={(event) => setSelectedDate(event.target.value)}
            />
            <Button loading={isLoading} onClick={() => void loadDashboard()}>
              새로고침
            </Button>
          </div>
        </div>

        {errorMessage ? <Alert className={styles.error} type="error" message={errorMessage} showIcon /> : null}

        {isLoading && !budgetData && !participantData ? <Skeleton active /> : null}

        {!isLoading && !budgetData && !participantData ? (
          <Empty description="조회 결과가 없습니다." />
        ) : (
          <Row gutter={[16, 16]}>
            <Col xs={24} md={8}>
              <Card>
                <Statistic
                  title="오늘 총 예산"
                  value={budgetData ? formatPoint(budgetData.totalBudget) : '-'}
                />
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card>
                <Statistic
                  title="오늘 참여자 수"
                  value={participantData ? participantData.participantCount : '-'}
                />
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card>
                <Statistic
                  title="오늘 지급 포인트"
                  value={participantData ? formatPoint(participantData.totalAwardedPoints) : '-'}
                />
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card>
                <Statistic
                  title="사용 예산"
                  value={budgetData ? formatPoint(budgetData.usedBudget) : '-'}
                />
              </Card>
            </Col>
            <Col xs={24} md={8}>
              <Card>
                <Statistic
                  title="잔여 예산"
                  value={budgetData ? formatPoint(budgetData.remainingBudget) : '-'}
                />
              </Card>
            </Col>
          </Row>
        )}
      </Card>
    </div>
  )
}
