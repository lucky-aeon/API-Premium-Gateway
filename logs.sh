#!/bin/bash

# API Premium Gateway 日志查看脚本

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项] [服务名]"
    echo
    echo "选项:"
    echo "  -f, --follow     跟踪日志输出"
    echo "  -t, --tail NUM   显示最后NUM行日志 (默认: 50)"
    echo "  -h, --help       显示此帮助信息"
    echo
    echo "服务名:"
    echo "  postgres         数据库服务日志"
    echo "  api-gateway      API网关服务日志"
    echo "  (空)             所有服务日志"
    echo
    echo "示例:"
    echo "  $0                    # 显示所有服务的最近50行日志"
    echo "  $0 -f                 # 跟踪所有服务日志"
    echo "  $0 -f api-gateway     # 跟踪API网关日志"
    echo "  $0 -t 100 postgres    # 显示数据库最近100行日志"
}

# 默认参数
FOLLOW=""
TAIL="50"
SERVICE=""

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--follow)
            FOLLOW="-f"
            shift
            ;;
        -t|--tail)
            TAIL="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        postgres|api-gateway)
            SERVICE="$1"
            shift
            ;;
        *)
            echo -e "${RED}未知参数: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# 构建docker compose logs命令
CMD="docker compose logs"

if [ -n "$FOLLOW" ]; then
    CMD="$CMD $FOLLOW"
else
    CMD="$CMD --tail=$TAIL"
fi

if [ -n "$SERVICE" ]; then
    CMD="$CMD $SERVICE"
fi

echo -e "${BLUE}查看 API Premium Gateway 日志...${NC}"
echo -e "${YELLOW}命令: $CMD${NC}"
echo

# 执行命令
eval $CMD