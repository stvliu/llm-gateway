/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
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
import zhCNApplications from './locales/zh-CN/applications.json';
import zhCNCatalog from './locales/zh-CN/catalog.json';
import zhCNChannels from './locales/zh-CN/channels.json';
import zhCNQuickstart from './locales/zh-CN/quickstart.json';
import zhCNApiKeys from './locales/zh-CN/apiKeys.json';
import zhCNResilience from './locales/zh-CN/resilience.json';
// 英文
import enUSCommon from './locales/en-US/common.json';
import enUSLogin from './locales/en-US/login.json';
import enUSModels from './locales/en-US/models.json';
import enUSUsers from './locales/en-US/users.json';
import enUSDashboard from './locales/en-US/dashboard.json';
import enUSProviders from './locales/en-US/providers.json';
import enUSChat from './locales/en-US/chat.json';
import enUSApplications from './locales/en-US/applications.json';
import enUSCatalog from './locales/en-US/catalog.json';
import enUSChannels from './locales/en-US/channels.json';
import enUSQuickstart from './locales/en-US/quickstart.json';
import enUSApiKeys from './locales/en-US/apiKeys.json';
import enUSResilience from './locales/en-US/resilience.json';

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
        applications: zhCNApplications,
        catalog: zhCNCatalog,
        channels: zhCNChannels,
        quickstart: zhCNQuickstart,
        apiKeys: zhCNApiKeys,
        resilience: zhCNResilience,
      },
      'en-US': {
        common: enUSCommon,
        login: enUSLogin,
        models: enUSModels,
        users: enUSUsers,
        dashboard: enUSDashboard,
        providers: enUSProviders,
        chat: enUSChat,
        applications: enUSApplications,
        catalog: enUSCatalog,
        channels: enUSChannels,
        quickstart: enUSQuickstart,
        apiKeys: enUSApiKeys,
        resilience: enUSResilience,
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