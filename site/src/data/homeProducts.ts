// 三产品卡数据（中英 label 由 i18n/home.ts 提供，此处仅结构）。
export interface ProductCardData {
  key: 'standard' | 'cloud' | 'enterprise';
}

export const homeProducts: ProductCardData[] = [
  { key: 'standard' },
  { key: 'cloud' },
  { key: 'enterprise' },
];
