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
// extractErrorMessage 工具单元测试
// 目标：把任意 unknown 错误对象抽取成可向用户展示的中文 / 后端返回原因字符串。
// 区分 AntD Form.validateFields 抛出的 { errorFields } 形态——返回空串，调用方据此跳过 toast。
import { describe, it, expect } from 'vitest';
import { extractErrorMessage } from '../errorMessage';

describe('extractErrorMessage', () => {
  it('AntD validateFields 失败时（带 errorFields 数组）应返回空字符串', () => {
    // AntD 校验失败时抛出形如 { errorFields: [{ name, errors }], outOfDate, values } 的对象
    const formError = {
      errorFields: [{ name: ['endpointUrl'], errors: ['请输入'] }],
      outOfDate: false,
      values: {},
    };
    expect(extractErrorMessage(formError)).toBe('');
  });

  it('AxiosError 形态（含 response.data.message）应返回后端 message', () => {
    const axiosErr = {
      isAxiosError: true,
      message: 'Request failed with status code 500',
      response: {
        status: 500,
        statusText: 'Internal Server Error',
        data: { message: '上游连接超时' },
      },
    };
    expect(extractErrorMessage(axiosErr)).toBe('上游连接超时');
  });

  it('AxiosError 仅含 response.data.error（字符串）也应抽取', () => {
    const axiosErr = {
      isAxiosError: true,
      message: 'Network Error',
      response: { status: 502, data: { error: 'bad gateway' } },
    };
    expect(extractErrorMessage(axiosErr)).toBe('bad gateway');
  });

  it('AxiosError 含 ApiResponse 嵌套 error.message（HTTP 400 业务错误）应返回后端 message', () => {
    // 后端非 2xx 由 GlobalExceptionHandler 包装为 { success:false, error:{ code, message } }，
    // 不经过响应拦截器解包，需从 response.data.error.message 提取
    const axiosErr = {
      isAxiosError: true,
      message: 'Request failed with status code 400',
      response: {
        status: 400,
        statusText: 'Bad Request',
        data: {
          success: false,
          error: { code: 'BAD_REQUEST', message: '清理天数必须大于 0' },
        },
      },
    };
    expect(extractErrorMessage(axiosErr)).toBe('清理天数必须大于 0');
  });

  it('AxiosError 嵌套 error 对象无 message 时回退到 statusText', () => {
    const axiosErr = {
      isAxiosError: true,
      message: 'Request failed with status code 400',
      response: {
        status: 400,
        statusText: 'Bad Request',
        data: { success: false, error: { code: 'BAD_REQUEST' } },
      },
    };
    expect(extractErrorMessage(axiosErr)).toBe('Bad Request');
  });

  it('AxiosError 无 response（网络错误）应回退到 error.message', () => {
    const axiosErr = {
      isAxiosError: true,
      message: 'Network Error',
    };
    expect(extractErrorMessage(axiosErr)).toBe('Network Error');
  });

  it('原生 Error 应返回 message', () => {
    expect(extractErrorMessage(new Error('boom'))).toBe('boom');
  });

  it('字符串异常应原样返回', () => {
    expect(extractErrorMessage('plain string')).toBe('plain string');
  });

  it('null / undefined 应返回通用兜底文案', () => {
    expect(extractErrorMessage(null)).toBe('未知错误');
    expect(extractErrorMessage(undefined)).toBe('未知错误');
  });

  it('未知形态应返回通用兜底文案', () => {
    expect(extractErrorMessage({ foo: 'bar' })).toBe('未知错误');
  });
});
