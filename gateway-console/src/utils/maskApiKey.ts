/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
