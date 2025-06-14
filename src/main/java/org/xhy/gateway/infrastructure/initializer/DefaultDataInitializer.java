package org.xhy.gateway.infrastructure.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.xhy.gateway.domain.apikey.service.ApiKeyDomainService;

/**
 * 默认数据初始化器
 * 在应用启动时自动初始化默认用户数据
 * 
 * @author xhy
 */
@Component
@Order(100) // 确保在其他初始化器之后执行
public class DefaultDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultDataInitializer.class);


    private final ApiKeyDomainService apiKeyDomainService;

    public DefaultDataInitializer(ApiKeyDomainService apiKeyDomainService) {
        this.apiKeyDomainService = apiKeyDomainService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("初始化默认api-key");
        
        try {
            initializeDefaultApiKey();
            log.info("api-premium-gateway初始化默认数据完成");
        } catch (Exception e) {
            log.error("api-premium-gateway初始化默认数据失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /**
     * 初始化默认用户
     */
    private void initializeDefaultApiKey() {
        log.info("正在初始化默认api-key...");
        String defaultApiKey = "default-api-key-1234567890";
        if (!apiKeyDomainService.existApiKey(defaultApiKey)) {
            apiKeyDomainService.createApiKey(defaultApiKey);
            log.info("初始化初始化默认api-key成功");
            return;
        }
        log.info("已存在默认api-key"+defaultApiKey);
    }
} 