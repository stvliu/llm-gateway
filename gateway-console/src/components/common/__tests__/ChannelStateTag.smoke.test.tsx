// 注意：原 plan 假设 ChannelStateTag 通过命名导出且 props=state="ACTIVE" 渲染英文 "active" 文案；
// 实际组件为默认导出（src/components/common/ChannelStateTag.tsx），ACTIVE 状态渲染中文 "运行中"。
// 这里按真实 API 调整 import 与断言文案，但保持"渲染并验证文本可见"的 smoke 本质。
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import ChannelStateTag from '@/components/common/ChannelStateTag';

describe('ChannelStateTag smoke', () => {
  // 验证 vitest + RTL + jsdom + jest-dom 匹配器整条链路可用
  it('应渲染 ACTIVE 状态对应的中文文案', () => {
    render(<ChannelStateTag state="ACTIVE" />);
    expect(screen.getByText('运行中')).toBeInTheDocument();
  });
});
