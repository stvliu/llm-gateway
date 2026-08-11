/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use client'

import { useEffect, useMemo, useState } from 'react'
import { Modal, Form, Input, Table, Button, Space, Select, message, Tooltip } from 'antd'
import { CopyOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons'
import type { UserApiKey, CreateUserApiKeyRequest, UpdateUserApiKeyRequest } from '@/types/userApiKey'
import { userApiKeyApi } from '@/services/api/userApiKey'
import { useUserApiKeys, useDeleteUserApiKey } from '@/services/query/useUserApiKeys'
import { useApplications } from '@/services/query/useApplications'
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
  const deleteMutation = useDeleteUserApiKey()
  // 应用列表（Key 的权限锚点：通过应用-渠道授权继承渠道访问权限）
  const { data: applications } = useApplications()
  const applicationMap = useMemo(() => {
    const map = new Map<number, { id: number; name: string }>()
    applications?.forEach((a) => map.set(a.id, a))
    return map
  }, [applications])

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
    // 补绑交互：回填已存在 Key 的 applicationId（null 时表单显示空，可补绑）
    form.setFieldsValue({
      name: key.name,
      applicationId: key.applicationId ?? undefined,
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
        // 编辑模式：update 传 applicationId 支持补绑/转移
        const request: UpdateUserApiKeyRequest = {
          name: values.name,
          applicationId: values.applicationId,
        }
        await userApiKeyApi.update(editingKey.id, request)
        message.success(t('apiKey.updateSuccess', { defaultValue: '更新成功' }))
      } else {
        // 创建模式：applicationId 为 Key 的权限锚点（必填）
        const request: CreateUserApiKeyRequest = {
          userId,
          applicationId: values.applicationId,
          name: values.name,
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
      title: t('apiKey.application', { defaultValue: '所属应用' }),
      dataIndex: 'applicationId',
      key: 'applicationId',
      render: (appId: number | null) => {
        if (appId == null) return '未绑定'
        const app = applicationMap.get(appId)
        return app ? `${app.name} (${app.id})` : `应用 ${appId}`
      },
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
          <Tooltip title={t('actions.edit', { ns: 'common' })}>
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          </Tooltip>
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
          <Button type="primary" onClick={handleCreate}>{t('addApiKey')}</Button>
        </div>
      )}

      {showForm && (
        <div style={{ marginBottom: 16, padding: 16, border: '1px solid #d9d9d9', borderRadius: 6 }}>
          <Form form={form} layout="vertical">
            <Form.Item
              name="applicationId"
              label={t('apiKey.application', { defaultValue: '所属应用' })}
              rules={[{ required: true, message: '请选择应用' }]}
              extra="Key 的渠道权限由应用-渠道授权决定"
            >
              <Select
                showSearch
                placeholder="选择应用（权限锚点）"
                filterOption={(input, option) =>
                  (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
                }
                options={(applications ?? []).map((a) => ({
                  label: `${a.name} (${a.id})`,
                  value: a.id,
                }))}
              />
            </Form.Item>
            <Form.Item name="name" label={t('apiKey.name')} rules={[{ required: true, message: t('apiKey.nameRequired', { defaultValue: '请输入名称' }) }]}>
              <Input placeholder={t('apiKey.namePlaceholder', { defaultValue: '请输入名称' })} />
            </Form.Item>
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