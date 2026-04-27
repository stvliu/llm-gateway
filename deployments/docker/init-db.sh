#!/bin/bash
# ===================================================================
# PostgreSQL 初始化脚本
# 在数据库首次创建时自动执行
# ===================================================================

set -e

echo "Initializing LLM-Gateway database..."

# 创建扩展
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- 创建 UUID 扩展 (用于 trace_id 等)
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

    -- 创建 hstore 扩展 (用于 JSON 属性)
    CREATE EXTENSION IF NOT EXISTS "hstore";
EOSQL

echo "Database initialization completed."
