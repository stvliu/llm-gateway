'use client'

import { useEffect, useState } from 'react'
import { Modal, Form, Input, Select, InputNumber, Table, Tag, Button, Space, message, Alert } from 'antd'
import { CopyOutlined, DeleteOutlined } from '@ant-design/icons'
import type { UserApiKey, CreateUserApiKeyRequest, UpdateUserApiKeyRequest } from '@/types/team'
import { userApiKeyApi } from '@/services/api/userApiKey'
import { useUserApiKeys, useDeleteUserApiKey } from '@/services/query/useUserApiKeys'
import { useConfirm } from '@/hooks/useConfirm'
import { useTranslation } from 'react-i18next'

interface UserApiKeyModalProps {
  open: boolean
  userId: number
  username: string
  onClose: () => void
}

export default function UserApiKeyModal({
  open,
  userId,
  username,
  onClose,
}: UserApiKeyModalProps) {
  const { t } = useTranslation('users')
  const { confirm } = useConfirm()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [createdKey, setCreatedKey] = useState<string | null>(null)
  const [editingKey, setEditingKey] = useState<UserApiKey | null>(null)
  const [showForm, setShowForm] = useState(false)

  const { data: apiKeys, isLoading } = useUserApiKeys(userId)
  const deleteMutation = useDeleteUserApiKey(userId)

  useEffect(() => {
    if (open) {
      setCreatedKey(null)
      setEditingKey(null)
      setShowForm(false)
    }
  }, [open])

  const handleCreate = () => {
    setEditingKey(null)
    setCreatedKey(null)
    form.resetFields()
    setShowForm(true)
  }

  const handleEdit = (key: UserApiKey) => {
    setEditingKey(key)
    setCreatedKey(null)
    form.setFieldsValue({
      name: key.name,
      models: key.models,
      quotaLimit: key.quotaLimit,
      state: key.state,
    })
    setShowForm(true)
  }

  const handleDelete = (keyId: number) => {
    confirm({
      type: 'danger',
      onConfirm: () => deleteMutation.mutateAsync(keyId),
    })
  }

  const handleCopyKey = (text: string) => {
    navigator.clipboard.writeText(text)
    message.success(t('apiKey.copySuccess'))
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      if (editingKey) {
        const request: UpdateUserApiKeyRequest = {
          name: values.name,
          models: values.models,
          quotaLimit: values.quotaLimit,
          state: values.state,
        }
        await userApiKeyApi.update(editingKey.id, request)
        message.success(t('apiKey.updateSuccess'))
      } else {
        const request: CreateUserApiKeyRequest = {
          userId,
          name: values.name,
          models: values.models,
          quotaLimit: values.quotaLimit,
        }
        const result = await userApiKeyApi.create(request)
        setCreatedKey(result.keyPlain)
        message.success(t('apiKey.createSuccess'))
      }

      setShowForm(false)
      setEditingKey(null)
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return
      message.error(editingKey ? t('apiKey.updateFailed') : t('apiKey.createFailed'))
    } finally {
      setLoading(false)
    }
  }

  const handleCancelForm = () => {
    setShowForm(false)
    setEditingKey(null)
    setCreatedKey(null)
  }

  const columns = [
    {
      title: t('apiKey.name'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('apiKey.key'),
      dataIndex: 'keyPrefix',
      key: 'keyPrefix',
      render: (prefix: string) => (
        <Space>
          <code>{prefix}...</code>
          <Button type="text" size="small" icon={<CopyOutlined />} onClick={() => handleCopyKey(prefix)} />
        </Space>
      ),
    },
    {
      title: t('apiKey.state'),
      dataIndex: 'state',
      key: 'state',
      render: (state: string) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'red'}>
          {state === 'ACTIVE' ? t('state.active') : t('state.disabled')}
        </Tag>
      ),
    },
    {
      title: t('apiKey.createdAt'),
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
    {
      title: t('actions.label', { ns: 'common' }),
      key: 'actions',
      width: 120,
      render: (_: unknown, record: UserApiKey) => (
        <Space>
          <Button type="text" size="small" onClick={() => handleEdit(record)}>
            {t('actions.edit', { ns: 'common' })}
          </Button>
          <Button type="text" danger size="small" icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    },
  ]

  return (
    <Modal
      title={`${t('apiKeyList')} — ${username}`}
      open={open}
      onCancel={onClose}
      width={680}
      footer={null}
    >
      {createdKey && (
        <div style={{ marginBottom: 16, padding: 12, background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6 }}>
          <div style={{ marginBottom: 4, fontWeight: 500 }}>{t('apiKey.createSuccess')}</div>
          <code style={{ wordBreak: 'break-all', fontSize: 13 }}>{createdKey}</code>
          <Button type="text" size="small" icon={<CopyOutlined />} onClick={() => handleCopyKey(createdKey)} />
          <div style={{ marginTop: 4, color: '#999', fontSize: 12 }}>{t('apiKey.oneTimeHint')}</div>
        </div>
      )}

      {!showForm && (
        <div style={{ marginBottom: 12 }}>
          <Alert
            message="权限说明"
            description="API Key 的渠道访问权限由用户所属团队决定，创建时无需选择渠道。"
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
          />
          <Button type="primary" onClick={handleCreate}>{t('addApiKey')}</Button>
        </div>
      )}

      {showForm && (
        <div style={{ marginBottom: 16, padding: 16, border: '1px solid #d9d9d9', borderRadius: 6 }}>
          <Form form={form} layout="vertical">
            <Form.Item name="name" label={t('apiKey.name')} rules={[{ required: true, message: t('apiKey.nameRequired') }]}>
              <Input placeholder={t('apiKey.namePlaceholder')} />
            </Form.Item>
            <Form.Item name="models" label={t('apiKey.models')}>
              <Select
                mode="tags"
                placeholder={t('apiKey.modelsPlaceholder')}
                tokenSeparators={[',']}
              />
            </Form.Item>
            <Form.Item name="quotaLimit" label={t('apiKey.quotaLimit')}>
              <InputNumber style={{ width: '100%' }} placeholder={t('apiKey.quotaPlaceholder')} min={0} />
            </Form.Item>
            {editingKey && (
              <Form.Item name="state" label={t('apiKey.state')}>
                <Select
                  options={[
                    { label: t('state.active'), value: 'ACTIVE' },
                    { label: t('state.disabled'), value: 'DISABLED' },
                  ]}
                />
              </Form.Item>
            )}
            <Form.Item>
              <Space>
                <Button type="primary" onClick={handleSubmit} loading={loading}>
                  {editingKey ? t('actions.save', { ns: 'common' }) : t('actions.create', { ns: 'common' })}
                </Button>
                <Button onClick={handleCancelForm}>{t('actions.cancel', { ns: 'common' })}</Button>
              </Space>
            </Form.Item>
          </Form>
        </div>
      )}

      <Table
        columns={columns}
        dataSource={apiKeys || []}
        rowKey="id"
        loading={isLoading}
        size="small"
        pagination={false}
      />
    </Modal>
  )
}