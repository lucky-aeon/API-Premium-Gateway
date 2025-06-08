package org.xhy.gateway.application.dto;

/**
 * 项目简化数据传输对象
 * 用于下拉选择器等场景，只包含基本信息
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ProjectSimpleDTO {

    /**
     * 项目ID
     */
    private String id;

    /**
     * 项目名称
     */
    private String name;

    public ProjectSimpleDTO() {
    }

    public ProjectSimpleDTO(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
} 