#!/bin/bash

# 前端构建部署脚本
# 构建前端并复制到 Spring Boot 的 static 目录

set -e

echo "Building frontend..."
cd "$(dirname "$0")/.."

# 安装依赖
pnpm install

# 构建
pnpm build

echo "Frontend build complete!"
echo "Output directory: ../gateway-boot/src/main/resources/static"
