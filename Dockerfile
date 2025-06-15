# 多阶段构建：第一阶段 - 构建应用
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /build

# 复制Maven配置文件
COPY settings.xml /root/.m2/settings.xml

# 复制pom.xml和源代码
COPY pom.xml .
COPY src ./src

# 下载依赖并构建应用（跳过测试）
# 添加重试机制和详细日志，使用自定义settings.xml
RUN mvn clean package -DskipTests -B -s /root/.m2/settings.xml \
    --fail-at-end --batch-mode \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120

# 多阶段构建：第二阶段 - 运行时镜像
FROM eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 安装必要的系统工具
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

# 从构建阶段复制jar文件
COPY --from=builder /build/target/api-premium-gateway-*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露应用端口
EXPOSE 8081

# 设置JVM参数和启动命令
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/api/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 