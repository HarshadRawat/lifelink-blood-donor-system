# ====================================================================
# LifeLink - Spring Boot Backend Multi-Stage Dockerfile (Industry Standard)
# ====================================================================

# --------------------------------------------------------------------
# Stage 1: Build & Package with Maven & JDK 17
# --------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 1. Cache Maven dependencies separately to optimize Docker build caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 2. Copy application source code and compile application JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# --------------------------------------------------------------------
# Stage 2: Hardened, Secure Runtime with Slim JRE 17
# --------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runner

# Install curl for container healthcheck and remove package manager caches to minimize CVE attack surface
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Run as non-root user for enterprise container security (Principle of Least Privilege)
RUN groupadd --system --gid 10001 spring && \
    useradd --system --uid 10001 --gid spring --create-home --home-dir /app spring

WORKDIR /app

# Copy the built executable jar from builder stage
COPY --from=builder --chown=spring:spring /build/target/blood-donor-system-*.jar /app/app.jar

# Switch to unprivileged runtime user
USER spring:spring

# Expose Spring Boot HTTP port
EXPOSE 8080

# Configure JVM container resource limits and entropy source
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Container healthcheck using OpenAPI documentation endpoint
HEALTHCHECK --interval=20s --timeout=5s --start-period=35s --retries=5 \
  CMD curl -f http://localhost:8080/v3/api-docs || exit 1

# Start Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
