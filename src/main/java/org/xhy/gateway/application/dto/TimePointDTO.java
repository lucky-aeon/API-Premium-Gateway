package org.xhy.gateway.application.dto;

/**
 * 时间点数据DTO
 * 
 * @author xhy
 * @since 1.0.0
 */
public class TimePointDTO {
    
    /**
     * 时间点（格式化后的时间字符串）
     */
    private String time;
    
    /**
     * 对应的数值
     */
    private Double value;
    
    // Constructors
    public TimePointDTO() {}
    
    public TimePointDTO(String time, Double value) {
        this.time = time;
        this.value = value;
    }
    
    // Getters and Setters
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public Double getValue() {
        return value;
    }
    
    public void setValue(Double value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return "TimePointDTO{" +
                "time='" + time + '\'' +
                ", value=" + value +
                '}';
    }
} 