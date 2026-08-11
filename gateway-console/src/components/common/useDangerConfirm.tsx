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
import { useCallback } from 'react';
import { Modal } from 'antd';
import { useTranslation } from 'react-i18next';

/**
 * useDangerConfirm 配置项。
 *
 * @property titleKey         i18n key（默认 channels namespace），用于 Modal 标题
 * @property descriptionKey   i18n key，用于 Modal 内容描述（应明确描述操作影响）
 * @property descriptionParams 描述插值变量（如 keyMasked、baseUrl、modelId、name）
 * @property onOk             点击确认时的回调，可返回 Promise；reject 将阻止 modal 关闭
 * @property okTextKey        OK 按钮 i18n key，缺省取 common.actions.delete
 * @property cancelTextKey    Cancel 按钮 i18n key，缺省取 common.actions.cancel
 * @property namespace        i18n namespace 覆盖（默认 'channels'）
 */
export interface DangerConfirmOptions {
  titleKey: string;
  descriptionKey: string;
  descriptionParams?: Record<string, unknown>;
  onOk: () => void | Promise<void>;
  okTextKey?: string;
  cancelTextKey?: string;
  namespace?: string;
}

/**
 * 危险操作确认 Hook。
 *
 * <p>基于 antd <code>Modal.useModal()</code> 的 contextHolder 模式，使 Modal 能复用
 * 调用方组件的 React 上下文（i18n / Theme / QueryClient）。调用方必须把
 * <code>contextHolder</code> 渲染到组件树，否则 modal 不会出现。</p>
 *
 * <h3>与 useSavePulse onError 的去重约定（重要）</h3>
 * <p>当 onOk 内部调用 React Query 的 <code>mutateAsync</code>，且该 mutation 已通过
 * useSavePulse 在 onError 钩子里弹 message.error 提示时，本 Hook 在 onOk 抛错时
 * <strong>不再</strong>额外调 message.error，避免双弹 toast。但仍 throw 错误，让 antd
 * 阻止 modal 关闭，便于用户重试或手动取消。</p>
 *
 * <p>当 onOk 没有任何错误反馈通道（例如直接 <code>fetch</code> 而无 onError 钩子），
 * 调用方应自行在 onOk 内部 catch 中调 message.error 兜底，再 throw 出来。本 Hook 只
 * 负责"阻止 modal 关闭"这一职责。</p>
 *
 * @returns confirm 触发器 + contextHolder 节点（必须渲染到组件树）
 */
export function useDangerConfirm() {
  const { t } = useTranslation('channels');
  const [modal, contextHolder] = Modal.useModal();

  /**
   * 弹出危险操作确认 Modal。
   *
   * <p>onOk reject 时本函数会重新 throw 异常，让 antd 内部停止 modal 关闭流程；
   * 错误的 toast 反馈由 onOk 链内部的 useSavePulse / 调用方负责，避免重复。</p>
   */
  const confirm = useCallback(
    (opts: DangerConfirmOptions) => {
      const ns = opts.namespace ?? 'channels';
      modal.confirm({
        title: t(opts.titleKey, { ns }),
        content: t(opts.descriptionKey, {
          ns,
          ...opts.descriptionParams,
        } as Record<string, unknown>),
        okType: 'danger',
        okText: t(opts.okTextKey ?? 'actions.delete', { ns: 'common' }),
        cancelText: t(opts.cancelTextKey ?? 'actions.cancel', { ns: 'common' }),
        onOk: async () => {
          try {
            await opts.onOk();
          } catch (err) {
            // 必须 throw 以阻止 antd modal 自动关闭。
            // 错误 toast 由 onOk 链内部（如 useSavePulse onError 或调用方 catch）负责。
            throw err;
          }
        },
      });
    },
    [modal, t]
  );

  return { confirm, contextHolder };
}
