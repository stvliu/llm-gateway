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

/** providerId 到 LobeHub 图标组件的映射 */
const PROVIDER_ICON_MAP: Record<string, FC<{ size?: number }>> = {
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
  /** 图标尺寸（默认 32） */
  iconSize?: number;
}

/**
 * 根据 providerId 渲染对应的 LobeHub 品牌图标。
 * 未匹配时显示 providerId 首字母的默认 Avatar。
 */
export const ProviderIcon: FC<ProviderIconProps> = ({
  providerId,
  iconSize = 32,
  ...avatarProps
}) => {
  const IconComponent = PROVIDER_ICON_MAP[providerId];

  if (IconComponent) {
    return (
      <Avatar
        {...avatarProps}
        src={<IconComponent size={iconSize} />}
      />
    );
  }

  // 降级：首字母 Avatar
  return (
    <Avatar {...avatarProps}>
      {providerId?.charAt(0)?.toUpperCase() || '?'}
    </Avatar>
  );
};