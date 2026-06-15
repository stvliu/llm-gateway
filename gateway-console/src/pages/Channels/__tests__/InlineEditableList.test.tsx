// InlineEditableList 删除回调签名验证
//
// 验证目标：
// 1) 点击"删除"按钮应直接调用 onDelete，不再插入 Popconfirm 二次确认
//    （由调用方注入自己的确认逻辑，例如 useDangerConfirm）
// 2) onDelete 接收到的 item 必须是被删除的那一行的原始数据
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/i18n';
import { InlineEditableList } from '../InlineEditableList';

beforeAll(() => {
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
});

beforeEach(async () => {
  await i18n.changeLanguage('zh-CN');
});

interface SampleItem {
  id: number;
  name: string;
}

const items: SampleItem[] = [
  { id: 1, name: 'first' },
  { id: 2, name: 'second' },
];

function renderList(onDelete: (item: SampleItem) => void) {
  return render(
    <I18nextProvider i18n={i18n}>
      <InlineEditableList
        items={items}
        renderItem={(item) => <span>{item.name}</span>}
        renderEditForm={() => <div />}
        renderAddForm={() => <div />}
        onAdd={() => {}}
        onDelete={onDelete}
        getKey={(item) => item.id}
      />
    </I18nextProvider>
  );
}

describe('InlineEditableList 删除回调', () => {
  it('点击"删除"按钮应直接调用 onDelete（不插入 Popconfirm 二次确认）', async () => {
    const onDelete = vi.fn();
    renderList(onDelete);

    const deleteBtns = screen.getAllByRole('button', { name: /删\s*除/ });
    expect(deleteBtns.length).toBeGreaterThanOrEqual(2);

    const user = userEvent.setup();
    await user.click(deleteBtns[0]);

    // 关键断言：onDelete 应被立即调用，且不弹出"确认/取消"二次确认
    expect(onDelete).toHaveBeenCalledTimes(1);
    expect(onDelete).toHaveBeenCalledWith(items[0]);

    // 进一步确认未出现 antd Popconfirm 的 OK / Cancel 按钮（说明没有二次确认包裹）
    const confirmBtn = screen.queryByRole('button', { name: /^确认$/ });
    expect(confirmBtn).toBeNull();
  });
});
