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

### **Database & Cache**
```yaml
Primary Database: PostgreSQL 14+
Cache: Redis 7.x
Connection Pool: HikariCP (Spring Boot Default)
ORM: Spring Data JPA + Hibernate 6.x
```

### **AI & Integration**
```yaml
AI Framework: Spring AI 1.0.0-M3
AI Provider: OpenAI GPT-4o
HTTP Client: Spring WebClient
Scheduling: Spring @Scheduled
```

### **Security & Authentication**
```yaml
Framework: Spring Security 6.x
Authentication: JWT (JSON Web Token)
JWT Library: JJWT 0.12.5
Password Encoding: BCryptPasswordEncoder
OAuth2: Spring OAuth2 Client
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
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
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

### **Database & Cache**
```gradle
dependencies {
    // PostgreSQL
    runtimeOnly 'org.postgresql:postgresql'

    // Redis
    implementation 'org.apache.commons:commons-pool2'

    // Test Database
    testRuntimeOnly 'com.h2database:h2'
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
  refresh-token:
    expiration: ${JWT_REFRESH_EXPIRATION:1209600000}  # 14일

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
DB_USERNAME=maruni_dev
DB_PASSWORD=dev_password
DB_URL=jdbc:postgresql://localhost:5432/maruni_dev

# Redis 설정
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password

# JWT 설정 (필수 - 32자 이상)
JWT_SECRET_KEY=your_super_secret_jwt_key_at_least_32_characters_long_for_security
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=1209600000

# OpenAI API 설정
OPENAI_API_KEY=sk-your-openai-api-key-here
OPENAI_MODEL=gpt-4o
OPENAI_MAX_TOKENS=100
OPENAI_TEMPERATURE=0.7

# Firebase 설정 (알림용)
FIREBASE_PROJECT_ID=maruni-project
FIREBASE_CREDENTIALS_PATH=path/to/firebase-credentials.json

# 암호화 설정
ENCRYPTION_KEY=your_32_byte_encryption_key_here
```

---

## 🐳 Docker 환경

### **docker-compose.yml**
```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:14-alpine
    container_name: maruni-postgres
    environment:
      POSTGRES_DB: maruni
      POSTGRES_USER: ${DB_USERNAME:-maruni}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-maruni_password}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: maruni-redis
    command: redis-server --requirepass ${REDIS_PASSWORD:-redis_password}
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  # MARUNI Application
  app:
    build: .
    container_name: maruni-app
    depends_on:
      - postgres
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_URL: jdbc:postgresql://postgres:5432/maruni
      DB_USERNAME: ${DB_USERNAME:-maruni}
      DB_PASSWORD: ${DB_PASSWORD:-maruni_password}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD:-redis_password}
    ports:
      - "8080:8080"
    volumes:
      - app_logs:/app/logs

volumes:
  postgres_data:
  redis_data:
  app_logs:
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
# 빌드 및 실행
docker-compose up --build

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
- H2 Console 활성화
- SQL 쿼리 로그 출력
- 상세 에러 메시지 표시
- Hot Reload 활성화

### **테스트 환경 (test)**
- H2 In-Memory Database
- 트랜잭션 자동 롤백
- Mock 서비스 활성화

### **운영 환경 (prod)**
- PostgreSQL 사용
- 로그 레벨 INFO 이상
- 보안 강화 설정
- 성능 최적화 옵션

---

**Version**: v1.0.0 | **Updated**: 2025-09-16