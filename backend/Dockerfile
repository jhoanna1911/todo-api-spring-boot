# syntax=docker/dockerfile:1.7

# ============================
# Stage 1: Build
# ============================
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace

# Copiar primero los descriptores de build para aprovechar cache de Docker.
# Si pom.xml no cambia, Docker reutiliza las dependencias descargadas.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Ahora copiar el código y compilar
COPY src src
RUN ./mvnw clean package -DskipTests -B \
    && mv target/*.jar app.jar

# ============================
# Stage 2: Runtime
# ============================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/app.jar app.jar

USER spring

EXPOSE 8080

# JVM tuning para contenedores: respeta cgroup limits y usa heap dinámico
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
