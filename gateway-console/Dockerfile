#
# Copyright © 2025-2026 codingas.com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
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
