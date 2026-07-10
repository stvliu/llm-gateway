// 顶部导航项（中英双语，营销页与文档站 Header 共用）。
export interface NavItem {
  label: string;
  href: string;
  enLabel: string;
}

export const navigation: NavItem[] = [
  { label: '产品', href: '/#products', enLabel: 'Product' },
  { label: '文档', href: '/docs/', enLabel: 'Docs' },
  { label: '版本对比', href: '/standard-vs-enterprise/', enLabel: 'Editions' },
  { label: '联系我们', href: '/contact-us/', enLabel: 'Contact' },
];
