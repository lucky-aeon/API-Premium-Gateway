#!/bin/bash

# API Premium Gateway 一键启动脚本
# 用于快速部署API网关服务

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

# 项目信息
echo -e "${BLUE}"
echo "   ▄▄▄       ██▓███   ██▓    ▄████ ▄▄▄     ▄▄▄█████▓▓█████ █     █░▄▄▄      ▓██   ██▓"
echo "  ▒████▄    ▓██░  ██▒▓██▒   ██▒ ▀█▒████▄   ▓  ██▒ ▓▒▓█   ▀▓█░ █ ░█░████▄     ▒██  ██▒"
echo "  ▒██  ▀█▄  ▓██░ ██▓▒▒██▒  ▒██░▄▄▄▒██  ▀█▄ ▒ ▓██░ ▒░▒███  ▒█░ █ ░█▒██  ▀█▄    ▒██ ██░"
echo "  ░██▄▄▄▄██ ▒██▄█▓▒ ▒░██░  ░▓█  ██▓██▄▄▄▄██░ ▓██▓ ░ ▒▓█  ▄░█░ █ ░█░██▄▄▄▄██   ░ ▐██▓░"
echo -e "   ▓█   ▓██▒▒██▒ ░  ░░██░  ░▒▓███▀▒▓█   ▓██▒ ▒██▒ ░ ░▒████▒░░██▒██▓ ▓█   ▓██▒  ░ ██▒▓░${NC}"
echo -e "${GREEN}                    高可用API网关 - 智能路由与负载均衡${NC}"
echo -e "${BLUE}================================================================${NC}"
echo

# 检查Docker环境
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}错误: Docker未安装，请先安装Docker${NC}"
        exit 1
    fi

    if ! docker compose version &> /dev/null; then
        echo -e "${RED}错误: Docker Compose未安装或版本过低${NC}"
        exit 1
    fi
}

# 准备环境配置
prepare_env() {
    if [ ! -f ".env" ]; then
        echo -e "${YELLOW}创建环境配置文件...${NC}"
        if [ -f ".env.example" ]; then
            cp ".env.example" ".env"
            echo -e "${GREEN}✅ 已创建 .env 文件，基于模板: .env.example${NC}"
        else
            echo -e "${RED}错误: 未找到 .env.example 模板文件${NC}"
            exit 1
        fi
    else
        echo -e "${GREEN}✅ 使用现有 .env 配置文件${NC}"
    fi
}

# 启动服务
start_services() {
    echo -e "${BLUE}启动API Premium Gateway服务...${NC}"
    echo

    # 启动服务
    docker compose up -d --build

    echo
    echo -e "${GREEN}🎉 API Premium Gateway启动完成！${NC}"
    echo
    echo -e "${BLUE}服务访问地址:${NC}"
    echo "  API网关: http://localhost:8081"
    echo "  健康检查: http://localhost:8081/api/health"
    echo "  数据库: localhost:5433"
    echo
    echo -e "${BLUE}数据库连接信息:${NC}"
    echo "  数据库名: api_gateway"
    echo "  用户名: gateway_user"
    echo "  密码: gateway_pass"
    echo
    echo -e "${YELLOW}常用命令:${NC}"
    echo "  查看日志: docker compose logs -f"
    echo "  停止服务: docker compose down"
    echo "  重启服务: docker compose restart"
    echo "  查看状态: docker compose ps"
}

# 主程序
main() {
    check_docker
    
    echo -e "${YELLOW}API Premium Gateway 启动中...${NC}"
    echo "智能API网关，提供高可用负载均衡和故障转移功能"
    echo
    
    prepare_env
    start_services
}

# 运行主程序
main "$@"