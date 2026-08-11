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
import { Modal, Button, Typography, Space, Alert, theme } from 'antd';
import { WarningOutlined, ReloadOutlined, BugOutlined } from '@ant-design/icons';
import { useState, useEffect, useCallback } from 'react';

const { Text, Paragraph } = Typography;

interface ServiceUnavailableModalProps {
  visible: boolean;
  endpoint?: string;
  error?: string;
  onClose?: () => void;
}

/**
 * 开发环境服务不可用弹窗
 * 显示详细错误信息和启动命令
 */
export function DevServiceUnavailableModal({
  visible,
  endpoint,
  error,
}: ServiceUnavailableModalProps) {
  const { token } = theme.useToken();
  const handleRetry = useCallback(() => {
    window.location.reload();
  }, []);

  return (
    <Modal
      open={visible}
      closable={true}
      footer={null}
      width={520}
      centered
      maskClosable={false}
    >
      <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
        <Alert
          message="后端服务未启动"
          description="无法连接到后端服务，请检查服务状态"
          type="error"
          icon={<WarningOutlined />}
          showIcon
        />

        <div>
          <Text strong>请检查：</Text>
          <ul style={{ marginTop: 8, paddingLeft: 20 }}>
            <li>后端服务是否已启动</li>
            <li>端口是否正确（默认 8080）</li>
            <li>网络连接是否正常</li>
          </ul>
        </div>

        <div>
          <Text strong>启动命令：</Text>
          <Paragraph
            copyable
            style={{
              background: token.colorFillAlter,
              padding: '8px 12px',
              borderRadius: 4,
              marginTop: 8,
              marginBottom: 0,
            }}
          >
            <BugOutlined style={{ marginRight: 8 }} />
            ./mvnw spring-boot:run -pl gateway-boot
          </Paragraph>
        </div>

        {endpoint && (
          <div>
            <Text type="secondary">请求端点：</Text>
            <Text code>{endpoint}</Text>
          </div>
        )}

        {error && (
          <div>
            <Text type="secondary">错误信息：</Text>
            <Paragraph
              type="secondary"
              style={{
                fontSize: 12,
                background: token.colorErrorBg,
                padding: 8,
                borderRadius: 4,
                marginBottom: 0,
              }}
            >
              {error}
            </Paragraph>
          </div>
        )}

        <Button type="primary" icon={<ReloadOutlined />} onClick={handleRetry} block>
          重试
        </Button>
      </Space>
    </Modal>
  );
}

interface ProductionErrorPageProps {
  onRetry?: () => void;
}

/**
 * 生产环境服务不可用页面
 * 显示友好提示并自动重试
 */
export function ProductionServiceUnavailablePage({ onRetry }: ProductionErrorPageProps) {
  const { token } = theme.useToken();
  const [retryCount, setRetryCount] = useState(0);
  const [isRetrying, setIsRetrying] = useState(false);
  const maxRetries = 3;
  const retryInterval = 3000; // 3秒

  const handleRetry = useCallback(() => {
    setIsRetrying(true);
    setRetryCount((prev) => prev + 1);
    setTimeout(() => {
      if (onRetry) {
        onRetry();
      } else {
        window.location.reload();
      }
      setIsRetrying(false);
    }, 1000);
  }, [onRetry]);

  useEffect(() => {
    // 自动重试逻辑
    if (retryCount > 0 && retryCount < maxRetries) {
      const timer = setTimeout(() => {
        handleRetry();
      }, retryInterval);
      return () => clearTimeout(timer);
    }
  }, [retryCount, handleRetry]);

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        background: token.colorBgLayout,
        padding: 24,
      }}
    >
      <div
        style={{
          background: token.colorBgContainer,
          borderRadius: 8,
          padding: 48,
          textAlign: 'center',
          maxWidth: 400,
          boxShadow: token.boxShadow,
        }}
      >
        <div style={{ fontSize: 64, marginBottom: 24 }}>🔧</div>
        <Text
          style={{
            display: 'block',
            fontSize: 20,
            fontWeight: 600,
            marginBottom: 12,
          }}
        >
          系统维护中
        </Text>
        <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
          服务暂时不可用，请稍后再试
        </Text>

        {retryCount > 0 && retryCount <= maxRetries && (
          <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            正在自动重试 ({retryCount}/{maxRetries})...
          </Text>
        )}

        <Button
          type="primary"
          icon={<ReloadOutlined />}
          onClick={handleRetry}
          loading={isRetrying}
          disabled={retryCount >= maxRetries}
        >
          {retryCount >= maxRetries ? '已达最大重试次数' : '刷新页面'}
        </Button>
      </div>
    </div>
  );
}

/**
 * 全局服务不可用状态管理
 */
let globalServiceUnavailable = false;
let listeners: Array<(visible: boolean, endpoint?: string, error?: string) => void> = [];

export function setServiceUnavailable(visible: boolean, endpoint?: string, error?: string) {
  globalServiceUnavailable = visible;
  listeners.forEach((listener) => listener(visible, endpoint, error));
}

export function useServiceUnavailable() {
  const [state, setState] = useState<{
    visible: boolean;
    endpoint?: string;
    error?: string;
  }>({ visible: globalServiceUnavailable });

  useEffect(() => {
    const listener = (visible: boolean, endpoint?: string, error?: string) => {
      setState({ visible, endpoint, error });
    };
    listeners.push(listener);
    return () => {
      listeners = listeners.filter((l) => l !== listener);
    };
  }, []);

  return state;
}
