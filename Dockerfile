# Multi-stage build for Spring Boot application
FROM gradle:8.5-jdk21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle/ gradle/

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jdk

# Install curl, locales, and tzdata for Korean support
RUN apt-get update && apt-get install -y \
    curl \
    locales \
    tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && locale-gen ko_KR.UTF-8

# Set locale and timezone environment variables
ENV LANG=ko_KR.UTF-8
ENV LC_ALL=ko_KR.UTF-8
ENV LANGUAGE=ko_KR:ko
ENV TZ=Asia/Seoul

# Create app user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# ARG 값을 ENV로 전달 (컨테이너 실행 시 사용 가능)
ARG PROFILE
ENV SPRING_PROFILES_ACTIVE=${PROFILE}

# Change ownership to app user
RUN chown appuser:appuser app.jar

# Switch to app user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=10m --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with UTF-8 encoding and Korean timezone
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.language=ko", "-Duser.country=KR", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
