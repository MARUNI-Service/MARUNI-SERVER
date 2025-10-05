# 기술 스택 & 환경 설정

**MARUNI 프로젝트 기술 스택, 의존성, 환경 설정**

---

## 🛠️ 기술 스택

### **Core Framework**
```yaml
Java: 21 (LTS)
Spring Boot: 3.5.3
Spring Framework: 6.x
Build Tool: Gradle 8.x
```

### **Database**
```yaml
Primary Database: PostgreSQL 15+
Connection Pool: HikariCP (Spring Boot Default)
ORM: Spring Data JPA + Hibernate 6.x
```

### **AI & Integration**
```yaml
AI Framework: Spring AI 1.0.0-M3
AI Provider: OpenAI GPT-4o
HTTP Client: Spring WebClient
Scheduling: Spring @Scheduled
Push Notification: Firebase Admin SDK 9.5.0
```

### **Security & Authentication**
```yaml
Framework: Spring Security 6.x
Authentication: JWT (JSON Web Token)
JWT Library: JJWT 0.12.5
Password Encoding: BCryptPasswordEncoder
OAuth2: Spring OAuth2 Client
Token Storage: Stateless (Client-side)
```

### **Documentation & Validation**
```yaml
API Documentation: Swagger/OpenAPI 3.0 (SpringDoc 2.8.8)
Validation: Hibernate Validator (Bean Validation)
Development Tools: Lombok
Monitoring: Spring Boot Actuator
```

### **Testing**
```yaml
Test Framework: JUnit 5
Mocking: Mockito
Integration Tests: Spring Boot Test
Test Database: H2 (In-Memory)
API Testing: MockWebServer
```

### **Deployment & Infrastructure**
```yaml
Containerization: Docker + Docker Compose
Configuration: External .env files
Logging: Logback (Spring Boot Default)
Metrics: Micrometer + Prometheus
```

---

## 📦 의존성 관리 (build.gradle)

### **Spring Boot & Core**
```gradle
dependencies {
    // Spring Boot 스타터
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Spring AI (BOM 사용)
    implementation platform('org.springframework.ai:spring-ai-bom:1.0.0-M3')
    implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'
}
```

### **Security & Authentication**
```gradle
dependencies {
    // Spring Security
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    testImplementation 'org.springframework.security:spring-security-test'

    // JWT 라이브러리
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    implementation 'io.jsonwebtoken:jjwt-impl:0.12.5'
    implementation 'io.jsonwebtoken:jjwt-jackson:0.12.5'
}
```

### **Database**
```gradle
dependencies {
    // PostgreSQL
    runtimeOnly 'org.postgresql:postgresql'

    // Test Database
    testRuntimeOnly 'com.h2database:h2'
}
```

### **Firebase & Push Notification**
```gradle
dependencies {
    // Firebase Admin SDK (서버에서 푸시 알림 발송용)
    implementation 'com.google.firebase:firebase-admin:9.5.0'

    // Google Cloud 인증 (Firebase 서비스 계정 인증용)
    implementation 'com.google.auth:google-auth-library-oauth2-http:1.39.0'
}
```

### **Documentation & Validation**
```gradle
dependencies {
    // API 문서화
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8'

    // 유효성 검사
    implementation 'org.hibernate.validator:hibernate-validator'

    // 개발 편의성
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

### **Testing**
```gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## ⚙️ 환경 설정

### **application.yml (공통 설정)**
```yaml
server:
  port: 8080

spring:
  profiles:
    active: dev
  config:
    import:
      - optional:file:.env[.properties]

  # Spring AI OpenAI 설정
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your_openai_api_key_here}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4o}
          temperature: ${OPENAI_TEMPERATURE:0.7}
          max-tokens: ${OPENAI_MAX_TOKENS:100}

# JWT 설정
jwt:
  secret-key: ${JWT_SECRET_KEY:your_jwt_secret_key_at_least_32_characters}
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION:3600000}  # 1시간

# Swagger 설정
swagger:
  api:
    title: "MARUNI API Documentation"
    description: "REST API Documentation for MARUNI elderly care service"
    version: "v1.0.0"
  contact:
    name: "MARUNI Development Team"
    email: "dev@maruni.com"

springdoc:
  swagger-ui:
    path: /api-docs

# MARUNI 커스텀 설정
maruni:
  scheduling:
    daily-check:
      cron: "0 0 9 * * *"  # 매일 오전 9시
    retry:
      cron: "0 */5 * * * *"  # 5분마다
  notification:
    push:
      enabled: true
```

