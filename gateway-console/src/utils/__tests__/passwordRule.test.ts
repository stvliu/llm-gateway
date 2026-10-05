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
import { describe, it, expect } from 'vitest';
import { utf8ByteLength, passwordRules, PASSWORD_MAX_BYTES } from '@/utils/passwordRule';

const t = (key: string) => `[${key}]`;

describe('utf8ByteLength', () => {
  it('ASCII 逐字符计数', () => {
    expect(utf8ByteLength('abc123')).toBe(6);
  });

  it('汉字按 3 字节计', () => {
    expect(utf8ByteLength('字')).toBe(3);
    expect(utf8ByteLength('字'.repeat(24))).toBe(72);
    expect(utf8ByteLength('字'.repeat(25))).toBe(75);
  });

  it('空串为 0', () => {
    expect(utf8ByteLength('')).toBe(0);
  });
});

describe('passwordRules', () => {
  const rules = passwordRules(t, 'validation.passwordSize');

  /** 逐条执行 rules，返回是否整体通过 */
  async function validate(value: string | undefined): Promise<boolean> {
    for (const rule of rules) {
      if (!('validator' in rule) && 'min' in rule) continue;
      if ('required' in rule && (value === undefined || value === '')) return false;
      if ('validator' in rule && rule.validator) {
        try {
          await (rule.validator as (r: unknown, v: string) => Promise<void>)(undefined, value ?? '');
        } catch {
          return false;
        }
      }
    }
    return true;
  }

  it('常量与后端策略一致：字节上限 72', () => {
    expect(PASSWORD_MAX_BYTES).toBe(72);
  });

  it('规则含 required / min-max / 字节校验三段', () => {
    expect(rules).toHaveLength(3);
    expect(rules[1]).toMatchObject({ min: 6, max: 64 });
  });

  it('24 个汉字（72 字节）通过字节校验', async () => {
    await expect(validate('字'.repeat(24))).resolves.toBe(true);
  });

  it('25 个汉字（75 字节）被字节校验拒绝', async () => {
    await expect(validate('字'.repeat(25))).resolves.toBe(false);
  });

  it('空值被 required 拒绝', async () => {
    await expect(validate(undefined)).resolves.toBe(false);
  });
});
