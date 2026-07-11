// 生态组件数据。
export interface EcosystemItem {
  name: string;
  desc: string;
}

export const homeEcosystem: EcosystemItem[] = [
  { name: 'PostgreSQL', desc: '主数据库 + pgvector 向量存储' },
  { name: 'Redis', desc: '分布式缓存与限流' },
  { name: 'OpenTelemetry', desc: '全链路追踪标准' },
  { name: 'Prometheus', desc: '指标采集与导出' },
  { name: 'Spring Boot 3.5', desc: '后端框架（Java 21）' },
  { name: 'Cloudflare Pages', desc: '官网与文档部署' },
];
