'use client'

import { useEffect, useState } from 'react'
import { Modal, Form, Input, Select, InputNumber, message } from 'antd'
import type { Team, UserApiKey, CreateUserApiKeyRequest, UpdateUserApiKeyRequest } from '@/types/team'
import type { Product } from '@/types/product'
import { teamApi } from '@/services/api/team'
import { productApi } from '@/services/api/product'

interface UserApiKeyManageModalProps {
  open: boolean
  team: Team
  editingKey?: UserApiKey | null
  onClose: () => void
  onSuccess?: () => void
}

export default function UserApiKeyManageModal({
  open,
  team,
  editingKey,
  onClose,
  onSuccess,
}: UserApiKeyManageModalProps) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [products, setProducts] = useState<Product[]>([])
  const [createdKey, setCreatedKey] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      loadProducts()
      if (editingKey) {
        form.setFieldsValue({
          name: editingKey.name,
          productIds: editingKey.productIds,
          models: editingKey.models,
          quotaLimit: editingKey.quotaLimit,
          state: editingKey.state,
        })
        setCreatedKey(null)
      } else {
        form.resetFields()
        setCreatedKey(null)
      }
    }
  }, [open, editingKey, form])

  const loadProducts = async () => {
    try {
      const res = await productApi.listAll()
      setProducts(res)
    } catch {
      // 产品列表加载失败时静默处理
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      if (editingKey) {
        const request: UpdateUserApiKeyRequest = {
          name: values.name,
          productIds: values.productIds,
          models: values.models,
          quotaLimit: values.quotaLimit,
          state: values.state,
        }
        await teamApi.updateApiKey(team.id, editingKey.id, request)
        message.success('API Key 更新成功')
      } else {
        const request: CreateUserApiKeyRequest = {
          teamId: team.id,
          userId: values.userId,
          productIds: values.productIds,
          name: values.name,
          models: values.models,
          quotaLimit: values.quotaLimit,
        }
        const result = await teamApi.createApiKey(team.id, request)
        setCreatedKey(result.keyPlain)
        message.success('API Key 创建成功')
      }

      onSuccess?.()
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return
      message.error(editingKey ? '更新失败' : '创建失败')
    } finally {
      setLoading(false)
    }
  }

  const productOptions = products.map((p) => ({
    label: p.name,
    value: p.id,
  }))

  return (
    <Modal
      title={editingKey ? '编辑 API Key' : '创建 API Key'}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      width={560}
      okText={editingKey ? '保存' : '创建'}
    >
      {createdKey && (
        <div style={{ marginBottom: 16, padding: 12, background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6 }}>
          <div style={{ marginBottom: 4, fontWeight: 500 }}>API Key 创建成功，请妥善保存：</div>
          <code style={{ wordBreak: 'break-all', fontSize: 13 }}>{createdKey}</code>
          <div style={{ marginTop: 4, color: '#999', fontSize: 12 }}>此密钥仅显示一次，关闭后无法再次查看</div>
        </div>
      )}

      <Form form={form} layout="vertical">
        <Form.Item name="name" label="密钥名称" rules={[{ required: true, message: '请输入密钥名称' }]}>
          <Input placeholder="例如：生产环境 Key" />
        </Form.Item>

        {!editingKey && (
          <Form.Item name="userId" label="用户 ID" rules={[{ required: true, message: '请输入用户 ID' }]}>
            <InputNumber style={{ width: '100%' }} placeholder="用户 ID" />
          </Form.Item>
        )}

        <Form.Item
          name="productIds"
          label="关联产品"
          rules={[{ required: true, message: '请选择至少一个产品' }]}
        >
          <Select
            mode="multiple"
            placeholder="选择关联的产品"
            options={productOptions}
            optionFilterProp="label"
            maxTagCount="responsive"
          />
        </Form.Item>

        <Form.Item name="models" label="可用模型">
          <Select
            mode="tags"
            placeholder="留空表示允许所有模型，或输入模型名称"
            tokenSeparators={[',']}
          />
        </Form.Item>

        <Form.Item name="quotaLimit" label="额度限制">
          <InputNumber style={{ width: '100%' }} placeholder="留空表示不限制" min={0} />
        </Form.Item>

        {editingKey && (
          <Form.Item name="state" label="状态">
            <Select
              options={[
                { label: '启用', value: 'ACTIVE' },
                { label: '禁用', value: 'DISABLED' },
              ]}
            />
          </Form.Item>
        )}
      </Form>
    </Modal>
  )
}
