package org.xhy.gateway.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.application.dto.InstanceObservationDTO;
import org.xhy.gateway.application.dto.ObservationOverviewDTO;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.repository.ApiInstanceRepository;
import org.xhy.gateway.domain.metrics.entity.ApiInstanceMetricsEntity;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.domain.metrics.repository.ApiInstanceMetricsRepository;
import org.xhy.gateway.domain.project.entity.ProjectEntity;
import org.xhy.gateway.domain.project.repository.ProjectRepository;
import org.xhy.gateway.interfaces.api.request.monitoring.InstanceObservationRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实例观测应用服务
 * 专门用于观测页面的数据查询和处理
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class InstanceObservationAppService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceObservationAppService.class);

    private final ApiInstanceRepository apiInstanceRepository;
    private final ApiInstanceMetricsRepository apiInstanceMetricsRepository;
    private final ProjectRepository projectRepository;

    public InstanceObservationAppService(ApiInstanceRepository apiInstanceRepository,
                                       ApiInstanceMetricsRepository apiInstanceMetricsRepository,
                                       ProjectRepository projectRepository) {
        this.apiInstanceRepository = apiInstanceRepository;
        this.apiInstanceMetricsRepository = apiInstanceMetricsRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * 获取观测概览数据
     * 根据时间窗口聚合统计信息
     */
    public ObservationOverviewDTO getObservationOverview(InstanceObservationRequest request) {
        logger.info("获取观测概览数据，请求参数: {}", request);

        // 获取实例列表
        List<ApiInstanceEntity> instances = getFilteredInstances(request);
        
        // 计算时间范围
        LocalDateTime startTime = calculateStartTime(request.getTimeWindow());
        
        // 获取时间窗口内的指标数据
        Map<String, ApiInstanceMetricsEntity> latestMetricsMap = getAggregatedMetrics(instances, startTime);
        
        // 统计各种状态的实例数量
        long totalInstances = instances.size();
        long activeInstances = instances.stream()
                .filter(instance -> ApiInstanceStatus.ACTIVE.equals(instance.getStatus()))
                .count();
        
        long healthyInstances = 0;
        long faultyInstances = 0;
        long circuitBreakerInstances = 0;
        long totalCallCount = 0;
        double totalSuccessRate = 0.0;
        double totalLatency = 0.0;
        int validMetricsCount = 0;

        for (ApiInstanceMetricsEntity metrics : latestMetricsMap.values()) {
            if (metrics != null) {
                GatewayStatus status = metrics.getCurrentGatewayStatus();
                if (GatewayStatus.HEALTHY.equals(status)) {
                    healthyInstances++;
                } else if (GatewayStatus.FAULTY.equals(status)) {
                    faultyInstances++;
                } else if (GatewayStatus.CIRCUIT_BREAKER_OPEN.equals(status)) {
                    circuitBreakerInstances++;
                }
                
                totalCallCount += metrics.getTotalCallCount();
                if (metrics.getTotalCallCount() > 0) {
                    totalSuccessRate += metrics.getSuccessRate();
                    totalLatency += metrics.getAverageLatencyMs();
                    validMetricsCount++;
                }
            }
        }
        
        double averageSuccessRate = validMetricsCount > 0 ? totalSuccessRate / validMetricsCount : 0.0;
        double averageLatency = validMetricsCount > 0 ? totalLatency / validMetricsCount : 0.0;
        
        ObservationOverviewDTO result = new ObservationOverviewDTO(
                totalInstances, healthyInstances, activeInstances, faultyInstances, 
                circuitBreakerInstances, totalCallCount, averageSuccessRate, averageLatency
        );
        
        logger.info("观测概览数据获取完成: {}", result);
        return result;
    }

    /**
     * 根据请求参数过滤实例
     */
    private List<ApiInstanceEntity> getFilteredInstances(InstanceObservationRequest request) {
        LambdaQueryWrapper<ApiInstanceEntity> wrapper = new LambdaQueryWrapper<>();
        
        // 项目ID过滤
        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            wrapper.eq(ApiInstanceEntity::getProjectId, request.getProjectId());
        }
        
        // 实例状态过滤
        if (request.getInstanceStatus() != null && !request.getInstanceStatus().isEmpty()) {
            try {
                ApiInstanceStatus status = ApiInstanceStatus.valueOf(request.getInstanceStatus());
                wrapper.eq(ApiInstanceEntity::getStatus, status);
            } catch (IllegalArgumentException e) {
                logger.warn("无效的实例状态参数: {}", request.getInstanceStatus());
            }
        }
        
        List<ApiInstanceEntity> instances = apiInstanceRepository.selectList(wrapper);
        logger.info("查询到 {} 个API实例", instances.size());
        if (!instances.isEmpty()) {
            logger.debug("实例ID列表: {}", instances.stream().map(ApiInstanceEntity::getId).toList());
        }
        
        return instances;
    }

    /**
     * 根据时间窗口字符串计算开始时间
     */
    private LocalDateTime calculateStartTime(String timeWindow) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (timeWindow.toLowerCase()) {
            case "1m":
                return now.minusMinutes(1);
            case "10m":
                return now.minusMinutes(10);
            case "30m":
                return now.minusMinutes(30);
            case "1h":
                return now.minusHours(1);
            case "6h":
                return now.minusHours(6);
            case "24h":
                return now.minusHours(24);
            default:
                logger.warn("未知的时间窗口参数: {}，使用默认1小时", timeWindow);
                return now.minusHours(1);
        }
    }

    /**
     * 获取聚合的指标数据
     * 在指定时间范围内聚合每个实例的指标
     */
    private Map<String, ApiInstanceMetricsEntity> getAggregatedMetrics(List<ApiInstanceEntity> instances, LocalDateTime startTime) {
        if (instances.isEmpty()) {
            return Map.of();
        }
        
        List<String> instanceIds = instances.stream()
                .map(ApiInstanceEntity::getId)
                .collect(Collectors.toList());
        
        logger.info("查询指标数据，实例ID列表: {}, 开始时间: {}", instanceIds, startTime);
        
        // 查询时间范围内的所有指标数据
        LambdaQueryWrapper<ApiInstanceMetricsEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ApiInstanceMetricsEntity::getRegistryId, instanceIds)
               .ge(ApiInstanceMetricsEntity::getTimestampWindow, startTime)
               .orderByDesc(ApiInstanceMetricsEntity::getTimestampWindow);
        
        List<ApiInstanceMetricsEntity> allMetrics = apiInstanceMetricsRepository.selectList(wrapper);
        logger.info("查询到 {} 条指标数据", allMetrics.size());
        if (!allMetrics.isEmpty()) {
            logger.info("指标数据registry_id列表: {}", allMetrics.stream().map(ApiInstanceMetricsEntity::getRegistryId).distinct().toList());
        } else {
            logger.warn("未找到匹配的指标数据，尝试查询所有指标数据进行诊断...");
            // 诊断性查询：查看数据库中都有哪些registry_id
            List<ApiInstanceMetricsEntity> allMetricsForDiag = apiInstanceMetricsRepository.selectList(null);
            logger.warn("数据库中所有指标数据的registry_id: {}", 
                allMetricsForDiag.stream().map(ApiInstanceMetricsEntity::getRegistryId).distinct().toList());
        }
        
        // 按实例ID分组并聚合数据
        return allMetrics.stream()
                .collect(Collectors.groupingBy(ApiInstanceMetricsEntity::getRegistryId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> aggregateMetricsForInstance(entry.getValue())
                ));
    }

    /**
     * 聚合单个实例的多个指标记录
     */
    private ApiInstanceMetricsEntity aggregateMetricsForInstance(List<ApiInstanceMetricsEntity> metrics) {
        if (metrics.isEmpty()) {
            return null;
        }
        
        // 使用最新的记录作为基础
        ApiInstanceMetricsEntity latest = metrics.get(0);
        
        // 如果只有一条记录，直接返回
        if (metrics.size() == 1) {
            return latest;
        }
        
        // 聚合多条记录
        long totalSuccessCount = metrics.stream().mapToLong(ApiInstanceMetricsEntity::getSuccessCount).sum();
        long totalFailureCount = metrics.stream().mapToLong(ApiInstanceMetricsEntity::getFailureCount).sum();
        long totalLatencyMs = metrics.stream().mapToLong(ApiInstanceMetricsEntity::getTotalLatencyMs).sum();
        
        // 创建聚合结果
        ApiInstanceMetricsEntity aggregated = new ApiInstanceMetricsEntity();
        aggregated.setRegistryId(latest.getRegistryId());
        aggregated.setTimestampWindow(latest.getTimestampWindow());
        aggregated.setSuccessCount(totalSuccessCount);
        aggregated.setFailureCount(totalFailureCount);
        aggregated.setTotalLatencyMs(totalLatencyMs);
        aggregated.setCurrentGatewayStatus(latest.getCurrentGatewayStatus());
        aggregated.setLastReportedAt(latest.getLastReportedAt());
        
        return aggregated;
    }

    /**
     * 获取实例观测列表
     * 用于表格展示
     */
    public List<InstanceObservationDTO> getInstanceObservationList(InstanceObservationRequest request) {
        logger.info("获取实例观测列表，请求参数: {}", request);

        // 获取过滤后的实例列表
        List<ApiInstanceEntity> instances = getFilteredInstances(request);
        if (instances.isEmpty()) {
            logger.info("未找到符合条件的实例");
            return List.of();
        }

        // 计算时间范围
        LocalDateTime startTime = calculateStartTime(request.getTimeWindow());
        
        // 获取聚合指标数据
        Map<String, ApiInstanceMetricsEntity> metricsMap = getAggregatedMetrics(instances, startTime);
        
        // 获取项目名称映射
        Map<String, String> projectNameMap = getProjectNameMap(instances);
        
        // 转换为DTO列表
        List<InstanceObservationDTO> result = instances.stream()
                .map(instance -> convertToObservationDTO(instance, metricsMap.get(instance.getId()), projectNameMap))
                .filter(dto -> matchesGatewayStatusFilter(dto, request.getGatewayStatus()))
                .collect(Collectors.toList());
        
        logger.info("实例观测列表获取完成，共 {} 个实例", result.size());
        return result;
    }

    /**
     * 获取项目名称映射
     */
    private Map<String, String> getProjectNameMap(List<ApiInstanceEntity> instances) {
        List<String> projectIds = instances.stream()
                .map(ApiInstanceEntity::getProjectId)
                .distinct()
                .collect(Collectors.toList());
        
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        
        LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ProjectEntity::getId, projectIds);
        List<ProjectEntity> projects = projectRepository.selectList(wrapper);
        
        return projects.stream()
                .collect(Collectors.toMap(
                        ProjectEntity::getId,
                        ProjectEntity::getName
                ));
    }

    /**
     * 转换为观测DTO
     */
    private InstanceObservationDTO convertToObservationDTO(ApiInstanceEntity instance, 
                                                          ApiInstanceMetricsEntity metrics, 
                                                          Map<String, String> projectNameMap) {
        InstanceObservationDTO dto = new InstanceObservationDTO();
        
        // 基础信息
        dto.setProjectName(projectNameMap.get(instance.getProjectId()));
        dto.setApiIdentifier(instance.getApiIdentifier());
        dto.setApiType(instance.getApiType());
        dto.setBusinessId(instance.getBusinessId());
        dto.setInstanceStatus(instance.getStatus());
        
        // 指标信息
        if (metrics != null) {
            dto.setGatewayStatus(metrics.getCurrentGatewayStatus());
            dto.setCallCount(metrics.getTotalCallCount());
            dto.setSuccessRate(metrics.getSuccessRate());
            dto.setAverageLatency(metrics.getAverageLatencyMs());
            dto.setLastActiveTime(metrics.getLastReportedAt());
        } else {
            // 无指标数据时的默认值
            dto.setGatewayStatus(GatewayStatus.HEALTHY);
            dto.setCallCount(0L);
            dto.setSuccessRate(0.0);
            dto.setAverageLatency(0.0);
            dto.setLastActiveTime(null);
        }
        
        return dto;
    }

    /**
     * 检查是否匹配网关状态过滤条件
     */
    private boolean matchesGatewayStatusFilter(InstanceObservationDTO dto, String gatewayStatusFilter) {
        if (gatewayStatusFilter == null || gatewayStatusFilter.isEmpty()) {
            return true;
        }
        
        try {
            GatewayStatus filterStatus = GatewayStatus.valueOf(gatewayStatusFilter);
            return filterStatus.equals(dto.getGatewayStatus());
        } catch (IllegalArgumentException e) {
            logger.warn("无效的网关状态过滤参数: {}", gatewayStatusFilter);
            return true;
        }
    }

} 