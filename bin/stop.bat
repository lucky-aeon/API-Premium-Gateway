@echo off
setlocal enabledelayedexpansion

REM API Premium Gateway 停止脚本 (Windows)
REM 用于停止所有相关的Docker容器和服务

REM 项目信息
set PROJECT_NAME=API Premium Gateway
set COMPOSE_FILE=docker-compose.app.yml

REM 处理参数
set CLEANUP=false
if "%1"=="--cleanup" set CLEANUP=true
if "%1"=="-c" set CLEANUP=true
if "%1"=="--help" goto show_help
if "%1"=="-h" goto show_help

echo ========================================
echo     %PROJECT_NAME% 停止脚本
echo ========================================
echo.

REM 停止服务
echo [INFO] 停止 %PROJECT_NAME% 服务...

if exist "%COMPOSE_FILE%" (
    docker-compose -f "%COMPOSE_FILE%" down --remove-orphans
    if errorlevel 1 (
        echo [ERROR] 停止服务失败
        pause
        exit /b 1
    )
    echo [SUCCESS] 服务已停止
) else (
    echo [WARNING] 未找到 %COMPOSE_FILE% 文件
)

REM 清理资源（如果指定）
if "%CLEANUP%"=="true" (
    echo.
    echo [INFO] 清理Docker资源...
    
    REM 清理悬空镜像
    docker image prune -f >nul 2>&1
    
    REM 清理未使用的网络
    docker network prune -f >nul 2>&1
    
    echo [SUCCESS] 资源清理完成
)

echo.
echo [SUCCESS] %PROJECT_NAME% 已停止！

if "%CLEANUP%"=="false" (
    echo.
    echo [INFO] 提示: 使用 '%~nx0 --cleanup' 可以同时清理Docker资源
)

pause
exit /b 0

:show_help
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo   -c, --cleanup    停止服务后清理Docker资源
echo   -h, --help       显示此帮助信息
echo.
echo 示例:
echo   %~nx0               # 仅停止服务
echo   %~nx0 --cleanup     # 停止服务并清理资源
pause
exit /b 0 