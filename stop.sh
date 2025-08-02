#!/bin/bash

# API Premium Gateway 停止脚本

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

echo -e "${BLUE}停止 API Premium Gateway 服务...${NC}"

# 停止服务
docker compose down --remove-orphans

echo -e "${GREEN}✅ API Premium Gateway 已停止${NC}"
echo
echo -e "${YELLOW}提示: 数据已保存到 Docker 卷中${NC}"
echo "如需完全清理，请手动删除相关 Docker 卷"