@echo off
setlocal enabledelayedexpansion

REM API Premium Gateway 启动脚本 (Windows)
REM 用于构建Docker镜像并启动完整的应用栈

REM 项目信息
set PROJECT_NAME=API Premium Gateway
set COMPOSE_FILE=docker-compose.app.yml

REM 默认参数
set RESET_DB=false
set CLEAN_BUILD=false

REM 解析命令行参数
:parse_args
if "%1"=="" goto main
if "%1"=="--reset-db" (
    set RESET_DB=true
    shift
    goto parse_args
)
if "%1"=="--clean-build" (
    set CLEAN_BUILD=true
    shift
    goto parse_args
)
if "%1"=="-h" goto show_help
if "%1"=="--help" goto show_help
echo [ERROR] 未知参数: %1
goto show_help

:main
echo ========================================
echo     %PROJECT_NAME% 启动脚本
echo ========================================
echo.

REM 显示当前配置
if "%RESET_DB%"=="true" (
    echo [WARNING] 将重置数据库
)
if "%CLEAN_BUILD%"=="true" (
    echo [WARNING] 将强制重新构建镜像
)
echo.

REM 检查必要的工具
echo [INFO] 检查系统要求...

REM 检查Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker 未安装，请先安装 Docker Desktop
    pause
    exit /b 1
)

REM 检查Docker Compose
docker-compose --version >nul 2>&1
if errorlevel 1 (
    docker compose version >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Docker Compose 未安装，请先安装 Docker Compose
        pause
        exit /b 1
    )
)

REM 检查Maven和Java（可选，Docker内构建）
set HAS_MAVEN=false
set HAS_JAVA=false
set USE_LOCAL_BUILD=false

mvn --version >nul 2>&1
if not errorlevel 1 (
    set HAS_MAVEN=true
)

java -version >nul 2>&1
if not errorlevel 1 (
    set HAS_JAVA=true
)

if "%HAS_MAVEN%"=="true" if "%HAS_JAVA%"=="true" (
    echo [SUCCESS] 检测到本地Maven和Java环境，将使用本地构建
    set USE_LOCAL_BUILD=true
) else (
    echo [WARNING] 未检测到Maven或Java环境，将使用Docker内构建
    echo [INFO] 这是正常的，Docker会自动处理所有依赖
    set USE_LOCAL_BUILD=false
)

echo [SUCCESS] 系统要求检查通过

REM 构建应用
echo.
if "%USE_LOCAL_BUILD%"=="true" (
    echo [INFO] 使用本地环境构建应用...

    echo [INFO] 执行 Maven 清理和编译...
    call mvn clean compile -q
    if errorlevel 1 (
        echo [ERROR] Maven 编译失败
        pause
        exit /b 1
    )

    REM 跳过测试（可选）
    REM echo [INFO] 运行单元测试...
    REM call mvn test -q
    REM if errorlevel 1 (
    REM     echo [ERROR] 单元测试失败
    REM     pause
    REM     exit /b 1
    REM )

    echo [INFO] 打包应用...
    call mvn package -DskipTests -q
    if errorlevel 1 (
        echo [ERROR] 应用打包失败
        pause
        exit /b 1
    )

    REM 检查jar文件是否生成
    if not exist "target\api-premium-gateway-*.jar" (
        echo [ERROR] 应用打包失败，未找到jar文件
        pause
        exit /b 1
    )

    echo [SUCCESS] 本地应用构建完成
) else (
    echo [INFO] 将在Docker容器内构建应用...
    echo [INFO] Docker会自动下载Maven、Java等所有依赖
    echo [SUCCESS] 准备使用Docker多阶段构建
)

REM 重置数据库
if "%RESET_DB%"=="true" (
    echo.
    echo [WARNING] 重置数据库：删除所有数据并重新初始化...
    
    REM 停止服务
    if exist "%COMPOSE_FILE%" (
        docker-compose -f "%COMPOSE_FILE%" down --remove-orphans >nul 2>&1
    )
    
    REM 删除数据库卷
    docker volume rm api-premium-gateway_postgres_data >nul 2>&1
    
    echo [SUCCESS] 数据库已重置
)

