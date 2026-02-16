import { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType, TablePaginationConfig } from 'antd'
import {
  createAdminProduct,
  deleteAdminProduct,
  getAdminProducts,
  updateAdminProduct,
} from '../../shared/api/adminApi'
import { getErrorMessage } from '../../shared/api/httpClient'
import { formatDateTime } from '../../shared/date/dateValue'
import { formatPoint } from '../../shared/format/displayFormat'
import type {
  AdminProductResponse,
  ApiConnection,
  ProductStatus,
} from '../../shared/types/adminApi'
import styles from './ProductPage.module.css'

interface ProductPageProps {
  connection: ApiConnection | null
}

interface ProductFormValues {
  name: string
  description: string
  pricePoints: number
  stock: number
  status: ProductStatus
}

const DEFAULT_PAGE = 0
const DEFAULT_SIZE = 20

const STATUS_OPTIONS: Array<{ label: string; value: ProductStatus }> = [
  { label: '활성', value: 'ACTIVE' },
  { label: '비활성', value: 'INACTIVE' },
]

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isFormValidationError(error: unknown): boolean {
  if (!isRecord(error)) {
    return false
  }

  return Array.isArray(error.errorFields)
}

function getStatusTag(status: ProductStatus): { color: string; label: string } {
  if (status === 'ACTIVE') {
    return { color: 'green', label: '활성' }
  }

  return { color: 'default', label: '비활성' }
}

