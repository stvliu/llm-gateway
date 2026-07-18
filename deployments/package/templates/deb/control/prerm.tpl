#!/bin/bash
# LLM-Gateway deb 卸载前脚本（JReleaser DebAssembler 模板，prerm.tpl -> prerm）
# deb: $1 = remove|upgrade|failed-upgrade|deconfigure|...
# 升级时不停止服务（postinst restart 会处理）；卸载时停止并禁用
set -e

SHOULD_STOP=0
case "${1:-}" in
  remove|deconfigure) SHOULD_STOP=1 ;;
esac

if [ "$SHOULD_STOP" = "1" ]; then
  if [ -x /usr/bin/systemctl ] || [ -x /bin/systemctl ]; then
    systemctl stop llmgateway.service 2>/dev/null || true
    systemctl disable llmgateway.service 2>/dev/null || true
  fi
fi

exit 0
