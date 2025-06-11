#!/bin/bash

# API Premium Gateway 停止脚本 (Mac/Linux)
# 用于停止所有相关的Docker容器和服务

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

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 停止服务
stop_services() {
    log_info "停止 $PROJECT_NAME 服务..."
    
    if [ -f "$COMPOSE_FILE" ]; then
        # 停止并移除容器
        docker-compose -f "$COMPOSE_FILE" down --remove-orphans
        log_success "服务已停止"
    else
        log_warning "未找到 $COMPOSE_FILE 文件"
    fi
}

# 清理资源（可选）
cleanup_resources() {
    if [ "$1" = "--cleanup" ] || [ "$1" = "-c" ]; then
        log_info "清理Docker资源..."
        
        # 清理悬空镜像
        docker image prune -f >/dev/null 2>&1 || true
        
        # 清理未使用的网络
        docker network prune -f >/dev/null 2>&1 || true
        
        log_success "资源清理完成"
    fi
}

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -c, --cleanup    停止服务后清理Docker资源"
    echo "  -h, --help       显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0               # 仅停止服务"
    echo "  $0 --cleanup     # 停止服务并清理资源"
}

# 主函数
main() {
    # 处理参数
    case "$1" in
        -h|--help)
            show_help
            exit 0
            ;;
        -c|--cleanup)
            CLEANUP=true
            ;;
        "")
            CLEANUP=false
            ;;
        *)
            log_error "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
    
    echo "========================================"
    echo "    $PROJECT_NAME 停止脚本"
    echo "========================================"
    echo ""
    
    # 停止服务
    stop_services
    
    # 清理资源（如果指定）
    if [ "$CLEANUP" = true ]; then
        cleanup_resources --cleanup
    fi
    
    echo ""
    log_success "$PROJECT_NAME 已停止！"
    
    if [ "$CLEANUP" != true ]; then
        echo ""
        log_info "提示: 使用 '$0 --cleanup' 可以同时清理Docker资源"
    fi
}

# 执行主函数
main "$@" 