import { Avatar, theme } from 'antd';
import {
  OpenAI,
  Anthropic,
  Gemini,
  DeepSeek,
  Zhipu,
  Qwen,
  Tencent,
  Volcengine,
  Moonshot,
  Minimax,
  IFlyTekCloud,
  Baidu,
} from '@lobehub/icons';
import type { FC } from 'react';

/** providerId 到 LobeHub 图标组件的映射（Mono 变体） */
const PROVIDER_ICON_MAP: Record<string, FC<{ size?: number | string }>> = {
  openai: OpenAI,
  anthropic: Anthropic,
  gemini: Gemini,
  deepseek: DeepSeek,
  zhipu: Zhipu,
  qwen: Qwen,
  tencent: Tencent,
  volcengine: Volcengine,
  moonshot: Moonshot,
  minimax: Minimax,
  xunfei: IFlyTekCloud,
  wenxin: Baidu,
};

export interface ProviderIconProps {
  /** 后端 providerId，用于匹配图标 */
  providerId: string;
  /** 容器尺寸（默认 24） */
  size?: number;
  /** 图标尺寸（默认与 size 一致） */
  iconSize?: number;
  /** 自定义样式 */
  style?: React.CSSProperties;
}

/**
 * 根据 providerId 渲染对应的 LobeHub 品牌图标（Mono 变体）。
 * 自动适配亮色/暗色主题。未匹配时显示首字母 Avatar。
 */
export const ProviderIcon: FC<ProviderIconProps> = ({
  providerId,
  iconSize,
  size = 24,
  style,
}) => {
  const { token } = theme.useToken();
  const resolvedIconSize = iconSize ?? size;
  const IconComponent = PROVIDER_ICON_MAP[providerId?.toLowerCase()];

  if (IconComponent) {
    return (
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: size,
          height: size,
          borderRadius: '50%',
          backgroundColor: token.colorFillQuaternary,
          color: token.colorText,
          overflow: 'hidden',
          ...style,
        }}
      >
        <IconComponent size={resolvedIconSize} />
      </span>
    );
  }

  return (
    <Avatar size={size} style={{ backgroundColor: token.colorFillQuaternary, color: token.colorText, ...style }}>
      {providerId?.charAt(0)?.toUpperCase() || '?'}
    </Avatar>
  );
};