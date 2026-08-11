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
import { theme } from 'antd';
import {
  // 品牌/产品图标（优先）
  OpenAI,
  Anthropic,
  Gemini,
  DeepSeek,
  ChatGLM,
  Qwen,
  Hunyuan,
  Doubao,
  Kimi,
  Minimax,
  Spark,
  Wenxin,
  Baichuan,
  Stepfun,
  Ai360,
  XAI,
  Mistral,
  Azure,
  Bedrock,
  // 供应商/公司图标（次选）
  Google,
  Volcengine,
  Zhipu,
  Tencent,
} from '@lobehub/icons';
import type { FC } from 'react';

type IconFC = FC<{ size?: number | string }>;

/** 图标变体类型 */
type IconVariant = 'brand-color' | 'brand-mono' | 'provider-color' | 'provider-mono';

type IconEntry = {
  /** 品牌/产品图标（辨识度最高） */
  Brand: IconFC;
  /** 品牌色变体 */
  BrandColor?: IconFC;
  /** 供应商/公司图标（品牌不可用时的次选） */
  Provider?: IconFC;
  /** 供应商色变体 */
  ProviderColor?: IconFC;
};

/**
 * providerId 到图标的映射。
 * 降级链路：品牌色 → 品牌单色 → 供应商色 → 供应商单色 → 字母兜底。
 *
 * 品牌 ≠ 供应商 的条目：
 *   google(Gemini/Google)、zhipu(ChatGLM/Zhipu)、tencent(混元/Tencent)、volcengine(豆包/Volcengine)
 * 品牌 = 供应商 的条目（Provider 可省略，降级自动跳到品牌单色）：
 *   openai、anthropic、deepseek、qwen、moonshot、minimax、xunfei、wenxin、baichuan、stepfun、360zhinao、xai、mistral、azure、aws_bedrock
 */
const PROVIDER_ICON_ENTRIES: Record<string, IconEntry> = {
  openai:      { Brand: OpenAI,    BrandColor: undefined },
  anthropic:   { Brand: Anthropic, BrandColor: undefined },
  google:      { Brand: Gemini,    BrandColor: Gemini.Color,   Provider: Google,     ProviderColor: Google.BrandColor },
  deepseek:    { Brand: DeepSeek,  BrandColor: DeepSeek.Color },
  qwen:        { Brand: Qwen,      BrandColor: Qwen.Color },
  zhipu:       { Brand: ChatGLM,   BrandColor: ChatGLM.Color,  Provider: Zhipu,     ProviderColor: Zhipu.Color },
  tencent:     { Brand: Hunyuan,   BrandColor: Hunyuan.Color,  Provider: Tencent,   ProviderColor: Tencent.Color },
  volcengine:  { Brand: Doubao,    BrandColor: Doubao.Color,   Provider: Volcengine, ProviderColor: Volcengine.Color },
  moonshot:    { Brand: Kimi,      BrandColor: Kimi.Color },
  minimax:     { Brand: Minimax,   BrandColor: Minimax.Color },
  xunfei:      { Brand: Spark,     BrandColor: Spark.Color },
  wenxin:      { Brand: Wenxin,    BrandColor: Wenxin.Color },
  baichuan:    { Brand: Baichuan,  BrandColor: Baichuan.Color },
  stepfun:     { Brand: Stepfun,   BrandColor: Stepfun.Color },
  '360zhinao': { Brand: Ai360,     BrandColor: Ai360.Color },
  xai:         { Brand: XAI,       BrandColor: undefined },
  mistral:     { Brand: Mistral,   BrandColor: Mistral.Color },
  azure:       { Brand: Azure,     BrandColor: Azure.Color },
  aws_bedrock: { Brand: Bedrock,   BrandColor: Bedrock.Color },
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
 * 降级链路：品牌色 → 品牌单色 → 供应商色 → 供应商单色 → 字母兜底。
 * 自动适配亮色/暗色主题。
 */
export const ProviderIcon: FC<ProviderIconProps> = ({
  providerId,
  size = 24,
  style,
  className,
}) => {
  const { token } = theme.useToken();
  const entry = providerId ? PROVIDER_ICON_ENTRIES[providerId.toLowerCase()] : undefined;

  if (entry) {
    let IconComponent: IconFC;
    let variant: IconVariant;

    if (entry.BrandColor) {
      IconComponent = entry.BrandColor;
      variant = 'brand-color';
    } else if (entry.Brand) {
      IconComponent = entry.Brand;
      variant = 'brand-mono';
    } else if (entry.ProviderColor) {
      IconComponent = entry.ProviderColor;
      variant = 'provider-color';
    } else {
      IconComponent = entry.Provider!;
      variant = 'provider-mono';
    }

    const isMono = variant === 'brand-mono' || variant === 'provider-mono';
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

  // 兜底：首字母占位符
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
