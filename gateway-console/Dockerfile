#
# Copyright (c) 2025 codingas.com
# Licensed under the Apache License, Version 2.0.
# See the LICENSE file for details.
#
# ===================================================================
# LLM-Gateway Console Dockerfile（开发/预览）
# ===================================================================
FROM node:20-alpine

WORKDIR /app

# 启用 corepack 支持 pnpm
RUN corepack enable && corepack prepare pnpm@9 --activate

# 复制 package 文件
COPY package.json pnpm-lock.yaml* ./

# 安装依赖
RUN pnpm install --frozen-lockfile || pnpm install

# 复制源码
COPY . .

# 暴露 Vite dev server 端口
EXPOSE 5173

# 启动 dev server（host 0.0.0.0 供容器外访问）
CMD ["pnpm", "dev", "--host", "0.0.0.0"]
