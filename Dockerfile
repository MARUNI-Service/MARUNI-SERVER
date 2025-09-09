# Multi-stage build for better optimization
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# 의존성 캐싱을 위해 build 파일들만 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

# 의존성 다운로드 (캐싱됨)
RUN gradle dependencies --no-daemon

# 소스코드 복사
COPY . .

# Gradle build (테스트 스킵하고 빌드만)
RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM openjdk:21-jdk-slim

WORKDIR /app

# 애플리케이션 실행에 필요한 패키지 설치
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 환경변수 설정 (기본값)
ENV SPRING_PROFILES_ACTIVE=dev
ENV SERVER_PORT=8080

# 포트 노출
EXPOSE 8080

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행 (환경별 프로파일 지원)
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]