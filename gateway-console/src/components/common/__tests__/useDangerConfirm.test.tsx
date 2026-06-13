// useDangerConfirm Hook 单元测试
//
// 验证目标：
// 1) confirm() 应弹出 Modal 含 danger okType（OK 按钮带 ant-btn-dangerous className）
// 2) 点击 OK 应调用 onOk
// 3) onOk 异步 reject 时 modal 不关闭（保留在 DOM）
// 4) 必须把 contextHolder 渲染到组件树，否则 modal 不出现
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { I18nextProvider } from 'react-i18next';
import { useEffect } from 'react';
import i18n from '@/i18n';
import { useDangerConfirm } from '../useDangerConfirm';

beforeEach(async () => {
  await i18n.changeLanguage('zh-CN');
});

/**
 * 测试包装器：暴露 confirm 给外部测试使用。
 * 必须把 contextHolder 渲染到 DOM 树。
 */
function HookHarness({
  options,
  exposeConfirm,
}: {
  options: Parameters<ReturnType<typeof useDangerConfirm>['confirm']>[0];
  exposeConfirm?: (fn: () => void) => void;
}) {
  const { confirm, contextHolder } = useDangerConfirm();

  useEffect(() => {
    if (exposeConfirm) {
      exposeConfirm(() => confirm(options));
    }
  }, [exposeConfirm, confirm, options]);

  return <div>{contextHolder}</div>;
}

function renderWithI18n(ui: React.ReactElement) {
  return render(<I18nextProvider i18n={i18n}>{ui}</I18nextProvider>);
}

describe('useDangerConfirm', () => {
  it('confirm() 应弹出 Modal，OK 按钮含 danger 样式', async () => {
    let trigger: () => void = () => {};
    const onOk = vi.fn();

    renderWithI18n(
      <HookHarness
        options={{
          titleKey: 'card.deleteConfirmTitle',
          descriptionKey: 'card.deleteConfirmContent',
          descriptionParams: { name: 'demo' },
          onOk,
        }}
        exposeConfirm={(fn) => {
          trigger = fn;
        }}
      />
    );

    act(() => {
      trigger();
    });

    // Modal 标题出现（中文）—— 使用 findAllByText 兼容标题在多节点出现
    await waitFor(() => {
      expect(screen.getAllByText(/确认删除/).length).toBeGreaterThan(0);
    });

    // OK 按钮带 dangerous 样式
    const okBtns = await screen.findAllByRole('button', { name: /删\s*除/ });
    const dangerOk = okBtns.find((b) => b.className.includes('ant-btn-dangerous'));
    expect(dangerOk).toBeDefined();
  });

  it('点击 OK 应调用 onOk', async () => {
    let trigger: () => void = () => {};
    const onOk = vi.fn(() => Promise.resolve());

    renderWithI18n(
      <HookHarness
        options={{
          titleKey: 'card.deleteConfirmTitle',
          descriptionKey: 'card.deleteConfirmContent',
          descriptionParams: { name: 'demo' },
          onOk,
        }}
        exposeConfirm={(fn) => {
          trigger = fn;
        }}
      />
    );

    act(() => {
      trigger();
    });

    const okBtns = await screen.findAllByRole('button', { name: /删\s*除/ });
    const dangerOk = okBtns.find((b) => b.className.includes('ant-btn-dangerous'));
    expect(dangerOk).toBeDefined();
    const user = userEvent.setup();
    await user.click(dangerOk!);

    await waitFor(() => {
      expect(onOk).toHaveBeenCalledTimes(1);
    });
  });

  it('onOk 异步 reject 时 modal 不关闭', async () => {
    let trigger: () => void = () => {};
    // 抑制 antd modal.confirm 在 onOk reject 后产生的 unhandledrejection 噪音
    // jsdom + Node 环境同时挂 window 与 process 钩子，确保两条路径都被吃掉
    const winHandler = (e: PromiseRejectionEvent) => {
      if (e.reason instanceof Error && e.reason.message === 'boom') {
        e.preventDefault();
      }
    };
    const procHandler = (reason: unknown) => {
      if (reason instanceof Error && reason.message === 'boom') {
        // no-op；登记拦截
      }
    };
    window.addEventListener('unhandledrejection', winHandler);
    process.on('unhandledRejection', procHandler);
    const onOk = vi.fn(() => Promise.reject(new Error('boom')));

    renderWithI18n(
      <HookHarness
        options={{
          titleKey: 'card.deleteConfirmTitle',
          descriptionKey: 'card.deleteConfirmContent',
          descriptionParams: { name: 'demo' },
          onOk,
        }}
        exposeConfirm={(fn) => {
          trigger = fn;
        }}
      />
    );

    act(() => {
      trigger();
    });

    const okBtns = await screen.findAllByRole('button', { name: /删\s*除/ });
    const dangerOk = okBtns.find((b) => b.className.includes('ant-btn-dangerous'));
    expect(dangerOk).toBeDefined();
    const user = userEvent.setup();
    await user.click(dangerOk!);

    await waitFor(() => {
      expect(onOk).toHaveBeenCalled();
    });

    // 等待 mutation reject 后，modal 应仍在 DOM 中（标题继续可见）
    await waitFor(() => {
      expect(screen.getAllByText(/确认删除/).length).toBeGreaterThan(0);
    });

    window.removeEventListener('unhandledrejection', winHandler);
    process.off('unhandledRejection', procHandler);
  });
});
