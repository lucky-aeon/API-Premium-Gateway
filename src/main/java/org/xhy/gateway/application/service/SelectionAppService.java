package org.xhy.gateway.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.gateway.application.assembler.SelectionAssembler;
import org.xhy.gateway.application.assembler.ApiInstanceAssembler;
import org.xhy.gateway.application.dto.ApiInstanceDTO;
import org.xhy.gateway.domain.apiinstance.command.InstanceSelectionCommand;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.service.ApiInstanceSelectionDomainService;
import org.xhy.gateway.domain.metrics.command.CallResultCommand;
import org.xhy.gateway.domain.metrics.service.MetricsCollectionDomainService;
import org.xhy.gateway.infrastructure.exception.BusinessException;
import org.xhy.gateway.interfaces.api.request.ReportResultRequest;
import org.xhy.gateway.interfaces.api.request.SelectInstanceRequest;

/**
 * 选择算法应用服务
 * 负责API实例选择和结果上报的编排
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class SelectionAppService {

    private static final Logger logger = LoggerFactory.getLogger(SelectionAppService.class);

    private final ApiInstanceSelectionDomainService selectionDomainService;
    private final MetricsCollectionDomainService metricsCollectionDomainService;
    private final SelectionAssembler selectionAssembler;

    public SelectionAppService(ApiInstanceSelectionDomainService selectionDomainService, 
                              MetricsCollectionDomainService metricsCollectionDomainService,
                              SelectionAssembler selectionAssembler) {
        this.selectionDomainService = selectionDomainService;
        this.metricsCollectionDomainService = metricsCollectionDomainService;
        this.selectionAssembler = selectionAssembler;
    }

    /**
     * 选择最佳API实例（支持降级）
     * 只读操作，不需要事务
     */
    public ApiInstanceDTO selectBestInstance(SelectInstanceRequest request, String currentProjectId) {
        logger.info("应用层开始选择API实例: {}", request);

        try {
            // 首先尝试正常的实例选择
            return selectInstanceInternal(request, currentProjectId);
        } catch (BusinessException e) {
            // 如果正常选择失败且有降级链，则尝试降级
            if (request.hasFallbackChain() && 
                ("NO_AVAILABLE_INSTANCE".equals(e.getErrorCode()) || "NO_HEALTHY_INSTANCE".equals(e.getErrorCode()))) {
                
                logger.warn("主要实例选择失败，开始尝试降级: {}", e.getMessage());
                return tryFallbackInstances(request, currentProjectId);
            }
            
            // 没有降级链或其他类型的异常，直接抛出
            throw e;
        }
    }

    /**
     * 内部实例选择方法
     */
    private ApiInstanceDTO selectInstanceInternal(SelectInstanceRequest request, String currentProjectId) {
        // 应用层通过Assembler将Request对象转换成领域命令对象
        InstanceSelectionCommand command = selectionAssembler.toCommand(request, currentProjectId);

        // 调用领域服务执行选择算法
        ApiInstanceEntity selectedEntity = selectionDomainService.selectBestInstance(command);

        // 转换为DTO返回
        ApiInstanceDTO result = ApiInstanceAssembler.toDTO(selectedEntity);

        logger.info("应用层选择API实例成功: businessId={}, instanceId={}", 
                result.getBusinessId(), result.getId());
        return result;
    }

    /**
     * 尝试降级实例选择
     */
    private ApiInstanceDTO tryFallbackInstances(SelectInstanceRequest request, String currentProjectId) {
        logger.info("开始尝试降级实例选择，降级链: {}", request.getFallbackChain());

        for (int i = 0; i < request.getFallbackChain().size(); i++) {
            String fallbackBusinessId = request.getFallbackChain().get(i);
            
            try {
                // 创建降级请求，使用相同的apiType和projectId，但使用降级的businessId作为apiIdentifier
                SelectInstanceRequest fallbackRequest = createFallbackRequest(request, fallbackBusinessId);
                
                logger.info("尝试降级到第{}个实例: businessId={}", i + 1, fallbackBusinessId);
                
                ApiInstanceDTO result = selectInstanceInternal(fallbackRequest, currentProjectId);
                
                logger.info("降级成功，选择到实例: businessId={}, instanceId={}", 
                        result.getBusinessId(), result.getId());
                return result;
                
            } catch (BusinessException e) {
                logger.warn("降级到第{}个实例失败: businessId={}, 错误: {}", 
                        i + 1, fallbackBusinessId, e.getMessage());
                
                // 如果不是最后一个降级选项，继续尝试下一个
                if (i < request.getFallbackChain().size() - 1) {
                    continue;
                }
                
                // 如果是最后一个降级选项也失败了，抛出异常
                throw new BusinessException("FALLBACK_EXHAUSTED", 
                        String.format("所有降级实例都不可用，主实例和%d个降级实例均失败", 
                                request.getFallbackChain().size()));
            }
        }
        
        // 理论上不会到达这里，但为了代码完整性
        throw new BusinessException("FALLBACK_EXHAUSTED", "降级链为空或处理异常");
    }

    /**
     * 创建降级请求
     * 复用原请求的apiType和projectId，但使用降级的businessId查找对应实例
     */
    private SelectInstanceRequest createFallbackRequest(SelectInstanceRequest originalRequest, String fallbackBusinessId) {
        SelectInstanceRequest fallbackRequest = new SelectInstanceRequest();
        fallbackRequest.setUserId(originalRequest.getUserId());
        fallbackRequest.setApiIdentifier(fallbackBusinessId); // 使用降级的businessId作为apiIdentifier
        fallbackRequest.setApiType(originalRequest.getApiType()); // 复用相同的apiType
        fallbackRequest.setAffinityKey(originalRequest.getAffinityKey());
        fallbackRequest.setAffinityType(originalRequest.getAffinityType());
        // 降级请求不再传递降级链，避免无限递归
        fallbackRequest.setFallbackChain(null);
        
        return fallbackRequest;
    }

    /**
     * 上报调用结果
     * 需要事务支持，因为涉及指标数据更新
     */
    @Transactional(rollbackFor = Exception.class)
    public void reportCallResult(ReportResultRequest request,String projectId) {
        logger.info("应用层开始处理调用结果上报: instanceId={}, success={}", 
                request.getInstanceId(), request.getSuccess());

        // 应用层通过Assembler将Request对象转换成领域命令对象
        CallResultCommand command = selectionAssembler.toCommand(request,projectId);

        // 调用领域服务处理结果上报
        metricsCollectionDomainService.recordCallResult(command);

        logger.info("应用层调用结果上报处理完成");
    }
} 