import axios, { type AxiosInstance, type AxiosRequestConfig, AxiosError } from 'axios';
import { setServiceUnavailable } from '@/components/common/ServiceUnavailable';

// 环境判断
const isDev = import.meta.env.DEV;

const instance: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * 判断是否为服务不可用错误
 */
function isServiceUnavailableError(error: unknown): boolean {
  // 检查自定义错误码
  if (error instanceof Error) {
    const errorCode = (error as unknown as Record<string, unknown>).code;
    if (errorCode === 'SERVICE_UNAVAILABLE') {
      return true;
    }
  }

  // 检查 AxiosError
  if (error instanceof AxiosError) {
    const status = error.response?.status;
    const code = error.code;

    // 服务不可用的情况
    if (
      status === 0 ||
      status === 503 ||
      status === 502 ||
      status === 504 ||
      code === 'ERR_CONNECTION_REFUSED' ||
      code === 'ERR_NETWORK' ||
      code === 'ECONNABORTED' ||
      code === 'ECONNREFUSED' ||
      !error.response
    ) {
      return true;
    }
  }

  // 检查普通 Error 对象（可能是代理错误）
  if (error instanceof Error) {
    const message = error.message.toLowerCase();
    const name = error.name.toLowerCase();

    // 网络错误关键字
    if (
      message.includes('network error') ||
      message.includes('failed to fetch') ||
      message.includes('connection refused') ||
      message.includes('timeout') ||
      message.includes('service unavailable') ||
      message.includes('backend service unavailable') ||
      name.includes('networkerror') ||
      name.includes('typeerror') // fetch 失败时可能是 TypeError
    ) {
      return true;
    }
  }

  return false;
}

/**
 * 检查响应是否为意外的 HTML 页面（Vite 代理失败时可能返回前端页面）
 */
function isUnexpectedHtmlResponse(response: unknown): boolean {
  if (response && typeof response === 'object') {
    const resp = response as { data?: unknown; status?: number };
    const data = resp.data;
    // 检查响应数据是否为 HTML 页面
    if (typeof data === 'string' && data.includes('<!DOCTYPE html>')) {
      return true;
    }
  }
  return false;
}

/**
 * 获取错误消息
 */
function getErrorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    if (error.code === 'ERR_CONNECTION_REFUSED') {
      return '连接被拒绝，后端服务可能未启动';
    }
    if (error.code === 'ERR_NETWORK') {
      return '网络错误，请检查网络连接';
    }
    if (error.code === 'ECONNABORTED') {
      return '请求超时';
    }
    return error.message || '未知错误';
  }
  return String(error);
}

// 响应拦截器
instance.interceptors.response.use(
  (response) => {
    // 检查是否为意外的 HTML 页面响应（Vite 代理失败时可能返回前端页面）
    if (isUnexpectedHtmlResponse(response)) {
      const endpoint = response.config?.url;
      const errorMessage = 'Backend service unavailable - received HTML instead of API response';

      if (isDev) {
        setServiceUnavailable(true, endpoint, errorMessage);
      } else {
        setServiceUnavailable(true, endpoint, errorMessage);
      }

      // 抛出错误以便调用方处理
      const error = new Error(errorMessage);
      (error as unknown as Record<string, unknown>).code = 'SERVICE_UNAVAILABLE';
      return Promise.reject(error);
    }

    // 自动解包 ApiResponse.data
    const data = response.data;
    if (data && typeof data === 'object' && 'success' in data) {
      if (data.success) {
        // 成功响应，返回 data 字段
        response.data = data.data;
      } else {
        // 业务错误，抛出异常
        const error = new Error(data.error?.message || 'Unknown error');
        (error as unknown as Record<string, unknown>).code = data.error?.code;
        (error as unknown as Record<string, unknown>).details = data.error?.details;
        return Promise.reject(error);
      }
    }
    return response;
  },
  (error: unknown) => {
    // 处理服务不可用
    if (isServiceUnavailableError(error)) {
      const endpoint = (error as AxiosError).config?.url;
      const errorMessage = getErrorMessage(error);

      if (isDev) {
        // 开发环境：触发开发环境弹窗
        setServiceUnavailable(true, endpoint, errorMessage);
      } else {
        // 生产环境：触发生产环境页面
        setServiceUnavailable(true, endpoint, errorMessage);
      }
    }

    // 处理 401 未授权
    if ((error as AxiosError).response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);

export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    instance.get<T>(url, config).then((res) => res.data),

  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    instance.post<T>(url, data, config).then((res) => res.data),

  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    instance.put<T>(url, data, config).then((res) => res.data),

  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    instance.delete<T>(url, config).then((res) => res.data),

  patch: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    instance.patch<T>(url, data, config).then((res) => res.data),
};

/**
 * 判断是否为服务不可用错误（导出供其他模块使用）
 */
export { isServiceUnavailableError };

export default instance;
