/** 渠道模型关联状态 */
export type ChannelModelState = 'ACTIVE' | 'INACTIVE';

/** 渠道模型关联（与后端 ChannelModelResponse 一致） */
export interface ChannelModel {
  id: number;
  channelId: number;
  modelSpecId: number;
  /** 供应商侧模型 ID（如 gpt-4o） */
  providerModelId: string;
  /** 模型展示名称 */
  displayName?: string;
  /** 模型系列 */
  modelFamily?: string;
  /** 关联状态 */
  state: ChannelModelState;
}

/** 创建渠道模型关联请求 */
export interface CreateChannelModelRequest {
  modelSpecId: number;
}