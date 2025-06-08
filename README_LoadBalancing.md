# API 网关负载均衡策略使用指南

## 概述

API Premium Gateway 支持多种负载均衡策略，用户可以手动指定策略，也可以使用智能策略让系统根据实例指标自动选择最优算法。

## 支持的策略类型

### 1. 智能策略 (SMART) - **默认推荐**
- **适用场景**: 通用场景，系统自动根据实例指标选择最优策略
- **选择逻辑**: 
  - 成功率差异大时 → 成功率优先策略
  - 延迟差异大时 → 延迟优先策略  
  - 性能相近时 → 轮询策略
- **优点**: 自适应，无需手动配置，智能化程度高
- **代码**: `LoadBalancingType.SMART`

### 2. 轮询策略 (ROUND_ROBIN)
- **适用场景**: 实例性能相近，追求简单均匀分配
- **选择逻辑**: 依次轮流选择可用实例
- **优点**: 简单稳定，分配均匀
- **代码**: `LoadBalancingType.ROUND_ROBIN`

### 3. 成功率优先策略 (SUCCESS_RATE_FIRST)
- **适用场景**: 对稳定性要求高的业务，如支付、通知服务
- **选择逻辑**: 优先选择历史成功率最高的实例
- **优点**: 最大化成功率，减少失败请求
- **代码**: `LoadBalancingType.SUCCESS_RATE_FIRST`

### 4. 延迟优先策略 (LATENCY_FIRST)
- **适用场景**: 对响应时间敏感的业务，如AI模型、实时API
- **选择逻辑**: 优先选择平均延迟最低的实例
- **优点**: 最小化响应时间，提升用户体验
- **代码**: `LoadBalancingType.LATENCY_FIRST`

## 使用方式

### 基本用法

```java
// 1. 智能策略（默认，推荐）
InstanceSelectionCommand command1 = new InstanceSelectionCommand(
    projectId, userId, apiIdentifier, apiType
);

// 2. 明确指定智能策略
InstanceSelectionCommand command2 = new InstanceSelectionCommand(
    projectId, userId, apiIdentifier, apiType, LoadBalancingType.SMART
);

// 3. 指定轮询策略
InstanceSelectionCommand command3 = new InstanceSelectionCommand(
    projectId, userId, apiIdentifier, apiType, LoadBalancingType.ROUND_ROBIN
);

// 4. 成功率优先策略
InstanceSelectionCommand command4 = new InstanceSelectionCommand(
    projectId, userId, apiIdentifier, apiType, LoadBalancingType.SUCCESS_RATE_FIRST
);

// 5. 延迟优先策略
InstanceSelectionCommand command5 = new InstanceSelectionCommand(
    projectId, userId, apiIdentifier, apiType, LoadBalancingType.LATENCY_FIRST
);

// 执行选择
String selectedInstanceId = selectionDomainService.selectBestInstance(command);
```

### 场景示例

```java
// 通用场景 - 使用智能策略（推荐）
InstanceSelectionCommand smartCommand = new InstanceSelectionCommand(
    "project-123", "user-456", "general-api", "OTHER"
);

// 支付API - 明确指定成功率优先（如果有特殊需求）
InstanceSelectionCommand paymentCommand = new InstanceSelectionCommand(
    "project-123", "user-456", "payment-api", "PAYMENT_GATEWAY", 
    LoadBalancingType.SUCCESS_RATE_FIRST
);

// AI模型API - 明确指定延迟优先（如果有特殊需求）
InstanceSelectionCommand aiCommand = new InstanceSelectionCommand(
    "project-123", "user-456", "gpt-4o", "MODEL", 
    LoadBalancingType.LATENCY_FIRST
);
```

## 智能策略详解

### 决策逻辑
智能策略会分析当前所有候选实例的指标数据，根据以下规则自动选择最优策略：

1. **成功率差异检测**
   - 计算所有实例成功率的方差
   - 如果方差 > 0.1（10%），选择成功率优先策略
   
