#!/bin/bash

# API Premium Gateway 数据库重置脚本

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

echo -e "${RED}⚠️  数据库重置操作${NC}"
echo -e "${YELLOW}此操作将删除所有数据库数据并重新初始化${NC}"
echo -e "${RED}所有现有数据将被永久删除！${NC}"
echo

read -p "确定要继续吗？(输入 'YES' 确认): " -r
if [[ $REPLY != "YES" ]]; then
    echo -e "${YELLOW}操作已取消${NC}"
    exit 0
fi

echo
echo -e "${BLUE}开始重置数据库...${NC}"

# 停止服务
echo -e "${YELLOW}1. 停止服务...${NC}"
docker compose down --remove-orphans

# 删除数据库卷
echo -e "${YELLOW}2. 删除数据库卷...${NC}"
docker volume rm api-premium-gateway_postgres_data 2>/dev/null || true

# 重新启动服务
echo -e "${YELLOW}3. 重新启动服务...${NC}"
docker compose up -d --build

echo
echo -e "${GREEN}✅ 数据库重置完成！${NC}"
echo -e "${BLUE}数据库将重新初始化，请等待服务启动完成${NC}"