### **개발 환경 (.env 파일)**
```env
# Database 설정
DB_USERNAME=postgres
DB_PASSWORD=your_db_password
DB_URL=jdbc:postgresql://localhost:5432/maruni_dev

# JWT 설정 (필수 - 32자 이상)
JWT_SECRET_KEY=your_super_secret_jwt_key_at_least_32_characters_long_for_security
JWT_ACCESS_EXPIRATION=3600000

# OpenAI API 설정
OPENAI_API_KEY=sk-your-openai-api-key-here
OPENAI_MODEL=gpt-4o
OPENAI_MAX_TOKENS=100
OPENAI_TEMPERATURE=0.7

# Firebase 설정 (알림용)
FIREBASE_PROJECT_ID=maruni-project
FIREBASE_PRIVATE_KEY_PATH=config/firebase-service-account.json

# 암호화 설정
ENCRYPTION_KEY=maruni_encryption_key_32_bytes_long
```

---

## 🐳 Docker 환경

### **docker-compose.yml**
```yaml
services:
  db:
    image: postgres:15
    container_name: postgres-db
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: maruni-db
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_INITDB_ARGS: "--encoding=UTF8"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d maruni-db"]
      start_period: 10s
      interval: 5s
      timeout: 10s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: maruni-app
    restart: unless-stopped
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      SWAGGER_SERVER_URL: ${SWAGGER_SERVER_URL:-http://localhost:8080}
    env_file:
      - .env
    networks:
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

volumes:
  postgres-data:
    driver: local

networks:
  backend:
    driver: bridge
```

### **docker-compose.prod.yml (운영 환경)**
```yaml
services:
  db:
    image: postgres:15
    container_name: postgres-db
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: maruni-db
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_INITDB_ARGS: "--encoding=UTF8"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d maruni-db"]
      start_period: 10s
      interval: 5s
      timeout: 10s
      retries: 5

  app:
    # Docker Hub에서 이미지 pull
    image: ${DOCKER_IMAGE:-your-dockerhub-username/maruni-server:latest}
    container_name: maruni-app
    restart: unless-stopped
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      SWAGGER_SERVER_URL: ${SWAGGER_SERVER_URL:-http://localhost:8080}
    env_file:
      - .env
    networks:
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

volumes:
  postgres-data:
    driver: local

networks:
  backend:
    driver: bridge
```

### **Dockerfile**
```dockerfile
# Multi-stage build
FROM eclipse-temurin:21-jdk-alpine AS build

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 래퍼와 빌드 스크립트 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# 운영 환경 설정
RUN addgroup -g 1001 -S maruni && \
    adduser -u 1001 -S maruni -G maruni

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 로그 디렉토리 생성
RUN mkdir -p /app/logs && chown -R maruni:maruni /app

USER maruni

# JVM 최적화 옵션
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 🔧 개발 환경 설정

### **IDE 설정 (IntelliJ IDEA)**
```xml
<!-- .idea/runConfigurations/MaruniApplication.xml -->
<configuration name="MaruniApplication" type="SpringBootApplicationConfigurationType">
  <option name="ACTIVE_PROFILES" value="dev" />
  <option name="MAIN_CLASS_NAME" value="com.anyang.maruni.MaruniApplication" />
  <option name="MODULE_NAME" value="maruni.main" />
  <envs>
    <env name="SPRING_PROFILES_ACTIVE" value="dev" />
  </envs>
</configuration>
```

### **Git Hooks 설정**
```bash
#!/bin/sh
# .git/hooks/pre-commit
# 커밋 전 테스트 실행

echo "Running tests before commit..."
./gradlew test

if [ $? -ne 0 ]; then
    echo "Tests failed. Commit rejected."
    exit 1
fi

echo "All tests passed. Proceeding with commit."
```

### **Gradle 래퍼 설정**
```properties
# gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
networkTimeout=10000
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

## 📊 모니터링 설정

### **Spring Boot Actuator**
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

### **로깅 설정**
```yaml
logging:
  level:
    root: INFO
    com.anyang.maruni: DEBUG
    org.springframework.security: DEBUG
    org.springframework.ai: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/maruni.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
```

---

## 🚀 실행 명령어

