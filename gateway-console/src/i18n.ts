import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// 中文
import zhCNCommon from './locales/zh-CN/common.json';
import zhCNLogin from './locales/zh-CN/login.json';
import zhCNModels from './locales/zh-CN/models.json';
import zhCNUsers from './locales/zh-CN/users.json';
import zhCNApiKeys from './locales/zh-CN/apiKeys.json';
import zhCNDashboard from './locales/zh-CN/dashboard.json';
import zhCNProviders from './locales/zh-CN/providers.json';
import zhCNApiKeyPool from './locales/zh-CN/apiKeyPool.json';
import zhCNChat from './locales/zh-CN/chat.json';
import zhCNTemplates from './locales/zh-CN/templates.json';

// 英文
import enUSCommon from './locales/en-US/common.json';
import enUSLogin from './locales/en-US/login.json';
import enUSModels from './locales/en-US/models.json';
import enUSUsers from './locales/en-US/users.json';
import enUSApiKeys from './locales/en-US/apiKeys.json';
import enUSDashboard from './locales/en-US/dashboard.json';
import enUSProviders from './locales/en-US/providers.json';
import enUSApiKeyPool from './locales/en-US/apiKeyPool.json';
import enUSChat from './locales/en-US/chat.json';
import enUSTemplates from './locales/en-US/templates.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      'zh-CN': {
        common: zhCNCommon,
        login: zhCNLogin,
        models: zhCNModels,
        users: zhCNUsers,
        apiKeys: zhCNApiKeys,
        dashboard: zhCNDashboard,
        providers: zhCNProviders,
        apiKeyPool: zhCNApiKeyPool,
        chat: zhCNChat,
        templates: zhCNTemplates,
      },
      'en-US': {
        common: enUSCommon,
        login: enUSLogin,
        models: enUSModels,
        users: enUSUsers,
        apiKeys: enUSApiKeys,
        dashboard: enUSDashboard,
        providers: enUSProviders,
        apiKeyPool: enUSApiKeyPool,
        chat: enUSChat,
        templates: enUSTemplates,
      },
    },
    fallbackLng: 'zh-CN',
    defaultNS: 'common',
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
    },
  });

export default i18n;
