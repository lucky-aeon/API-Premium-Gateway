#!/bin/bash

# API Premium Gateway - 配置同步检查脚本
# 验证所有配置文件中的数据库连接信息是否与 Docker 设置一致

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 检查配置文件同步状态...${NC}"

# 切换到项目根目录
cd "$(dirname "$0")/.."

# Docker 配置信息（标准配置）
DOCKER_HOST="localhost"
DOCKER_PORT="5433"
DOCKER_DB="api_gateway"
DOCKER_USER="gateway_user"
DOCKER_PASS="gateway_pass"

echo -e "${YELLOW}📋 Docker 标准配置:${NC}"
echo -e "  🔗 主机: ${DOCKER_HOST}"
echo -e "  🔌 端口: ${DOCKER_PORT}"
echo -e "  🗄️  数据库: ${DOCKER_DB}"
echo -e "  👤 用户名: ${DOCKER_USER}"
echo -e "  🔐 密码: ${DOCKER_PASS}"
echo ""

# 检查函数
check_config_file() {
    local file="$1"
    local description="$2"
    
    if [ ! -f "$file" ]; then
        echo -e "${YELLOW}⚠️  $description: 文件不存在${NC}"
        return 1
    fi
    
    echo -e "${BLUE}🔍 检查 $description...${NC}"
    
    # 检查端口
    if grep -q "localhost:${DOCKER_PORT}" "$file"; then
        echo -e "  ✅ 端口配置正确: ${DOCKER_PORT}"
    else
        echo -e "  ❌ 端口配置错误 (应为 ${DOCKER_PORT})"
        return 1
    fi
    
    # 检查数据库名
    if grep -q "/${DOCKER_DB}" "$file"; then
        echo -e "  ✅ 数据库名正确: ${DOCKER_DB}"
    else
        echo -e "  ❌ 数据库名错误 (应为 ${DOCKER_DB})"
        return 1
    fi
    
    # 检查用户名
    if grep -q "username: ${DOCKER_USER}" "$file"; then
        echo -e "  ✅ 用户名正确: ${DOCKER_USER}"
    else
        echo -e "  ❌ 用户名错误 (应为 ${DOCKER_USER})"
        return 1
    fi
    
    # 检查密码
    if grep -q "password: ${DOCKER_PASS}" "$file"; then
        echo -e "  ✅ 密码正确: ${DOCKER_PASS}"
    else
        echo -e "  ❌ 密码错误 (应为 ${DOCKER_PASS})"
        return 1
    fi
    
    echo -e "  ${GREEN}✅ $description 配置同步正确${NC}"
    echo ""
    return 0
}

# 检查所有配置文件
all_correct=true

check_config_file "src/main/resources/application.yml" "主配置文件" || all_correct=false
check_config_file "src/test/resources/application-test.yml" "测试配置文件" || all_correct=false
check_config_file "docs/application-dev.yml" "开发配置示例" || all_correct=false

if [ "$all_correct" = true ]; then
    echo -e "${GREEN}🎉 所有配置文件与 Docker 设置同步正确！${NC}"
    echo ""
    echo -e "${YELLOW}💡 现在可以启动应用程序:${NC}"
    echo -e "  mvn spring-boot:run"
    echo -e "  或者"
    echo -e "  ./mvnw spring-boot:run"
else
    echo -e "${RED}❌ 发现配置不一致，请检查并修复${NC}"
    exit 1
fi 