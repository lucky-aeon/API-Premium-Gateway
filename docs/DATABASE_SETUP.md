# 数据库设置指南

本项目使用 Docker 运行 PostgreSQL 数据库，提供开箱即用的开发环境。

## 🚀 快速启动

### 方式一：使用根目录快速脚本

```bash
# 启动数据库
./start-db.sh

# 停止数据库
./stop-db.sh

# 检查数据库状态
./scripts/check-db.sh
```

### 方式二：使用详细脚本

```bash
# 启动数据库（包含详细日志）
./scripts/start-postgres.sh

# 停止数据库
./scripts/stop-postgres.sh

# 检查数据库健康状态
./scripts/check-db.sh
```

### 方式三：直接使用 Docker Compose

```bash
# 启动
docker-compose up -d postgres

# 停止
docker-compose down

# 查看状态
docker-compose ps
```

## 📋 数据库连接信息

| 配置项 | 值 |
|--------|-----|
| 主机 | localhost |
| 端口 | 5433 |
| 数据库名 | api_gateway |
| 用户名 | gateway_user |
| 密码 | gateway_pass |

**JDBC URL**: `jdbc:postgresql://localhost:5433/api_gateway`

## 🔧 功能特性

- ✅ 自动创建数据库和用户
- ✅ 自动执行 SQL 初始化脚本（`docs/sql/sql.sql`）
- ✅ 数据持久化存储（使用 Docker Volume）
- ✅ 健康检查
- ✅ 避免端口冲突（使用 5433 端口）
- ✅ 完整的错误检查和用户友好的输出

## 📁 数据持久化

数据存储在 Docker Volume `postgres_data` 中，即使容器删除，数据也不会丢失。

如需完全清理数据：
```bash
docker-compose down -v
```

## 🛠️ 故障排查

### Docker 未启动
```
❌ 错误: Docker 未运行，请先启动 Docker
```
**解决方案**: 启动 Docker Desktop 或 Docker 服务

### 端口被占用
如果 5433 端口被占用，可以修改 `docker-compose.yml` 中的端口映射：
```yaml
ports:
  - "5434:5432"  # 修改为其他端口
```

### 网络问题导致镜像下载失败
```bash
# 可以尝试使用国内镜像源
docker pull registry.cn-hangzhou.aliyuncs.com/library/postgres:15-alpine
docker tag registry.cn-hangzhou.aliyuncs.com/library/postgres:15-alpine postgres:15-alpine
```

### 查看容器日志
```bash
docker-compose logs postgres
```

### 连接数据库进行调试
```bash
docker-compose exec postgres psql -U gateway_user -d api_gateway
```

## 🔄 重置数据库

如果需要重置数据库到初始状态：

```bash
# 停止并删除容器和数据
docker-compose down -v

# 重新启动（会重新初始化）
./start-db.sh
```

## 📊 数据库表结构

项目包含以下主要数据表：

1. **projects** - 项目信息管理
2. **api_instance_registry** - API 实例注册表
3. **api_instance_metrics** - API 实例指标数据
4. **api_keys** - API Key 管理

详细表结构请查看：[docs/sql/sql.sql](sql/sql.sql)

## 📚 Spring Boot 配置

项目提供了完整的 Spring Boot 配置示例：[docs/application-dev.yml](application-dev.yml)

主要配置：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/api_gateway
    username: gateway_user
    password: gateway_pass
    driver-class-name: org.postgresql.Driver
```

## 🔗 相关文件

- `docker-compose.yml` - Docker Compose 配置
- `scripts/start-postgres.sh` - 详细启动脚本
- `scripts/stop-postgres.sh` - 停止脚本
- `scripts/check-db.sh` - 健康检查脚本
- `docs/sql/sql.sql` - 数据库初始化脚本
- `start-db.sh` / `stop-db.sh` - 快速启动/停止脚本
- `docs/application-dev.yml` - Spring Boot 配置示例 