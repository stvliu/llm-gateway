import { useState } from 'react';
import { Space, Button, Typography, message, Tooltip } from 'antd';
import { EyeOutlined, EyeInvisibleOutlined, CopyOutlined, EditOutlined } from '@ant-design/icons';

const { Text } = Typography;

export interface MaskedKeyDisplayProps {
  keyMasked: string;
  keyPlain?: string;
  mode?: 'editable' | 'readonly';
  onEdit?: () => void;
  showCopy?: boolean;
  size?: 'small' | 'default';
  onFetchPlain?: () => Promise<string | undefined>;
}

export const MaskedKeyDisplay: React.FC<MaskedKeyDisplayProps> = ({
  keyMasked,
  keyPlain,
  mode = 'readonly',
  onEdit,
  showCopy = true,
  size = 'default',
  onFetchPlain,
}) => {
  const [visible, setVisible] = useState(false);
  const [plain, setPlain] = useState<string | undefined>(keyPlain);
  const [loading, setLoading] = useState(false);

  const displayText = visible && plain ? plain : keyMasked;

  const handleToggleVisibility = async () => {
    if (!visible && !plain && onFetchPlain) {
      setLoading(true);
      try {
        const fetched = await onFetchPlain();
        if (fetched) {
          setPlain(fetched);
          setVisible(true);
        }
      } finally {
        setLoading(false);
      }
    } else {
      setVisible(!visible);
    }
  };

  const handleCopy = async () => {
    let textToCopy = plain || keyMasked;
    if (!plain && onFetchPlain) {
      setLoading(true);
      try {
        const fetched = await onFetchPlain();
        if (fetched) {
          setPlain(fetched);
          textToCopy = fetched;
        }
      } finally {
        setLoading(false);
      }
    }
    await navigator.clipboard.writeText(textToCopy);
    message.success('已复制到剪贴板');
  };

  const iconSize = size === 'small' ? 12 : 14;
  const buttonSize = size === 'small' ? 'small' : 'middle';

  return (
    <Space size={4}>
      <Text code style={{ fontSize: size === 'small' ? 12 : 13 }}>
        {displayText}
      </Text>
      <Tooltip title={visible ? '隐藏' : '显示'}>
        <Button
          type="text"
          size={buttonSize}
          icon={visible ? <EyeInvisibleOutlined style={{ fontSize: iconSize }} /> : <EyeOutlined style={{ fontSize: iconSize }} />}
          onClick={handleToggleVisibility}
          loading={loading}
          style={{ padding: '0 4px' }}
        />
      </Tooltip>
      {showCopy && (
        <Tooltip title="复制">
          <Button
            type="text"
            size={buttonSize}
            icon={<CopyOutlined style={{ fontSize: iconSize }} />}
            onClick={handleCopy}
            style={{ padding: '0 4px' }}
          />
        </Tooltip>
      )}
      {mode === 'editable' && onEdit && (
        <Tooltip title="编辑">
          <Button
            type="text"
            size={buttonSize}
            icon={<EditOutlined style={{ fontSize: iconSize }} />}
            onClick={onEdit}
            style={{ padding: '0 4px' }}
          />
        </Tooltip>
      )}
    </Space>
  );
};

export default MaskedKeyDisplay;