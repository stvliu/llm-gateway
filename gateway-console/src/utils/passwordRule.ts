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
import type { Rule } from 'antd/es/form';

/**
 * 密码策略常量（与后端 UserCreateRequest/ChangePasswordRequest 严格一致）：
 * 长度 6-64 字符，且 UTF-8 编码不超过 72 字节（BCrypt 输入上限，一个汉字占 3 字节）。
 */
export const PASSWORD_MIN = 6;
export const PASSWORD_MAX = 64;
export const PASSWORD_MAX_BYTES = 72;

/**
 * 计算 UTF-8 编码字节数
 *
 * @param value 原始字符串
 * @return UTF-8 编码后的字节数
 */
export function utf8ByteLength(value: string): number {
  return new TextEncoder().encode(value).length;
}

/**
 * 密码字段表单校验规则（创建用户 / 修改密码共用，与后端策略同源）。
 *
 * @param t i18n 翻译函数
 * @param messageKey 长度提示文案的 i18n key（各调用方自带 namespace）
 * @return antd Form rules
 */
export function passwordRules(t: (key: string) => string, messageKey: string): Rule[] {
  return [
    { required: true },
    { min: PASSWORD_MIN, max: PASSWORD_MAX, message: t(messageKey) },
    {
      validator: (_rule: unknown, value: string) =>
        utf8ByteLength(value ?? '') <= PASSWORD_MAX_BYTES
          ? Promise.resolve()
          : Promise.reject(new Error(t(messageKey))),
    },
  ];
}
