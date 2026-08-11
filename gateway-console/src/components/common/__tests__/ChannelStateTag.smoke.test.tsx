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
// 注意：原 plan 假设 ChannelStateTag 通过命名导出且 props=state="ACTIVE" 渲染英文 "active" 文案；
// 实际组件为默认导出（src/components/common/ChannelStateTag.tsx），ACTIVE 状态渲染中文 "运行中"。
// 这里按真实 API 调整 import 与断言文案，但保持"渲染并验证文本可见"的 smoke 本质。
//
// 任务 6.3 改造后：组件文案从 SSOT + i18n 派生，需要 import @/i18n 触发 init 副作用，
// 让 useTranslation('channels') 能解析到 channel.state.active = "运行中"。
import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeAll } from 'vitest';
import i18n from '@/i18n';
import ChannelStateTag from '@/components/common/ChannelStateTag';

describe('ChannelStateTag smoke', () => {
  // 锁定语言为 zh-CN，避免环境检测器（localStorage / navigator）影响断言
  beforeAll(async () => {
    await i18n.changeLanguage('zh-CN');
  });

  // 验证 vitest + RTL + jsdom + jest-dom 匹配器整条链路可用
  it('应渲染 ACTIVE 状态对应的中文文案', () => {
    render(<ChannelStateTag state="ACTIVE" />);
    expect(screen.getByText('运行中')).toBeInTheDocument();
  });
});
