#!/bin/bash

# API Premium Gateway - PostgreSQL 停止脚本
# 该脚本用于停止 PostgreSQL 数据库服务

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🛑 正在停止 API Premium Gateway PostgreSQL 数据库...${NC}"

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ 错误: Docker 未运行${NC}"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose > /dev/null 2>&1; then
    echo -e "${RED}❌ 错误: Docker Compose 未安装${NC}"
    exit 1
fi

# 切换到项目根目录
cd "$(dirname "$0")/.."

# 检查 docker-compose.yml 文件是否存在
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}❌ 错误: docker-compose.yml 文件不存在${NC}"
    exit 1
fi

# 停止 PostgreSQL 服务
echo -e "${YELLOW}🔧 停止 PostgreSQL 容器...${NC}"
docker-compose down

echo -e "${GREEN}✅ PostgreSQL 数据库已成功停止！${NC}"
echo ""
echo -e "${YELLOW}💡 提示:${NC}"
echo -e "  - 数据已保存在 Docker volume 中，下次启动时会自动恢复"
echo -e "  - 使用 'scripts/start-postgres.sh' 重新启动数据库"
echo -e "  - 如需完全清理数据，请运行: docker-compose down -v"
echo "" 