package org.xhy.gateway.domain.apiinstance.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.domain.apiinstance.entity.AffinityBinding;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 亲和性服务
 * 负责管理亲和性绑定关系
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class AffinityService {

    private static final Logger logger = LoggerFactory.getLogger(AffinityService.class);

    /**
     * 亲和性绑定缓存
     * Key: affinityType:affinityKey
     * Value: AffinityBinding
     */
    private final Cache<String, AffinityBinding> affinityBindingCache;

    public AffinityService() {
        this.affinityBindingCache = Caffeine.newBuilder()
                .maximumSize(10000)                                    // 最大存储1万个绑定
                .expireAfterWrite(Duration.ofMinutes(60))              // 60分钟无写入自动过期
                .expireAfterAccess(Duration.ofMinutes(30))             // 30分钟无访问自动过期
                .removalListener(this::onBindingRemoved)               // 绑定移除时的回调
                .build();
    }

    /**
     * 获取绑定的实例ID
     */
    public String getBoundInstance(String affinityType, String affinityKey) {
        String bindingKey = buildBindingKey(affinityType, affinityKey);
        AffinityBinding binding = affinityBindingCache.getIfPresent(bindingKey);

        if (binding != null) {
            if (binding.isExpired()) {
                // 绑定已过期，清除它
                affinityBindingCache.invalidate(bindingKey);
                logger.debug("亲和性绑定已过期，已清除: {}", bindingKey);
                return null;
            }

            logger.debug("找到亲和性绑定: {} -> {}, 使用次数: {}", 
                bindingKey, binding.getInstanceId(), binding.getUseCount());
            return binding.getInstanceId();
        }

        logger.debug("未找到亲和性绑定: {}", bindingKey);
        return null;
    }

    /**
     * 创建新的绑定
     */
    public void createBinding(String affinityType, String affinityKey, String instanceId) {
        String bindingKey = buildBindingKey(affinityType, affinityKey);

        AffinityBinding binding = new AffinityBinding(instanceId);
        affinityBindingCache.put(bindingKey, binding);

        logger.info("创建亲和性绑定: {} -> {}", bindingKey, instanceId);
    }

    /**
     * 刷新绑定（延长过期时间，增加使用次数）
     */
    public void refreshBinding(String affinityType, String affinityKey, String instanceId) {
        String bindingKey = buildBindingKey(affinityType, affinityKey);
        AffinityBinding binding = affinityBindingCache.getIfPresent(bindingKey);

        if (binding != null && binding.getInstanceId().equals(instanceId)) {
            // 创建新的绑定对象，延长过期时间
            LocalDateTime newExpireTime = LocalDateTime.now().plusMinutes(60);
            AffinityBinding refreshedBinding = binding.withNewExpireTime(newExpireTime);

            affinityBindingCache.put(bindingKey, refreshedBinding);

            logger.debug("刷新亲和性绑定: {} -> {}, 使用次数: {}, 新过期时间: {}", 
                bindingKey, instanceId, refreshedBinding.getUseCount(), newExpireTime);
        } else {
            logger.warn("尝试刷新不存在或不匹配的亲和性绑定: {}, 期望实例: {}, 实际绑定: {}", 
                bindingKey, instanceId, binding != null ? binding.getInstanceId() : "null");
        }
    }

    /**
     * 清除绑定
     */
    public void clearBinding(String affinityType, String affinityKey) {
        String bindingKey = buildBindingKey(affinityType, affinityKey);
        AffinityBinding binding = affinityBindingCache.getIfPresent(bindingKey);
        
        affinityBindingCache.invalidate(bindingKey);

        if (binding != null) {
            logger.info("清除亲和性绑定: {} -> {}, 存活时间: {}分钟, 使用次数: {}", 
                bindingKey, binding.getInstanceId(), 
                binding.getAliveDurationMinutes(), binding.getUseCount());
        } else {
            logger.debug("尝试清除不存在的亲和性绑定: {}", bindingKey);
        }
    }

    /**
     * 获取当前绑定数量
     */
    public long getBindingCount() {
        return affinityBindingCache.estimatedSize();
    }

    /**
     * 清除所有绑定（主要用于测试）
     */
    public void clearAllBindings() {
        long count = affinityBindingCache.estimatedSize();
        affinityBindingCache.invalidateAll();
        logger.info("清除所有亲和性绑定，共清除: {} 个", count);
    }

    /**
     * 构建绑定键
     */
    private String buildBindingKey(String affinityType, String affinityKey) {
        return affinityType + ":" + affinityKey;
    }

    /**
     * 绑定被移除时的回调
     */
    private void onBindingRemoved(String key, AffinityBinding binding, RemovalCause cause) {
        if (binding != null) {
            logger.info("亲和性绑定被移除: key={}, instanceId={}, cause={}, 存活时间={}分钟, 使用次数={}", 
                key, binding.getInstanceId(), cause, 
                binding.getAliveDurationMinutes(), binding.getUseCount());
        }
    }
} 