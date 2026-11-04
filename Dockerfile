# ==========================================
# Stage 1: Build Environment
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache dependency layer by copying only pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable shaded JAR
COPY src ./src
RUN mvn clean package -DskipTests=true

# ==========================================
# Stage 2: Minimal Runtime Environment
# ==========================================
FROM eclipse-temurin:17-jre-alpine

# Enforce security best practice: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy compiled artifact from the builder stage
COPY --from=builder /build/target/devops-java-app-1.0.0.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]