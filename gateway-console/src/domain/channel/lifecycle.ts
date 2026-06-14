/**
 * 渠道五态生命周期 SSOT (Single Source of Truth)
 *
 * <p>本模块是渠道状态语义、视觉、转换规则的**唯一**来源，替换原 `STATE_CONFIG`
 * （ChannelStateTag 私有常量）与 `STATE_TRANSITION_LABELS`（stateTransitions 私有常量）。
 * 所有状态相关 UI（Tag / Tooltip / 卡片视觉 / 转换菜单 / 健康指示）必须从此读取，
 * 禁止重新定义同名映射，避免 SSOT 漂移。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>label / descriptionKey 均为 i18n key，由调用方注入 t()</li>
 *   <li>color 为十六进制色，用于卡片左边框 / 健康点等需要精确色值的场景</li>
 *   <li>tagColor 为 antd Tag 语义色，用于 Tag 组件 props.color</li>
 *   <li>isRoutable / isBilling 是业务标志，DEPRECATED 仍参与流量与计费</li>
 *   <li>nextStates 与后端 `Channel.State.canTransitionTo()` 保持镜像一致</li>
 * </ul>
 *
 * 决策来源：docs/superpowers/specs/2026-06-13-channel-ux-overhaul-design.md §1。
 */

/** 渠道状态枚举（与后端 Channel.State 一致） */
export type ChannelState =
  | 'PENDING'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'DEPRECATED'
  | 'RETIRED';

/** 卡片视觉风格枚举：normal=正常 / muted=低饱和 / strikethrough=删除线 */
export type LifecycleVisualStyle = 'normal' | 'muted' | 'strikethrough';

/** 单条状态元数据 */
export interface LifecycleMeta {
  /** i18n key，如 channel.state.active */
  label: string;
  /** Tooltip 主文案 i18n key，如 channel.state.activeDesc */
  descriptionKey: string;
  /** 用于卡片左边框、健康点等十六进制色 */
  color: string;
  /** antd Tag 语义色（success / warning / error / default） */
  tagColor: string;
  /** 是否参与流量分配 */
  isRoutable: boolean;
  /** 是否计费 */
  isBilling: boolean;
  /** 后续可转换至的状态（不含自身） */
  nextStates: readonly ChannelState[];
  /** 卡片视觉风格 */
  visualStyle: LifecycleVisualStyle;
}

/**
 * 渠道生命周期五态完整元数据。
 *
 * <p>关键决策：</p>
 * <ul>
 *   <li>PENDING.color = #d48806 —— 比原 #faad14 加深，与 #ffffff 背景对比度 ≥ 4.5:1（WCAG AA）</li>
 *   <li>DEPRECATED.isRoutable = true —— 仍参与流量分配，仅标记不推荐</li>
 *   <li>RETIRED.visualStyle = 'strikethrough' —— 卡片渠道名加删除线，禁用整卡 opacity 降透</li>
 *   <li>RETIRED.nextStates = [] —— 终态，不可再转换</li>
 * </ul>
 */
export const CHANNEL_LIFECYCLE: Record<ChannelState, LifecycleMeta> = {
  PENDING: {
    label: 'channel.state.pending',
    descriptionKey: 'channel.state.pendingDesc',
    color: '#d48806',
    tagColor: 'warning',
    isRoutable: false,
    isBilling: false,
    nextStates: ['ACTIVE'],
    visualStyle: 'normal',
  },
  ACTIVE: {
    label: 'channel.state.active',
    descriptionKey: 'channel.state.activeDesc',
    color: '#52c41a',
    tagColor: 'success',
    isRoutable: true,
    isBilling: true,
    nextStates: ['SUSPENDED', 'DEPRECATED'],
    visualStyle: 'normal',
  },
  SUSPENDED: {
    label: 'channel.state.suspended',
    descriptionKey: 'channel.state.suspendedDesc',
    color: '#bfbfbf',
    tagColor: 'default',
    isRoutable: false,
    isBilling: false,
    nextStates: ['ACTIVE', 'DEPRECATED', 'RETIRED'],
    visualStyle: 'muted',
  },
  DEPRECATED: {
    label: 'channel.state.deprecated',
    descriptionKey: 'channel.state.deprecatedDesc',
    color: '#fa8c16',
    tagColor: 'warning',
    isRoutable: true,
    isBilling: true,
    nextStates: ['RETIRED'],
    visualStyle: 'normal',
  },
  RETIRED: {
    label: 'channel.state.retired',
    descriptionKey: 'channel.state.retiredDesc',
    color: '#ff4d4f',
    tagColor: 'error',
    isRoutable: false,
    isBilling: false,
    nextStates: [],
    visualStyle: 'strikethrough',
  },
};

/**
 * 状态是否参与流量分配。
 * @param s 渠道状态
 */
export const isRoutable = (s: ChannelState): boolean =>
  CHANNEL_LIFECYCLE[s].isRoutable;

/**
 * 状态是否参与计费。
 * @param s 渠道状态
 */
export const isBilling = (s: ChannelState): boolean =>
  CHANNEL_LIFECYCLE[s].isBilling;

/**
 * 当前状态可后继转换的状态列表（不含自身）。
 * @param s 当前状态
 */
export const allowedTransitions = (s: ChannelState): readonly ChannelState[] =>
  CHANNEL_LIFECYCLE[s].nextStates;

/**
 * 校验从 from 到 to 的转换是否合法。
 * @param from 源状态
 * @param to 目标状态
 */
export const canTransitionTo = (from: ChannelState, to: ChannelState): boolean =>
  CHANNEL_LIFECYCLE[from].nextStates.includes(to);

/**
 * 构建状态 Tooltip 文案。
 *
 * <p>由调用方注入 i18n t() 函数；输出多行文本，包含：</p>
 * <ol>
 *   <li>状态描述（descriptionKey 翻译）</li>
 *   <li>是否参与流量（tooltipRoutable）</li>
 *   <li>是否计费（tooltipBilling）</li>
 *   <li>可后继状态列表（tooltipNext）或终态文案（tooltipTerminal）</li>
 * </ol>
 *
 * @param state 渠道状态
 * @param t i18n 翻译函数
 * @returns 多行 tooltip 文本（用 \n 分隔）
 */
export function buildStateTooltip(
  state: ChannelState,
  t: (key: string) => string,
): string {
  const meta = CHANNEL_LIFECYCLE[state];
  const lines: string[] = [
    t(meta.descriptionKey),
    `${t('channel.state.tooltipRoutable')}: ${meta.isRoutable ? t('common.yes') : t('common.no')}`,
    `${t('channel.state.tooltipBilling')}: ${meta.isBilling ? t('common.yes') : t('common.no')}`,
  ];
  if (meta.nextStates.length > 0) {
    const nextLabels = meta.nextStates
      .map((s) => t(CHANNEL_LIFECYCLE[s].label))
      .join(' / ');
    lines.push(`${t('channel.state.tooltipNext')}: ${nextLabels}`);
  } else {
    lines.push(t('channel.state.tooltipTerminal'));
  }
  return lines.join('\n');
}
