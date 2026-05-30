-- V44: team_channels 表补充审计字段
ALTER TABLE team_channels ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE team_channels ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE team_channels ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;