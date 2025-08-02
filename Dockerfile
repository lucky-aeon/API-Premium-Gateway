# API Premium Gateway 一体化镜像
# 包含 PostgreSQL 数据库 + Spring Boot 应用

# 第一阶段：构建应用
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# 安装Maven
RUN apt-get update && apt-get install -y wget && \
    wget https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz && \
    tar -xzf apache-maven-3.9.6-bin.tar.gz && \
    mv apache-maven-3.9.6 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/bin/mvn

# 复制Maven配置和源代码
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src

# 构建应用（跳过测试）
RUN mvn clean package -DskipTests -B -s /root/.m2/settings.xml \
    --fail-at-end --batch-mode \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120

# 第二阶段：运行时镜像（基于PostgreSQL）
FROM postgres:15-alpine

# 安装Java运行时和工具
RUN apk add --no-cache \
    openjdk17-jre \
    curl \
    bash \
    supervisor

# 设置PostgreSQL环境变量
ENV POSTGRES_DB=api_gateway
ENV POSTGRES_USER=gateway_user
ENV POSTGRES_PASSWORD=gateway_pass
ENV PGDATA=/var/lib/postgresql/data

# 创建应用目录
WORKDIR /app

# 从构建阶段复制JAR文件
COPY --from=builder /build/target/api-premium-gateway-*.jar /app/app.jar

# 复制数据库初始化脚本
COPY docs/sql/sql.sql /docker-entrypoint-initdb.d/01-init.sql

# 创建supervisor配置文件
RUN mkdir -p /etc/supervisor/conf.d && \
    echo '[supervisord]' > /etc/supervisor/conf.d/supervisord.conf && \
    echo 'nodaemon=true' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'user=root' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'logfile=/var/log/supervisor/supervisord.log' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'pidfile=/var/run/supervisord.pid' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo '' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo '[program:postgres]' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'command=/usr/local/bin/docker-entrypoint.sh postgres' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'user=postgres' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'autostart=true' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'autorestart=true' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'stdout_logfile=/var/log/postgres.log' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'stderr_logfile=/var/log/postgres.log' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'environment=POSTGRES_DB="api_gateway",POSTGRES_USER="gateway_user",POSTGRES_PASSWORD="gateway_pass"' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'priority=100' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo '' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo '[program:gateway]' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'command=java -Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'directory=/app' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'user=root' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'autostart=true' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'autorestart=true' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'stdout_logfile=/var/log/gateway.log' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'stderr_logfile=/var/log/gateway.log' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'priority=200' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo 'startsecs=30' >> /etc/supervisor/conf.d/supervisord.conf

# 创建启动脚本
RUN echo '#!/bin/bash' > /app/start-gateway.sh && \
    echo 'echo "🚀 Starting API Premium Gateway All-in-One"' >> /app/start-gateway.sh && \
    echo 'echo "=========================================="' >> /app/start-gateway.sh && \
    echo '' >> /app/start-gateway.sh && \
    echo '# 初始化PostgreSQL数据目录（如果不存在）' >> /app/start-gateway.sh && \
    echo 'if [ ! -d "$PGDATA" ]; then' >> /app/start-gateway.sh && \
    echo '    echo "🔧 Initializing PostgreSQL database..."' >> /app/start-gateway.sh && \
    echo '    mkdir -p "$PGDATA"' >> /app/start-gateway.sh && \
    echo '    chown -R postgres:postgres "$PGDATA"' >> /app/start-gateway.sh && \
    echo '    su - postgres -c "initdb -D $PGDATA"' >> /app/start-gateway.sh && \
    echo 'fi' >> /app/start-gateway.sh && \
    echo '' >> /app/start-gateway.sh && \
    echo '# 创建必要目录' >> /app/start-gateway.sh && \
    echo 'mkdir -p /var/log/supervisor /app/logs /var/run/postgresql' >> /app/start-gateway.sh && \
    echo 'chown -R postgres:postgres /var/lib/postgresql /var/run/postgresql' >> /app/start-gateway.sh && \
    echo '' >> /app/start-gateway.sh && \
    echo 'echo "🎯 Starting services with supervisor..."' >> /app/start-gateway.sh && \
    echo '' >> /app/start-gateway.sh && \
    echo '# 启动supervisor管理所有服务' >> /app/start-gateway.sh && \
    echo 'exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf' >> /app/start-gateway.sh

