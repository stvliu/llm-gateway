import { useState, useEffect } from 'react';
import { Tag, Input, Button, Space, Form, message, Typography, theme } from 'antd';
import { InlineEditableList } from './InlineEditableList';
import type { ChannelModel, CreateChannelModelRequest } from '@/types/channel';
import { useChannelModels, useCreateChannelModel, useDeleteChannelModel, useUpdateChannelModel } from '@/services/query/useChannels';

const { Link } = Typography;

interface ModelMappingSectionProps {
  channelId: number;
  onFetchUpstream?: () => void;
}

/**
 * 模型映射区组件
 * 展示渠道的模型映射列表，支持行内编辑
 */
export function ModelMappingSection({ channelId, onFetchUpstream }: ModelMappingSectionProps) {
  const { token } = theme.useToken();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [showAll, setShowAll] = useState(false);
  const [editingId] = useState<number | null>(null);

  // 获取模型映射
  const { data: models = [] } = useChannelModels(channelId);

  // Mutations
  const createModel = useCreateChannelModel();
  const deleteModel = useDeleteChannelModel();
  const updateModel = useUpdateChannelModel();

  /** 编辑时同步表单值 */
  useEffect(() => {
    if (editingId !== null) {
      const model = models.find(m => m.id === editingId);
      if (model) {
        form.setFieldsValue({
          modelName: model.modelName,
          upstreamModelName: model.upstreamModelName,
        });
      }
    }
  }, [editingId, models, form]);

  /** channelId 变化时重置展开状态 */
  useEffect(() => {
    setShowAll(false);
  }, [channelId]);

  /** 渲染展示行 */
  const renderItem = (model: ChannelModel) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: '100%' }}>
      {/* 模型名 */}
      <span style={{ fontWeight: 500, minWidth: 150 }}>{model.modelName}</span>
      {/* 箭头 */}
      <span style={{ color: token.colorTextSecondary }}>→</span>
      {/* 上游模型名 */}
      <span style={{ fontFamily: 'monospace', flex: 1 }}>
        {model.upstreamModelName}
      </span>
      {/* 定价占位（暂无数据） */}
      <Tag color="default">$--/$-- per 1M</Tag>
    </div>
  );

  /** 渲染编辑表单 */
  const renderEditForm = (
    model: ChannelModel,
    onSave: (updated: ChannelModel) => void,
    onCancel: () => void
  ) => {
    const handleSave = async () => {
      try {
        setLoading(true);
        const values = await form.validateFields();
        // 仅更新上游模型名（后端 API 限制）
        if (values.upstreamModelName !== model.upstreamModelName) {
          await updateModel.mutateAsync({
            channelId,
            modelId: model.id,
            upstreamModelName: values.upstreamModelName,
          });
        }
        message.success('模型映射更新成功');
        onSave({ ...model, ...values });
      } catch (error) {
        message.error('模型映射更新失败');
      } finally {
        setLoading(false);
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <Form.Item
          name="modelName"
          label="模型名"
          rules={[{ required: true, message: '请输入模型名' }]}
        >
          <Input style={{ width: 200 }} placeholder="gpt-4o" disabled />
        </Form.Item>
        <Form.Item
          name="upstreamModelName"
          label="上游模型名"
          rules={[{ required: true, message: '请输入上游模型名' }]}
        >
          <Input style={{ width: 200 }} placeholder="gpt-4o-2024-05-13" />
        </Form.Item>
        <Space>
          <Button type="primary" size="small" onClick={handleSave} loading={loading}>
            保存
          </Button>
          <Button size="small" onClick={onCancel}>
            取消
          </Button>
        </Space>
      </Form>
    );
  };

  /** 渲染新增表单 */
  const renderAddForm = (
    onSave: (newItem: Partial<ChannelModel>) => void,
    onCancel: () => void
  ) => {
    const handleSave = async () => {
      try {
        setLoading(true);
        const values = await form.validateFields();
        const data: CreateChannelModelRequest = {
          modelName: values.modelName,
          upstreamModelName: values.upstreamModelName,
        };
        const result = await createModel.mutateAsync({ channelId, data });
        message.success('模型映射添加成功');
        onSave(result);
      } catch (error) {
        message.error('模型映射添加失败');
      } finally {
        setLoading(false);
      }
    };

    return (
      <Form form={form} layout="inline" style={{ gap: 12 }}>
        <Form.Item
          name="modelName"
          label="模型名"
          rules={[{ required: true, message: '请输入模型名' }]}
        >
          <Input style={{ width: 200 }} placeholder="gpt-4o" />
        </Form.Item>
        <Form.Item
          name="upstreamModelName"
          label="上游模型名"
          rules={[{ required: true, message: '请输入上游模型名' }]}
        >
          <Input style={{ width: 200 }} placeholder="gpt-4o-2024-05-13" />
        </Form.Item>
        <Space>
          <Button type="primary" size="small" onClick={handleSave} loading={loading}>
            保存
          </Button>
          <Button size="small" onClick={onCancel}>
            取消
          </Button>
        </Space>
      </Form>
    );
  };

  /** 删除模型映射 */
  const handleDelete = async (model: ChannelModel) => {
    try {
      await deleteModel.mutateAsync({ channelId, modelId: model.id });
      message.success('模型映射删除成功');
    } catch (error) {
      message.error('模型映射删除失败');
    }
  };

  // 显示前3个或全部
  const displayedModels = showAll ? models : models.slice(0, 3);

  return (
    <div>
      {/* 从上游获取按钮 */}
      <div style={{ marginBottom: 12 }}>
        <Button
          type="dashed"
          size="small"
          onClick={() => {
            if (onFetchUpstream) {
              onFetchUpstream();
            } else {
              message.info('此功能将在后续实现');
            }
          }}
        >
          从上游获取
        </Button>
      </div>

      {/* 模型列表 */}
      <InlineEditableList
        items={displayedModels}
        renderItem={renderItem}
        renderEditForm={renderEditForm}
        renderAddForm={renderAddForm}
        onAdd={() => {
          form.resetFields();
        }}
        onDelete={handleDelete}
        getKey={(model) => model.id}
        addLabel="添加模型映射"
      />

      {/* 查看全部链接 */}
      {models.length > 3 && !showAll && (
        <div style={{ marginTop: 8, textAlign: 'center' }}>
          <Link onClick={() => setShowAll(true)}>
            查看全部 {models.length} 个 →
          </Link>
        </div>
      )}
    </div>
  );
}