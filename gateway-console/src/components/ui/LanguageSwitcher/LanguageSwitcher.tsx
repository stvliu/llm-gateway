import { useTranslation } from 'react-i18next';
import { Dropdown, Button } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';

const languages = [
  { key: 'zh-CN', label: '简体中文', flag: '🇨🇳' },
  { key: 'en-US', label: 'English', flag: '🇺🇸' },
];

/**
 * 语言切换组件
 * 支持中英文切换
 */
export function LanguageSwitcher() {
  const { i18n } = useTranslation();

  const currentLang =
    languages.find((l) => l.key === i18n.language) || languages[0];

  const menu = {
    items: languages.map((lang) => ({
      key: lang.key,
      label: (
        <span className={lang.key === i18n.language ? 'font-bold' : ''}>
          {lang.flag} {lang.label}
        </span>
      ),
    })),
    onClick: ({ key }: { key: string }) => {
      i18n.changeLanguage(key);
    },
  };

  return (
    <Dropdown menu={menu} trigger={['click']}>
      <Button type="text" icon={<GlobalOutlined />}>
        {currentLang.flag}
      </Button>
    </Dropdown>
  );
}