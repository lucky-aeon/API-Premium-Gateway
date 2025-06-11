#!/bin/bash

# API Premium Gateway 启动脚本 (Mac/Linux)
# 用于构建Docker镜像并启动完整的应用栈

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

# 默认参数
RESET_DB=false
CLEAN_BUILD=false

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

# 检查必要的工具
check_requirements() {
    log_info "检查系统要求..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    # 检查Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    # 检查Maven和Java（可选，Docker内构建）
    local has_maven=false
    local has_java=false
    
    if command -v mvn &> /dev/null; then
        has_maven=true
    fi
    
    if command -v java &> /dev/null; then
        has_java=true
    fi
    
    if [ "$has_maven" = true ] && [ "$has_java" = true ]; then
        log_success "检测到本地Maven和Java环境，将使用本地构建"
        USE_LOCAL_BUILD=true
    else
        log_warning "未检测到Maven或Java环境，将使用Docker内构建"
        log_info "这是正常的，Docker会自动处理所有依赖"
        USE_LOCAL_BUILD=false
    fi
    
    log_success "系统要求检查通过"
}

# 构建应用
build_application() {
    if [ "$USE_LOCAL_BUILD" = true ]; then
        log_info "使用本地环境构建应用..."
        
        # 清理并编译
        log_info "执行 Maven 清理和编译..."
        mvn clean compile -q
        
        # 跳过测试（可选）
        # log_info "运行单元测试..."
        # mvn test -q
        
        # 打包应用
        log_info "打包应用..."
        mvn package -DskipTests -q
        
        # 检查jar文件是否生成
        if [ ! -f target/api-premium-gateway-*.jar ]; then
            log_error "应用打包失败，未找到jar文件"
            exit 1
        fi
        
        log_success "本地应用构建完成"
    else
        log_info "将在Docker容器内构建应用..."
        log_info "Docker会自动下载Maven、Java等所有依赖"
        log_success "准备使用Docker多阶段构建"
    fi
}

# 重置数据库
reset_database() {
    if [ "$RESET_DB" = true ]; then
        log_warning "重置数据库：删除所有数据并重新初始化..."
        
        # 停止服务
        if [ -f "$COMPOSE_FILE" ]; then
            docker-compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
        fi
        
        # 删除数据库卷
        docker volume rm api-premium-gateway_postgres_data 2>/dev/null || true
        
        log_success "数据库已重置"
    fi
}

# 停止现有容器
stop_existing_containers() {
    log_info "停止现有容器..."
    
    if [ -f "$COMPOSE_FILE" ]; then
        docker-compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
    fi
    
    # 清理悬空镜像
    if [ "$CLEAN_BUILD" = true ]; then
        log_info "清理Docker镜像缓存..."
        docker image prune -f >/dev/null 2>&1 || true
        # 删除应用镜像强制重新构建
        docker rmi api-premium-gateway_api-gateway 2>/dev/null || true
    else
        docker image prune -f >/dev/null 2>&1 || true
    fi
    
    log_success "现有容器已停止"
}

# 启动服务
start_services() {
    log_info "启动 $PROJECT_NAME 服务..."
    
    # 构建并启动服务
    if [ "$CLEAN_BUILD" = true ]; then
        log_info "强制重新构建Docker镜像..."
        docker-compose -f "$COMPOSE_FILE" up --build --force-recreate -d
    else
        docker-compose -f "$COMPOSE_FILE" up --build -d
    fi
    
    log_success "服务启动完成"
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务启动..."
    
    # 等待数据库就绪
    log_info "等待数据库启动..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U gateway_user -d api_gateway >/dev/null 2>&1; then
            log_success "数据库已就绪"
            break
        fi
        sleep 2
        timeout=$((timeout-2))
    done
    
    if [ $timeout -le 0 ]; then
        log_error "数据库启动超时"
        exit 1
    fi
    
    # 等待应用就绪
    log_info "等待应用启动..."
    timeout=120
    while [ $timeout -gt 0 ]; do
        if curl -f http://localhost:8081/api/health >/dev/null 2>&1; then
            log_success "应用已就绪"
            break
        fi
        sleep 3
        timeout=$((timeout-3))
    done
    
    if [ $timeout -le 0 ]; then
        log_error "应用启动超时"
        show_logs
        exit 1
    fi
}

# 显示服务状态
show_status() {
    log_info "服务状态："
    docker-compose -f "$COMPOSE_FILE" ps
    
    echo ""
    log_info "服务访问地址："
    echo "  - 应用地址: http://localhost:8081/api"
    echo "  - 健康检查: http://localhost:8081/api/health"
    echo "  - 数据库: localhost:5433"
    echo ""
    log_info "查看日志命令："
    echo "  - 应用日志: docker-compose -f $COMPOSE_FILE logs -f api-gateway"
    echo "  - 数据库日志: docker-compose -f $COMPOSE_FILE logs -f postgres"
    echo "  - 所有日志: docker-compose -f $COMPOSE_FILE logs -f"
}

# 显示日志
show_logs() {
    log_info "显示应用日志（最近50行）："
    docker-compose -f "$COMPOSE_FILE" logs --tail=50 api-gateway
}

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --reset-db       重置数据库（删除所有数据并重新初始化）"
    echo "  --clean-build    强制重新构建Docker镜像"
    echo "  -h, --help       显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                    # 正常启动"
    echo "  $0 --reset-db         # 重置数据库并启动"
    echo "  $0 --clean-build      # 强制重新构建并启动"
    echo "  $0 --reset-db --clean-build  # 完全重置并启动"
    echo ""
    echo "说明:"
    echo "  --reset-db: 删除数据库持久化数据，重新执行初始化脚本"
    echo "  --clean-build: 删除Docker镜像缓存，确保使用最新代码构建"
}

# 解析命令行参数
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --reset-db)
                RESET_DB=true
                shift
                ;;
            --clean-build)
                CLEAN_BUILD=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# 主函数
main() {
    # 解析参数
    parse_arguments "$@"
    
    echo "========================================"
    echo "    $PROJECT_NAME 启动脚本"
    echo "========================================"
    echo ""
    
    # 显示当前配置
    if [ "$RESET_DB" = true ]; then
        log_warning "将重置数据库"
    fi
    if [ "$CLEAN_BUILD" = true ]; then
        log_warning "将强制重新构建镜像"
    fi
    echo ""
    
    # 检查系统要求
    check_requirements
    
    # 构建应用
    build_application
    
    # 重置数据库（如果需要）
    reset_database
    
    # 停止现有容器
    stop_existing_containers
    
    # 启动服务
    start_services
    
    # 等待服务就绪
    wait_for_services
    
    # 显示状态
    show_status
    
    echo ""
    log_success "$PROJECT_NAME 启动成功！"
    echo ""
    echo "使用 './bin/stop.sh' 停止服务"
    echo "使用 './bin/logs.sh' 查看日志"
}

# 执行主函数
main "$@" 