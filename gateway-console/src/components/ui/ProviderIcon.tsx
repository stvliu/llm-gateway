import { theme } from 'antd';
import {
  OpenAI,
  Anthropic,
  Google,
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

/** 图标变体类型：color=品牌色SVG，mono=单色SVG */
type IconVariant = 'color' | 'mono';

type IconEntry = {
  /** Mono 变体（始终可用，是 default export） */
  Mono: FC<{ size?: number | string }>;
  /** Color 变体（部分品牌有，品牌色 SVG；部分品牌用 BrandColor 代替） */
  Color?: FC<{ size?: number | string }>;
};

/** providerId 到 LobeHub 图标组件的映射 */
const PROVIDER_ICON_ENTRIES: Record<string, IconEntry> = {
  openai:     { Mono: OpenAI },
  anthropic:  { Mono: Anthropic },
  gemini:     { Mono: Gemini,     Color: Google.BrandColor },
  deepseek:   { Mono: DeepSeek,   Color: DeepSeek.Color },
  zhipu:      { Mono: Zhipu,      Color: Zhipu.Color },
  qwen:       { Mono: Qwen,       Color: Qwen.Color },
  tencent:    { Mono: Tencent,    Color: Tencent.Color },
  volcengine: { Mono: Volcengine, Color: Volcengine.Color },
  moonshot:   { Mono: Moonshot },
  minimax:    { Mono: Minimax,    Color: Minimax.Color },
  xunfei:     { Mono: IFlyTekCloud, Color: IFlyTekCloud.Color },
  wenxin:     { Mono: Baidu,      Color: Baidu.Color },
};

export interface ProviderIconProps {
  /** 后端 providerId，用于匹配图标 */
  providerId?: string;
  /** 尺寸（默认 24） */
  size?: number;
  /** 自定义样式 */
  style?: React.CSSProperties;
  /** 自定义类名 */
  className?: string;
}

/**
 * 根据 providerId 渲染对应的 LobeHub 品牌图标。
 * 有 Color 变体用 Color（品牌色），无 Color 用 Mono（单色）。
 * 找不到图标时显示首字母降级 Avatar。
 * 自动适配亮色/暗色主题。
 */
export const ProviderIcon: FC<ProviderIconProps> = ({
  providerId,
  size = 24,
  style,
  className,
}) => {
  const { token } = theme.useToken();
  const entry = PROVIDER_ICON_ENTRIES[providerId?.toLowerCase()];
  const variant: IconVariant = entry?.Color ? 'color' : 'mono';
  const IconComponent = entry?.Color ?? entry?.Mono;

  if (IconComponent) {
    // Color 变体自带品牌色，不需要背景衬托
    // Mono 变体用 fill="currentColor"，需要背景圆 + 文字色确保可读
    const isMono = variant === 'mono';
    return (
      <span
        className={className}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: isMono ? size : undefined,
          height: isMono ? size : undefined,
          borderRadius: isMono ? '50%' : undefined,
          backgroundColor: isMono ? token.colorFillQuaternary : undefined,
          color: isMono ? token.colorText : undefined,
          overflow: 'hidden',
          ...style,
        }}
      >
        <IconComponent size={size} />
      </span>
    );
  }

  // 降级：首字母圆形容器
  const letter = providerId && providerId.length > 0
    ? providerId.charAt(0).toUpperCase()
    : '?';

  return (
    <span
      className={className}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: size,
        height: size,
        borderRadius: '50%',
        backgroundColor: token.colorFillQuaternary,
        color: token.colorText,
        fontSize: size * 0.45,
        fontWeight: 600,
        lineHeight: 1,
        ...style,
      }}
    >
      {letter}
    </span>
  );
};