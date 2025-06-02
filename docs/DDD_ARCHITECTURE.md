# API-Premium Gateway DDD架构设计

## 📚 DDD分层架构概览

本项目采用领域驱动设计（Domain-Driven Design, DDD）的经典四层架构模式：

```
src/main/java/org/xhy/gateway/
├── interfaces/          # 接口层 (User Interface Layer)
│   └── web/            # Web控制器
├── application/         # 应用层 (Application Layer)  
│   ├── service/        # 应用服务
│   └── dto/            # 数据传输对象
├── domain/             # 领域层 (Domain Layer)
│   ├── project/        # 项目领域
│   │   ├── entity/     # 实体
│   │   ├── repository/ # 仓储接口
│   │   └── service/    # 领域服务
│   ├── apiinstance/    # API实例领域
│   │   ├── entity/     # 实体
│   │   ├── repository/ # 仓储接口
│   │   └── service/    # 领域服务
│   └── metrics/        # 指标领域
│       ├── entity/     # 实体
│       ├── repository/ # 仓储接口
│       └── service/    # 领域服务
└── infrastructure/     # 基础设施层 (Infrastructure Layer)
    ├── config/         # 配置
    └── persistence/    # 持久化实现
```

## 🏗️ 各层职责详解

### 1. 接口层 (Interfaces Layer)
**位置**: `org.xhy.gateway.interfaces`

**职责**:
- 处理外部请求（HTTP、MQ等）
- 数据格式转换（DTO ↔ Domain Object）
- 请求验证和响应格式化
- 调用应用层服务

**包含组件**:
- `web/` - REST控制器
- 未来可扩展：`rpc/`、`mq/` 等

### 2. 应用层 (Application Layer)
**位置**: `org.xhy.gateway.application`

**职责**:
- 编排业务流程（Use Case）
- 事务管理
- 安全控制
- 调用领域服务和仓储

**包含组件**:
- `service/` - 应用服务（Use Case实现）
- `dto/` - 数据传输对象

### 3. 领域层 (Domain Layer) ⭐
**位置**: `org.xhy.gateway.domain`

**职责**:
- 核心业务逻辑
- 领域实体和值对象
- 领域服务
- 仓储接口定义

**三个主要领域**:

#### 3.1 项目领域 (Project Domain)
```java
org.xhy.gateway.domain.project/
├── entity/
│   ├── ProjectEntity.java      # 项目实体
│   └── ProjectStatus.java      # 项目状态枚举
├── repository/
│   └── ProjectRepository.java  # 项目仓储接口
└── service/
    └── ProjectDomainService.java  # 项目领域服务
```

#### 3.2 API实例领域 (ApiInstance Domain)
```java
org.xhy.gateway.domain.apiinstance/
├── entity/
│   ├── ApiInstanceEntity.java     # API实例实体
│   ├── ApiType.java              # API类型枚举
│   └── ApiInstanceStatus.java    # API实例状态枚举
├── repository/
│   └── ApiInstanceRepository.java  # API实例仓储接口
└── service/
    └── ApiInstanceDomainService.java  # API实例领域服务
```

#### 3.3 指标领域 (Metrics Domain)
```java
org.xhy.gateway.domain.metrics/
├── entity/
│   ├── ApiInstanceMetricsEntity.java  # API实例指标实体
│   └── GatewayStatus.java            # Gateway状态枚举
├── repository/
│   └── MetricsRepository.java  # 指标仓储接口
└── service/
    └── MetricsDomainService.java  # 指标领域服务
```

### 4. 基础设施层 (Infrastructure Layer)
**位置**: `org.xhy.gateway.infrastructure`

**职责**:
- 技术实现细节
- 外部系统集成
- 仓储接口的具体实现

**包含组件**:
- `config/` - 框架配置（MyBatis、Spring等）
- `persistence/` - 数据持久化实现

## 🎯 DDD核心概念在项目中的体现

### 实体 (Entity)
- `ProjectEntity` - 项目聚合根
- `ApiInstanceEntity` - API实例聚合根  
- `ApiInstanceMetricsEntity` - 指标聚合根

### 值对象 (Value Object)
- `ProjectStatus` - 项目状态
- `ApiType` - API类型
- `ApiInstanceStatus` - API实例状态
- `GatewayStatus` - Gateway状态

### 聚合 (Aggregate)
每个领域都是一个聚合，有明确的边界：
- **项目聚合**: 管理项目信息和API Key
- **API实例聚合**: 管理API实例注册和路由参数
- **指标聚合**: 管理性能指标和健康状态

### 仓储 (Repository)
- 接口定义在领域层（`domain/*/repository/`）
- 具体实现在基础设施层（使用MyBatis Plus）

### 领域服务 (Domain Service)
处理跨实体的复杂业务逻辑：
- 项目验证和API Key生成
- API实例选择算法
- 指标聚合和健康评估

## 🔄 数据流转模式

```
HTTP请求 → 接口层 → 应用层 → 领域层 → 基础设施层
         ↓        ↓        ↓        ↓
    Controller → AppService → Domain → Repository
                          ↓
                    Entity/Value Object
```

## 📋 开发规范

### 依赖规则
- **接口层** 只能依赖 **应用层**
- **应用层** 只能依赖 **领域层**
- **基础设施层** 实现 **领域层** 的接口
- **领域层** 不依赖任何外层

### 命名规范
- **Entity**: 名词 + "Entity"后缀，如 `ProjectEntity`、`ApiInstanceEntity`
- **Value Object**: 纯名词，如 `ProjectStatus`、`ApiType`
- **Repository**: `{Entity}Repository`，如 `ProjectRepository`
- **Domain Service**: `{Entity}DomainService`，如 `ProjectDomainService`
- **Application Service**: `{UseCase}AppService`，如 `ProjectManagementAppService`
- **Controller**: `{Resource}Controller`，如 `ProjectController`

### 事务边界
- 一个聚合 = 一个事务边界
- 跨聚合操作通过应用层协调
- 最终一致性通过领域事件实现

## 🚀 未来扩展点

1. **领域事件**: 实现聚合间的解耦通信
2. **CQRS**: 读写分离提升性能
3. **事件溯源**: 完整的状态变更历史
4. **微服务拆分**: 按聚合边界拆分服务

---

这种DDD架构确保了：
- ✅ 业务逻辑集中在领域层
- ✅ 高内聚、低耦合
- ✅ 可测试性强
- ✅ 易于维护和扩展 