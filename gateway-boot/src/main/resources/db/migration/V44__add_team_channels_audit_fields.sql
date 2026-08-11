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
-- V44: 创建 team_channels 表并添加审计字段
-- 权限模型改为团队继承渠道，需要 team_channels 关联表

-- 1. 创建团队-渠道关联表（如果不存在）
CREATE TABLE IF NOT EXISTS team_channels (
    team_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (team_id, channel_id)
);

-- 2. 创建索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_team_channels_team ON team_channels(team_id);
CREATE INDEX IF NOT EXISTS idx_team_channels_channel ON team_channels(channel_id);

-- 3. 补充审计字段
ALTER TABLE team_channels ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE team_channels ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE team_channels ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
