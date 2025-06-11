@echo off
setlocal enabledelayedexpansion

REM API Premium Gateway 日志查看脚本 (Windows)
REM 用于查看Docker容器日志

REM 项目信息
set PROJECT_NAME=API Premium Gateway
set COMPOSE_FILE=docker-compose.app.yml

REM 默认参数
set SERVICE=api-gateway
set FOLLOW=false
set TAIL_LINES=100

REM 解析参数
:parse_args
if "%1"=="" goto main
if "%1"=="-h" goto show_help
if "%1"=="--help" goto show_help
if "%1"=="-f" (
    set FOLLOW=true
    shift
    goto parse_args
)
if "%1"=="--follow" (
    set FOLLOW=true
    shift
    goto parse_args
)
if "%1"=="-t" (
    set TAIL_LINES=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--tail" (
    set TAIL_LINES=%2
    shift
    shift
    goto parse_args
)
if "%1"=="api-gateway" (
    set SERVICE=api-gateway
    shift
    goto parse_args
)
if "%1"=="postgres" (
    set SERVICE=postgres
    shift
    goto parse_args
)
if "%1"=="all" (
    set SERVICE=all
    shift
    goto parse_args
)

echo [ERROR] 未知参数: %1
goto show_help

:main
echo ========================================
echo     %PROJECT_NAME% 日志查看
echo ========================================
echo.

REM 检查服务状态
if not exist "%COMPOSE_FILE%" (
    echo [ERROR] 未找到 %COMPOSE_FILE% 文件
    pause
    exit /b 1
)

REM 检查容器是否运行
docker-compose -f "%COMPOSE_FILE%" ps | findstr "Up" >nul
if errorlevel 1 (
    echo [ERROR] 没有运行中的服务，请先启动服务
    echo 使用 'bin\start.bat' 启动服务
    pause
    exit /b 1
)

REM 显示当前配置
echo [INFO] 服务: %SERVICE%
echo [INFO] 跟踪: %FOLLOW%
if "%FOLLOW%"=="false" (
    echo [INFO] 显示行数: %TAIL_LINES%
)
echo.

REM 构建命令
set CMD=docker-compose -f %COMPOSE_FILE% logs

REM 添加tail参数
if "%FOLLOW%"=="false" (
    set CMD=!CMD! --tail=%TAIL_LINES%
)

REM 添加follow参数
if "%FOLLOW%"=="true" (
    set CMD=!CMD! -f
)

REM 添加服务名
if not "%SERVICE%"=="all" (
    set CMD=!CMD! %SERVICE%
)

echo [INFO] 执行命令: !CMD!
echo.

REM 执行命令
!CMD!

pause
exit /b 0

:show_help
echo 用法: %~nx0 [服务名] [选项]
echo.
echo 服务名:
echo   api-gateway      查看应用日志（默认）
echo   postgres         查看数据库日志
echo   all              查看所有服务日志
echo.
echo 选项:
echo   -f, --follow     实时跟踪日志
echo   -t, --tail N     显示最后N行日志（默认100）
echo   -h, --help       显示此帮助信息
echo.
echo 示例:
echo   %~nx0                    # 查看应用日志（最后100行）
echo   %~nx0 -f                 # 实时跟踪应用日志
echo   %~nx0 postgres           # 查看数据库日志
echo   %~nx0 all -f             # 实时跟踪所有服务日志
echo   %~nx0 api-gateway -t 50  # 查看应用最后50行日志
pause
exit /b 0 