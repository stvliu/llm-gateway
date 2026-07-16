#!/bin/bash
# LLM-Gateway deb 卸载后脚本（JReleaser DebAssembler 模板，postrm.tpl -> postrm）
# deb: $1 = remove|purge|upgrade|failed-upgrade|...
# 卸载时清理 systemd unit；保留数据目录（仅 purge 清数据）
set -e

# 卸载时清理 systemd unit（升级不清理）
SHOULD_CLEAN=0
case "${1:-}" in
  remove|purge) SHOULD_CLEAN=1 ;;
esac

if [ "$SHOULD_CLEAN" = "1" ]; then
  rm -f /etc/systemd/system/llm-gateway.service
  rm -f /usr/lib/systemd/system/llm-gateway.service 2>/dev/null || true
  rm -f /lib/systemd/system/llm-gateway.service 2>/dev/null || true
  systemctl daemon-reload 2>/dev/null || true
fi

# purge 模式清理数据目录与配置
if [ "${1:-}" = "purge" ]; then
  echo "[postrm] purge 模式：清理数据目录与配置..."
  rm -rf /var/lib/llm-gateway /var/log/llm-gateway /etc/llm-gateway
  echo "[postrm] 数据已清除。如需完全移除用户：userdel llm-gateway"
fi

echo "[postrm] 卸载完成。数据目录 /var/lib/llm-gateway 已保留（除非 purge）。"

exit 0
