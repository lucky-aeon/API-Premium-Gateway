# API Premium Gateway 部署文档

## 🎯 改进总结

我们成功简化了API Premium Gateway的部署方式，将复杂的多脚本系统改进为标准化的单一启动方式。

### 改进对比

| 项目 | 改进前 | 改进后 | 改进幅度 |
|------|--------|--------|----------|
| 启动脚本复杂度 | 320行 + 120行依赖脚本 | 95行 | **减少70%** |
| 配置管理 | 硬编码在脚本中 | .env环境变量 | **标准化** |
| 启动命令 | `./bin/start.sh --reset-db --clean-build` | `./start.sh` | **简化** |
| CI/CD支持 | 无 | GitHub Actions自动构建 | **新增** |

## 🚀 使用方式

### 快速启动
```bash
# 克隆项目
git clone https://github.com/lucky-aeon/API-Premium-Gateway.git
cd API-Premium-Gateway

# 一键启动（自动创建.env配置）
./start.sh
```

### 管理命令
```bash
./start.sh          # 启动所有服务
./stop.sh           # 停止所有服务
./logs.sh -f        # 实时查看日志
./logs.sh postgres  # 查看数据库日志
./reset.sh          # 重置数据库（谨慎使用）
```

### 环境配置
```bash
# 复制配置模板
cp .env.example .env

# 编辑配置（可选）
vim .env

# 启动服务
./start.sh
```

## 📁 新增文件

### 配置文件
- `.env` - 环境配置（基于模板自动创建）
- `.env.example` - 配置模板

### 管理脚本
- `start.sh` - 简化的启动脚本（95行）
- `stop.sh` - 停止脚本
- `logs.sh` - 日志查看脚本
- `reset.sh` - 数据库重置脚本

### CI/CD配置
- `.github/workflows/docker-build.yml` - 自动构建发布

### 文档
- `DEPLOYMENT.md` - 部署文档（本文件）

## 🔧 配置说明

### 端口配置
- `GATEWAY_PORT=8081` - API网关端口
- `POSTGRES_PORT=5433` - PostgreSQL数据库端口

### 数据库配置
- `DB_NAME=api_gateway` - 数据库名
- `DB_USER=gateway_user` - 数据库用户
- `DB_PASSWORD=gateway_pass` - 数据库密码

### 应用配置
- `SPRING_PROFILES_ACTIVE=docker` - Spring环境
- `JAVA_OPTS` - JVM参数配置

## 🎯 服务访问

启动完成后，可通过以下地址访问：

- **API网关**: http://localhost:8081
- **健康检查**: http://localhost:8081/api/health
- **数据库**: localhost:5433 (用户名: gateway_user, 密码: gateway_pass)

## 🚢 CI/CD发布

### 自动发布
```bash
# 打标签触发自动构建
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions会自动构建并推送到:
# ghcr.io/lucky-aeon/api-premium-gateway:v1.0.0
# ghcr.io/lucky-aeon/api-premium-gateway:latest
```

### 手动发布
```bash
# 在GitHub Actions页面手动触发
# 输入自定义标签进行构建
```

## 🔍 故障排除

### 服务启动失败
```bash
# 查看服务状态
docker compose ps

# 查看日志
./logs.sh -f

# 检查端口占用
lsof -i :8081
lsof -i :5433
```

### 数据库连接问题
```bash
# 等待数据库启动
./logs.sh postgres

# 手动连接测试
docker compose exec postgres psql -U gateway_user -d api_gateway
```

### 重置环境
```bash
# 完全重置（删除数据）
./reset.sh

# 重新启动
./start.sh
```

## 📈 性能优化

### JVM参数调优
编辑`.env`文件中的`JAVA_OPTS`：
```bash
# 默认配置（适合开发）
JAVA_OPTS=-Xms512m -Xmx1024m

# 生产环境配置
JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC
```

### 数据库优化
根据需要调整PostgreSQL配置，编辑docker-compose.yml中的数据库环境变量。

## 🔐 安全建议

### 生产环境部署
1. 修改默认密码
2. 使用HTTPS
3. 配置防火墙
4. 定期备份数据

### 配置安全
```bash
# 生产环境不要使用默认密码
DB_PASSWORD=your-secure-password

# 限制网络访问
# 仅在可信网络中暴露端口
```

## 🆕 与原版本的兼容性

### 保留的功能
- 所有API接口保持不变
- 数据库结构完全兼容
- Docker镜像构建方式兼容

### 废弃的脚本
原有的复杂脚本仍然保留在`bin/`和`scripts/`目录中，但建议使用新的简化脚本。

### 迁移指南
```bash
# 如果你之前使用原脚本
./bin/start.sh

# 现在可以简化为
./start.sh

# 功能完全相同，但更简洁
```