package org.xhy.gateway.interfaces.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 健康检查接口
     */
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now());
        result.put("service", "API Premium Gateway");
        result.put("version", "1.0.0-SNAPSHOT");
        return result;
    }
} 