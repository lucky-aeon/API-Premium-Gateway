const { createApp } = Vue;

// API基础URL - 需要携带context-path前缀
const API_BASE_URL = '/api';

// HTTP请求配置
axios.defaults.baseURL = API_BASE_URL;
axios.defaults.headers.common['Content-Type'] = 'application/json';

const app = createApp({
    data() {
        // 从URL hash获取初始菜单
        const hash = window.location.hash.slice(1);
        const validMenus = ['dashboard', 'projects', 'apikeys', 'instances', 'monitoring'];
        const initialMenu = validMenus.includes(hash) ? hash : 'dashboard';
        
        return {
            // 当前激活的菜单
            activeMenu: initialMenu,
            
            // 统计数据
            stats: {
                totalProjects: 0,
                totalApiKeys: 0,
                totalInstances: 0
            },
            
            // 项目相关
            projects: [],
            projectLoading: false,
            projectSearch: '',
            
            // API Key相关
            apiKeys: [],
            apiKeyLoading: false,
            createApiKeyDialogVisible: false,
            newApiKey: {
                description: '',
                expiresAt: null
            },
            editApiKeyDialogVisible: false,
            editingApiKey: {
                id: '',
                apiKeyValue: '',
                description: '',
                expiresAt: null
            },
            
            // API实例相关
            instances: [],
            instanceLoading: false,
            instanceSearchForm: {
                projectId: '',
                status: ''
            },
            
            // 项目列表（用于筛选）
            projectList: [],
            projectListLoading: false,
            
            // 监控相关
            monitoringStats: {
                totalInstances: 0,
                healthyInstances: 0,
                activeInstances: 0,
                averageSuccessRate: 0,
                averageLatency: 0,
                totalCalls: 0
            },
            monitoringInstances: [],
            monitoringLoading: false,
            monitoringFilter: {
                projectId: '',
                status: '',
                gatewayStatus: ''
            },
            monitoringTimeRange: '24h',
            autoRefreshPaused: false,
            autoRefreshTimer: null,
            alerts: [],
            // 图表实例
            charts: {
                callVolume: null,
                successRate: null,
                latency: null
            },
            // 模拟时间序列数据
            timeSeriesData: {
                callVolume: [],
                successRate: [],
                latency: []
            }

        };
    },
    
    computed: {
        // 过滤后的项目列表
        filteredProjects() {
            if (!this.projectSearch) {
                return this.projects;
            }
            return this.projects.filter(project => 
                project.name.toLowerCase().includes(this.projectSearch.toLowerCase())
            );
        },
        
        // 过滤后的实例列表
        filteredInstances() {
            return this.instances;
        }
    },
    
    mounted() {
        // 监听浏览器前进后退按钮
        window.addEventListener('hashchange', this.handleHashChange);
        
        // 如果URL没有hash，设置默认的dashboard
        if (!window.location.hash) {
            window.location.hash = this.activeMenu;
        }
        
        // 根据当前菜单加载对应数据
        this.initializeMenuData();
    },
    
    beforeUnmount() {
        // 清理事件监听器
        window.removeEventListener('hashchange', this.handleHashChange);
        // 清理自动刷新定时器
        this.stopAutoRefresh();
        // 清理图表实例
        Object.values(this.charts).forEach(chart => {
            if (chart) chart.dispose();
        });
    },
    
    methods: {
        // 获取初始激活菜单
        getInitialActiveMenu() {
            const hash = window.location.hash.slice(1); // 去掉 # 号
            const validMenus = ['dashboard', 'projects', 'apikeys', 'instances', 'monitoring'];
            return validMenus.includes(hash) ? hash : 'dashboard';
        },
        
        // URL hash变化处理
        handleHashChange() {
            const newMenu = this.getInitialActiveMenu();
            if (newMenu !== this.activeMenu) {
                this.activeMenu = newMenu;
                this.initializeMenuData();
            }
        },
        
        // 根据当前菜单初始化数据
        initializeMenuData() {
            console.log('初始化菜单数据:', this.activeMenu);
            
            // 停止之前的自动刷新
            this.stopAutoRefresh();
            
            switch(this.activeMenu) {
                case 'dashboard':
                    this.loadDashboardData();
                    break;
                case 'projects':
                    this.loadProjects();
                    break;
                case 'apikeys':
                    this.loadApiKeys();
                    break;
                case 'instances':
                    this.loadProjectList();
                    this.loadInstances();
                    break;
                case 'monitoring':
                    this.loadProjectList();
                    this.loadMonitoringData();
                    // 启动监控页面的自动刷新
                    this.startAutoRefresh();
                    break;
            }
        },
        
        // 菜单选择处理
        handleMenuSelect(key) {
            console.log('菜单切换到:', key);
            
            // 先清空当前数据，避免显示问题
            this.clearCurrentData();
            
            this.activeMenu = key;
            // 更新URL hash
            window.location.hash = key;
            
            // 延迟加载数据，确保DOM更新完成
            this.$nextTick(() => {
                setTimeout(() => {
                    this.initializeMenuData();
                }, 50);
            });
        },
        
        // 清空当前数据
        clearCurrentData() {
            // 清空各个模块的数据
            this.projects = [];
            this.apiKeys = [];
            this.instances = [];
            this.monitoringInstances = [];
            this.monitoringStats = {
                totalInstances: 0,
                healthyInstances: 0,
                activeInstances: 0,
                averageSuccessRate: 0,
                averageLatency: 0,
                totalCalls: 0
            };
        },
        
        // 加载仪表盘数据
        async loadDashboardData() {
            try {
                // 加载统计数据
                await Promise.all([
                    this.loadProjects(),
                    this.loadApiKeys(),
                    this.loadInstances()
                ]);
                
                // 更新统计信息
                this.stats.totalProjects = this.projects.length;
                this.stats.totalApiKeys = this.apiKeys.filter(key => key.status === 'ACTIVE').length;
                this.stats.totalInstances = this.instances.length;
                
            } catch (error) {
                console.error('加载仪表盘数据失败:', error);
                this.$message.error('加载仪表盘数据失败');
            }
        },
        

        
        // 加载项目列表
        async loadProjects() {
            this.projectLoading = true;
            try {
                const response = await axios.get('/admin/projects');
                if (response.data.code === 200) {
                    this.projects = response.data.data || [];
                } else {
                    this.$message.error(response.data.message || '加载项目列表失败');
                }
            } catch (error) {
                console.error('加载项目列表失败:', error);
                this.$message.error('加载项目列表失败');
                // 使用模拟数据
                this.projects = this.getMockProjects();
            } finally {
                this.projectLoading = false;
            }
        },
        
        // 刷新项目列表
        refreshProjects() {
            this.loadProjects();
        },
        
        // 查看项目详情
        viewProject(project) {
            this.$message.info(`查看项目详情: ${project.name}`);
            // TODO: 实现项目详情弹窗
        },
        
        // 删除项目
        async deleteProject(projectId) {
            try {
                const response = await axios.delete(`/admin/projects/${projectId}`);
                if (response.data.code === 200) {
                    this.$message.success('项目删除成功');
                    this.loadProjects();
                } else {
                    this.$message.error(response.data.message || '删除项目失败');
                }
            } catch (error) {
                console.error('删除项目失败:', error);
                this.$message.error('删除项目失败');
            }
        },
        
        // 加载API Key列表
        async loadApiKeys() {
            this.apiKeyLoading = true;
            try {
                const response = await axios.get('/admin/apikeys');
                if (response.data.code === 200) {
                    this.apiKeys = response.data.data || [];
                } else {
                    this.$message.error(response.data.message || '加载API Key列表失败');
                }
            } catch (error) {
                console.error('加载API Key列表失败:', error);
                this.$message.error('加载API Key列表失败');
                // 使用模拟数据
                this.apiKeys = this.getMockApiKeys();
            } finally {
                this.apiKeyLoading = false;
            }
        },
        
        // 刷新API Key列表
        refreshApiKeys() {
            this.loadApiKeys();
        },
        
        // 显示创建API Key对话框
        showCreateApiKeyDialog() {
            this.newApiKey = {
                description: '',
                expiresAt: null
            };
            this.createApiKeyDialogVisible = true;
        },
        
        // 创建API Key
        async createApiKey() {
            if (!this.newApiKey.description) {
                this.$message.error('请输入API Key描述');
                return;
            }
            
            try {
                const requestData = {
                    description: this.newApiKey.description
                };
                
                // 只有选择了过期时间才传递expiresAt字段
                if (this.newApiKey.expiresAt) {
                    requestData.expiresAt = this.formatDateTime(this.newApiKey.expiresAt);
                }
                
                const response = await axios.post('/admin/apikeys', requestData);
                if (response.data.code === 200) {
                    this.$message.success('API Key生成成功');
                    this.createApiKeyDialogVisible = false;
                    this.loadApiKeys();
                } else {
                    this.$message.error(response.data.message || '生成API Key失败');
                }
            } catch (error) {
                console.error('生成API Key失败:', error);
                this.$message.error('生成API Key失败');
            }
        },
        
        // 编辑API Key
        editApiKey(apiKey) {
            this.editingApiKey = {
                id: apiKey.id,
                apiKeyValue: apiKey.apiKeyValue,
                description: apiKey.description,
                expiresAt: apiKey.expiresAt ? new Date(apiKey.expiresAt) : null
            };
            this.editApiKeyDialogVisible = true;
        },

        // 更新API Key
        async updateApiKey() {
            if (!this.editingApiKey.description) {
                this.$message.error('请输入API Key描述');
                return;
            }
            
            try {
                const requestData = {
                    description: this.editingApiKey.description
                };
                
                // 只有选择了过期时间才传递expiresAt字段
                if (this.editingApiKey.expiresAt) {
                    requestData.expiresAt = this.formatDateTime(this.editingApiKey.expiresAt);
                }
                
                const response = await axios.put(`/admin/apikeys/${this.editingApiKey.id}`, requestData);
                if (response.data.code === 200) {
                    this.$message.success('API Key更新成功');
                    this.editApiKeyDialogVisible = false;
                    this.loadApiKeys();
                } else {
                    this.$message.error(response.data.message || '更新API Key失败');
                }
            } catch (error) {
                console.error('更新API Key失败:', error);
                this.$message.error('更新API Key失败');
            }
        },
        
        // 切换API Key状态
        async toggleApiKeyStatus(apiKey) {
            const newStatus = apiKey.status === 'ACTIVE' ? 'REVOKED' : 'ACTIVE';
            try {
                const response = await axios.put(`/admin/apikeys/${apiKey.id}/status`, {
                    status: newStatus
                });
                if (response.data.code === 200) {
                    this.$message.success('API Key状态更新成功');
                    this.loadApiKeys();
                } else {
                    this.$message.error(response.data.message || '更新API Key状态失败');
                }
            } catch (error) {
                console.error('更新API Key状态失败:', error);
                this.$message.error('更新API Key状态失败');
            }
        },
        
        // 删除API Key
        async deleteApiKey(apiKeyId) {
            try {
                const response = await axios.delete(`/admin/apikeys/${apiKeyId}`);
                if (response.data.code === 200) {
                    this.$message.success('API Key删除成功');
                    this.loadApiKeys();
                } else {
                    this.$message.error(response.data.message || '删除API Key失败');
                }
            } catch (error) {
                console.error('删除API Key失败:', error);
                this.$message.error('删除API Key失败');
            }
        },
        
        // 复制API Key
        async copyApiKey(keyValue) {
            try {
                await navigator.clipboard.writeText(keyValue);
                this.$message.success('API Key已复制到剪贴板');
            } catch (error) {
                console.error('复制失败:', error);
                this.$message.error('复制失败');
            }
        },
        

        
        // 获取API Key状态类型
        getApiKeyStatusType(status) {
            switch(status) {
                case 'ACTIVE': return 'success';
                case 'REVOKED': return 'danger';
                case 'EXPIRED': return 'warning';
                case 'UNUSED': return 'info';
                default: return 'info';
            }
        },
        
        // 格式化日期时间为后端期望的格式
        formatDateTime(date) {
            if (!date) return null;
            
            // 如果是字符串，直接返回（适用于显示）
            if (typeof date === 'string') {
                return this.formatDisplayDateTime(date);
            }
            
            // 如果是Date对象，格式化为后端期望的格式
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            const seconds = String(date.getSeconds()).padStart(2, '0');
            
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        },
        
        // 格式化显示用的日期时间
        formatDisplayDateTime(dateTimeStr) {
            if (!dateTimeStr) return null;
            
            // 处理 ISO 格式的日期字符串
            const date = new Date(dateTimeStr);
            if (isNaN(date.getTime())) return dateTimeStr;
            
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            const seconds = String(date.getSeconds()).padStart(2, '0');
            
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        },
        
        // 加载项目列表（用于筛选）
        async loadProjectList() {
            this.projectListLoading = true;
            try {
                const response = await axios.get('/admin/projects/simple');
                if (response.data.code === 200) {
                    this.projectList = response.data.data || [];
                } else {
                    this.$message.error(response.data.message || '获取项目列表失败');
                }
            } catch (error) {
                console.error('获取项目列表失败:', error);
                this.$message.error('获取项目列表失败');
            } finally {
                this.projectListLoading = false;
            }
        },

        // 加载API实例列表
        async loadInstances() {
            this.instanceLoading = true;
            try {
                const response = await axios.get('/admin/instances/with-projects', {
                    params: {
                        projectId: this.instanceSearchForm.projectId,
                        status: this.instanceSearchForm.status
                    }
                });
                
                if (response.data.code === 200) {
                    this.instances = response.data.data || [];
                } else {
                    this.$message.error(response.data.message || '获取API实例列表失败');
                    // 使用模拟数据
                    this.instances = this.getMockInstances();
                }
            } catch (error) {
                console.error('加载API实例列表失败:', error);
                this.$message.error('加载API实例列表失败');
                // 使用模拟数据
                this.instances = this.getMockInstances();
            } finally {
                this.instanceLoading = false;
            }
        },
        
        // 刷新API实例列表
        refreshInstances() {
            this.loadProjectList();
            this.loadInstances();
        },
        
        // 查看实例详情
        viewInstance(instance) {
            this.$message.info(`查看实例详情: ${instance.businessId}`);
            // TODO: 实现实例详情弹窗
        },
        
        // 获取实例状态类型
        getInstanceStatusType(status) {
            switch(status) {
                case 'ACTIVE': return 'success';
                case 'INACTIVE': return 'danger';
                case 'MAINTENANCE': return 'warning';
                default: return 'info';
            }
        },
        

        
        // 获取模拟项目数据
        getMockProjects() {
            return [
                {
                    id: 'proj_001',
                    name: 'Demo Project',
                    description: '演示项目',
                    status: 'ACTIVE',
                    createdAt: '2024-01-15 10:00:00'
                },
                {
                    id: 'proj_002',
                    name: 'Test Project',
                    description: '测试项目',
                    status: 'INACTIVE',
                    createdAt: '2024-01-14 15:30:00'
                }
            ];
        },
        
        // 获取模拟API Key数据
        getMockApiKeys() {
            return [
                {
                    id: 'key_001',
                    apiKeyValue: 'ak_test_1234567890abcdef1234567890abcdef',
                    description: '测试API Key',
                    status: 'ACTIVE',
                    expiresAt: '2024-12-31 23:59:59',
                    createdAt: '2024-01-15 10:00:00'
                },
                {
                    id: 'key_002',
                    apiKeyValue: 'ak_demo_abcdef1234567890abcdef1234567890',
                    description: '演示API Key',
                    status: 'INACTIVE',
                    expiresAt: '2024-06-30 23:59:59',
                    createdAt: '2024-01-14 15:30:00'
                }
            ];
        },
        
        // 获取模拟API实例数据
        getMockInstances() {
            return [
                {
                    projectName: 'Demo Project',
                    businessId: 'biz_001',
                    apiType: 'REST',
                    status: 'ACTIVE'
                },
                {
                    projectName: 'Demo Project',
                    businessId: 'biz_002',
                    apiType: 'GraphQL',
                    status: 'INACTIVE'
                }
            ];
        },

        // ===== 监控相关方法 =====
        
        // 加载监控数据
        async loadMonitoringData() {
            await Promise.all([
                this.loadMonitoringOverview(),
                this.loadMonitoringInstances(),
                this.loadTimeSeriesData()
            ]);
            
            // 延迟初始化图表，确保DOM已渲染
            this.$nextTick(() => {
                setTimeout(() => {
                    this.initCharts();
                    this.updateAlerts();
                }, 100);
            });
        },
        
        // 启动自动刷新
        startAutoRefresh() {
            if (this.autoRefreshTimer) {
                clearInterval(this.autoRefreshTimer);
            }
            
            this.autoRefreshTimer = setInterval(() => {
                if (!this.autoRefreshPaused && this.activeMenu === 'monitoring') {
                    console.log('自动刷新监控数据');
                    this.loadMonitoringData();
                }
            }, 60000); // 1分钟刷新
        },
        
        // 停止自动刷新
        stopAutoRefresh() {
            if (this.autoRefreshTimer) {
                clearInterval(this.autoRefreshTimer);
                this.autoRefreshTimer = null;
            }
        },
        
        // 切换自动刷新状态
        toggleAutoRefresh() {
            this.autoRefreshPaused = !this.autoRefreshPaused;
            if (!this.autoRefreshPaused && this.activeMenu === 'monitoring') {
                this.loadMonitoringData();
            }
        },
        
        // 加载监控概览
        async loadMonitoringOverview() {
            try {
                const params = {};
                if (this.monitoringFilter.projectId) {
                    params.projectId = this.monitoringFilter.projectId;
                }
                
                const response = await axios.get('/admin/monitoring/overview', { params });
                if (response.data.code === 200) {
                    this.monitoringStats = response.data.data || {
                        totalInstances: 0,
                        healthyInstances: 0,
                        activeInstances: 0,
                        averageSuccessRate: 0,
                        averageLatency: 0,
                        totalCalls: 0
                    };
                } else {
                    this.$message.error(response.data.message || '加载监控概览失败');
                }
            } catch (error) {
                console.error('加载监控概览失败:', error);
                this.$message.error('加载监控概览失败');
                // 使用模拟数据
                this.monitoringStats = this.getMockMonitoringStats();
            }
        },
        
        // 加载监控实例列表
        async loadMonitoringInstances() {
            this.monitoringLoading = true;
            try {
                const params = {};
                if (this.monitoringFilter.projectId) {
                    params.projectId = this.monitoringFilter.projectId;
                }
                if (this.monitoringFilter.status) {
                    params.status = this.monitoringFilter.status;
                }
                if (this.monitoringFilter.gatewayStatus) {
                    params.gatewayStatus = this.monitoringFilter.gatewayStatus;
                }
                
                console.log('请求监控实例数据，参数:', params);
                const response = await axios.get('/admin/monitoring/instances', { params });
                console.log('监控实例API响应:', response.data);
                
                if (response.data.code === 200) {
                    this.monitoringInstances = response.data.data || [];
                    console.log('监控实例数据加载成功，数量:', this.monitoringInstances.length);
                } else {
                    console.error('监控实例API错误:', response.data);
                    this.$message.error(response.data.message || '加载监控实例列表失败');
                    // 如果API出错，使用模拟数据
                    this.monitoringInstances = this.getMockMonitoringInstances();
                }
            } catch (error) {
                console.error('加载监控实例列表失败:', error);
                this.$message.error('加载监控实例列表失败');
                // 使用模拟数据
                this.monitoringInstances = this.getMockMonitoringInstances();
                console.log('使用模拟数据，数量:', this.monitoringInstances.length);
            } finally {
                this.monitoringLoading = false;
            }
        },
        
        // 刷新监控数据
        async refreshMonitoringData() {
            this.loadMonitoringData();
        },
        
        // 加载时间序列数据
        async loadTimeSeriesData() {
            try {
                console.log('开始加载时间序列数据, timeRange:', this.monitoringTimeRange);
                
                // 构建请求参数
                const params = { timeRange: this.monitoringTimeRange };
                if (this.monitoringFilter.projectId) {
                    params.projectId = this.monitoringFilter.projectId;
                }
                
                // 调用后端API获取时间序列数据
                const response = await axios.get('/admin/monitoring/timeseries', { params });
                
                if (response.data && response.data.code === 200 && response.data.data) {
                    this.timeSeriesData = response.data.data;
                    console.log('从后端加载时间序列数据成功:', this.timeSeriesData);
                } else {
                    console.warn('后端返回数据格式异常，使用模拟数据');
                    console.log('后端响应:', response.data);
                    this.timeSeriesData = this.generateMockTimeSeriesData();
                }
            } catch (error) {
                console.error('加载时间序列数据失败:', error);
                console.log('使用模拟数据作为fallback');
                this.timeSeriesData = this.generateMockTimeSeriesData();
            }
        },
        
        // 生成模拟时间序列数据
        generateMockTimeSeriesData() {
            const now = new Date();
            const hours = this.getHoursFromTimeRange(this.monitoringTimeRange);
            const interval = Math.max(1, Math.floor(hours / 24)); // 最多24个数据点
            
            const callVolumeData = [];
            const successRateData = [];
            const latencyData = [];
            
            for (let i = hours; i >= 0; i -= interval) {
                const time = new Date(now.getTime() - i * 60 * 60 * 1000);
                const timeStr = time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
                
                // 模拟调用量数据（波动）
                const baseVolume = 100 + Math.sin(i / 4) * 30;
                const volume = Math.max(0, Math.floor(baseVolume + (Math.random() - 0.5) * 40));
                
                // 模拟成功率（偶尔降低）
                const baseSuccessRate = 98;
                const successRate = Math.random() < 0.1 ? 
                    85 + Math.random() * 10 : 
                    baseSuccessRate + (Math.random() - 0.5) * 4;
                
                // 模拟延迟（偶尔尖峰）
                const baseLatency = 200;
                const latency = Math.random() < 0.05 ? 
                    800 + Math.random() * 1200 : 
                    baseLatency + Math.random() * 300;
                
                callVolumeData.push({ time: timeStr, value: volume });
                successRateData.push({ time: timeStr, value: Math.min(100, Math.max(0, successRate)) });
                latencyData.push({ time: timeStr, value: Math.max(0, latency) });
            }
            
            return {
                callVolume: callVolumeData.reverse(),
                successRate: successRateData.reverse(),
                latency: latencyData.reverse()
            };
        },
        
        // 从时间范围获取小时数
        getHoursFromTimeRange(timeRange) {
            const rangeMap = {
                '1h': 1,
                '6h': 6,
                '24h': 24,
                '7d': 168
            };
            return rangeMap[timeRange] || 24;
        },
        
        // 初始化图表
        initCharts() {
            this.initCallVolumeChart();
            this.initSuccessRateChart();
            this.initLatencyChart();
        },
        
        // 初始化调用量趋势图
        initCallVolumeChart() {
            if (!this.$refs.callVolumeChart) return;
            
            if (this.charts.callVolume) {
                this.charts.callVolume.dispose();
            }
            
            this.charts.callVolume = echarts.init(this.$refs.callVolumeChart);
            const option = {
                grid: { top: 40, right: 40, bottom: 60, left: 60 },
                xAxis: {
                    type: 'category',
                    data: this.timeSeriesData.callVolume.map(item => item.time),
                    axisLabel: { fontSize: 12 }
                },
                yAxis: {
                    type: 'value',
                    name: '调用次数',
                    axisLabel: { fontSize: 12 }
                },
                series: [{
                    type: 'line',
                    data: this.timeSeriesData.callVolume.map(item => item.value),
                    smooth: true,
                    itemStyle: { color: '#409eff' },
                    areaStyle: { 
                        color: {
                            type: 'linear',
                            x: 0, y: 0, x2: 0, y2: 1,
                            colorStops: [
                                { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
                                { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
                            ]
                        }
                    }
                }],
                tooltip: {
                    trigger: 'axis',
                    formatter: '{b}<br/>调用量: {c}次'
                }
            };
            this.charts.callVolume.setOption(option);
        },
        
        // 初始化成功率趋势图
        initSuccessRateChart() {
            if (!this.$refs.successRateChart) return;
            
            if (this.charts.successRate) {
                this.charts.successRate.dispose();
            }
            
            this.charts.successRate = echarts.init(this.$refs.successRateChart);
            const option = {
                grid: { top: 40, right: 40, bottom: 60, left: 60 },
                xAxis: {
                    type: 'category',
                    data: this.timeSeriesData.successRate.map(item => item.time),
                    axisLabel: { fontSize: 12 }
                },
                yAxis: {
                    type: 'value',
                    name: '成功率(%)',
                    min: 80,
                    max: 100,
                    axisLabel: { fontSize: 12 }
                },
                series: [{
                    type: 'line',
                    data: this.timeSeriesData.successRate.map(item => item.value),
                    smooth: true,
                    itemStyle: { color: '#67c23a' },
                    markLine: {
                        data: [{ yAxis: 95, lineStyle: { color: '#e6a23c', type: 'dashed' } }],
                        label: { formatter: 'SLA: 95%' }
                    }
                }],
                tooltip: {
                    trigger: 'axis',
                    formatter: '{b}<br/>成功率: {c}%'
                }
            };
            this.charts.successRate.setOption(option);
        },
        
        // 初始化延迟分布图
        initLatencyChart() {
            if (!this.$refs.latencyChart) return;
            
            if (this.charts.latency) {
                this.charts.latency.dispose();
            }
            
            this.charts.latency = echarts.init(this.$refs.latencyChart);
            const option = {
                grid: { top: 40, right: 40, bottom: 60, left: 60 },
                xAxis: {
                    type: 'category',
                    data: this.timeSeriesData.latency.map(item => item.time),
                    axisLabel: { fontSize: 12 }
                },
                yAxis: {
                    type: 'value',
                    name: '延迟(ms)',
                    axisLabel: { fontSize: 12 }
                },
                series: [{
                    type: 'bar',
                    data: this.timeSeriesData.latency.map(item => ({
                        value: item.value,
                        itemStyle: { 
                            color: item.value > 1000 ? '#f56c6c' : 
                                   item.value > 500 ? '#e6a23c' : '#67c23a'
                        }
                    })),
                    barWidth: '60%'
                }],
                tooltip: {
                    trigger: 'axis',
                    formatter: '{b}<br/>延迟: {c}ms'
                }
            };
            this.charts.latency.setOption(option);
        },
        
        // 更新告警信息
        updateAlerts() {
            this.alerts = [];
            
            this.monitoringInstances.forEach(instance => {
                // 检查成功率告警
                if (instance.successRate < 90) {
                    this.alerts.push({
                        id: `success-${instance.instanceId}`,
                        level: 'critical',
                        message: `${instance.businessId} 成功率严重偏低: ${this.formatPercentage(instance.successRate)}`
                    });
                } else if (instance.successRate < 95) {
                    this.alerts.push({
                        id: `success-${instance.instanceId}`,
                        level: 'warning',
                        message: `${instance.businessId} 成功率偏低: ${this.formatPercentage(instance.successRate)}`
                    });
                }
                
                // 检查延迟告警
                if (instance.averageLatency > 2000) {
                    this.alerts.push({
                        id: `latency-${instance.instanceId}`,
                        level: 'critical',
                        message: `${instance.businessId} 延迟严重超标: ${this.formatLatency(instance.averageLatency)}`
                    });
                } else if (instance.averageLatency > 1000) {
                    this.alerts.push({
                        id: `latency-${instance.instanceId}`,
                        level: 'warning',
                        message: `${instance.businessId} 延迟超标: ${this.formatLatency(instance.averageLatency)}`
                    });
                }
                
                // 检查熔断状态
                if (instance.gatewayStatus === 'CIRCUIT_BREAKER_OPEN') {
                    this.alerts.push({
                        id: `circuit-${instance.instanceId}`,
                        level: 'critical',
                        message: `${instance.businessId} 服务熔断中`
                    });
                }
            });
            
            console.log('告警信息更新完成:', this.alerts.length);
        },
        
        // 查看实例详情
        viewInstanceDetails(instance) {
            this.$message.info(`查看实例详情: ${instance.businessId}`);
            // TODO: 实现实例详情弹窗
        },
        
        // 获取网关状态类型
        getGatewayStatusType(status) {
            switch(status) {
                case 'HEALTHY':
                    return 'success';
                case 'DEGRADED':
                    return 'warning';
                case 'CIRCUIT_BREAKER_OPEN':
                    return 'danger';
                default:
                    return 'info';
            }
        },
        
        // 获取网关状态文本
        getGatewayStatusText(status) {
            switch(status) {
                case 'HEALTHY':
                    return '健康';
                case 'DEGRADED':
                    return '降级';
                case 'CIRCUIT_BREAKER_OPEN':
                    return '熔断';
                default:
                    return '未知';
            }
        },
        
        // 获取成功率样式类
        getSuccessRateClass(rate) {
            if (rate >= 95) return 'text-success';
            if (rate >= 85) return 'text-warning';
            return 'text-danger';
        },
        
        // 获取延迟样式类
        getLatencyClass(latency) {
            if (latency <= 500) return 'text-success';
            if (latency <= 2000) return 'text-warning';
            return 'text-danger';
        },
        
        // 格式化百分比
        formatPercentage(value) {
            if (value == null || value === undefined) return '-';
            return `${Number(value).toFixed(1)}%`;
        },
        
        // 格式化延迟
        formatLatency(value) {
            if (value == null || value === undefined) return '-';
            if (value < 1000) {
                return `${Math.round(value)}ms`;
            } else {
                return `${(value / 1000).toFixed(1)}s`;
            }
        },
        
        // 计算QPS
        calculateQPS(recentCalls) {
            if (!recentCalls) return 0;
            // 假设recentCalls是最近一分钟的调用次数
            return Math.round(recentCalls / 60);
        },
        
        // 获取实例卡片样式类
        getInstanceCardClass(instance) {
            if (instance.gatewayStatus === 'CIRCUIT_BREAKER_OPEN' || instance.successRate < 90) {
                return 'critical';
            }
            if (instance.successRate < 95 || instance.averageLatency > 1000) {
                return 'warning';
            }
            return 'healthy';
        },
        
        // 获取实例状态点样式类
        getInstanceStatusClass(instance) {
            const cardClass = this.getInstanceCardClass(instance);
            if (cardClass === 'critical') return 'status-critical';
            if (cardClass === 'warning') return 'status-warning';
            return 'status-healthy';
        },
        
        // 获取成功率值样式类
        getSuccessRateValueClass(rate) {
            if (rate < 90) return 'critical';
            if (rate < 95) return 'warning';
            return 'success';
        },
        
        // 获取延迟值样式类
        getLatencyValueClass(latency) {
            if (latency > 2000) return 'critical';
            if (latency > 1000) return 'warning';
            return 'success';
        },
        
        // 获取模拟监控统计数据
        getMockMonitoringStats() {
            return {
                totalInstances: 5,
                healthyInstances: 4,
                activeInstances: 5,
                averageSuccessRate: 96.5,
                averageLatency: 245,
                totalCalls: 12500
            };
        },
        
        // 获取模拟监控实例数据
        getMockMonitoringInstances() {
            return [
                {
                    instanceId: 'inst_001',
                    projectName: 'Demo Project',
                    businessId: 'gpt4o-001',
                    apiIdentifier: 'gpt4o',
                    apiType: 'MODEL',
                    status: 'ACTIVE',
                    gatewayStatus: 'HEALTHY',
                    successRate: 98.5,
                    averageLatency: 180,
                    concurrency: 3,
                    recentCalls: 1250,
                    lastReportedAt: '2024-01-15T10:30:00'
                },
                {
                    instanceId: 'inst_002',
                    projectName: 'Demo Project',
                    businessId: 'gpt4o-002',
                    apiIdentifier: 'gpt4o',
                    apiType: 'MODEL',
                    status: 'ACTIVE',
                    gatewayStatus: 'DEGRADED',
                    successRate: 85.2,
                    averageLatency: 850,
                    concurrency: 1,
                    recentCalls: 650,
                    lastReportedAt: '2024-01-15T10:25:00'
                }
            ];
        }
    }
});

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

// 使用Element Plus
app.use(ElementPlus);

// 挂载应用
app.mount('#app'); 