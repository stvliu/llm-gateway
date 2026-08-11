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
import { useState } from 'react';
import { Space, Button, Typography, message, Tooltip } from 'antd';
import { EyeOutlined, EyeInvisibleOutlined, CopyOutlined, EditOutlined } from '@ant-design/icons';
import { maskApiKey } from '@/utils/maskApiKey';

const { Text } = Typography;

export interface MaskedKeyDisplayProps {
  keyPlain: string;
  mode?: 'editable' | 'readonly';
  onEdit?: () => void;
  showCopy?: boolean;
  size?: 'small' | 'default';
}

export const MaskedKeyDisplay: React.FC<MaskedKeyDisplayProps> = ({
  keyPlain,
  mode = 'readonly',
  onEdit,
  showCopy = true,
  size = 'default',
}) => {
  const [visible, setVisible] = useState(false);

  const displayText = visible ? keyPlain : maskApiKey(keyPlain);

  const handleToggleVisibility = () => {
    setVisible(!visible);
  };

  const handleCopy = async () => {
    const textToCopy = keyPlain;
    try {
      await navigator.clipboard.writeText(textToCopy);
      message.success('已复制到剪贴板');
    } catch {
      const textArea = document.createElement('textarea');
      textArea.value = textToCopy;
      textArea.style.position = 'fixed';
      textArea.style.left = '-9999px';
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand('copy');
        message.success('已复制到剪贴板');
      } catch {
        message.error('复制失败，请手动复制');
      }
      document.body.removeChild(textArea);
    }
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