REM 停止现有容器
echo.
echo [INFO] 停止现有容器...
if exist "%COMPOSE_FILE%" (
    docker-compose -f "%COMPOSE_FILE%" down --remove-orphans >nul 2>&1
)

REM 清理镜像
if "%CLEAN_BUILD%"=="true" (
    echo [INFO] 清理Docker镜像缓存...
    docker image prune -f >nul 2>&1
    REM 删除应用镜像强制重新构建
    docker rmi api-premium-gateway_api-gateway >nul 2>&1
) else (
    docker image prune -f >nul 2>&1
)

echo [SUCCESS] 现有容器已停止

REM 启动服务
echo.
echo [INFO] 启动 %PROJECT_NAME% 服务...
if "%CLEAN_BUILD%"=="true" (
    echo [INFO] 强制重新构建Docker镜像...
    docker-compose -f "%COMPOSE_FILE%" up --build --force-recreate -d
) else (
    docker-compose -f "%COMPOSE_FILE%" up --build -d
)
if errorlevel 1 (
    echo [ERROR] 服务启动失败
    pause
    exit /b 1
)

echo [SUCCESS] 服务启动完成

REM 等待服务就绪
echo.
echo [INFO] 等待服务启动...

REM 等待数据库就绪
echo [INFO] 等待数据库启动...
set timeout=60
:wait_db
docker-compose -f "%COMPOSE_FILE%" exec -T postgres pg_isready -U gateway_user -d api_gateway >nul 2>&1
if not errorlevel 1 (
    echo [SUCCESS] 数据库已就绪
    goto db_ready
)
timeout /t 2 /nobreak >nul
set /a timeout-=2
if %timeout% gtr 0 goto wait_db

echo [ERROR] 数据库启动超时
docker-compose -f "%COMPOSE_FILE%" logs --tail=50 postgres
pause
exit /b 1

:db_ready

REM 等待应用就绪
echo [INFO] 等待应用启动...
set timeout=120
:wait_app
curl -f http://localhost:8081/api/health >nul 2>&1
if not errorlevel 1 (
    echo [SUCCESS] 应用已就绪
    goto app_ready
)
timeout /t 3 /nobreak >nul
set /a timeout-=3
if %timeout% gtr 0 goto wait_app

echo [ERROR] 应用启动超时
docker-compose -f "%COMPOSE_FILE%" logs --tail=50 api-gateway
pause
exit /b 1

:app_ready

REM 显示服务状态
echo.
echo [INFO] 服务状态：
docker-compose -f "%COMPOSE_FILE%" ps

echo.
echo [INFO] 服务访问地址：
echo   - 应用地址: http://localhost:8081/api
echo   - 健康检查: http://localhost:8081/api/health
echo   - 数据库: localhost:5433

echo.
echo [INFO] 查看日志命令：
echo   - 应用日志: docker-compose -f %COMPOSE_FILE% logs -f api-gateway
echo   - 数据库日志: docker-compose -f %COMPOSE_FILE% logs -f postgres
echo   - 所有日志: docker-compose -f %COMPOSE_FILE% logs -f

echo.
echo [SUCCESS] %PROJECT_NAME% 启动成功！
echo.
echo 使用 'bin\stop.bat' 停止服务
echo 使用 'bin\logs.bat' 查看日志

pause
exit /b 0

:show_help
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo   --reset-db       重置数据库（删除所有数据并重新初始化）
echo   --clean-build    强制重新构建Docker镜像
echo   -h, --help       显示此帮助信息
echo.
echo 示例:
echo   %~nx0                    # 正常启动
echo   %~nx0 --reset-db         # 重置数据库并启动
echo   %~nx0 --clean-build      # 强制重新构建并启动
echo   %~nx0 --reset-db --clean-build  # 完全重置并启动
echo.
echo 说明:
echo   --reset-db: 删除数据库持久化数据，重新执行初始化脚本
echo   --clean-build: 删除Docker镜像缓存，确保使用最新代码构建
pause
exit /b 0 