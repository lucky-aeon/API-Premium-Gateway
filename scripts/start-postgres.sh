#!/bin/bash

# API Premium Gateway - PostgreSQL 启动脚本
# 该脚本用于一键启动 PostgreSQL 数据库服务

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 正在启动 API Premium Gateway PostgreSQL 数据库...${NC}"

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ 错误: Docker 未运行，请先启动 Docker${NC}"
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

# 检查 SQL 初始化文件是否存在
if [ ! -f "docs/sql/sql.sql" ]; then
    echo -e "${RED}❌ 错误: docs/sql/sql.sql 文件不存在${NC}"
    exit 1
fi

# 检查是否已有镜像，如果没有则提示用户
if ! docker images postgres:15-alpine --format "table {{.Repository}}" | grep -q postgres; then
    echo -e "${YELLOW}📥 PostgreSQL 镜像不存在，正在下载...${NC}"
    echo -e "${YELLOW}   这可能需要几分钟时间，请确保网络连接正常${NC}"
fi

# 停止并移除已存在的容器（如果有的话）
echo -e "${YELLOW}🔄 清理旧容器...${NC}"
docker-compose down --remove-orphans

# 启动 PostgreSQL 服务
echo -e "${YELLOW}🔧 启动 PostgreSQL 容器...${NC}"
docker-compose up -d postgres

# 等待数据库启动
echo -e "${YELLOW}⏳ 等待数据库启动完成...${NC}"
until docker-compose exec postgres pg_isready -U gateway_user -d api_gateway > /dev/null 2>&1; do
    printf "."
    sleep 2
done

echo ""
echo -e "${GREEN}✅ PostgreSQL 数据库启动成功！${NC}"
echo ""
echo -e "${GREEN}📋 数据库连接信息:${NC}"
echo -e "  🔗 主机: localhost"
echo -e "  🔌 端口: 5433"
echo -e "  🗄️  数据库: api_gateway"
echo -e "  👤 用户名: gateway_user"
echo -e "  🔐 密码: gateway_pass"
echo ""
echo -e "${GREEN}🔗 JDBC URL: jdbc:postgresql://localhost:5433/api_gateway${NC}"
echo ""
echo -e "${YELLOW}💡 提示:${NC}"
echo -e "  - 使用 'scripts/stop-postgres.sh' 停止数据库"
echo -e "  - 使用 'docker-compose logs postgres' 查看日志"
echo -e "  - 数据将持久化存储在 Docker volume 中"
echo "" 