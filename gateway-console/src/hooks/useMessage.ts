import { App } from 'antd';

/**
 * 封装 antd message 实例，替代静态方法调用
 */
export function useMessage() {
  const { message } = App.useApp();
  return message;
}
