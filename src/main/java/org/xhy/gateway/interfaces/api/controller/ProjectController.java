package org.xhy.gateway.interfaces.api.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.gateway.application.dto.ProjectDTO;
import org.xhy.gateway.application.service.ProjectAppService;
import org.xhy.gateway.interfaces.api.common.Result;
import org.xhy.gateway.interfaces.api.request.ProjectCreateRequest;

import java.util.List;

/**
 * 项目控制器
 * 提供项目相关的REST API接口
 * 
 * @author xhy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/projects")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectAppService projectAppService;

    public ProjectController(ProjectAppService projectAppService) {
        this.projectAppService = projectAppService;
    }

    /**
     * 创建项目
     */
    @PostMapping
    public Result<ProjectDTO> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        logger.info("接收创建项目请求: {}", request);

        ProjectDTO result = projectAppService.createProject(request);
        
        logger.info("创建项目成功，项目ID: {}", result.getId());
        return Result.success("项目创建成功", result);
    }

    /**
     * 根据ID获取项目详情
     */
    @GetMapping("/{id}")
    public Result<ProjectDTO> getProjectById(@PathVariable String id) {
        logger.debug("接收获取项目详情请求，项目ID: {}", id);

        ProjectDTO result = projectAppService.getProjectById(id);
        
        return Result.success(result);
    }

    /**
     * 获取项目列表
     */
    @GetMapping
    public Result<List<ProjectDTO>> getProjectList() {
        logger.debug("接收获取项目列表请求");

        List<ProjectDTO> result = projectAppService.getProjectList();
        
        return Result.success(result);
    }
} 