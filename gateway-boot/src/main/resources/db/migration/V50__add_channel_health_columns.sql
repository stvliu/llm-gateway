-- 渠道健康状态字段（last-write-wins，无版本锁）
-- 用于支撑前端健康指示与连通性测试矩阵聚合结果持久化
ALTER TABLE channels ADD COLUMN last_health_check_at TIMESTAMP NULL;
ALTER TABLE channels ADD COLUMN last_health_status VARCHAR(16) NULL;
ALTER TABLE channels ADD COLUMN last_health_source VARCHAR(16) NULL;

CREATE INDEX idx_channels_last_health_status ON channels(last_health_status);
