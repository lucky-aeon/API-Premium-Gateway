# API Premium Gateway 启动脚本

本目录包含用于构建、启动和管理 API Premium Gateway 项目的脚本。

## 系统要求

### 必需软件
- **Docker**: 用于容器化部署
- **Docker Compose**: 用于多容器编排
- **Java 17+**: 用于编译和运行应用
- **Maven**: 用于项目构建

### Mac/Linux 系统
- Bash shell
- curl (用于健康检查)

### Windows 系统
- Windows 10/11
- PowerShell 或 Command Prompt
- curl (通常已内置)

## 脚本说明

### 启动脚本

#### Mac/Linux: `start.sh`
```bash
./bin/start.sh                    # 正常启动
./bin/start.sh --reset-db         # 重置数据库并启动
./bin/start.sh --clean-build      # 强制重新构建并启动
./bin/start.sh --reset-db --clean-build  # 完全重置并启动
```

#### Windows: `start.bat`
```cmd
bin\start.bat                    # 正常启动
bin\start.bat --reset-db         # 重置数据库并启动
bin\start.bat --clean-build      # 强制重新构建并启动
bin\start.bat --reset-db --clean-build  # 完全重置并启动
```

**功能：**
- 检查系统依赖（智能检测本地环境）
- 构建应用（本地或Docker内构建，**跳过测试**）
- 可选：重置数据库（删除持久化数据）
- 可选：强制重新构建镜像
- 启动 PostgreSQL 数据库
- 启动 API Gateway 应用
- 等待服务就绪
- 显示服务状态和访问地址

**参数说明：**
- `--reset-db`: 删除数据库持久化数据，重新执行初始化脚本
- `--clean-build`: 删除Docker镜像缓存，确保使用最新代码构建

**构建优化：**
- 启动时跳过单元测试，提高启动速度
- 使用多阶段Docker构建，优化镜像大小
- 智能检测本地环境，优先使用本地构建

### 停止脚本

#### Mac/Linux: `stop.sh`
```bash
./bin/stop.sh                # 仅停止服务
./bin/stop.sh --cleanup      # 停止服务并清理Docker资源
```

#### Windows: `stop.bat`
```cmd
bin\stop.bat                 # 仅停止服务
bin\stop.bat --cleanup       # 停止服务并清理Docker资源
```

**功能：**
- 停止所有运行中的容器
- 移除容器和网络
- 可选：清理Docker镜像和网络

### 日志查看脚本

#### Mac/Linux: `logs.sh`
```bash
./bin/logs.sh                    # 查看应用日志（最后100行）
./bin/logs.sh -f                 # 实时跟踪应用日志
./bin/logs.sh postgres           # 查看数据库日志
./bin/logs.sh all -f             # 实时跟踪所有服务日志
./bin/logs.sh api-gateway -t 50  # 查看应用最后50行日志
```

#### Windows: `logs.bat`
```cmd
bin\logs.bat                    # 查看应用日志（最后100行）
bin\logs.bat -f                 # 实时跟踪应用日志
bin\logs.bat postgres           # 查看数据库日志
bin\logs.bat all -f             # 实时跟踪所有服务日志
bin\logs.bat api-gateway -t 50  # 查看应用最后50行日志
```

**功能：**
- 查看指定服务的日志
- 支持实时跟踪
- 可指定显示行数

## 服务信息

### 启动后的服务访问地址

- **应用地址**: http://localhost:8081/api
- **健康检查**: http://localhost:8081/api/health
- **数据库**: localhost:5433
  - 数据库名: `api_gateway`
  - 用户名: `gateway_user`
  - 密码: `gateway_pass`

### Docker 容器

- **api-gateway-app**: API Gateway 应用容器
- **api-gateway-postgres**: PostgreSQL 数据库容器

## 使用流程

### 首次启动
1. 确保已安装所有必需软件
2. 运行启动脚本
3. 等待服务启动完成
4. 访问健康检查接口验证服务状态

### 日常使用
```bash
# Mac/Linux
./bin/start.sh      # 启动服务
./bin/logs.sh -f    # 查看日志
./bin/stop.sh       # 停止服务

# Windows
bin\start.bat       # 启动服务
bin\logs.bat -f     # 查看日志
bin\stop.bat        # 停止服务
```

### 故障排查
```bash
# 查看应用日志
./bin/logs.sh api-gateway

# 查看数据库日志
./bin/logs.sh postgres

# 查看所有服务状态
docker-compose -f docker-compose.app.yml ps

# 重新构建并启动
./bin/stop.sh --cleanup
./bin/start.sh
```

## 数据库初始化机制

### 数据持久化
- 数据库数据存储在Docker卷 `postgres_data` 中
- 初始化脚本位于 `docs/sql/` 目录
- **首次启动**: 自动执行初始化脚本创建表结构
- **后续启动**: 数据库数据保持不变，不会重复初始化

### 何时需要重置数据库
- 修改了数据库表结构（`docs/sql/sql.sql`）
- 需要重新导入初始化数据
- 数据库出现问题需要重新初始化

### 重置方法
```bash
# 重置数据库并启动
./bin/start.sh --reset-db

# 完全重置（数据库 + 镜像）
./bin/start.sh --reset-db --clean-build
```

## 注意事项

1. **端口占用**: 确保端口 8081 和 5433 未被其他服务占用
2. **磁盘空间**: 确保有足够的磁盘空间用于Docker镜像和数据
3. **网络连接**: 首次运行需要下载Docker镜像，确保网络连接正常
4. **权限问题**: 在某些系统上可能需要管理员权限运行Docker命令
5. **数据安全**: 使用 `--reset-db` 会删除所有数据库数据，请谨慎使用

## 开发模式

如果需要在开发模式下运行（不使用Docker），可以：

1. 启动数据库：
   ```bash
   docker-compose -f docker-compose.yml up -d postgres
   ```

2. 本地运行应用：
   ```bash
   mvn spring-boot:run
   ```

## 故障排查指南

### 常见问题

1. **Docker 未启动**
   - 确保 Docker Desktop 正在运行

2. **端口被占用**
   - 检查端口 8081 和 5433 是否被其他服务占用
   - 使用 `netstat` 或 `lsof` 命令检查端口使用情况

3. **构建失败**
   - 检查 Java 和 Maven 版本
   - 清理 Maven 缓存：`mvn clean`

4. **数据库连接失败**
   - 检查数据库容器是否正常启动
   - 查看数据库日志：`./bin/logs.sh postgres`

5. **应用启动超时**
   - 检查系统资源使用情况
   - 增加启动超时时间（修改脚本中的 timeout 值）

### 获取帮助

如果遇到问题，可以：
1. 查看相关日志文件
2. 检查 Docker 容器状态
3. 查看项目文档
4. 联系开发团队 