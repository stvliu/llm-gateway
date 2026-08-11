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
// 任务 10.1：ProviderForm 受控组件单元测试
//
// 验证：
// 1) 受控组件 value/onChange 双向绑定
// 2) code 必填校验
// 3) expectedProviderCode 一致性校验（不一致时提示中文错误）
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, vi } from 'vitest';
import { App as AntApp } from 'antd';
import i18n from '@/i18n';
import { I18nextProvider } from 'react-i18next';
import { ProviderForm, type ProviderFormValue } from '@/pages/Channels/ProviderForm';

beforeAll(async () => {
  // antd 依赖 matchMedia / ResizeObserver，jsdom 默认不提供
  if (!window.matchMedia) {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: () => {},
        removeListener: () => {},
        addEventListener: () => {},
        removeEventListener: () => {},
        dispatchEvent: () => false,
      }),
    });
  }
  if (!(globalThis as { ResizeObserver?: unknown }).ResizeObserver) {
    class ResizeObserverStub {
      observe(): void {}
      unobserve(): void {}
      disconnect(): void {}
    }
    (globalThis as unknown as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver =
      ResizeObserverStub;
  }
  await i18n.changeLanguage('zh-CN');
});

/** 测试辅助：渲染 ProviderForm 并提供 i18n + antd App 上下文 */
function renderForm(
  value: ProviderFormValue,
  onChange: (next: ProviderFormValue) => void,
  expectedProviderCode?: string,
) {
  return render(
    <I18nextProvider i18n={i18n}>
      <AntApp>
        <ProviderForm
          value={value}
          onChange={onChange}
          expectedProviderCode={expectedProviderCode}
        />
      </AntApp>
    </I18nextProvider>,
  );
}

const emptyValue: ProviderFormValue = {
  code: '',
  name: '',
};

describe('ProviderForm 受控组件', () => {
  it('受控：在 code 输入框中键入应触发 onChange，且外部 value 渲染回字段', async () => {
    const onChange = vi.fn();
    const { rerender } = renderForm(emptyValue, onChange);
    const codeInput = screen.getByLabelText('品牌标识') as HTMLInputElement;
    await userEvent.type(codeInput, 'a');
    expect(onChange).toHaveBeenCalled();
    // 模拟外部受控更新
    rerender(
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <ProviderForm value={{ code: 'foo', name: 'bar' }} onChange={onChange} />
        </AntApp>
      </I18nextProvider>,
    );
    await waitFor(() => {
      expect((screen.getByLabelText('品牌标识') as HTMLInputElement).value).toBe('foo');
      expect((screen.getByLabelText('供应商名称') as HTMLInputElement).value).toBe('bar');
    });
  });

  it('校验：code 为空时显示必填错误', async () => {
    const onChange = vi.fn();
    renderForm(emptyValue, onChange);
    const codeInput = screen.getByLabelText('品牌标识') as HTMLInputElement;
    // 输入再清空，触发 antd onChange 校验
    await userEvent.type(codeInput, 'x');
    await userEvent.clear(codeInput);
    await waitFor(() => {
      expect(screen.queryByText(/请输入品牌标识/)).toBeInTheDocument();
    });
  });

  it('校验：code 与 expectedProviderCode 不一致时报错（中文文案含"代码必须与"）', async () => {
    const onChange = vi.fn();
    renderForm(emptyValue, onChange, 'openai');
    const codeInput = screen.getByLabelText('品牌标识') as HTMLInputElement;
    // 输入与 expected 不一致的 code，触发 antd onChange 校验
    await userEvent.type(codeInput, 'wrong-code');
    await waitFor(() => {
      // 错误文案含中文"代码必须与"提示
      expect(screen.queryByText(/代码必须与/)).toBeInTheDocument();
    });
  });
});
