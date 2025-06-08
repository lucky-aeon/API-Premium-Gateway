package org.xhy.gateway.application.dto;

import java.util.List;

/**
 * 时间序列数据DTO
 * 
 * @author xhy
 * @since 1.0.0
 */
public class TimeSeriesDTO {
    
    /**
     * 调用量时间序列
     */
    private List<TimePointDTO> callVolume;
    
    /**
     * 成功率时间序列
     */
    private List<TimePointDTO> successRate;
    
    /**
     * 延迟时间序列
     */
    private List<TimePointDTO> latency;
    
    // Constructors
    public TimeSeriesDTO() {}
    
    public TimeSeriesDTO(List<TimePointDTO> callVolume, 
                        List<TimePointDTO> successRate, 
                        List<TimePointDTO> latency) {
        this.callVolume = callVolume;
        this.successRate = successRate;
        this.latency = latency;
    }
    
    // Getters and Setters
    public List<TimePointDTO> getCallVolume() {
        return callVolume;
    }
    
    public void setCallVolume(List<TimePointDTO> callVolume) {
        this.callVolume = callVolume;
    }
    
    public List<TimePointDTO> getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(List<TimePointDTO> successRate) {
        this.successRate = successRate;
    }
    
    public List<TimePointDTO> getLatency() {
        return latency;
    }
    
    public void setLatency(List<TimePointDTO> latency) {
        this.latency = latency;
    }
    
    @Override
    public String toString() {
        return "TimeSeriesDTO{" +
                "callVolume=" + (callVolume != null ? callVolume.size() : 0) + " points, " +
                "successRate=" + (successRate != null ? successRate.size() : 0) + " points, " +
                "latency=" + (latency != null ? latency.size() : 0) + " points" +
                '}';
    }
} 