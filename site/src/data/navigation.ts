// 顶部导航项（中英双语，营销页与文档站 Header 共用）。
export interface NavItem {
  label: string;
  href: string;
  enLabel: string;
  // 外部链接：渲染为 target="_blank" rel="noopener noreferrer"（新窗口打开）。
  external?: boolean;
}

export const navigation: NavItem[] = [
  { label: '产品', href: '/#products', enLabel: 'Product' },
  // 演示站点为外部地址（P0 占位，域名确定后改此一处）。
  { label: '演示', href: 'https://demo.codingas.com', enLabel: 'Demo', external: true },
  { label: '文档', href: '/features/', enLabel: 'Docs' },
  { label: '版本对比', href: '/standard-vs-enterprise/', enLabel: 'Editions' },
  { label: '联系我们', href: '/contact-us/', enLabel: 'Contact' },
];