RUN chmod +x /app/start-gateway.sh

# 创建入口脚本（用户友好的输出）
RUN echo '#!/bin/bash' > /docker-entrypoint.sh && \
    echo 'echo "🎉 Welcome to API Premium Gateway All-in-One!"' >> /docker-entrypoint.sh && \
    echo 'echo "=============================================="' >> /docker-entrypoint.sh && \
    echo 'echo ""' >> /docker-entrypoint.sh && \
    echo 'echo "🔧 Initializing system..."' >> /docker-entrypoint.sh && \
    echo '' >> /docker-entrypoint.sh && \
    echo '# 启动应用' >> /docker-entrypoint.sh && \
    echo '/app/start-gateway.sh &' >> /docker-entrypoint.sh && \
    echo 'APP_PID=$!' >> /docker-entrypoint.sh && \
    echo '' >> /docker-entrypoint.sh && \
    echo '# 等待服务启动' >> /docker-entrypoint.sh && \
    echo 'echo "⏳ Waiting for services to start (this may take 1-2 minutes)..."' >> /docker-entrypoint.sh && \
    echo 'sleep 45' >> /docker-entrypoint.sh && \
    echo '' >> /docker-entrypoint.sh && \
    echo '# 检查服务状态' >> /docker-entrypoint.sh && \
    echo 'for i in {1..40}; do' >> /docker-entrypoint.sh && \
    echo '    if curl -s http://localhost:8081/api/health >/dev/null 2>&1; then' >> /docker-entrypoint.sh && \
    echo '        echo ""' >> /docker-entrypoint.sh && \
    echo '        echo "✅ All services are ready!"' >> /docker-entrypoint.sh && \
    echo '        echo ""' >> /docker-entrypoint.sh && \
    echo '        echo "🌐 Access URLs:"' >> /docker-entrypoint.sh && \
    echo '        echo "   API Gateway:  http://localhost:8081/api"' >> /docker-entrypoint.sh && \
    echo '        echo "   Health Check: http://localhost:8081/api/health"' >> /docker-entrypoint.sh && \
    echo '        echo "   Database:     postgresql://gateway_user:gateway_pass@localhost:5432/api_gateway"' >> /docker-entrypoint.sh && \
    echo '        echo ""' >> /docker-entrypoint.sh && \
    echo '        echo "📚 Documentation: https://github.com/lucky-aeon/API-Premium-Gateway"' >> /docker-entrypoint.sh && \
    echo '        echo ""' >> /docker-entrypoint.sh && \
    echo '        break' >> /docker-entrypoint.sh && \
    echo '    fi' >> /docker-entrypoint.sh && \
    echo '    ' >> /docker-entrypoint.sh && \
    echo '    if [ $((i % 10)) -eq 0 ]; then' >> /docker-entrypoint.sh && \
    echo '        echo "   Still starting... ($i/40)"' >> /docker-entrypoint.sh && \
    echo '    fi' >> /docker-entrypoint.sh && \
    echo '    sleep 3' >> /docker-entrypoint.sh && \
    echo 'done' >> /docker-entrypoint.sh && \
    echo '' >> /docker-entrypoint.sh && \
    echo '# 保持容器运行' >> /docker-entrypoint.sh && \
    echo 'wait $APP_PID' >> /docker-entrypoint.sh

RUN chmod +x /docker-entrypoint.sh

# 暴露端口
EXPOSE 8081 5432

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:8081/api/health || exit 1

# 设置入口点
ENTRYPOINT ["/docker-entrypoint.sh"] 