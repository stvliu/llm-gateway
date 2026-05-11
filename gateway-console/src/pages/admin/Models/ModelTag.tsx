import { Tag, Space, Tooltip } from 'antd';
import {
  MessageOutlined,
  FileTextOutlined,
  AudioOutlined,
  PictureOutlined,
  ApiOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Model } from '@/types/model';

interface ModelTagProps {
  model: Model;
  onClick?: (model: Model) => void;
}

/**
 * 根据模型能力推断类型
 */
function inferModelType(model: Model): string {
  if (model.capabilities) {
    if (model.capabilities.chat) return 'CHAT';
    if (model.capabilities.completion) return 'COMPLETION';
    if (model.capabilities.embedding) return 'EMBEDDING';
    if (model.capabilities.image) return 'IMAGE';
    if (model.capabilities.audio) return 'AUDIO';
  }
  return 'CHAT';
}

type ModelType = 'CHAT' | 'COMPLETION' | 'EMBEDDING' | 'IMAGE' | 'AUDIO';

/**
 * 获取模型类型图标
 */
function getModelTypeIcon(type: ModelType) {
  const iconMap: Record<ModelType, React.ReactNode> = {
    CHAT: <MessageOutlined />,
    COMPLETION: <FileTextOutlined />,
    EMBEDDING: <ApiOutlined />,
    IMAGE: <PictureOutlined />,
    AUDIO: <AudioOutlined />,
  };
  return iconMap[type] || <ApiOutlined />;
}

/**
 * 获取模型类型颜色
 */
function getModelTypeColor(type: ModelType): string {
  const colorMap: Record<ModelType, string> = {
    CHAT: 'blue',
    COMPLETION: 'cyan',
    EMBEDDING: 'purple',
    IMAGE: 'orange',
    AUDIO: 'green',
  };
  return colorMap[type] || 'default';
}

/**
 * Model 标签组件
 * 显示模型名称、类型图标和状态
 * 点击打开详情抽屉
 */
export function ModelTag({ model, onClick }: ModelTagProps) {
  const { t } = useTranslation('models');

  const isDisabled = model.state !== 'ACTIVE';
  const modelType = inferModelType(model) as ModelType;
  const displayName = model.displayName || model.providerModelId || `Model ${model.id}`;

  return (
    <Tag
      color={isDisabled ? undefined : getModelTypeColor(modelType)}
      style={{
        margin: '4px',
        padding: '2px 8px',
        borderRadius: 4,
        cursor: 'pointer',
        opacity: isDisabled ? 0.5 : 1,
        transition: 'all 0.2s',
      }}
      onClick={() => onClick?.(model)}
    >
      <Space size={4}>
        <span style={{ fontSize: 12 }}>{getModelTypeIcon(modelType)}</span>
        <span>{displayName}</span>
        {isDisabled && (
          <Tooltip title={t('state.disabled', { ns: 'common' })}>
            <span style={{ fontSize: 10, color: '#999' }}>⏸</span>
          </Tooltip>
        )}
      </Space>
    </Tag>
  );
}
