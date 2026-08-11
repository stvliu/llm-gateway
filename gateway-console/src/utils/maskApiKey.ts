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
/**
 * API Key 脱敏工具函数
 * 规则（脱敏后长度与明文长度一致）：
 * - 长度 <= 3：全部替换为星号
 * - 长度 4~10：保留前 4 位，其余为星号
 * - 长度 > 10：保留前 6 位 + 等长星号 + 保留后 4 位
 */
export function maskApiKey(key: string): string {
  if (!key || key.length === 0) {
    return '';
  }
  const len = key.length;

  if (len <= 3) {
    return '*'.repeat(len);
  }

  if (len < 11) {
    return key.slice(0, 4) + '*'.repeat(len - 4);
  }

  const middleLen = len - 6 - 4;
  return key.slice(0, 6) + '*'.repeat(middleLen) + key.slice(-4);
}
