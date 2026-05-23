-- 密钥归于用户，移除 team_id 列
ALTER TABLE user_api_keys DROP COLUMN team_id;