2. **延迟差异检测**  
   - 计算所有实例延迟的方差
   - 如果方差 > 500ms，选择延迟优先策略
   
3. **性能相近场景**
   - 如果成功率和延迟差异都不大，选择轮询策略
   
4. **冷启动场景**
   - 如果没有指标数据，退化为轮询策略

### 智能策略的优势
- **自适应**: 根据实际运行数据动态调整
- **零配置**: 无需手动分析和选择策略
- **智能化**: 基于统计学方法科学决策
- **兜底机制**: 异常情况下自动退化为轮询策略

## 策略特性对比

| 策略 | 性能考量 | 稳定性 | 复杂度 | 适用场景 | 推荐度 |
|------|----------|--------|--------|----------|--------|
| 智能策略 | **最优** | **最高** | 低 | **通用场景** | ⭐⭐⭐⭐⭐ |
| 轮询 | 中等 | 高 | 低 | 性能相近 | ⭐⭐⭐ |
| 成功率优先 | 高 | 最高 | 中 | 稳定性优先 | ⭐⭐⭐⭐ |
| 延迟优先 | 最高 | 中 | 中 | 性能优先 | ⭐⭐⭐⭐ |

## 实现原理

### 策略模式 + 自动注册设计
- `LoadBalancingStrategy` 接口定义策略规范
- 每个策略实现通过 `@Component` 注解自动注册到 Spring 容器
- `LoadBalancingStrategyFactory` 使用 `@Autowired` 自动注入所有策略实现
- 通过 `@PostConstruct` 方法自动建立策略类型映射关系

### 自动注册流程
```java
@Component
public class LoadBalancingStrategyFactory {
    
    @Autowired
    private List<LoadBalancingStrategy> allStrategies; // Spring 自动注入所有实现
    
    @PostConstruct
    public void initStrategies() {
        // 自动注册所有策略到映射表
        for (LoadBalancingStrategy strategy : allStrategies) {
            strategyMap.put(strategy.getStrategyType(), strategy);
        }
    }
}
```

### 选择流程
1. 用户指定负载均衡策略类型（或使用默认智能策略）
2. 系统查找符合条件的候选实例
3. 获取实例的历史指标数据
4. 过滤掉被熔断的实例
5. 使用指定策略选择最佳实例（智能策略会动态选择子策略）
6. 返回选中的实例ID

## 注意事项

1. **默认策略**: 不指定策略时，系统默认使用智能策略
2. **熔断保护**: 所有策略都会自动排除被熔断的实例
3. **冷启动**: 对于没有历史数据的新实例，系统会给予合理的默认评分
4. **并发安全**: 所有策略实现都保证线程安全
5. **自动注册**: 新增策略只需实现接口并添加 @Component 注解即可自动注册

## 扩展新策略

添加新的负载均衡策略非常简单，系统会自动发现和注册：

1. 在 `LoadBalancingType` 枚举中添加新类型
2. 实现 `LoadBalancingStrategy` 接口
3. 添加 `@Component` 注解
4. 编写相应的单元测试

```java
// 示例：权重优先策略
@Component
public class WeightFirstStrategy implements LoadBalancingStrategy {
    
    @Override
    public String getStrategyName() {
        return "WEIGHT_FIRST";
    }
    
    @Override
    public String getDescription() {
        return "权重优先策略：根据实例权重分配流量";
    }
    
    @Override
    public LoadBalancingType getStrategyType() {
        return LoadBalancingType.WEIGHT_FIRST; // 需要在枚举中添加
    }
    
    @Override
    public ApiInstanceEntity selectInstance(List<ApiInstanceEntity> instances, 
                                          Map<String, InstanceMetricsEntity> metricsMap) {
        // 实现权重优先选择逻辑
        return instances.stream()
            .max(Comparator.comparing(ApiInstanceEntity::getPriority))
            .orElse(instances.get(0));
    }
}
```

策略会在应用启动时自动注册，无需修改工厂类代码。这种设计提供了出色的可扩展性和维护性。 