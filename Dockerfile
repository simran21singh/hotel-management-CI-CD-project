# ===================================================================
# Multi-Stage Dockerfile for Spring Boot Hotel Management System
# Production-ready, secure, and optimized for minimal image size
# ===================================================================

# -------------------------------------------------------------------
# STAGE 1: Build the Application
# -------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy Maven POM and pre-fetch dependencies (leverages Docker layer cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source code (including static frontend assets)
COPY src ./src

# Compile and package application (skip tests as CI executes tests prior to dockerization)
RUN mvn clean package -DskipTests

# -------------------------------------------------------------------
# STAGE 2: Lightweight Runtime Environment
# -------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Security: Create non-root system user and group to run container safely
RUN addgroup -S spring && adduser -S spring -G spring

# Copy compiled JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Change ownership of application directory to non-root user
RUN chown -R spring:spring /app

# Switch to unprivileged user
USER spring:spring

# Expose HTTP port
EXPOSE 8080

# Configure JVM flags for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Built-in container health check querying Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Execute Spring Boot fat JAR
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
