-- 开启 UUID 扩展，如果你的 PostgreSQL 实例尚未开启
-- 这不是必须的，因为我们使用 VARCHAR 存储 UUID，但如果未来想转为原生 UUID 类型，提前开启有益
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

---
-- Table: projects
-- Description: 存储使用 API-Premium Gateway 的项目信息，用于认证和资源隔离。
---
CREATE TABLE projects (
    id VARCHAR(36) PRIMARY KEY, -- 项目的唯一标识符 (UUID 字符串，由应用层生成)
    name VARCHAR(128) NOT NULL, -- 项目名称，必须唯一
    description TEXT, -- 项目的详细描述
    api_key VARCHAR(256) NOT NULL, -- 用于项目认证的 API Key，必须唯一且安全存储
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- 项目状态：ACTIVE (活跃), INACTIVE (非活跃)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), -- 记录创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT NOW() -- 记录最后更新时间，每次更新时自动修改
);

-- 添加表级别和列级别评论
COMMENT ON TABLE projects IS '管理 API-Premium Gateway 的项目信息，用于认证和多租户隔离';
COMMENT ON COLUMN projects.id IS '项目的唯一标识符 (UUID 字符串，由应用层生成)';
COMMENT ON COLUMN projects.name IS '项目名称，必须唯一';
COMMENT ON COLUMN projects.description IS '项目的详细描述';
COMMENT ON COLUMN projects.api_key IS '用于项目认证的 API Key，必须唯一且安全存储';
COMMENT ON COLUMN projects.status IS '项目状态：ACTIVE (活跃), INACTIVE (非活跃)';
COMMENT ON COLUMN projects.created_at IS '记录创建时间';
COMMENT ON COLUMN projects.updated_at IS '记录最后更新时间，每次更新时自动修改';

---
-- Table: api_instance_registry
-- Description: 存储所有注册到 Gateway 的后端 API 业务实例的元数据。Gateway 基于这些信息进行选择决策。
---
CREATE TABLE api_instance_registry (
    id VARCHAR(36) PRIMARY KEY, -- API 业务实例的唯一标识符 (UUID 字符串，由应用层生成)
    project_id VARCHAR(36) NOT NULL, -- 所属项目的 ID，外键关联 projects 表
    user_id VARCHAR(64), -- 所属用户 ID (可选)，用于用户级别的 API 资源隔离
    api_identifier VARCHAR(128) NOT NULL, -- API 的逻辑标识符，如 "gpt4o", "sms_sender"
    api_type VARCHAR(32) NOT NULL, -- API 的类型，如 "MODEL", "PAYMENT_GATEWAY", "NOTIFICATION_SERVICE"
    business_id VARCHAR(128) NOT NULL, -- 项目方内部用于识别此 API 实例的业务 ID，由 Gateway 返回给调用方
    routing_params JSONB DEFAULT '{}'::JSONB, -- 影响 Gateway 调度决策的实例级参数，JSONB 格式
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- API 实例的当前状态：ACTIVE (活跃), INACTIVE (非活跃), DEPRECATED (已弃用)
    metadata JSONB DEFAULT '{}'::JSONB, -- 额外扩展信息，JSONB 格式，供 Gateway 内部决策或未来扩展使用
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), -- 记录创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT NOW() -- 记录最后更新时间，每次更新时自动修改
);

-- 添加表级别和列级别评论
COMMENT ON TABLE api_instance_registry IS '存储注册到 Gateway 的后端 API 业务实例的元数据，用于智能调度决策';
COMMENT ON COLUMN api_instance_registry.id IS 'API 业务实例的唯一标识符 (UUID 字符串，由应用层生成)';
COMMENT ON COLUMN api_instance_registry.project_id IS '所属项目的 ID，外键关联 projects 表';
COMMENT ON COLUMN api_instance_registry.user_id IS '所属用户 ID (可选)，用于用户级别的 API 资源隔离';
COMMENT ON COLUMN api_instance_registry.api_identifier IS 'API 的逻辑标识符，如 "gpt4o", "sms_sender"';
COMMENT ON COLUMN api_instance_registry.api_type IS 'API 的类型，如 "MODEL", "PAYMENT_GATEWAY", "NOTIFICATION_SERVICE"';
COMMENT ON COLUMN api_instance_registry.business_id IS '项目方内部用于识别此 API 实例的业务 ID，由 Gateway 返回给调用方';
COMMENT ON COLUMN api_instance_registry.routing_params IS '影响 Gateway 调度决策的实例级参数，JSONB 格式。例如：{"priority": 100, "cost_per_unit": 0.0001, "initial_weight": 50}';
COMMENT ON COLUMN api_instance_registry.status IS 'API 实例的当前状态：ACTIVE (活跃), INACTIVE (非活跃), DEPRECATED (已弃用)';
COMMENT ON COLUMN api_instance_registry.metadata IS '额外扩展信息，JSONB 格式，供 Gateway 内部决策或未来扩展使用 (如功能特性、地域信息)';
COMMENT ON COLUMN api_instance_registry.created_at IS '记录创建时间';
COMMENT ON COLUMN api_instance_registry.updated_at IS '记录最后更新时间，每次更新时自动修改';

