import { useState } from 'react';
import { Tag, Button, Input, message, theme, Empty } from 'antd';
import { PlusOutlined, ArrowRightOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { InlineEditableList } from './InlineEditableList';
import type { ChannelModel, CreateChannelModelRequest } from '@/types/channel';
import {
  useCreateChannelModel,
  useDeleteChannelModel,
  useUpdateChannelModel,
} from '@/services/query/useChannels';
import { extractErrorMessage } from '@/utils/errorMessage';

interface ModelMappingSectionProps {
  channelId: number;
  channelModels: ChannelModel[];
}

/**
 * 模型映射区组件
 * 展示渠道的模型映射列表，支持行内编辑、添加和删除
 */
export function ModelMappingSection({ channelId, channelModels }: ModelMappingSectionProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const [showAll, setShowAll] = useState(false);
  const [addMode, setAddMode] = useState(false);
  const [newModelId, setNewModelId] = useState('');
  const [newUpstreamName, setNewUpstreamName] = useState('');
  const [loading, setLoading] = useState(false);

  const createModel = useCreateChannelModel();
  const deleteModel = useDeleteChannelModel();
  const updateUpstreamName = useUpdateChannelModel();

  const displayModels = showAll ? channelModels : channelModels.slice(0, 5);

  /** 渲染展示行 */
  const renderItem = (mapping: ChannelModel) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: '100%' }}>
      <Tag color="blue">{mapping.modelName || String(mapping.modelId)}</Tag>
      {mapping.upstreamModelName && mapping.upstreamModelName !== mapping.modelName && (
        <>
          <ArrowRightOutlined style={{ color: token.colorTextSecondary }} />
          <Tag color="geekblue">{mapping.upstreamModelName}</Tag>
        </>
      )}
      <Tag color={mapping.state === 'ACTIVE' ? 'green' : 'default'}>
        {mapping.state === 'ACTIVE' ? t('status.active') : t('status.inactive')}
      </Tag>
    </div>
  );

  /** 渲染编辑表单 */
  const renderEditForm = (
    mapping: ChannelModel,
    onSave: (updated: ChannelModel) => void,
    onCancel: () => void
  ) => {
    const [editUpstreamName, setEditUpstreamName] = useState(mapping.upstreamModelName || '');
    const [editLoading, setEditLoading] = useState(false);

    const handleSave = async () => {
      try {
        setEditLoading(true);
        await updateUpstreamName.mutateAsync({
          channelId,
          modelId: mapping.id,
          upstreamModelName: editUpstreamName || '',
        });
        message.success(t('modelMapping.updateSuccess'));
        onSave({ ...mapping, upstreamModelName: editUpstreamName || null });
      } catch (err) {
        // 把后端 / 网络具体原因带给用户；无原因时回退到既有兜底文案
        const reason = extractErrorMessage(err);
        message.error(
          reason
            ? t('common:message.saveFailed', { reason })
            : t('modelMapping.updateFail')
        );
      } finally {
        setEditLoading(false);
      }
    };

    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Tag color="blue">{mapping.modelName || String(mapping.modelId)}</Tag>
        <ArrowRightOutlined style={{ color: token.colorTextSecondary }} />
        <Input
          size="small"
          value={editUpstreamName}
          onChange={(e) => setEditUpstreamName(e.target.value)}
          placeholder={t('modelMapping.fetchFromUpstream')}
          style={{ width: 200 }}
        />
        <Button type="primary" size="small" onClick={handleSave} loading={editLoading}>
          {t('drawer.save')}
        </Button>
        <Button size="small" onClick={onCancel}>
          {t('drawer.cancel')}
        </Button>
      </div>
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
        const data: CreateChannelModelRequest = {
          modelId: Number(newModelId),
          upstreamModelName: newUpstreamName || undefined,
        };
        const result = await createModel.mutateAsync({ channelId, data });
        message.success(t('modelMapping.addSuccess'));
        onSave(result);
        setAddMode(false);
        setNewModelId('');
        setNewUpstreamName('');
      } catch (err) {
        const reason = extractErrorMessage(err);
        message.error(
          reason
            ? t('common:message.saveFailed', { reason })
            : t('modelMapping.addFail')
        );
      } finally {
        setLoading(false);
      }
    };

    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Input
          size="small"
          value={newModelId}
          onChange={(e) => setNewModelId(e.target.value)}
          placeholder={t('modelMapping.modelIdPlaceholder')}
          style={{ width: 150 }}
        />
        <ArrowRightOutlined style={{ color: token.colorTextSecondary }} />
        <Input
          size="small"
          value={newUpstreamName}
          onChange={(e) => setNewUpstreamName(e.target.value)}
          placeholder={t('modelMapping.fetchFromUpstream')}
          style={{ width: 200 }}
        />
        <Button type="primary" size="small" onClick={handleSave} loading={loading}>
          {t('drawer.save')}
        </Button>
        <Button size="small" onClick={onCancel}>
          {t('drawer.cancel')}
        </Button>
      </div>
    );
  };

  /** 删除映射 */
  const handleDelete = async (mapping: ChannelModel) => {
    try {
      await deleteModel.mutateAsync({ channelId, modelId: mapping.id });
      message.success(t('modelMapping.deleteSuccess'));
    } catch (err) {
      const reason = extractErrorMessage(err);
      message.error(
        reason
          ? t('common:message.saveFailed', { reason })
          : t('modelMapping.deleteFail')
      );
    }
  };

  if (channelModels.length === 0 && !addMode) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description={t('modelMapping.empty')}
      >
        <Button type="dashed" onClick={() => setAddMode(true)} icon={<PlusOutlined />}>
          {t('modelMapping.addMapping')}
        </Button>
      </Empty>
    );
  }

  return (
    <>
      <InlineEditableList
        items={displayModels}
        renderItem={renderItem}
        renderEditForm={renderEditForm}
        renderAddForm={renderAddForm}
        onAdd={() => setAddMode(true)}
        onDelete={handleDelete}
        getKey={(mapping) => mapping.id}
        addLabel={t('modelMapping.addMapping')}
      />

      {!showAll && channelModels.length > 5 && (
        <Button
          type="link"
          size="small"
          style={{ marginTop: 8 }}
          onClick={() => setShowAll(true)}
        >
          {t('modelMapping.viewAll', { count: channelModels.length })}
        </Button>
      )}

      {showAll && channelModels.length > 5 && (
        <Button
          type="link"
          size="small"
          style={{ marginTop: 8 }}
          onClick={() => setShowAll(false)}
        >
          {t('modelMapping.collapse')}
        </Button>
      )}
    </>
  );
}
