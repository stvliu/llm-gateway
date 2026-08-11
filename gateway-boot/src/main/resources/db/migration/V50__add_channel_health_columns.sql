--
-- Copyright © 2025-2026 codingas.com
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
-- 渠道健康状态字段（last-write-wins，无版本锁）
-- 用于支撑前端健康指示与连通性测试矩阵聚合结果持久化
ALTER TABLE channels ADD COLUMN last_health_check_at TIMESTAMP NULL;
ALTER TABLE channels ADD COLUMN last_health_status VARCHAR(16) NULL;
ALTER TABLE channels ADD COLUMN last_health_source VARCHAR(16) NULL;

CREATE INDEX idx_channels_last_health_status ON channels(last_health_status);
