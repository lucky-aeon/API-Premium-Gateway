package org.xhy.gateway.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.application.assembler.MonitoringAssembler;
import org.xhy.gateway.application.dto.ApiInstanceMonitoringDTO;
import org.xhy.gateway.application.dto.MonitoringOverviewDTO;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.repository.ApiInstanceRepository;
import org.xhy.gateway.domain.metrics.entity.ApiInstanceMetricsEntity;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.domain.metrics.repository.ApiInstanceMetricsRepository;
import org.xhy.gateway.domain.project.entity.ProjectEntity;
import org.xhy.gateway.domain.project.repository.ProjectRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监控应用服务
 * 提供API实例监控相关的业务逻辑
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class MonitoringAppService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringAppService.class);

    private final ApiInstanceRepository apiInstanceRepository;
    private final ApiInstanceMetricsRepository apiInstanceMetricsRepository;
    private final ProjectRepository projectRepository;
    private final MonitoringAssembler monitoringAssembler;

    public MonitoringAppService(ApiInstanceRepository apiInstanceRepository,
                               ApiInstanceMetricsRepository apiInstanceMetricsRepository,
                               ProjectRepository projectRepository,
                               MonitoringAssembler monitoringAssembler) {
        this.apiInstanceRepository = apiInstanceRepository;
        this.apiInstanceMetricsRepository = apiInstanceMetricsRepository;
        this.projectRepository = projectRepository;
        this.monitoringAssembler = monitoringAssembler;
    }

    /**
     * 获取监控概览数据
     */
    public MonitoringOverviewDTO getMonitoringOverview(String projectId) {
        logger.info("获取监控概览数据，项目ID: {}", projectId);

        // 获取实例列表
        List<ApiInstanceEntity> instances;
        if (projectId != null && !projectId.isEmpty()) {
            LambdaQueryWrapper<ApiInstanceEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiInstanceEntity::getProjectId, projectId);
            instances = apiInstanceRepository.selectList(wrapper);
        } else {
            instances = apiInstanceRepository.selectList(null);
        }

        long totalInstances = instances.size();
        long activeInstances = instances.stream()
                .filter(instance -> ApiInstanceStatus.ACTIVE.equals(instance.getStatus()))
                .count();

        // 获取最新指标数据
        List<String> instanceIds = instances.stream()
                .map(ApiInstanceEntity::getId)
                .collect(Collectors.toList());

        if (instanceIds.isEmpty()) {
            return new MonitoringOverviewDTO(0L, 0L, 0L, 0.0, 0.0, 0L);
        }

        // 获取最近时间窗口的指标数据
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(10);
        LambdaQueryWrapper<ApiInstanceMetricsEntity> metricsWrapper = new LambdaQueryWrapper<>();
        metricsWrapper.in(ApiInstanceMetricsEntity::getRegistryId, instanceIds)
                     .gt(ApiInstanceMetricsEntity::getTimestampWindow, recentTime);
        List<ApiInstanceMetricsEntity> recentMetrics = apiInstanceMetricsRepository.selectList(metricsWrapper);

        // 计算健康实例数
        long healthyInstances = recentMetrics.stream()
                .filter(metrics -> GatewayStatus.HEALTHY.equals(metrics.getCurrentGatewayStatus()))
                .map(ApiInstanceMetricsEntity::getRegistryId)
                .distinct()
                .count();

        // 计算平均成功率和延迟
        double averageSuccessRate = 0.0;
        double averageLatency = 0.0;
        long totalCalls = 0L;

        if (!recentMetrics.isEmpty()) {
            totalCalls = recentMetrics.stream()
                    .mapToLong(ApiInstanceMetricsEntity::getTotalCallCount)
                    .sum();

            double totalSuccessRate = recentMetrics.stream()
                    .filter(metrics -> metrics.getTotalCallCount() > 0)
                    .mapToDouble(ApiInstanceMetricsEntity::getSuccessRate)
                    .average()
                    .orElse(0.0);

            double totalLatency = recentMetrics.stream()
                    .filter(metrics -> metrics.getTotalCallCount() > 0)
                    .mapToDouble(ApiInstanceMetricsEntity::getAverageLatencyMs)
                    .average()
                    .orElse(0.0);

            averageSuccessRate = totalSuccessRate;
            averageLatency = totalLatency;
        }

        MonitoringOverviewDTO result = new MonitoringOverviewDTO(
                totalInstances, healthyInstances, activeInstances,
                averageSuccessRate, averageLatency, totalCalls
        );

        logger.info("监控概览数据获取完成: {}", result);
        return result;
    }

    /**
     * 获取实例监控列表
     */
    public List<ApiInstanceMonitoringDTO> getInstancesWithMetrics(String projectId, 
                                                                 ApiInstanceStatus status, 
                                                                 GatewayStatus gatewayStatus) {
        logger.info("获取实例监控列表，项目ID: {}，实例状态: {}，网关状态: {}", projectId, status, gatewayStatus);

        // 获取实例列表
        List<ApiInstanceEntity> instances;
        if (projectId != null && !projectId.isEmpty()) {
            LambdaQueryWrapper<ApiInstanceEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiInstanceEntity::getProjectId, projectId);
            instances = apiInstanceRepository.selectList(wrapper);
        } else {
            instances = apiInstanceRepository.selectList(null);
        }

        // 按状态过滤
        if (status != null) {
            instances = instances.stream()
                    .filter(instance -> status.equals(instance.getStatus()))
                    .collect(Collectors.toList());
        }

        if (instances.isEmpty()) {
            return List.of();
        }

        // 获取项目信息
        Map<String, String> projectNames = getProjectNamesMap(instances);

        // 获取最新指标数据
        List<String> instanceIds = instances.stream()
                .map(ApiInstanceEntity::getId)
                .collect(Collectors.toList());

        Map<String, ApiInstanceMetricsEntity> latestMetrics = getLatestMetricsMap(instanceIds);

        // 组装结果
        List<ApiInstanceMonitoringDTO> result = instances.stream()
                .map(instance -> {
                    ApiInstanceMetricsEntity metrics = latestMetrics.get(instance.getId());
                    String projectName = projectNames.get(instance.getProjectId());
                    return monitoringAssembler.toMonitoringDTO(instance, metrics, projectName);
                })
                .collect(Collectors.toList());

        // 按网关状态过滤
        if (gatewayStatus != null) {
            result = result.stream()
                    .filter(dto -> gatewayStatus.equals(dto.getGatewayStatus()))
                    .collect(Collectors.toList());
        }

        logger.info("实例监控列表获取完成，共 {} 个实例", result.size());
        return result;
    }

    /**
     * 获取项目名称映射
     */
    private Map<String, String> getProjectNamesMap(List<ApiInstanceEntity> instances) {
        List<String> projectIds = instances.stream()
                .map(ApiInstanceEntity::getProjectId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<ProjectEntity> projectWrapper = new LambdaQueryWrapper<>();
        projectWrapper.in(ProjectEntity::getId, projectIds);
        List<ProjectEntity> projects = projectRepository.selectList(projectWrapper);
        
        return projects.stream()
                .collect(Collectors.toMap(
                        ProjectEntity::getId,
                        ProjectEntity::getName
                ));
    }

    /**
     * 获取最新指标数据映射
     */
    private Map<String, ApiInstanceMetricsEntity> getLatestMetricsMap(List<String> instanceIds) {
        logger.debug("获取最新指标数据映射，实例ID列表: {}", instanceIds);
        
        // 获取每个实例的最新指标数据
        Map<String, ApiInstanceMetricsEntity> result = new java.util.HashMap<>();
        
        for (String instanceId : instanceIds) {
            LambdaQueryWrapper<ApiInstanceMetricsEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiInstanceMetricsEntity::getRegistryId, instanceId)
                   .orderByDesc(ApiInstanceMetricsEntity::getTimestampWindow)
                   .last("LIMIT 1");
            List<ApiInstanceMetricsEntity> metrics = apiInstanceMetricsRepository.selectList(wrapper);
            
            if (metrics.isEmpty()) {
                logger.debug("实例 {} 暂无指标数据", instanceId);
                result.put(instanceId, null);
            } else {
                logger.debug("实例 {} 找到指标数据: {}", instanceId, metrics.get(0).getId());
                result.put(instanceId, metrics.get(0));
            }
        }
        
        logger.debug("指标数据映射获取完成，共 {} 个实例", result.size());
        return result;
    }
} 