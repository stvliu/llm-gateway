import { theme } from 'antd';
import {
  OpenAI,
  Anthropic,
  Google,
  DeepSeek,
  ChatGLM,
  Qwen,
  Hunyuan,
  Doubao,
  Kimi,
  Minimax,
  Spark,
  Wenxin,
} from '@lobehub/icons';
import type { FC } from 'react';

/** 图标变体类型：color=品牌色SVG，mono=单色SVG */
type IconVariant = 'color' | 'mono';

type IconEntry = {
  Mono: FC<{ size?: number | string }>;
  Color?: FC<{ size?: number | string }>;
};

/**
 * providerId 到 LobeHub 图标组件的映射。
 * 优先使用产品品牌图标（ChatGLM、Kimi、Doubao 等）而非公司图标，
 * 因为产品品牌辨识度更高。
 */
const PROVIDER_ICON_ENTRIES: Record<string, IconEntry> = {
  openai:     { Mono: OpenAI },
  anthropic:  { Mono: Anthropic },
  gemini:     { Mono: Google,     Color: Google.BrandColor },
  deepseek:   { Mono: DeepSeek,   Color: DeepSeek.Color },
  zhipu:      { Mono: ChatGLM,    Color: ChatGLM.Color },
  qwen:       { Mono: Qwen,       Color: Qwen.Color },
  tencent:    { Mono: Hunyuan,    Color: Hunyuan.Color },
  volcengine: { Mono: Doubao,     Color: Doubao.Color },
  moonshot:   { Mono: Kimi,       Color: Kimi.Color },
  minimax:    { Mono: Minimax,    Color: Minimax.Color },
  xunfei:     { Mono: Spark,      Color: Spark.Color },
  wenxin:     { Mono: Wenxin,     Color: Wenxin.Color },
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
 * 找不到图标时显示首字母降级。
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