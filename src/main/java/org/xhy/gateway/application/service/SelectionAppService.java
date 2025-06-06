package org.xhy.gateway.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.gateway.application.assembler.SelectionAssembler;
import org.xhy.gateway.domain.apiinstance.command.InstanceSelectionCommand;
import org.xhy.gateway.domain.apiinstance.service.ApiInstanceSelectionDomainService;
import org.xhy.gateway.domain.metrics.command.CallResultCommand;
import org.xhy.gateway.domain.metrics.service.MetricsCollectionDomainService;
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
     * 选择最佳API实例
     * 只读操作，不需要事务
     */
    public String selectBestInstance(SelectInstanceRequest request) {
        logger.info("应用层开始选择API实例: {}", request);

        // 应用层通过Assembler将Request对象转换成领域命令对象
        InstanceSelectionCommand command = selectionAssembler.toCommand(request);

        // 调用领域服务执行选择算法
        String businessId = selectionDomainService.selectBestInstance(command);

        logger.info("应用层选择API实例成功: businessId={}", businessId);
        return businessId;
    }

    /**
     * 上报调用结果
     * 需要事务支持，因为涉及指标数据更新
     */
    @Transactional(rollbackFor = Exception.class)
    public void reportCallResult(ReportResultRequest request) {
        logger.info("应用层开始处理调用结果上报: instanceId={}, success={}", 
                request.getInstanceId(), request.getSuccess());

        // 应用层通过Assembler将Request对象转换成领域命令对象
        CallResultCommand command = selectionAssembler.toCommand(request);

        // 调用领域服务处理结果上报
        metricsCollectionDomainService.recordCallResult(command);

        logger.info("应用层调用结果上报处理完成");
    }
} 