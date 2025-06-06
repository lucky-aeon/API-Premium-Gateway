#!/bin/bash

# API Premium Gateway - 数据库健康检查脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🔍 检查数据库状态...${NC}"

# 切换到项目根目录
cd "$(dirname "$0")/.."

# 检查容器是否在运行
if ! docker-compose ps postgres | grep -q "Up"; then
    echo -e "${RED}❌ PostgreSQL 容器未运行${NC}"
    echo -e "${YELLOW}💡 运行 './start-db.sh' 启动数据库${NC}"
    exit 1
fi

# 检查数据库连接
echo -e "${YELLOW}🔗 测试数据库连接...${NC}"
if docker-compose exec -T postgres pg_isready -U gateway_user -d api_gateway > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 数据库连接正常${NC}"
else
    echo -e "${RED}❌ 数据库连接失败${NC}"
    exit 1
fi

# 检查表是否存在
echo -e "${YELLOW}📋 检查数据表...${NC}"
TABLE_COUNT=$(docker-compose exec -T postgres psql -U gateway_user -d api_gateway -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';")

if [ "$TABLE_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✅ 发现 $TABLE_COUNT 个数据表${NC}"
    
    # 列出所有表
    echo -e "${YELLOW}📊 数据表列表:${NC}"
    docker-compose exec -T postgres psql -U gateway_user -d api_gateway -t -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;"
else
    echo -e "${RED}❌ 未发现数据表，可能初始化失败${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}🎉 数据库健康检查通过！${NC}"
echo ""
echo -e "${YELLOW}💡 其他有用命令:${NC}"
echo -e "  - 连接数据库: docker-compose exec postgres psql -U gateway_user -d api_gateway"
echo -e "  - 查看日志: docker-compose logs postgres"
echo -e "  - 重启数据库: ./stop-db.sh && ./start-db.sh"
echo "" 