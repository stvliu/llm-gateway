/**
 * 错误信息抽取工具。
 *
 * 把任意 unknown 错误对象抽取成可向用户展示的字符串：
 * - AntD `Form.validateFields()` 抛出的对象（含 `errorFields` 数组）→ 返回空字符串。
 *   调用方据此跳过 toast，因为 AntD 表单已就地展示行内错误。
 * - AxiosError → 优先取 `response.data.message` / `response.data.error`，再回退到 `error.message`。
 * - 原生 Error → 返回 `message`。
 * - string → 原样返回。
 * - 其它 → 返回通用中文兜底"未知错误"。
 */
export function extractErrorMessage(err: unknown): string {
  if (err == null) {
    return '未知错误';
  }

  if (typeof err === 'string') {
    return err;
  }

  if (typeof err === 'object') {
    // AntD Form.validateFields() 校验失败：表单已就地显示 error，无需 toast
    if ('errorFields' in err && Array.isArray((err as { errorFields: unknown }).errorFields)) {
      return '';
    }

    // 类 AxiosError：通过 isAxiosError 标志位识别（避免硬依赖 axios 实例方法）
    const maybeAxios = err as {
      isAxiosError?: boolean;
      response?: { data?: unknown; statusText?: string };
      message?: string;
    };
    if (maybeAxios.isAxiosError) {
      const data = maybeAxios.response?.data;
      if (data && typeof data === 'object') {
        const dataObj = data as { message?: unknown; error?: unknown };
        if (typeof dataObj.message === 'string' && dataObj.message.trim()) {
          return dataObj.message;
        }
        if (typeof dataObj.error === 'string' && dataObj.error.trim()) {
          return dataObj.error;
        }
      }
      if (typeof data === 'string' && data.trim()) {
        return data;
      }
      if (maybeAxios.response?.statusText) {
        return maybeAxios.response.statusText;
      }
      if (typeof maybeAxios.message === 'string' && maybeAxios.message.trim()) {
        return maybeAxios.message;
      }
    }

    // 原生 Error
    if (err instanceof Error) {
      return err.message || '未知错误';
    }

    // 兜底：对象但无可识别字段
    if (typeof maybeAxios.message === 'string' && maybeAxios.message.trim()) {
      return maybeAxios.message;
    }
  }

  return '未知错误';
}

const ERROR_CODE_I18N_MAP: Record<string, string> = {
  CHANNEL_NOT_FOUND: 'channel.error.CHANNEL_NOT_FOUND',
  CHANNEL_NAME_DUPLICATE: 'channel.error.CHANNEL_NAME_DUPLICATE',
  INVALID_STATE_TRANSITION: 'channel.error.INVALID_STATE_TRANSITION',
  CHANNEL_NO_ENDPOINT: 'channel.error.CHANNEL_NO_ENDPOINT',
  CHANNEL_NO_CREDENTIAL: 'channel.error.CHANNEL_NO_CREDENTIAL',
  CHANNEL_NO_MODEL_INSTANCE: 'channel.error.CHANNEL_NO_MODEL_INSTANCE',
};

/**
 * 从 AxiosError 响应中提取后端错误码
 */
export function extractErrorCode(err: unknown): string | null {
  const maybeAxios = err as {
    isAxiosError?: boolean;
    response?: { data?: { error?: { code?: string } } };
  };
  if (maybeAxios.isAxiosError && maybeAxios.response?.data) {
    const data = maybeAxios.response.data as { error?: { code?: string } };
    if (data.error?.code) return data.error.code;
  }
  return null;
}

/**
 * 使用 i18n 提取后端错误码对应的用户友好提示
 * 无映射时回退到 extractErrorMessage
 */
export function extractErrorMessageI18n(
  err: unknown,
  t: (key: string, defaultValue?: string) => string,
): string {
  const code = extractErrorCode(err);
  if (code && ERROR_CODE_I18N_MAP[code]) {
    return t(ERROR_CODE_I18N_MAP[code]);
  }
  return extractErrorMessage(err);
}
