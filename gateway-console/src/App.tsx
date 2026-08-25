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
import { RouterProvider } from 'react-router-dom';
import { App as AntApp } from 'antd';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { router } from '@/router';
import {
  useServiceUnavailable,
  DevServiceUnavailableModal,
  ProductionServiceUnavailablePage,
} from '@/components/common/ServiceUnavailable';
import { setForbiddenHandler } from '@/services/api/client';
import '@/i18n';

// 环境判断
const isDev = import.meta.env.DEV;

/**
 * 服务不可用处理组件
 */
function ServiceUnavailableHandler() {
  const { visible, endpoint, error } = useServiceUnavailable();

  if (!visible) {
    return null;
  }

  if (isDev) {
    return (
      <DevServiceUnavailableModal
        visible={visible}
        endpoint={endpoint}
        error={error}
      />
    );
  }

  return <ProductionServiceUnavailablePage />;
}

/**
 * 403 无权限全局通知组件
 *
 * <p>注册 axios 拦截器的 403 处理器（携带 Ant App context 的 message），
 * 权限不足时统一提示，而不是静默失败。</p>
 */
function ForbiddenNotifier() {
  const { message } = AntApp.useApp();
  const { t } = useTranslation('common');

  useEffect(() => {
    setForbiddenHandler(() => {
      message.error(t('forbidden'));
    });
    return () => setForbiddenHandler(null);
  }, [message, t]);

  return null;
}

/**
 * 应用根组件
 * 配置路由提供者和国际化
 * 使用 Ant Design 的 App 组件包裹以支持 useApp Hook
 */
function App() {
  return (
    <AntApp>
      <ForbiddenNotifier />
      <RouterProvider router={router} />
      <ServiceUnavailableHandler />
    </AntApp>
  );
}

export default App;
