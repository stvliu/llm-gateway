-- Task 3: 应用-渠道授权关联表新增 priority 列
-- 转移顺序由全局 ModelInstance.priority 改为应用级 ApplicationChannel.priority，
-- 同一渠道对不同应用可有不同转移顺序。priority 为 NULL 表示未配置，PriorityRouter 回退默认值 100。
ALTER TABLE application_channels ADD COLUMN priority INT NULL;