### **로컬 개발 환경**
```bash
# 전체 환경 시작 (Docker)
docker-compose up -d

# 애플리케이션만 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 빌드
./gradlew build

# 운영 JAR 실행
java -jar build/libs/maruni-0.0.1-SNAPSHOT.jar
```

### **Docker 환경**
```bash
# 개발 환경 빌드 및 실행
docker-compose up --build

# 운영 환경 실행 (Docker Hub 이미지)
docker-compose -f docker-compose.prod.yml up -d

# 백그라운드 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 컨테이너 종료
docker-compose down

# 볼륨까지 삭제
docker-compose down -v
```

---

## 🎯 환경별 설정

### **개발 환경 (dev)**
- PostgreSQL 개발 DB 사용
- SQL 쿼리 로그 출력
- 상세 에러 메시지 표시
- Hot Reload 활성화
- Swagger UI 활성화

### **테스트 환경 (test)**
- H2 In-Memory Database
- 트랜잭션 자동 롤백
- Mock 서비스 활성화
- OpenAI API Mock 사용

### **운영 환경 (prod)**
- PostgreSQL 운영 DB 사용
- 로그 레벨 INFO 이상
- 보안 강화 설정
- 성능 최적화 옵션
- Swagger UI 비활성화 권장

---

## 🔄 아키텍처 다이어그램

### **시스템 아키텍처**
```
┌─────────────────────────────────────────────────────────────┐
│                        클라이언트                            │
│              (모바일 앱 / 웹 브라우저)                        │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS/REST API
                         │ JWT Access Token
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   MARUNI Backend Server                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Spring Security Filter Chain             │  │
│  │  - JwtAuthenticationFilter (JWT 검증)                │  │
│  │  - LoginFilter (로그인 처리)                          │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Spring Boot Controllers                  │  │
│  │  - Member, Guardian, Conversation, AlertRule, etc.   │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │           Application Services (DDD)                  │  │
│  │  - MemberService, ConversationService, etc.          │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Domain Layer (DDD)                       │  │
│  │  - Entities, Repositories, Domain Services           │  │
│  └───────────────────────────────────────────────────────┘  │
└────────┬──────────────────┬──────────────────┬──────────────┘
         │                  │                  │
         │ JPA              │ HTTP             │ Firebase Admin
         ▼                  ▼                  ▼
┌────────────────┐  ┌──────────────┐  ┌──────────────────┐
│  PostgreSQL    │  │  OpenAI API  │  │ Firebase FCM     │
│   Database     │  │   (GPT-4o)   │  │ (Push Notify)    │
└────────────────┘  └──────────────┘  └──────────────────┘
```

### **인증 아키텍처**
```
Stateless JWT Authentication (Access Token Only)

클라이언트                         서버
    │                              │
    │  1. POST /api/members/login  │
    │  {email, password}           │
    ├─────────────────────────────>│
    │                              │ 2. DB 인증
    │                              │ 3. Access Token 생성
    │                              │    (1시간 유효)
    │  4. Authorization: Bearer .. │
    │<─────────────────────────────┤
    │                              │
    │  5. 토큰 저장                │
    │  (로컬 스토리지/메모리)       │
    │                              │
    │  6. API 호출                 │
    │  Authorization: Bearer ..    │
    ├─────────────────────────────>│
    │                              │ 7. JWT 검증
    │                              │ 8. SecurityContext 설정
    │  9. 응답                     │
    │<─────────────────────────────┤
    │                              │
    │  10. 로그아웃 (클라이언트)   │
    │  - 토큰 삭제                 │
    │                              │
```

---

## 📋 의존성 버전 관리

### **주요 버전**
```yaml
Java: 21 (LTS)
Spring Boot: 3.5.3
Spring Security: 6.x (Spring Boot 제공)
Spring Data JPA: 3.x (Spring Boot 제공)
PostgreSQL Driver: Latest (Spring Boot 제공)
JJWT: 0.12.5
Spring AI: 1.0.0-M3
Firebase Admin: 9.5.0
SpringDoc OpenAPI: 2.8.8
Lombok: Latest (Spring Boot 제공)
```

### **호환성**
- Java 21 이상 필수
- PostgreSQL 14 이상 권장
- Docker 20.10 이상
- Docker Compose v2 이상

---

**Version**: v2.0.0 | **Updated**: 2025-10-05 | **Status**: Simplified Architecture (PostgreSQL Only, No Redis)
