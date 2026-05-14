import { useState } from 'react';
import { Form, Input, Button, Checkbox, Select, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { authApi } from '@/services/api/auth';
import { useAuthStore } from '@/stores/authStore';
import { isServiceUnavailableError } from '@/services/api/client';
import styles from './style.module.css';

export default function Login() {
  const { t, i18n } = useTranslation('login');
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { setUser, setToken } = useAuthStore();

  const from = (location.state as { from?: string })?.from;

  const handleLanguageChange = (lng: string) => {
    localStorage.setItem('i18nextLng', lng);
    window.location.reload();
  };

  const handleSubmit = async (values: LoginForm) => {
    setLoading(true);
    setError(null);

    try {
      const response = await authApi.login({
        username: values.username,
        password: values.password,
        rememberMe: values.rememberMe,
      });

      setUser(response.user);
      if (response.token) {
        setToken(response.token);
      }

      message.success(t('message.success', { ns: 'common' }));

      navigate(from || '/dashboard', { replace: true });
    } catch (err: unknown) {
      if (isServiceUnavailableError(err)) {
        setError(t('error.serviceUnavailable'));
      } else {
        setError(t('error.message'));
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      {/* 左侧登录面板 */}
      <div className={styles.loginPanel}>
        <div className={styles.card}>
          <h1 className={styles.title}>{t('title')}</h1>
          <p className={styles.subtitle}>{t('subtitle')}</p>

          {error && (
            <div className={styles.error}>
              {t('error.title')}: {error}
            </div>
          )}

          <Form<LoginForm>
            layout="vertical"
            onFinish={handleSubmit}
            initialValues={{ rememberMe: true }}
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: t('validation.usernameRequired') }]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder={t('username')}
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: t('validation.passwordRequired') }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder={t('password')}
              />
            </Form.Item>

            <Form.Item>
              <div className={styles.rememberRow}>
                <Form.Item name="rememberMe" valuePropName="checked" noStyle>
                  <Checkbox>{t('rememberMe')}</Checkbox>
                </Form.Item>
                <Select
                  value={localStorage.getItem('i18nextLng') || 'zh-CN'}
                  onChange={handleLanguageChange}
                  variant="borderless"
                  size="small"
                  options={[
                    { value: 'zh-CN', label: '简体中文' },
                    { value: 'en-US', label: 'English' },
                  ]}
                />
              </div>
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" loading={loading} block>
                {t('submit')}
              </Button>
            </Form.Item>
          </Form>
        </div>

        <div className={styles.footer}>
          © {new Date().getFullYear()} LLM Gateway · <a href="#">帮助文档</a>
        </div>
      </div>

      {/* 右侧欢迎图片 */}
      <div className={styles.welcomeImage} />
    </div>
  );
}
