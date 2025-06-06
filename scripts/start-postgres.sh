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

# 检查是否需要初始化数据库表
echo -e "${YELLOW}🔍 检查数据库表...${NC}"
TABLE_COUNT=$(docker-compose exec -T postgres psql -U gateway_user -d api_gateway -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' \n')

if [ "$TABLE_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}📋 未发现数据表，开始初始化数据库...${NC}"
    initialize_database=true
elif [ "$TABLE_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  发现 $TABLE_COUNT 个数据表已存在${NC}"
    echo -e "${RED}🗑️  重新初始化将删除所有现有数据表和数据${NC}"
    echo ""
    read -p "是否要删除现有数据表并重新初始化？(y/N): " -n 1 -r
    echo ""
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}🗑️  正在删除现有数据表...${NC}"
        docker-compose exec -T postgres psql -U gateway_user -d api_gateway -c "DROP TABLE IF EXISTS api_instance_metrics, api_instance_registry, api_keys, projects CASCADE;" > /dev/null 2>&1
        echo -e "${GREEN}✅ 现有数据表已删除${NC}"
        initialize_database=true
    else
        echo -e "${YELLOW}⏭️  跳过数据库初始化，保留现有数据表${NC}"
        initialize_database=false
    fi
fi

# 执行数据库初始化
if [ "$initialize_database" = true ]; then
    echo -e "${YELLOW}🔧 正在初始化数据库表...${NC}"
    if docker-compose exec -T postgres psql -U gateway_user -d api_gateway < docs/sql/sql.sql > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 数据库表初始化成功！${NC}"
        # 再次检查表数量
        NEW_TABLE_COUNT=$(docker-compose exec -T postgres psql -U gateway_user -d api_gateway -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' \n')
        echo -e "${GREEN}📊 成功创建 $NEW_TABLE_COUNT 个数据表${NC}"
    else
        echo -e "${RED}❌ 数据库表初始化失败${NC}"
        echo -e "${YELLOW}💡 请检查 docs/sql/sql.sql 文件是否存在语法错误${NC}"
        exit 1
    fi
fi

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