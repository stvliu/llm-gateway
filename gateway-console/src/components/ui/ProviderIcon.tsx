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

/** 图标组件类型（接受 size 参数） */
type IconComponent = FC<{ size?: number | string }>;

/** providerId 到 LobeHub 图标组件的映射，优先使用 Color 变体（品牌色），无 Color 则用 Mono */
const PROVIDER_ICON_MAP: Record<string, IconComponent> = {
  openai: OpenAI as IconComponent,
  anthropic: Anthropic as IconComponent,
  gemini: Gemini.Color as IconComponent,
  deepseek: DeepSeek.Color as IconComponent,
  zhipu: Zhipu.Color as IconComponent,
  qwen: Qwen.Color as IconComponent,
  tencent: Tencent.Color as IconComponent,
  volcengine: Volcengine.Color as IconComponent,
  moonshot: Moonshot as IconComponent,
  minimax: Minimax.Color as IconComponent,
  xunfei: IFlyTekCloud.Color as IconComponent,
  wenxin: Baidu.Color as IconComponent,
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
 * 根据 providerId 渲染对应的 LobeHub 品牌图标。
 * 优先使用 Color 变体（品牌色），无 Color 则用 Mono。
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