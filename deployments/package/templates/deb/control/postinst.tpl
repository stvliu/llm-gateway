#!/bin/bash
# LLM-Gateway deb 安装后脚本（JReleaser DebAssembler 模板，postinst.tpl -> postinst）
# 首次安装：生成 conf 密钥占位符替换；升级：conffile 保留，跳过生成
set -e

DATA_DIR="/var/lib/llmgateway"
LOG_DIR="/var/log/llmgateway"
CONF_DIR="/etc/llmgateway"
CONF_FILE="$CONF_DIR/llmgateway.conf"
RUNTIME_BIN="/opt/llmgateway/runtime/bin"

# 1. 创建系统用户与组（已存在则跳过）
if ! getent group llmgateway >/dev/null; then
  groupadd --system llmgateway
fi
if ! id -u llmgateway >/dev/null 2>&1; then
  useradd --system --no-create-home --shell /usr/sbin/nologin \
    --gid llmgateway --home-dir "$DATA_DIR" llmgateway
fi

# 2. 创建数据/日志/配置目录
mkdir -p "$DATA_DIR" "$LOG_DIR" "$CONF_DIR"
chown -R llmgateway:llmgateway "$DATA_DIR" "$LOG_DIR"

# 3. 生成加密密钥（首次安装：conf 含 __GENERATE_KEY__ 占位符则替换；升级：conffile 保留，grep 不命中则跳过）
if [ -f "$CONF_FILE" ] && grep -q '__GENERATE_KEY__' "$CONF_FILE"; then
  NEW_KEY="$(head -c 32 /dev/urandom | base64)"
  # 用 | 分隔符避免 base64 密钥中的 / 冲突
  sed -i "s|__GENERATE_KEY__|${NEW_KEY}|" "$CONF_FILE"
  echo "[postinst] 生成新的 GATEWAY_ENCRYPTION_KEY（请妥善备份）" >&2
else
  echo "[postinst] 保留已有 GATEWAY_ENCRYPTION_KEY" >&2
fi

# 4. 设置 conf 权限（root 可读写，llmgateway 组可读）
chmod 640 "$CONF_FILE" 2>/dev/null || true
chown root:llmgateway "$CONF_FILE" 2>/dev/null || true

# 5. chmod 兜底可执行位（JReleaser 不保证 per-file mode，postinst 矫正 bin/java 与 llmgateway.sh）
if [ -d "$RUNTIME_BIN" ]; then
  chmod -R 0755 "$RUNTIME_BIN"
fi
chmod 0755 /opt/llmgateway/bin/llmgateway.sh 2>/dev/null || true

# 6. 注册 systemd unit（JReleaser 打包到 /usr/lib/systemd/system，FHS 现代路径）
systemctl daemon-reload
systemctl enable llmgateway.service

# 7. 启动服务（升级时 restart，首次安装 start）
if systemctl is-active --quiet llmgateway.service; then
  systemctl restart llmgateway.service
else
  systemctl start llmgateway.service
fi

echo "LLM-Gateway 已安装并启动。"
echo "  配置文件: $CONF_FILE（改后 systemctl restart llmgateway 生效）"
echo "  数据目录: $DATA_DIR"
echo "  服务状态: systemctl status llmgateway"

exit 0
