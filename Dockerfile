# syntax=docker/dockerfile:1.6
# Multi-stage Dockerfile para MS-Usuarios Boot
# Stage 1: build con Maven + Java 21
# Stage 2: imagen runtime ligera con JRE 21

# ============== Stage 1: build ==============
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Cache de dependencias
COPY pom.xml .
RUN mvn -B -ntp -q dependency:go-offline

# Build de la app
COPY src ./src
RUN mvn -B -ntp -q clean package -DskipTests \
    && cp target/ms-usuarios-boot.jar /workspace/app.jar

# ============== Stage 2: runtime ==============
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Usuario no-root para runtime
RUN groupadd --system app && useradd --system --gid app app

COPY --from=builder /workspace/app.jar /app/app.jar

# Variables de entorno por defecto
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom" \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

USER app
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
