/** 渠道模型关联状态 */
export type ChannelModelState = 'ACTIVE' | 'INACTIVE';

/** 渠道模型关联（与后端 ChannelModelResponse 一致） */
export interface ChannelModel {
  id: number;
  channelId: number;
  modelId: number;
  /** 供应商侧模型名称（如 gpt-4o） */
  modelName?: string;
  /** 模型展示名称 */
  displayName?: string;
  /** 模型系列 */
  modelFamily?: string;
  /** 上游模型名，null 表示与 model.modelName 相同 */
  upstreamModelName?: string | null;
  /** 关联状态 */
  state: ChannelModelState;
}

/** 创建渠道模型关联请求 */
export interface CreateChannelModelRequest {
  modelId: number;
  /** 上游模型名，为空表示与 Model.modelName 相同 */
  upstreamModelName?: string;
}

/** 更新上游模型名请求 */
export interface UpdateUpstreamModelNameRequest {
  upstreamModelName: string | null;
}