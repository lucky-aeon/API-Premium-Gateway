package org.xhy.gateway.interfaces.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xhy.gateway.interfaces.api.common.Result;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * @author xhy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 健康检查接口（不需要API Key）
     */
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "API Premium Gateway");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("version", "1.0.0");
        
        return Result.success("服务运行正常", healthInfo);
    }

    /**
     * 需要API Key的受保护接口（用于测试拦截器）
     */
    @GetMapping("/protected")
    public Result<Map<String, Object>> protectedHealth() {
        Map<String, Object> info = new HashMap<>();
        info.put("message", "这是一个受保护的接口");
        info.put("timestamp", LocalDateTime.now());
        info.put("authenticated", true);
        
        return Result.success("API Key验证成功", info);
    }
} 