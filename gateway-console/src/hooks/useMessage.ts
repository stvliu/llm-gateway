/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { App } from 'antd';

/**
 * 封装 antd message 实例，替代静态方法调用
 */
export function useMessage() {
  const { message } = App.useApp();
  return message;
}
