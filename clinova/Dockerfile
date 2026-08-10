# ==========================================
# STAGE 1: Build JAR with Maven & OpenJDK 21
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Install curl for healthcheck / tools if needed
RUN apk add --no-cache curl

# Cache Maven wrapper and POM dependencies
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# STAGE 2: Minimal Production Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Add non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create directory for uploads with permissions
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy built JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose Spring Boot port
EXPOSE 8080

# Configure JVM memory limits for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8"

# Run Spring Boot
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]