export function ProductPage({ connection }: ProductPageProps) {
  const [products, setProducts] = useState<AdminProductResponse[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [page, setPage] = useState(DEFAULT_PAGE)
  const [size, setSize] = useState(DEFAULT_SIZE)
  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [deletingProductId, setDeletingProductId] = useState<number | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<AdminProductResponse | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [form] = Form.useForm<ProductFormValues>()

  const loadProducts = useCallback(async (): Promise<void> => {
    if (!connection) {
      setProducts([])
      setTotalItems(0)
      setErrorMessage(null)
      return
    }

    try {
      setIsLoading(true)
      setErrorMessage(null)

      const response = await getAdminProducts(connection)
      setProducts(response.items)
      setTotalItems(response.totalItems)
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
      setProducts([])
      setTotalItems(0)
    } finally {
      setIsLoading(false)
    }
  }, [connection])

  useEffect(() => {
    void loadProducts()
  }, [loadProducts])

  const openCreateModal = (): void => {
    setEditingProduct(null)
    form.setFieldsValue({
      name: '',
      description: '',
      pricePoints: 100,
      stock: 0,
      status: 'ACTIVE',
    })
    setIsModalOpen(true)
  }

  const openEditModal = (product: AdminProductResponse): void => {
    setEditingProduct(product)
    form.setFieldsValue({
      name: product.name,
      description: product.description,
      pricePoints: product.pricePoints,
      stock: product.stock,
      status: product.status,
    })
    setIsModalOpen(true)
  }

  const closeModal = (): void => {
    setIsModalOpen(false)
    setEditingProduct(null)
    form.resetFields()
  }

  const handleSubmitProduct = async (): Promise<void> => {
    if (!connection) {
      return
    }

    try {
      const values = await form.validateFields()
      setIsSaving(true)
      setErrorMessage(null)

      if (editingProduct) {
        await updateAdminProduct(connection, editingProduct.productId, {
          name: values.name,
          description: values.description,
          pricePoints: values.pricePoints,
          stock: values.stock,
          status: values.status,
        })
      } else {
        await createAdminProduct(connection, {
          name: values.name,
          description: values.description,
          pricePoints: values.pricePoints,
          stock: values.stock,
          status: values.status,
        })
      }

      closeModal()
      await loadProducts()
    } catch (error) {
      if (isFormValidationError(error)) {
        return
      }

      setErrorMessage(getErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  const handleDeleteProduct = async (productId: number): Promise<void> => {
    if (!connection) {
      return
    }

    try {
      setDeletingProductId(productId)
      setErrorMessage(null)

      await deleteAdminProduct(connection, productId)

      if (products.length === 1 && page > DEFAULT_PAGE) {
        setPage(page - 1)
      } else {
        await loadProducts()
      }
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
    } finally {
      setDeletingProductId(null)
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

  const columns: TableColumnsType<AdminProductResponse> = [
    {
      title: '상품 ID',
      dataIndex: 'productId',
      key: 'productId',
      width: 110,
    },
    {
      title: '상품명',
      dataIndex: 'name',
      key: 'name',
      width: 220,
    },
    {
      title: '설명',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: '가격',
      dataIndex: 'pricePoints',
      key: 'pricePoints',
      render: (value: number) => formatPoint(value),
      width: 130,
    },
    {
      title: '재고',
      dataIndex: 'stock',
      key: 'stock',
      width: 100,
    },
    {
      title: '상태',
      dataIndex: 'status',
      key: 'status',
      render: (value: ProductStatus) => {
        const statusTag = getStatusTag(value)
        return <Tag color={statusTag.color}>{statusTag.label}</Tag>
      },
      width: 100,
    },
    {
      title: '수정일',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      render: (value: string) => formatDateTime(value),
      width: 170,
    },
    {
      title: '작업',
      key: 'action',
      render: (_, record) => (
        <div className={styles.toolbarActions}>
          <Button size="small" onClick={() => openEditModal(record)}>
            수정
          </Button>
          <Popconfirm
            title="상품을 삭제할까요?"
            description="삭제 후 복구할 수 없습니다."
            okText="삭제"
            cancelText="취소"
            onConfirm={() => void handleDeleteProduct(record.productId)}
          >
            <Button
              danger
              size="small"
              loading={deletingProductId === record.productId}
            >
              삭제
            </Button>
          </Popconfirm>
        </div>
      ),
      width: 170,
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
          <Typography.Title level={4}>상품 관리</Typography.Title>
          <div className={styles.toolbarActions}>
            <Button onClick={() => void loadProducts()} loading={isLoading}>
              새로고침
            </Button>
            <Button type="primary" onClick={openCreateModal}>
              상품 등록
            </Button>
          </div>
        </div>

        {errorMessage ? <Alert className={styles.error} type="error" message={errorMessage} showIcon /> : null}

        <Table<AdminProductResponse>
          rowKey="productId"
          columns={columns}
          dataSource={products}
          loading={isLoading}
          locale={{ emptyText: <Empty description="등록된 상품이 없습니다." /> }}
          pagination={{
            current: page + 1,
            pageSize: size,
            total: totalItems,
            showSizeChanger: true,
          }}
          onChange={handlePaginationChange}
        />
      </Card>

      <Modal
        title={editingProduct ? '상품 수정' : '상품 등록'}
        open={isModalOpen}
        onCancel={closeModal}
        onOk={() => void handleSubmitProduct()}
        confirmLoading={isSaving}
        okText={editingProduct ? '수정' : '등록'}
        cancelText="닫기"
        destroyOnClose
      >
        <Form<ProductFormValues> form={form} layout="vertical" requiredMark={false}>
          <Form.Item
            label="상품명"
            name="name"
            rules={[
              { required: true, message: '상품명을 입력해 주세요.' },
              { min: 2, message: '상품명은 2자 이상이어야 합니다.' },
              { max: 100, message: '상품명은 100자 이하로 입력해 주세요.' },
            ]}
          >
            <Input />
          </Form.Item>

          <Form.Item
            label="설명"
            name="description"
            rules={[
              { required: true, message: '설명을 입력해 주세요.' },
              { min: 1, message: '설명을 입력해 주세요.' },
              { max: 500, message: '설명은 500자 이하로 입력해 주세요.' },
            ]}
          >
            <Input.TextArea rows={3} />
          </Form.Item>

          <Form.Item
            label="가격(포인트)"
            name="pricePoints"
            rules={[{ required: true, message: '가격을 입력해 주세요.' }]}
          >
            <InputNumber className={styles.fullWidthInput} min={1} max={1000000} />
          </Form.Item>

          <Form.Item
            label="재고"
            name="stock"
            rules={[{ required: true, message: '재고를 입력해 주세요.' }]}
          >
            <InputNumber className={styles.fullWidthInput} min={0} max={1000000} />
          </Form.Item>

          <Form.Item
            label="상태"
            name="status"
            rules={[{ required: true, message: '상태를 선택해 주세요.' }]}
          >
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
