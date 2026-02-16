import { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  InputNumber,
  Popconfirm,
  Row,
  Skeleton,
  Statistic,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType, TablePaginationConfig } from 'antd'
import {
  cancelAdminParticipation,
  getAdminBudgetByDate,
  getAdminParticipants,
  updateAdminBudgetByDate,
} from '../../shared/api/adminApi'
import { getErrorMessage } from '../../shared/api/httpClient'
import { formatDateTime, getTodayDateValue } from '../../shared/date/dateValue'
import { formatPoint } from '../../shared/format/displayFormat'
import type {
  AdminBudgetResponse,
  AdminRouletteParticipantItemResponse,
  ApiConnection,
} from '../../shared/types/adminApi'
import styles from './BudgetPage.module.css'

interface BudgetPageProps {
  connection: ApiConnection | null
}

const DEFAULT_PAGE = 0
const DEFAULT_SIZE = 20

export function BudgetPage({ connection }: BudgetPageProps) {
  const [selectedDate, setSelectedDate] = useState(getTodayDateValue)
  const [budgetData, setBudgetData] = useState<AdminBudgetResponse | null>(null)
  const [budgetInput, setBudgetInput] = useState<number | null>(null)
  const [participants, setParticipants] = useState<AdminRouletteParticipantItemResponse[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [page, setPage] = useState(DEFAULT_PAGE)
  const [size, setSize] = useState(DEFAULT_SIZE)
  const [isLoading, setIsLoading] = useState(false)
  const [isSavingBudget, setIsSavingBudget] = useState(false)
  const [cancelingParticipationId, setCancelingParticipationId] = useState<number | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const loadPageData = useCallback(async (): Promise<void> => {
    if (!connection) {
      setBudgetData(null)
      setParticipants([])
      setTotalItems(0)
      setErrorMessage(null)
      return
    }

    try {
      setIsLoading(true)
      setErrorMessage(null)

      const [budgetResponse, participantResponse] = await Promise.all([
        getAdminBudgetByDate(connection, selectedDate),
        getAdminParticipants(connection, {
          date: selectedDate,
        }),
      ])

      setBudgetData(budgetResponse)
      setBudgetInput(budgetResponse.totalBudget)
      setParticipants(participantResponse.items)
      setTotalItems(participantResponse.totalItems)
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
      setBudgetData(null)
      setParticipants([])
      setTotalItems(0)
    } finally {
      setIsLoading(false)
    }
  }, [connection, selectedDate])

  useEffect(() => {
    void loadPageData()
  }, [loadPageData])

  const handleBudgetUpdate = async (): Promise<void> => {
    if (!connection) {
      return
    }

    if (budgetInput === null || budgetInput < 0) {
      setErrorMessage('총 예산은 0 이상의 숫자여야 합니다.')
      return
    }

    try {
      setIsSavingBudget(true)
      setErrorMessage(null)

      await updateAdminBudgetByDate(connection, selectedDate, {
        totalBudget: budgetInput,
      })

      await loadPageData()
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
    } finally {
      setIsSavingBudget(false)
    }
  }

  const handleCancelParticipation = async (participationId: number): Promise<void> => {
    if (!connection) {
      return
    }

    try {
      setCancelingParticipationId(participationId)
      setErrorMessage(null)

      await cancelAdminParticipation(connection, participationId)
      await loadPageData()
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
    } finally {
      setCancelingParticipationId(null)
    }
  }

  const handlePaginationChange = (pagination: TablePaginationConfig): void => {
    const nextSize = pagination.pageSize ?? size
    const nextPage = (pagination.current ?? 1) - 1

    if (nextSize !== size) {
      setPage(DEFAULT_PAGE)
      setSize(nextSize)
      return
    }

    setPage(nextPage)
  }

  const participantColumns: TableColumnsType<AdminRouletteParticipantItemResponse> = [
    {
      title: '참여 ID',
      dataIndex: 'participationId',
      key: 'participationId',
      width: 120,
    },
    {
      title: '사용자 ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 140,
    },
    {
      title: '지급 포인트',
      dataIndex: 'awardedPoints',
      key: 'awardedPoints',
      render: (value: number) => formatPoint(value),
      width: 140,
    },
    {
      title: '지급 시각',
      dataIndex: 'awardedAt',
      key: 'awardedAt',
      render: (value: string) => formatDateTime(value),
      width: 180,
    },
    {
      title: '상태',
      dataIndex: 'canceled',
      key: 'canceled',
      render: (value: boolean) =>
        value ? <Tag color="default">취소됨</Tag> : <Tag color="green">정상</Tag>,
      width: 110,
    },
    {
      title: '작업',
      key: 'action',
      render: (_, record) => {
        if (record.canceled) {
          return <Typography.Text type="secondary">완료</Typography.Text>
        }

        return (
          <Popconfirm
            title="이 참여를 취소할까요?"
            description="지급 포인트가 회수됩니다."
            onConfirm={() => void handleCancelParticipation(record.participationId)}
            okText="취소 처리"
            cancelText="닫기"
          >
            <Button
              loading={cancelingParticipationId === record.participationId}
              danger
              size="small"
            >
              참여 취소
            </Button>
          </Popconfirm>
        )
      },
      width: 130,
    },
  ]

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
          <Typography.Title level={4}>예산 관리</Typography.Title>
          <div className={styles.toolbarActions}>
            <Input
              className={styles.dateInput}
              type="date"
              value={selectedDate}
              onChange={(event) => {
                setSelectedDate(event.target.value)
                setPage(DEFAULT_PAGE)
              }}
            />
            <Button loading={isLoading} onClick={() => void loadPageData()}>
              새로고침
            </Button>
          </div>
        </div>

        {errorMessage ? <Alert className={styles.error} type="error" message={errorMessage} showIcon /> : null}

        {isLoading && !budgetData ? (
          <Skeleton active />
        ) : budgetData ? (
          <>
            <Row gutter={[16, 16]}>
              <Col xs={24} md={8}>
                <Card>
                  <Statistic title="총 예산" value={formatPoint(budgetData.totalBudget)} />
                </Card>
              </Col>
              <Col xs={24} md={8}>
                <Card>
                  <Statistic title="사용 예산" value={formatPoint(budgetData.usedBudget)} />
                </Card>
              </Col>
              <Col xs={24} md={8}>
                <Card>
                  <Statistic title="잔여 예산" value={formatPoint(budgetData.remainingBudget)} />
                </Card>
              </Col>
            </Row>

            <div className={styles.budgetForm}>
              <InputNumber
                className={styles.budgetInput}
                min={0}
                max={100000000}
                value={budgetInput}
                onChange={(value) => setBudgetInput(value)}
                placeholder="총 예산"
              />
              <Button type="primary" loading={isSavingBudget} onClick={() => void handleBudgetUpdate()}>
                예산 저장
              </Button>
            </div>
          </>
        ) : (
          <Empty description="예산 정보를 조회할 수 없습니다." />
        )}
      </Card>

      <Card>
        <Typography.Title level={4}>룰렛 참여 취소</Typography.Title>
        <Table<AdminRouletteParticipantItemResponse>
          rowKey="participationId"
          columns={participantColumns}
          dataSource={participants}
          loading={isLoading}
          locale={{ emptyText: <Empty description="참여 데이터가 없습니다." /> }}
          pagination={{
            current: page + 1,
            pageSize: size,
            total: totalItems,
            showSizeChanger: true,
          }}
          onChange={handlePaginationChange}
        />
      </Card>
    </div>
  )
}
