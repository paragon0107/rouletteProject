import { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Empty,
  Popconfirm,
  Select,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType, TablePaginationConfig } from 'antd'
import {
  cancelAdminOrder,
  getAdminOrders,
  updateAdminOrderStatus,
} from '../../shared/api/adminApi'
import { getErrorMessage } from '../../shared/api/httpClient'
import { formatDateTime } from '../../shared/date/dateValue'
import { formatPoint } from '../../shared/format/displayFormat'
import type { ApiConnection, OrderResponse, OrderStatus } from '../../shared/types/adminApi'
import styles from './OrderPage.module.css'

interface OrderPageProps {
  connection: ApiConnection | null
}

const DEFAULT_PAGE = 0
const DEFAULT_SIZE = 20

type OrderStatusFilter = 'ALL' | OrderStatus

const STATUS_FILTER_OPTIONS: Array<{ label: string; value: OrderStatusFilter }> = [
  { label: '전체', value: 'ALL' },
  { label: '주문 접수', value: 'PLACED' },
  { label: '처리 완료', value: 'COMPLETED' },
  { label: '취소됨', value: 'CANCELED' },
]

function getOrderStatusTag(status: OrderStatus): { color: string; label: string } {
  if (status === 'PLACED') {
    return {
      color: 'green',
      label: '주문 접수',
    }
  }

  if (status === 'COMPLETED') {
    return {
      color: 'blue',
      label: '처리 완료',
    }
  }

  return {
    color: 'default',
    label: '취소됨',
  }
}

export function OrderPage({ connection }: OrderPageProps) {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [page, setPage] = useState(DEFAULT_PAGE)
  const [size, setSize] = useState(DEFAULT_SIZE)
  const [statusFilter, setStatusFilter] = useState<OrderStatusFilter>('ALL')
  const [isLoading, setIsLoading] = useState(false)
  const [updatingOrderId, setUpdatingOrderId] = useState<number | null>(null)
  const [cancelingOrderId, setCancelingOrderId] = useState<number | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const loadOrders = useCallback(async (): Promise<void> => {
    if (!connection) {
      setOrders([])
      setTotalItems(0)
      setErrorMessage(null)
      return
    }

    try {
      setIsLoading(true)
      setErrorMessage(null)

      const response = await getAdminOrders(connection, {
        status: statusFilter === 'ALL' ? undefined : statusFilter,
      })

      setOrders(response.items)
      setTotalItems(response.totalItems)
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
      setOrders([])
      setTotalItems(0)
    } finally {
      setIsLoading(false)
    }
  }, [connection, statusFilter])

  useEffect(() => {
    void loadOrders()
  }, [loadOrders])

  const handleCancelOrder = async (orderId: number): Promise<void> => {
    if (!connection) {
      return
    }

    try {
      setCancelingOrderId(orderId)
      setErrorMessage(null)
      await cancelAdminOrder(connection, orderId)
      await loadOrders()
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
    } finally {
      setCancelingOrderId(null)
    }
  }

  const handleCompleteOrder = async (orderId: number): Promise<void> => {
    if (!connection) {
      return
    }

    try {
      setUpdatingOrderId(orderId)
      setErrorMessage(null)
      await updateAdminOrderStatus(connection, orderId, { status: 'COMPLETED' })
      await loadOrders()
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
    } finally {
      setUpdatingOrderId(null)
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

  const columns: TableColumnsType<OrderResponse> = [
    {
      title: '주문 ID',
      dataIndex: 'orderId',
      key: 'orderId',
      width: 100,
    },
    {
      title: '상품 ID',
      dataIndex: 'productId',
      key: 'productId',
      width: 120,
    },
    {
      title: '사용자 ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 120,
    },
    {
      title: '수량',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 90,
    },
    {
      title: '사용 포인트',
      dataIndex: 'usedPoints',
      key: 'usedPoints',
      render: (value: number) => formatPoint(value),
      width: 130,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      render: (value: OrderStatus) => {
        const statusTag = getOrderStatusTag(value)
        return <Tag color={statusTag.color}>{statusTag.label}</Tag>
      },
      width: 120,
    },
    {
      title: '주문 시각',
      dataIndex: 'orderedAt',
      key: 'orderedAt',
      render: (value: string) => formatDateTime(value),
      width: 170,
    },
    {
      title: '작업',
      key: 'action',
      render: (_, record) => {
        if (record.status === 'CANCELED') {
          return <Typography.Text type="secondary">취소 완료</Typography.Text>
        }

        if (record.status === 'COMPLETED') {
          return <Typography.Text type="secondary">처리 완료</Typography.Text>
        }

        return (
          <div className={styles.toolbarActions}>
            <Button
              size="small"
              loading={updatingOrderId === record.orderId}
              onClick={() => void handleCompleteOrder(record.orderId)}
            >
              완료 처리
            </Button>
            <Popconfirm
              title="주문을 취소할까요?"
              description="차감 포인트가 환불됩니다."
              okText="주문 취소"
              cancelText="닫기"
              onConfirm={() => void handleCancelOrder(record.orderId)}
            >
              <Button danger size="small" loading={cancelingOrderId === record.orderId}>
                주문 취소
              </Button>
            </Popconfirm>
          </div>
        )
      },
      width: 210,
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
          <Typography.Title level={4}>주문 내역</Typography.Title>
          <div className={styles.toolbarActions}>
            <Select<OrderStatusFilter>
              className={styles.statusFilter}
              value={statusFilter}
              options={STATUS_FILTER_OPTIONS}
              onChange={(value) => {
                setPage(DEFAULT_PAGE)
                setStatusFilter(value)
              }}
            />
            <Button loading={isLoading} onClick={() => void loadOrders()}>
              새로고침
            </Button>
          </div>
        </div>

        {errorMessage ? <Alert className={styles.error} type="error" message={errorMessage} showIcon /> : null}

        <Table<OrderResponse>
          rowKey="orderId"
          columns={columns}
          dataSource={orders}
          loading={isLoading}
          locale={{ emptyText: <Empty description="주문 데이터가 없습니다." /> }}
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