---
-- Table: api_instance_metrics
-- Description: 记录每个 API 实例在特定时间窗口内的实时和历史调用指标，是 Gateway 智能决策的核心数据来源。
---
CREATE TABLE api_instance_metrics (
    id VARCHAR(36) PRIMARY KEY, -- 指标记录的唯一标识符 (UUID 字符串，由应用层生成)
    registry_id VARCHAR(36) NOT NULL, -- 关联的 API 业务实例 ID，外键关联 api_instance_registry 表
    timestamp_window TIMESTAMP NOT NULL DEFAULT date_trunc('minute', NOW()), -- 指标统计的时间窗口起始点 1分钟内的请求监控
    success_count BIGINT NOT NULL DEFAULT 0, -- 该时间窗口内成功的 API 调用次数
    failure_count BIGINT NOT NULL DEFAULT 0, -- 该时间窗口内失败的 API 调用次数
    total_latency_ms BIGINT NOT NULL DEFAULT 0, -- 该时间窗口内所有 API 调用的总延迟（毫秒）
    concurrency INT NOT NULL DEFAULT 0, -- 该时间窗口内观察到的最大或当前活跃并发连接数
    current_gateway_status VARCHAR(32) NOT NULL DEFAULT 'HEALTHY', -- Gateway 根据内部逻辑判断的 API 实例状态
    last_reported_at TIMESTAMP NOT NULL DEFAULT NOW(), -- 最后一次上报数据到该指标记录的时间
    additional_metrics JSONB DEFAULT '{}'::JSONB -- 额外指标，JSONB 格式
);

-- 添加表级别和列级别评论
COMMENT ON TABLE api_instance_metrics IS '存储 API 实例的实时和历史调用指标，用于 Gateway 的高可用决策和智能调度';
COMMENT ON COLUMN api_instance_metrics.id IS '指标记录的唯一标识符 (UUID 字符串，由应用层生成)';
COMMENT ON COLUMN api_instance_metrics.registry_id IS '关联的 API 业务实例 ID，外键关联 api_instance_registry 表';
COMMENT ON COLUMN api_instance_metrics.timestamp_window IS '指标统计的时间窗口起始点 (YYYY-MM-DD HH:MM:00)，例如，记录从该时间点开始的 1 分钟内的聚合数据';
COMMENT ON COLUMN api_instance_metrics.success_count IS '该时间窗口内成功的 API 调用次数';
COMMENT ON COLUMN api_instance_metrics.failure_count IS '该时间窗口内失败的 API 调用次数';
COMMENT ON COLUMN api_instance_metrics.total_latency_ms IS '该时间窗口内所有 API 调用的总延迟（毫秒），用于计算平均延迟';
COMMENT ON COLUMN api_instance_metrics.concurrency IS '该时间窗口内观察到的最大或当前活跃并发连接数（由上报方提供，用于实时负载均衡）';
COMMENT ON COLUMN api_instance_metrics.current_gateway_status IS 'Gateway 根据内部逻辑判断的 API 实例状态：HEALTHY, DEGRADED, FAULTY, CIRCUIT_BREAKER_OPEN';
COMMENT ON COLUMN api_instance_metrics.last_reported_at IS '最后一次上报数据到该指标记录的时间';
COMMENT ON COLUMN api_instance_metrics.additional_metrics IS '额外指标，JSONB 格式 (例如：{"total_prompt_tokens": 12345, "total_completion_tokens": 67890, "total_cost": 0.123})';

---
-- Table: api_keys
-- Description: 独立存储和管理 API Keys 及其生命周期信息。Key 可被项目绑定。
---
CREATE TABLE api_keys (
    id VARCHAR(36) PRIMARY KEY, -- API Key 记录的唯一标识符 (UUID 字符串，由应用层生成)
    api_key_value VARCHAR(256) NOT NULL UNIQUE, -- 实际的 API Key 字符串，必须全局唯一且安全存储
    description TEXT, -- 对该 API Key 的描述
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- API Key 的状态
    issued_at TIMESTAMP NOT NULL DEFAULT NOW(), -- API Key 的颁发时间
    expires_at TIMESTAMP, -- API Key 的过期时间 (可选)
    last_used_at TIMESTAMP, -- API Key 最后一次被使用的时间
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), -- 记录创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT NOW() -- 记录最后更新时间
);

-- 添加表级别和列级别评论
COMMENT ON TABLE api_keys IS '独立管理 API Keys 及其生命周期，Key 可被项目绑定';
COMMENT ON COLUMN api_keys.id IS 'API Key 记录的唯一标识符 (UUID 字符串，由应用层生成)';
COMMENT ON COLUMN api_keys.api_key_value IS '实际的 API Key 字符串，必须全局唯一且安全存储';
COMMENT ON COLUMN api_keys.description IS '对该 API Key 的描述，例如 "为生产环境项目A预留的Key", "测试用途Key"';
COMMENT ON COLUMN api_keys.status IS 'API Key 的状态：ACTIVE (激活), REVOKED (已撤销), EXPIRED (已过期), UNUSED (未使用)';
COMMENT ON COLUMN api_keys.issued_at IS 'API Key 的颁发时间';
COMMENT ON COLUMN api_keys.expires_at IS 'API Key 的过期时间 (可选)。如果为 NULL，则永不过期';
COMMENT ON COLUMN api_keys.last_used_at IS 'API Key 最后一次被使用的时间，可用于审计和清理过期/不活跃 Key';
COMMENT ON COLUMN api_keys.created_at IS '记录创建时间';
COMMENT ON COLUMN api_keys.updated_at IS '记录最后更新时间，每次更新时自动修改'; 