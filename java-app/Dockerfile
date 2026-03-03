# ============================================
# Multi-stage build for optimized image size
# ============================================

# Stage 1: Builder - compile with Maven
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom.xml first (cache dependencies)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime - minimal JRE image
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from builder
COPY --from=builder /build/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8000/ || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
