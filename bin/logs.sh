#!/bin/bash

# API Premium Gateway 日志查看脚本 (Mac/Linux)
# 用于查看Docker容器日志

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目信息
PROJECT_NAME="API Premium Gateway"
COMPOSE_FILE="docker-compose.app.yml"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    echo "用法: $0 [服务名] [选项]"
    echo ""
    echo "服务名:"
    echo "  api-gateway      查看应用日志（默认）"
    echo "  postgres         查看数据库日志"
    echo "  all              查看所有服务日志"
    echo ""
    echo "选项:"
    echo "  -f, --follow     实时跟踪日志"
    echo "  -t, --tail N     显示最后N行日志（默认100）"
    echo "  -h, --help       显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                    # 查看应用日志（最后100行）"
    echo "  $0 -f                 # 实时跟踪应用日志"
    echo "  $0 postgres           # 查看数据库日志"
    echo "  $0 all -f             # 实时跟踪所有服务日志"
    echo "  $0 api-gateway -t 50  # 查看应用最后50行日志"
}

# 检查服务是否运行
check_services() {
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "未找到 $COMPOSE_FILE 文件"
        exit 1
    fi
    
    # 检查容器是否运行
    if ! docker-compose -f "$COMPOSE_FILE" ps | grep -q "Up"; then
        log_error "没有运行中的服务，请先启动服务"
        echo "使用 './bin/start.sh' 启动服务"
        exit 1
    fi
}

# 查看日志
view_logs() {
    local service="$1"
    local follow="$2"
    local tail_lines="$3"
    
    local cmd="docker-compose -f $COMPOSE_FILE logs"
    
    # 添加tail参数
    if [ -n "$tail_lines" ]; then
        cmd="$cmd --tail=$tail_lines"
    fi
    
    # 添加follow参数
    if [ "$follow" = "true" ]; then
        cmd="$cmd -f"
    fi
    
    # 添加服务名
    if [ "$service" != "all" ]; then
        cmd="$cmd $service"
    fi
    
    log_info "执行命令: $cmd"
    echo ""
    
    # 执行命令
    eval $cmd
}

# 主函数
main() {
    # 默认参数
    local service="api-gateway"
    local follow="false"
    local tail_lines="100"
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -f|--follow)
                follow="true"
                shift
                ;;
            -t|--tail)
                tail_lines="$2"
                shift 2
                ;;
            api-gateway|postgres|all)
                service="$1"
                shift
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    echo "========================================"
    echo "    $PROJECT_NAME 日志查看"
    echo "========================================"
    echo ""
    
    # 检查服务状态
    check_services
    
    # 显示当前配置
    log_info "服务: $service"
    log_info "跟踪: $follow"
    if [ "$follow" != "true" ]; then
        log_info "显示行数: $tail_lines"
    fi
    echo ""
    
    # 查看日志
    view_logs "$service" "$follow" "$tail_lines"
}

# 执行主函数
main "$@" 