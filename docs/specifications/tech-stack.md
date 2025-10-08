# 기술 스택

**MARUNI 프로젝트 기술 스택 및 환경 설정**

## 🛠️ 주요 기술 스택

### Core
```
Java: 21 (LTS)
Spring Boot: 3.5.3
Gradle: 8.x
PostgreSQL: 15+
```

### AI & Integration
```
Spring AI: 1.0.0-M3
OpenAI: GPT-4o
Firebase Admin SDK: 9.5.0
```

### Security
```
Spring Security: 6.x
JWT: JJWT 0.12.5 (Stateless)
Password: BCrypt
```

### Documentation & Tools
```
Swagger/OpenAPI: SpringDoc 2.8.8
Lombok
Spring Boot Actuator
```

### Testing
```
JUnit 5
Mockito
H2 (In-Memory)
```

## 📦 핵심 의존성

### build.gradle (주요 부분만)
```gradle
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Spring AI
    implementation platform('org.springframework.ai:spring-ai-bom:1.0.0-M3')
    implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'

    // Database
    runtimeOnly 'org.postgresql:postgresql'

    // Firebase
    implementation 'com.google.firebase:firebase-admin:9.5.0'

    // Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8'

    // Utilities
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'com.h2database:h2'
}
```

## ⚙️ 환경 설정

### 필수 환경 변수 (.env)
```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT (Access Token Only)
JWT_SECRET_KEY=your_jwt_secret_key_at_least_32_characters
JWT_ACCESS_EXPIRATION=3600000  # 1시간

# OpenAI
OPENAI_API_KEY=your_openai_api_key

# Firebase
FIREBASE_PROJECT_ID=maruni-project
FIREBASE_PRIVATE_KEY_PATH=config/firebase-service-account.json
```

### application.yml (핵심 설정)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/maruni_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
          max-tokens: 100

jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION}

maruni:
  scheduling:
    daily-check:
      cron: "0 0 9 * * *"  # 매일 오전 9시
    retry:
      cron: "0 */5 * * * *"  # 5분마다
```

## 🐳 Docker 설정

### docker-compose.yml
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: maruni_db
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    env_file:
      - .env

volumes:
  postgres_data:
```

### Dockerfile
```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 🚀 개발 명령어

### Gradle
```bash
./gradlew build          # 빌드
./gradlew bootRun        # 로컬 실행
./gradlew test           # 테스트
./gradlew clean          # 빌드 정리
```

### Docker
```bash
docker-compose up -d     # 전체 환경 실행
docker-compose down      # 환경 종료
docker-compose logs -f app  # 로그 확인
docker-compose ps        # 상태 확인
```

### 개발 워크플로우
```bash
# 1. 환경 변수 설정
cp .env.example .env
# .env 파일 수정

# 2. Docker로 DB 실행
docker-compose up -d postgres

# 3. 애플리케이션 로컬 실행
./gradlew bootRun

# 4. Swagger 확인
open http://localhost:8080/swagger-ui.html
```

---

**상세 설정은 각 도메인 문서와 specifications/ 다른 가이드를 참조하세요.**
