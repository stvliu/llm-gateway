import { Avatar, type AvatarProps } from 'antd';
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

export interface ProviderIconProps extends Omit<AvatarProps, 'src'> {
  /** 后端 providerId，用于匹配图标 */
  providerId: string;
  /** 图标尺寸（默认 24） */
  iconSize?: number;
}

/**
 * 根据 providerId 渲染对应的 LobeHub 品牌图标（Mono 变体）。
 * 未匹配时显示 providerId 首字母的默认 Avatar。
 */
export const ProviderIcon: FC<ProviderIconProps> = ({
  providerId,
  iconSize = 24,
  size = iconSize,
  style,
  ...avatarProps
}) => {
  const IconComponent = PROVIDER_ICON_MAP[providerId?.toLowerCase()];

  if (IconComponent) {
    // 直接渲染 SVG 图标，不使用 Avatar 包装
    return (
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: size,
          height: size,
          borderRadius: '50%',
          backgroundColor: '#f5f5f5',
          overflow: 'hidden',
          ...style,
        }}
      >
        <IconComponent size={iconSize} />
      </span>
    );
  }

  // 降级：首字母 Avatar
  return (
    <Avatar size={size} style={style} {...avatarProps}>
      {providerId?.charAt(0)?.toUpperCase() || '?'}
    </Avatar>
  );
};