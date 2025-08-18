# Multi-stage Dockerfile for High Performance Cache Middleware
FROM openjdk:17-jdk-slim as builder

# Install Maven
RUN apt-get update && apt-get install -y maven

# Set working directory
WORKDIR /app

# Copy pom files for dependency resolution
COPY pom.xml .
COPY cache-server/pom.xml cache-server/
COPY cache-server/cache-server-core/pom.xml cache-server/cache-server-core/
COPY cache-client-java/pom.xml cache-client-java/
COPY cache-client-java/cache-client-core/pom.xml cache-client-java/cache-client-core/
COPY cache-common/pom.xml cache-common/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY . .

# Build application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM openjdk:17-jre-slim

# Create non-root user
RUN groupadd -r cache && useradd -r -g cache cache

# Install tools for monitoring and debugging
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/cache-server/cache-server-core/target/cache-server-core-*.jar app.jar

# Create directories
RUN mkdir -p /app/data /app/logs /app/config && \
    chown -R cache:cache /app

# Copy default configuration
COPY deployment/docker/application.yml /app/config/

# Switch to non-root user
USER cache

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose ports
EXPOSE 8080 9090 50051

# Environment variables
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication" \
    SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8080 \
    MANAGEMENT_PORT=9090 \
    GRPC_PORT=50051

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]