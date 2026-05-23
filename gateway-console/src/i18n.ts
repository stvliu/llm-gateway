import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// 中文
import zhCNCommon from './locales/zh-CN/common.json';
import zhCNLogin from './locales/zh-CN/login.json';
import zhCNModels from './locales/zh-CN/models.json';
import zhCNUsers from './locales/zh-CN/users.json';
import zhCNDashboard from './locales/zh-CN/dashboard.json';
import zhCNProviders from './locales/zh-CN/providers.json';
import zhCNChat from './locales/zh-CN/chat.json';
import zhCNMetadata from './locales/zh-CN/metadata.json';
import zhCNExperience from './locales/zh-CN/experience.json';
import zhCNProducts from './locales/zh-CN/products.json';
import zhCNTeams from './locales/zh-CN/teams.json';

// 英文
import enUSCommon from './locales/en-US/common.json';
import enUSLogin from './locales/en-US/login.json';
import enUSModels from './locales/en-US/models.json';
import enUSUsers from './locales/en-US/users.json';
import enUSDashboard from './locales/en-US/dashboard.json';
import enUSProviders from './locales/en-US/providers.json';
import enUSChat from './locales/en-US/chat.json';
import enUSMetadata from './locales/en-US/metadata.json';
import enUSExperience from './locales/en-US/experience.json';
import enUSProducts from './locales/en-US/products.json';
import enUSTeams from './locales/en-US/teams.json';

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
        dashboard: zhCNDashboard,
        providers: zhCNProviders,
        chat: zhCNChat,
        metadata: zhCNMetadata,
        experience: zhCNExperience,
        products: zhCNProducts,
        teams: zhCNTeams,
      },
      'en-US': {
        common: enUSCommon,
        login: enUSLogin,
        models: enUSModels,
        users: enUSUsers,
        dashboard: enUSDashboard,
        providers: enUSProviders,
        chat: enUSChat,
        metadata: enUSMetadata,
        experience: enUSExperience,
        products: enUSProducts,
        teams: enUSTeams,
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
