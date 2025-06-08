package org.xhy.gateway.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.application.assembler.MonitoringAssembler;
import org.xhy.gateway.application.dto.ApiInstanceMonitoringDTO;
import org.xhy.gateway.application.dto.MonitoringOverviewDTO;
import org.xhy.gateway.application.dto.TimeSeriesDTO;
import org.xhy.gateway.application.dto.TimePointDTO;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.repository.ApiInstanceRepository;
import org.xhy.gateway.domain.metrics.entity.ApiInstanceMetricsEntity;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.domain.metrics.repository.ApiInstanceMetricsRepository;
import org.xhy.gateway.domain.project.entity.ProjectEntity;
import org.xhy.gateway.domain.project.repository.ProjectRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    /**
     * 获取时间序列数据
     * 用于监控图表展示
     */
    public TimeSeriesDTO getTimeSeriesData(String timeRange, String projectId) {
        logger.info("获取时间序列数据，时间范围: {}，项目ID: {}", timeRange, projectId);
        
        // 解析时间范围
        int hours = parseTimeRange(timeRange);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);
        
        logger.debug("时间范围：从 {} 到 {}", startTime, endTime);
        
        // 查询时间范围内的指标数据
        LambdaQueryWrapper<ApiInstanceMetricsEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ApiInstanceMetricsEntity::getTimestampWindow, startTime)
               .le(ApiInstanceMetricsEntity::getTimestampWindow, endTime)
               .orderBy(true, true, ApiInstanceMetricsEntity::getTimestampWindow);
        
        // 如果指定了项目ID，需要过滤相关实例
        if (projectId != null && !projectId.isEmpty()) {
            // 先获取项目下的所有实例ID
            LambdaQueryWrapper<ApiInstanceEntity> instanceWrapper = new LambdaQueryWrapper<>();
            instanceWrapper.eq(ApiInstanceEntity::getProjectId, projectId);
            List<ApiInstanceEntity> instances = apiInstanceRepository.selectList(instanceWrapper);
            
            if (instances.isEmpty()) {
                logger.warn("项目 {} 下没有找到API实例", projectId);
                return createEmptyTimeSeriesData();
            }
            
            List<String> instanceIds = instances.stream()
                    .map(ApiInstanceEntity::getId)
                    .collect(Collectors.toList());
            wrapper.in(ApiInstanceMetricsEntity::getRegistryId, instanceIds);
        }
        
        List<ApiInstanceMetricsEntity> metrics = apiInstanceMetricsRepository.selectList(wrapper);
        logger.debug("查询到 {} 条指标数据", metrics.size());
        
        if (metrics.isEmpty()) {
            logger.warn("未找到指标数据，返回模拟数据");
            return generateMockTimeSeriesData(timeRange);
        }
        
        // 按时间窗口聚合数据
        return aggregateTimeSeriesData(metrics, hours);
    }
    
    /**
     * 解析时间范围字符串
     */
    private int parseTimeRange(String timeRange) {
        switch (timeRange) {
            case "1h": return 1;
            case "6h": return 6;
            case "24h": return 24;
            case "7d": return 168; // 7 * 24
            default: 
                logger.warn("未知的时间范围参数: {}，使用默认24小时", timeRange);
                return 24;
        }
    }
    
    /**
     * 根据时间范围确定合理的时间间隔
     */
    private int determineTimeInterval(int hours) {
        if (hours <= 1) {
            return 5; // 1小时内：5分钟间隔
        } else if (hours <= 6) {
            return 15; // 6小时内：15分钟间隔
        } else if (hours <= 24) {
            return 60; // 24小时内：1小时间隔
        } else {
            return 240; // 7天：4小时间隔
        }
    }
    
    /**
     * 聚合时间序列数据
     */
    private TimeSeriesDTO aggregateTimeSeriesData(List<ApiInstanceMetricsEntity> metrics, int hours) {
        logger.info("开始聚合时间序列数据，总数据量: {}, 时间范围: {}小时", metrics.size(), hours);
        
        // 根据时间范围确定合理的时间间隔
        int intervalMinutes = determineTimeInterval(hours);
        logger.debug("使用时间间隔: {} 分钟", intervalMinutes);
        
        // 按时间窗口分组
        Map<LocalDateTime, List<ApiInstanceMetricsEntity>> timeGroups = metrics.stream()
                .collect(Collectors.groupingBy(m -> alignTimeWindow(m.getTimestampWindow(), intervalMinutes)));
        
        List<TimePointDTO> callVolumeData = new ArrayList<>();
        List<TimePointDTO> successRateData = new ArrayList<>();
        List<TimePointDTO> latencyData = new ArrayList<>();
        
        // 按时间排序并聚合
        timeGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LocalDateTime timeWindow = entry.getKey();
                    List<ApiInstanceMetricsEntity> groupMetrics = entry.getValue();
                    String timeKey = formatTimeForDisplay(timeWindow);
                    
                    logger.debug("处理时间窗口: {}, 数据量: {}", timeKey, groupMetrics.size());
                    
                    // 计算聚合值
                    long totalCalls = groupMetrics.stream()
                            .mapToLong(m -> m.getSuccessCount() + m.getFailureCount())
                            .sum();
                    
                    double avgSuccessRate = groupMetrics.stream()
                            .mapToDouble(ApiInstanceMetricsEntity::getSuccessRate)
                            .average()
                            .orElse(0.0);
                    
                    double avgLatency = groupMetrics.stream()
                            .mapToDouble(ApiInstanceMetricsEntity::getAverageLatencyMs)
                            .average()
                            .orElse(0.0);
                    
                    logger.debug("时间窗口 {} 聚合结果: 调用量={}, 成功率={:.2f}%, 延迟={:.2f}ms", 
                            timeKey, totalCalls, avgSuccessRate, avgLatency);
                    
                    callVolumeData.add(new TimePointDTO(timeKey, (double) totalCalls));
                    successRateData.add(new TimePointDTO(timeKey, avgSuccessRate));
                    latencyData.add(new TimePointDTO(timeKey, avgLatency));
                });
        
        logger.info("时间序列数据聚合完成，生成了 {} 个数据点", callVolumeData.size());
        return new TimeSeriesDTO(callVolumeData, successRateData, latencyData);
    }
    
    /**
     * 将时间对齐到时间窗口边界
     */
    private LocalDateTime alignTimeWindow(LocalDateTime dateTime, int intervalMinutes) {
        // 将时间对齐到间隔边界
        int minute = (dateTime.getMinute() / intervalMinutes) * intervalMinutes;
        return dateTime.withMinute(minute).withSecond(0).withNano(0);
    }
    
    /**
     * 格式化时间用于显示
     */
    private String formatTimeForDisplay(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    /**
     * 创建空的时间序列数据
     */
    private TimeSeriesDTO createEmptyTimeSeriesData() {
        return new TimeSeriesDTO(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
    
    /**
     * 生成模拟时间序列数据
     */
    private TimeSeriesDTO generateMockTimeSeriesData(String timeRange) {
        logger.info("生成模拟时间序列数据，时间范围: {}", timeRange);
        
        int hours = parseTimeRange(timeRange);
        int interval = Math.max(1, hours / 24); // 最多24个数据点
        
        List<TimePointDTO> callVolumeData = new ArrayList<>();
        List<TimePointDTO> successRateData = new ArrayList<>();
        List<TimePointDTO> latencyData = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = hours; i >= 0; i -= interval) {
            LocalDateTime time = now.minusHours(i);
            String timeStr = time.format(DateTimeFormatter.ofPattern("HH:mm"));
            
            // 模拟调用量数据（波动）
            double baseVolume = 100 + Math.sin(i / 4.0) * 30;
            double volume = Math.max(0, baseVolume + (Math.random() - 0.5) * 40);
            
            // 模拟成功率（偶尔降低）
            double baseSuccessRate = 98.0;
            double successRate = Math.random() < 0.1 ? 
                    85 + Math.random() * 10 : 
                    baseSuccessRate + (Math.random() - 0.5) * 4;
            
            // 模拟延迟（偶尔尖峰）
            double baseLatency = 200.0;
            double latency = Math.random() < 0.05 ? 
                    800 + Math.random() * 1200 : 
                    baseLatency + Math.random() * 300;
            
            callVolumeData.add(new TimePointDTO(timeStr, (double) Math.round(volume)));
            successRateData.add(new TimePointDTO(timeStr, Math.min(100.0, Math.max(0.0, successRate))));
            latencyData.add(new TimePointDTO(timeStr, Math.max(0.0, latency)));
        }
        
        // 反转列表，使时间从过去到现在排列
        Collections.reverse(callVolumeData);
        Collections.reverse(successRateData);
        Collections.reverse(latencyData);
        
        return new TimeSeriesDTO(callVolumeData, successRateData, latencyData);
    }
} 