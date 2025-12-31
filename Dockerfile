# Build stage
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Gradle wrapper/설정 먼저 복사 (캐시 이점)
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/
RUN chmod +x gradlew

# 소스 복사 후 빌드
COPY src/ src/
RUN ./gradlew clean bootJar --no-daemon

# Runtime stage (JRE)
FROM eclipse-temurin:21-jre
WORKDIR /app

# (선택) actuator healthcheck 쓸 때만 curl 설치
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# non-root 유저
RUN useradd -m appuser
USER appuser

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# (선택) actuator 켜져 있을 때만 사용
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
