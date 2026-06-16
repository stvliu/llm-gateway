import { useState, useEffect, useCallback, useRef } from 'react';
import { Tag, Button, Input, message, theme, Empty, Tooltip, Select, Spin } from 'antd';
import { PlusOutlined, ArrowRightOutlined, EyeOutlined, UpOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { InlineEditableList } from './InlineEditableList';
import type { ChannelModel, CreateChannelModelRequest } from '@/types/channel';
import {
  useCreateChannelModel,
  useDeleteChannelModel,
  useUpdateChannelModelFull,
  channelKeys,
} from '@/services/query/useChannels';
import { useModels } from '@/services/query/useModels';
import { extractErrorMessage } from '@/utils/errorMessage';
import type { PulseState } from '@/components/common/useSavePulse';
import { useDangerConfirm } from '@/components/common/useDangerConfirm';
import '@/components/common/SavePulse.css';

interface ModelMappingSectionProps {
  channelId: number;
  channelModels: ChannelModel[];
  /** 状态转换回调，由父组件（如 ChannelDetailDrawer）注入 */
  onStateTransition?: (modelId: number, targetState: string) => void;
}

/** 行级保存反馈状态（与 CredentialSection 保持同形态） */
interface RowPulse {
  state: PulseState;
  errorMsg?: string;
}

const ROW_PULSE_IDLE: RowPulse = { state: 'idle' };

function pulseClassName(p: RowPulse | undefined): string {
  if (!p) return '';
  if (p.state === 'success') return 'save-pulse-success';
  if (p.state === 'error') return 'save-pulse-error';
  return '';
}

/**
 * 模型映射区组件
 * 展示渠道的模型映射列表，支持行内编辑、添加和删除。
 * 编辑保存采用乐观更新 + 失败回滚 + 行内脉冲反馈策略。
 */
export function ModelMappingSection({ channelId, channelModels }: ModelMappingSectionProps) {
  const { t } = useTranslation('channels');
  const { token } = theme.useToken();
  const queryClient = useQueryClient();
  const [showAll, setShowAll] = useState(false);
  // 模型列表（用于添加表单的选择器）
  const { data: modelsPage, isLoading: modelsLoading } = useModels({ limit: 200 });
  const availableModels = modelsPage?.items ?? [];

  const [addMode, setAddMode] = useState(false);
  const [selectedModelId, setSelectedModelId] = useState<number | undefined>(undefined);
  const [newUpstreamName, setNewUpstreamName] = useState('');
  const [loading, setLoading] = useState(false);

  // 危险删除确认（任务 8.6）：注意必须把 contextHolder 渲染到组件树
  const { confirm: confirmDelete, contextHolder: dangerContextHolder } = useDangerConfirm();

  // Section 级 RowPulse 表（按 mapping.id 索引），自动归位定时器引用
  const [pulses, setPulses] = useState<Record<number, RowPulse>>({});
  const successTimers = useRef<Record<number, ReturnType<typeof setTimeout>>>({});

  /** 触发某行成功脉冲：3 秒后归位 idle */
  const triggerRowSuccess = useCallback((id: number) => {
    if (successTimers.current[id]) {
      clearTimeout(successTimers.current[id]);
    }
    setPulses((prev) => ({ ...prev, [id]: { state: 'success' } }));
    successTimers.current[id] = setTimeout(() => {
      setPulses((prev) => ({ ...prev, [id]: ROW_PULSE_IDLE }));
      delete successTimers.current[id];
    }, 3000);
  }, []);

  /** 触发某行错误脉冲：常驻直至下次 trigger */
  const triggerRowError = useCallback((id: number, msg: string) => {
    if (successTimers.current[id]) {
      clearTimeout(successTimers.current[id]);
      delete successTimers.current[id];
    }
    setPulses((prev) => ({ ...prev, [id]: { state: 'error', errorMsg: msg } }));
  }, []);

  // 卸载时清理所有定时器
  useEffect(() => {
    const timers = successTimers.current;
    return () => {
      Object.values(timers).forEach((tid) => clearTimeout(tid));
    };
  }, []);

  const createModel = useCreateChannelModel();
  const deleteModel = useDeleteChannelModel();
  const updateModelFull = useUpdateChannelModelFull();

  const displayModels = showAll ? channelModels : channelModels.slice(0, 5);

  const savedLabel = t('common:message.saved', { defaultValue: '已保存' });

  /** 渲染展示行（容器附 pulse className，行尾追加 ✓ / ✗ 反馈节点） */
  const renderItem = (mapping: ChannelModel) => {
    const pulse = pulses[mapping.id];
    const className = pulseClassName(pulse);
    return (
      <div
        className={className}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          width: '100%',
          padding: 4,
          borderRadius: 4,
        }}
      >
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
        {pulse?.state === 'success' && (
          <span className="save-tip-ok">✓ {savedLabel}</span>
        )}
        {pulse?.state === 'error' && (
          <span className="save-tip-err">✗ {pulse.errorMsg}</span>
        )}
      </div>
    );
  };

  /** 渲染编辑表单 */
  const renderEditForm = (
    mapping: ChannelModel,
    onSave: (updated: ChannelModel) => void,
    onCancel: () => void
  ) => {
    /**
     * 内嵌子组件：编辑表单局部状态。
     * 拆为独立组件，避免在 renderEditForm 函数体内调用 useState，
     * 引发 InlineEditableList 渲染分支变化时的 "Rendered more hooks" 错误。
     */
    const EditForm = () => {
      const [editModelId, setEditModelId] = useState<number>(mapping.modelId);
      const [editUpstreamName, setEditUpstreamName] = useState(
        mapping.upstreamModelName || ''
      );
      const [editLoading, setEditLoading] = useState(false);

      // 已关联的模型 ID 集合（排除自身）
      const mappedModelIds = new Set(
        channelModels.filter((m) => m.id !== mapping.id).map((m) => m.modelId)
      );

      const handleSave = async () => {
        setEditLoading(true);

        // 乐观更新：备份并写缓存（按 channel detail 下的 models 子键）
        const modelsKey = [...channelKeys.detail(channelId), 'models'] as const;
        await queryClient.cancelQueries({ queryKey: modelsKey });
        const prev = queryClient.getQueryData<ChannelModel[]>(modelsKey);
        if (prev) {
          queryClient.setQueryData<ChannelModel[]>(
            modelsKey,
            prev.map((m) =>
              m.id === mapping.id
                ? {
                    ...m,
                    modelId: editModelId,
                    upstreamModelName: editUpstreamName || null,
                  }
                : m
            )
          );
        }

        try {
          await updateModelFull.mutateAsync({
            channelId,
            modelId: mapping.id,
            data: {
              modelId: editModelId !== mapping.modelId ? editModelId : undefined,
              upstreamModelName: editUpstreamName || null,
            },
          });
          triggerRowSuccess(mapping.id);
          message.success(t('modelMapping.updateSuccess'));
          onSave({ ...mapping, modelId: editModelId, upstreamModelName: editUpstreamName || null });
        } catch (err) {
          // 失败：回滚缓存 + 触发 error 脉冲 + 保留 toast
          if (prev !== undefined) {
            queryClient.setQueryData(modelsKey, prev);
          }
          const reason = extractErrorMessage(err);
          triggerRowError(
            mapping.id,
            reason || t('modelMapping.updateFail')
          );
          message.error(
            reason
              ? t('common:message.saveFailed', { reason })
              : t('modelMapping.updateFail')
          );
          // 失败后退出编辑态，让展示态显示回滚后的原值（同时显示红框 + ✗ 错误）
          onCancel();
        } finally {
          setEditLoading(false);
          queryClient.invalidateQueries({ queryKey: modelsKey });
        }
      };

      return (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Select
            size="small"
            showSearch
            value={editModelId}
            loading={modelsLoading}
            notFoundContent={modelsLoading ? <Spin size="small" /> : null}
            style={{ width: 220 }}
            filterOption={(input, option) =>
              (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
            }
            onChange={(value) => setEditModelId(value)}
            options={availableModels
              .filter((m) => !mappedModelIds.has(m.id) || m.id === mapping.modelId)
              .map((m) => ({
                value: m.id,
                label: `${m.modelName}${m.displayName ? ` (${m.displayName})` : ''}`,
              }))}
          />
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

    return <EditForm />;
  };

  /** 渲染新增表单 */
  const renderAddForm = (
    onSave: (newItem: Partial<ChannelModel>) => void,
    onCancel: () => void
  ) => {
    const handleSave = async () => {
      if (selectedModelId === undefined) {
        message.warning(t('modelMapping.selectModelHint'));
        return;
      }
      try {
        setLoading(true);
        const data: CreateChannelModelRequest = {
          modelId: selectedModelId,
          upstreamModelName: newUpstreamName || undefined,
        };
        const result = await createModel.mutateAsync({ channelId, data });
        message.success(t('modelMapping.addSuccess'));
        onSave(result);
        setAddMode(false);
        setSelectedModelId(undefined);
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

    // 已关联的模型 ID 集合，用于过滤不可选的选项
    const mappedModelIds = new Set(channelModels.map((m) => m.modelId));

    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Select
          size="small"
          showSearch
          value={selectedModelId}
          placeholder={t('modelMapping.selectModelPlaceholder')}
          loading={modelsLoading}
          notFoundContent={modelsLoading ? <Spin size="small" /> : null}
          style={{ width: 220 }}
          filterOption={(input, option) =>
            (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
          }
          onChange={(value) => setSelectedModelId(value)}
          options={availableModels
            .filter((m) => !mappedModelIds.has(m.id))
            .map((m) => ({
              value: m.id,
              label: `${m.modelName}${m.displayName ? ` (${m.displayName})` : ''}`,
            }))}
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

  /**
   * 删除映射：弹危险确认 Modal（任务 8.6）。
   * description 含 modelId + "删除后，模型 ID xxx 不再被路由到此渠道"。
   */
  const handleDelete = (mapping: ChannelModel) => {
    const modelIdLabel = mapping.modelName || String(mapping.modelId);
    confirmDelete({
      titleKey: 'modelMapping.deleteTitle',
      descriptionKey: 'modelMapping.deleteDescription',
      descriptionParams: { modelId: modelIdLabel },
      onOk: async () => {
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
          // 必须 throw，让 useDangerConfirm 阻止 modal 关闭
          throw err;
        }
      },
    });
  };

  if (channelModels.length === 0 && !addMode) {
    return (
      <>
        {/* useDangerConfirm 的 contextHolder 必须挂载到组件树，否则 modal 不出现 */}
        {dangerContextHolder}
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={t('modelMapping.empty')}
        >
          <Button type="dashed" onClick={() => setAddMode(true)} icon={<PlusOutlined />}>
            {t('modelMapping.addMapping')}
          </Button>
        </Empty>
      </>
    );
  }

  return (
    <>
      {/* useDangerConfirm 的 contextHolder 必须挂载到组件树，否则 modal 不出现 */}
      {dangerContextHolder}
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
        <Tooltip title={t('modelMapping.viewAll', { count: channelModels.length })}>
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined />}
            style={{ marginTop: 8 }}
            onClick={() => setShowAll(true)}
          />
        </Tooltip>
      )}

      {showAll && channelModels.length > 5 && (
        <Tooltip title={t('modelMapping.collapse')}>
          <Button
            type="text"
            size="small"
            icon={<UpOutlined />}
            style={{ marginTop: 8 }}
            onClick={() => setShowAll(false)}
          />
        </Tooltip>
      )}
    </>
  );
}
