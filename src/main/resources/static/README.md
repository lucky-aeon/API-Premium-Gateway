# API网关管理后台

## 简介

这是一个基于Vue3 + Element Plus开发的API网关管理后台系统，提供了项目管理、API Key管理、API实例管理和监控功能。

## 功能特性

### 1. 仪表盘
- 📊 系统整体运行状况概览
- 📈 关键指标统计（项目数、API Key数、实例数、请求量）
- 📋 最近活动记录

### 2. 项目管理
- 📁 查看所有API项目
- 🔍 按项目名称搜索
- 📊 项目统计信息
- 🗑️ 项目删除功能

### 3. API Key管理
- 🔑 生成新的API Key
- 👀 查看所有API Key列表
- ✏️ 编辑API Key信息
- 🔄 启用/禁用API Key
- 📋 复制API Key到剪贴板
- 🗑️ 删除API Key

### 4. API实例管理
- 🔌 查看所有API实例
- 🏷️ 按项目和状态筛选
- 📊 实例状态监控
- 📋 实例详情查看

### 5. 监控中心
- 💚 系统健康状态检查
- 📊 实时监控数据
- 🔄 一键刷新所有数据

## 技术栈

- **前端框架**: Vue 3
- **UI组件库**: Element Plus
- **HTTP客户端**: Axios
- **图标**: Element Plus Icons
- **后端API**: Spring Boot REST API

## 访问方式

### 本地开发环境
```bash
# 启动后端服务
mvn spring-boot:run

# 访问管理后台
http://localhost:8081/api/
```

### 生产环境
```bash
# 访问管理后台
http://your-domain:port/api/
```

## API端点

管理后台调用以下API端点（注意：所有API都需要`/api`前缀）：

### 项目管理
- `GET /api/admin/projects` - 获取项目列表
- `GET /api/admin/projects/{id}` - 获取项目详情
- `DELETE /api/admin/projects/{id}` - 删除项目
- `GET /api/admin/projects/search?projectName={name}` - 搜索项目

### API Key管理
- `GET /api/admin/apikeys` - 获取API Key列表
- `POST /api/admin/apikeys` - 生成新的API Key
- `PUT /api/admin/apikeys/{id}` - 更新API Key
- `PUT /api/admin/apikeys/{id}/status` - 更新API Key状态
- `DELETE /api/admin/apikeys/{id}` - 删除API Key

### API实例管理
- `GET /api/admin/instances?projectId={id}` - 获取实例列表
- `GET /api/admin/instances/{id}` - 获取实例详情
- `GET /api/admin/instances/status/{status}` - 按状态获取实例

### 系统监控
- `GET /api/health` - 健康检查

## 功能说明

### 仪表盘功能
1. **统计卡片**: 显示系统核心指标
2. **最近活动**: 展示系统最新的操作记录
3. **快速导航**: 通过侧边栏快速切换功能模块

### 项目管理功能
1. **列表展示**: 表格形式展示所有项目
2. **搜索过滤**: 支持按项目名称实时搜索
3. **状态标识**: 不同颜色标识项目状态
4. **操作按钮**: 提供查看详情和删除功能

### API Key管理功能
1. **安全显示**: API Key采用掩码方式显示，保护敏感信息
2. **一键复制**: 点击复制按钮快速复制完整API Key
3. **状态管理**: 支持启用/禁用API Key
4. **过期提醒**: 显示API Key过期时间

### API实例管理功能
1. **项目筛选**: 按项目ID筛选相关实例
2. **状态筛选**: 按实例状态筛选（ACTIVE/INACTIVE/MAINTENANCE）
3. **详情查看**: 查看实例的详细配置信息

### 监控中心功能
1. **健康检查**: 实时检查系统服务状态
2. **数据刷新**: 一键刷新所有模块数据
3. **状态展示**: 清晰展示服务运行状态和版本信息

## 数据处理

系统具备完善的错误处理机制：

1. **API异常处理**: 当后端API不可用时，自动使用模拟数据
2. **用户提示**: 使用Element Plus的消息组件提供友好的操作反馈
3. **加载状态**: 所有数据加载过程都有对应的loading状态
4. **数据验证**: 表单提交前进行基础数据验证

## 响应式设计

- 支持不同屏幕尺寸的自适应布局
- 移动端友好的交互设计
- 使用CSS Grid和Flexbox实现灵活布局

## 安全特性

1. **无需登录**: 管理后台为内部系统，不暴露给外部
2. **API Key保护**: 敏感信息采用掩码显示
3. **CORS配置**: 合理的跨域资源共享配置
4. **路径隔离**: 管理接口与业务接口分离

## 扩展性

系统采用模块化设计，易于扩展：

1. **组件化**: Vue组件化开发，便于复用和维护
2. **API抽象**: 统一的API调用层，便于切换后端服务
3. **配置化**: 关键配置项可通过环境变量调整
4. **插件化**: 支持新增功能模块

## 开发调试

### 开发模式
在开发过程中，可以通过浏览器开发者工具查看：
- Network标签：监控API调用
- Console标签：查看日志输出
- Vue DevTools：调试Vue组件状态

### 模拟数据
当后端服务不可用时，系统会自动切换到模拟数据模式，确保前端功能正常开发和测试。

## 注意事项

1. **浏览器兼容性**: 建议使用现代浏览器（Chrome、Firefox、Safari、Edge）
2. **网络要求**: 需要访问CDN资源（Vue、Element Plus等）
3. **后端依赖**: 完整功能需要对应的Spring Boot后端服务
4. **时区设置**: 时间显示基于系统本地时区

## 问题排查

### 常见问题
1. **页面空白**: 检查浏览器控制台是否有JavaScript错误
2. **API调用失败**: 确认后端服务是否正常运行
3. **样式异常**: 检查Element Plus CSS是否正确加载
4. **图标不显示**: 确认Element Plus Icons是否正确加载

### 日志查看
- 前端日志：浏览器开发者工具Console
- 后端日志：Spring Boot应用日志
- 网络请求：浏览器开发者工具Network

---

> 💡 **提示**: 这是一个管理后台系统，主要用于内部管理和监控。在生产环境中，建议配置适当的访问控制和安全措施。 