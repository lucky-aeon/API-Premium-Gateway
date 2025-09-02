# API Premium Gateway

[![Build and Push Docker Image](https://github.com/lucky-aeon/API-Premium-Gateway/actions/workflows/docker-build.yml/badge.svg)](https://github.com/lucky-aeon/API-Premium-Gateway/actions/workflows/docker-build.yml)

## 🚀 快速开始

### 一键启动（推荐）
```bash
docker run -d \
  --name api-premium-gateway \
  -p 8081:8081 \
  -v gateway_data:/var/lib/postgresql/data \
  -v gateway_logs:/app/logs \
  ghcr.nju.edu.cn/lucky-aeon/api-premium-gateway:latest
````


### 开发模式
```bash
# 克隆项目
git clone https://github.com/lucky-aeon/API-Premium-Gateway.git
cd API-Premium-Gateway

# 启动开发环境
docker compose up -d --build
```

### 服务访问
- **API网关**: http://localhost:8081
- **健康检查**: http://localhost:8081/api/health
- **数据库**: localhost:5433 (用户名: gateway_user, 密码: gateway_pass)

### 常用命令
```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f

# 停止服务
docker compose down

# 重启服务
docker compose restart

# 重置数据库（会删除所有数据）
docker compose down
docker volume rm api-premium-gateway_postgres_data
docker compose up -d --build
```

-----

## 项目的由来

该项目由 AgentX 项目引入，因需要对模型的选择高可用。

## 🚀 项目概览

**API-Premium Gateway** 是一个轻量级、自研的 Java 服务，旨在为您的分布式系统和应用提供**强大的 API 高可用与智能调度能力**。它作为您服务调用各类后端 API（包括第三方服务、自建微服务、AI 模型等）的**智能中间层**，确保您的业务在面对外部API波动时依然稳定可靠。

**核心解决问题：**

  * **API 不可用：** 某个第三方 API 或自建服务宕机了怎么办？
  * **性能瓶颈：** 某个 API 实例响应慢或负载高，如何自动切换？
  * **多租户管理：** 不同项目或用户如何隔离地使用和管理 API 资源？
  * **调用可观测性：** 如何实时了解后端 API 的健康状况和性能表现？

-----

## ✨ 核心特性

  * **⚡️ API 高可用：**
      * **平替（Fallback）：** 当首选 API 实例失效时，自动无缝切换到其他可用实例。
      * **降级（Degradation）：** 在高优先级API不可用时，可选择切换到功能受限但更稳定的备用API。
      * **熔断（Circuit Breaker）：** 自动隔离故障API，防止雪崩效应。
      * **限流（Rate Limiting）：** 保护后端API不被过量请求压垮。
  * **🧠 智能调度与负载均衡：**
      * 根据API实例的**实时性能指标**（成功率、延迟）、**成本**、**优先级**等，动态选择最佳的调用路径。
  * **🔐 安全与隔离：**
      * 支持**多项目、多用户**的API资源隔离管理。
      * 提供统一的**API Key/Token 鉴权**机制，确保接口安全。
  * **📊 调用可观测性：**
      * 实时**收集并分析**API调用结果（成功/失败、延迟、Token使用等），为决策和监控提供数据支撑。
  * **🛠️ 简洁高效：**
      * 基于 Spring Boot 自研，不依赖复杂网关框架，专注于核心功能，保持轻量级和高性能。
      * 通过**SDK**简化上游服务集成。

-----

## 🚀 工作原理

API-Premium Gateway 扮演着**智能决策者**与**状态收集者**的角色。它不直接代理实际的 API 请求，而是提供一套机制，让上游服务：

1.  **注册 API 实例：** 上游服务启动时，通过 Gateway SDK 上报自身可用的后端 API 实例（如 OpenAI 的 GPT-4o、自建的图片生成服务等），并附带其 `actualEndpoint`、`providerInfo`、`优先级`、`成本` 等元数据。
2.  **请求智能选择：** 当上游服务需要调用某个逻辑 API（如 `gpt4o`）时，它首先向 Gateway 发送请求，Gateway 会根据内部算法（高可用、负载均衡、熔断等）选择当前**最佳**或**可用**的后端 API 实例。
3.  **上游服务执行调用：** Gateway 返回选定的 API 实例的详细信息（如 `actualEndpoint`、`businessId`、`providerInfo`）。**上游服务拿到这些信息后，自行发起对后端 API 的实际调用。**
4.  **上报调用结果：** 无论调用成功或失败，上游服务都必须将结果（成功/失败、延迟、错误信息等）上报给 Gateway。Gateway 利用这些数据实时更新后端 API 实例的健康状况和性能指标，为下一次决策提供依据。

-----

## 📐 架构概览

![mermaid-diagram-2025-06-02-170627.png](docs/images/mermaid-diagram-2025-06-02-170627.png)

-----

## 🛠️ 技术栈

  * **后端框架：** Spring Boot 3.2.0
  * **Java 版本：** Java 17
  * **数据存储：** PostgreSQL 15
  * **ORM 框架：** MyBatis Plus
  * **容器化：** Docker + Docker Compose
  * **架构模式：** DDD (领域驱动设计)

-----

## 📦 部署方式

### 方式1：Docker 镜像（推荐）

**最简单的部署方式 - 一条命令启动完整服务（应用+数据库）**

```bash
# 拉取并运行最新镜像
docker run -d \
  --name api-premium-gateway \
  -p 8081:8081 \
  -v gateway_data:/var/lib/postgresql/data \
  -v gateway_logs:/app/logs \
  ghcr.io/lucky-aeon/api-premium-gateway:latest

# 等待约1-2分钟启动完成，然后访问
curl http://localhost:8081/api/health
```

**🎯 一体化镜像特性：**
- ✅ **零配置部署**：应用 + PostgreSQL 数据库打包在同一镜像中
- ✅ **自动初始化**：首次启动自动创建数据库表结构
- ✅ **数据持久化**：使用 Docker Volume 保存数据，重启不丢失
- ✅ **多架构支持**：支持 AMD64 和 ARM64 架构
- ✅ **健康检查**：内置应用和数据库健康检查

**🔗 访问地址：**
- **应用首页**：http://localhost:8081/api
- **健康检查**：http://localhost:8081/api/health
- **管理接口**：http://localhost:8081/api/admin/

### 方式2：Docker Compose（开发推荐）

如果您需要进行开发或自定义配置：

```bash
# 克隆项目
git clone https://github.com/lucky-aeon/API-Premium-Gateway
cd API-Premium-Gateway

# 启动服务
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f

# 停止服务
docker compose down
```

**启动后的服务信息：**
- **应用地址**: http://localhost:8081/api
- **健康检查**: http://localhost:8081/api/health
- **数据库**: localhost:5433
  - 数据库名: `api_gateway`
  - 用户名: `gateway_user`
  - 密码: `gateway_pass`
  - JDBC URL: `jdbc:postgresql://localhost:5433/api_gateway`

### 方式3：本地开发

如果您需要在本地开发环境中运行项目：

```bash
# 启动数据库
docker compose up -d postgres

# 本地运行应用
mvn spring-boot:run

# 或者构建后运行
mvn clean package -DskipTests
java -jar target/api-premium-gateway-*.jar
```

**开发环境配置：**
- 应用端口：8080
- 数据库端口：5433
- 配置文件：`application.yml`

-----

## 🔧 管理命令

### Docker 镜像管理
```bash
# 拉取最新版本
docker pull ghcr.io/lucky-aeon/api-premium-gateway:latest

# 拉取指定版本
docker pull ghcr.io/lucky-aeon/api-premium-gateway:v1.0.0

# 查看本地镜像版本
docker images | grep api-premium-gateway

# 启动容器
docker run -d \
  --name api-premium-gateway \
  -p 8081:8081 \
  -v gateway_data:/var/lib/postgresql/data \
  -v gateway_logs:/app/logs \
  ghcr.io/lucky-aeon/api-premium-gateway:latest

# 查看容器日志
docker logs api-premium-gateway

# 停止容器
docker stop api-premium-gateway

# 重启容器
docker restart api-premium-gateway

# 删除容器和数据（危险操作）
docker stop api-premium-gateway
docker rm api-premium-gateway
docker volume rm gateway_data gateway_logs
```

### Docker Compose 管理
```bash
# 查看服务状态
docker compose ps

# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f api-gateway
docker compose logs -f postgres

# 重启特定服务
docker compose restart api-gateway

# 重启所有服务
docker compose restart

# 停止所有服务
docker compose down

# 强制重新构建
docker compose build --no-cache
docker compose up -d
```

### 数据库管理
```bash
# 连接数据库
docker exec -it api-premium-gateway-postgres psql -U gateway_user -d api_gateway

# 备份数据库
docker exec api-premium-gateway-postgres pg_dump -U gateway_user api_gateway > backup.sql

# 重置数据库（会删除所有数据）
docker compose down
docker volume rm api-premium-gateway_postgres_data
docker compose up -d --build
```

-----

## 🔧 故障排查

### 常见问题

#### 1. 端口被占用
```bash
# 检查端口占用情况
lsof -i :8081  # 应用端口
lsof -i :5433  # 数据库端口

# 或者使用 netstat
netstat -tulpn | grep :8081
netstat -tulpn | grep :5433
```

#### 2. 容器启动失败
```bash
# 查看容器状态
docker compose ps

# 查看详细启动日志
docker compose logs api-gateway
docker compose logs postgres

# 重新构建镜像
docker compose down
docker compose build --no-cache
docker compose up -d
```

#### 3. 数据库连接问题
```bash
# 重置数据库
docker compose down
docker volume rm api-premium-gateway_postgres_data
docker compose up -d --build

# 检查数据库连接
docker exec -it api-premium-gateway-postgres psql -U gateway_user -d api_gateway
```

#### 4. 应用健康检查失败
```bash
# 检查应用日志
docker compose logs api-gateway

# 检查健康状态
curl -v http://localhost:8081/api/health

# 检查服务端口
docker compose ps
```

-----

## 🎯 上游服务集成 (Java SDK)

1.  **添加 SDK 依赖：**
    ```xml
    <dependency>
        <groupId>com.your-org</groupId>
        <artifactId>api-premium-gateway-sdk</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    ```

2.  **配置 SDK：**
    在您的 Spring Boot 应用中配置 Gateway 的基础 URL 和 API Key。

3.  **注册 API 实例：**
    ```java
    // 示例：注册一个模型API实例
    gatewaySdkClient.registerApi(
        "my-project-id",
        "user-id-optional",
        "gpt4o-model",
        "model",
        "my-gpt4o-business-id-001",
        "https://api.openai.com/v1/chat/completions",
        Map.of("provider", "OpenAI", "version", "gpt-4o"),
        Map.of("priority", 100, "costPerToken", 0.000015)
    );
    ```

4.  **选择并调用 API：**
    ```java
    // 1. 请求 Gateway 选择最佳 API
    SelectedApiInstance selectedApi = gatewaySdkClient.selectApi(
        "my-project-id",
        "user-id-optional",
        "gpt4o-model",
        "model"
    );

    // 2. 根据 Gateway 返回信息，执行实际调用
    // ... 使用 selectedApi.getActualEndpoint() 和 selectedApi.getProviderInfo()

    // 3. 上报调用结果
    gatewaySdkClient.reportApiResult(
        "my-project-id",
        "user-id-optional",
        "gpt4o-model",
        selectedApi.getBusinessId(),
        true, // success
        150L, // latencyMs
        null, // errorMessage
        null, // errorType
        Map.of("promptTokens", 100, "completionTokens", 200)
    );
    ```

-----

## 🤝 贡献

我们欢迎所有形式的贡献！如果您有任何问题、建议或发现了 Bug，请随时提交 Issue 或 Pull Request。

-----

## 📄 许可证

本项目采用 [MIT 许可证](LICENSE) 发布。
