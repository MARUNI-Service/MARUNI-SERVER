# MARUNI 프로젝트 컨테이너 다이어그램

**노인 돌봄을 위한 AI 기반 소통 서비스의 시스템 아키텍처 및 컨테이너 구조**

## 📋 문서 개요

이 문서는 MARUNI 프로젝트의 **컨테이너 레벨 아키텍처**를 시각화하고, 각 컨테이너 간의 관계와 통신 방식을 설명합니다.

### 🎯 아키텍처 개요
- **마이크로서비스 지향**: Spring Boot 애플리케이션 중심
- **컨테이너화**: Docker + Docker Compose
- **데이터 영속성**: PostgreSQL + Redis
- **외부 서비스**: Firebase FCM, OpenAI GPT-4o

---

## 🏗️ 전체 시스템 컨테이너 다이어그램

```mermaid
C4Container
    title Container Diagram - MARUNI 노인 돌봄 플랫폼

    Person(elderly, "노인 사용자", "60세 이상 어르신<br/>스마트폰 앱/웹 사용")
    Person(guardian, "보호자", "가족, 친구, 돌봄제공자<br/>실시간 알림 수신")
    Person(admin, "시스템 관리자", "서비스 운영 및 모니터링")

    System_Boundary(maruni_system, "MARUNI 시스템") {

        Container(web_app, "Spring Boot Application", "Java 21, Spring Boot 3.5", "REST API 서버<br/>6개 도메인 비즈니스 로직<br/>JWT 인증, Swagger API")

        ContainerDb(postgres, "PostgreSQL Database", "PostgreSQL 15", "사용자 데이터, 대화 기록<br/>알림 규칙, 발송 이력<br/>관계형 데이터 영속화")

        ContainerDb(redis, "Redis Cache", "Redis 7", "JWT Refresh Token<br/>세션 관리, 캐시<br/>Token Blacklist")

        Container(scheduler, "Task Scheduler", "Spring @Scheduled", "매일 안부 메시지 발송<br/>재시도 처리<br/>Cron 기반 스케줄링")
    }

    System_Ext(firebase, "Firebase FCM", "Google Cloud", "실시간 푸시 알림 서비스<br/>모바일/웹 알림 전송")

    System_Ext(openai, "OpenAI API", "GPT-4o", "AI 대화 생성<br/>자연어 처리<br/>감정 분석 지원")

    Rel(elderly, web_app, "HTTP/HTTPS", "대화 메시지 전송<br/>안부 응답")
    Rel(guardian, web_app, "HTTP/HTTPS", "보호자 설정<br/>알림 기록 조회")
    Rel(admin, web_app, "HTTP/HTTPS", "시스템 관리<br/>API 모니터링")

    Rel(web_app, postgres, "JDBC", "사용자/대화/알림 데이터<br/>CRUD 연산")
    Rel(web_app, redis, "Lettuce", "JWT 토큰 저장<br/>세션 캐시")

    Rel(scheduler, web_app, "Internal", "스케줄링 트리거<br/>일일 안부 체크")

    Rel(web_app, firebase, "HTTPS/FCM SDK", "푸시 알림 발송<br/>실시간 알림")
    Rel(web_app, openai, "HTTPS/REST API", "AI 대화 생성<br/>GPT-4o 호출")

    UpdateElementStyle(web_app, $fontColor="white", $bgColor="blue")
    UpdateElementStyle(postgres, $fontColor="white", $bgColor="green")
    UpdateElementStyle(redis, $fontColor="white", $bgColor="red")
    UpdateElementStyle(scheduler, $fontColor="white", $bgColor="orange")
```

---

## 🐳 Docker 컨테이너 구성

### 📦 **컨테이너 목록 (3개 컨테이너)**

```mermaid
graph TB
    subgraph "Docker Compose 환경"
        subgraph "Backend Network"
            A[maruni-app<br/>:8080]
            B[postgres-db<br/>:5432]
            C[redis<br/>:6379]
        end
    end

    A -.->|JDBC| B
    A -.->|Lettuce| C
    B -.->|Health Check| B
    C -.->|Health Check| C
    A -.->|Health Check| A

    style A fill:#e1f5fe
    style B fill:#e8f5e8
    style C fill:#fff3e0
```

#### **1. maruni-app (Spring Boot 애플리케이션)**
```dockerfile
# Multi-stage build 최적화
FROM gradle:8.5-jdk21 AS builder
FROM openjdk:21-jdk-slim

WORKDIR /app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/actuator/health
```

- **기술 스택**: Java 21, Spring Boot 3.5, Gradle 8.5
- **포트**: 8080
- **헬스체크**: `/actuator/health` 엔드포인트
- **프로파일**: `dev`, `prod` 환경별 설정

#### **2. postgres-db (PostgreSQL 데이터베이스)**
```yaml
image: postgres:15
environment:
  POSTGRES_DB: maruni-db
  POSTGRES_USER: ${DB_USERNAME}
  POSTGRES_PASSWORD: ${DB_PASSWORD}
```

- **버전**: PostgreSQL 15
- **포트**: 5432
- **볼륨**: `postgres-data` (영속적 데이터 저장)
- **헬스체크**: `pg_isready` 명령어

#### **3. redis (Redis 캐시)**
```yaml
image: redis:7
command: ["redis-server", "--requirepass", "${REDIS_PASSWORD}", "--appendonly", "yes"]
```

- **버전**: Redis 7
- **포트**: 6379
- **인증**: 비밀번호 보호
- **볼륨**: `redis-data` (AOF 영속화)

---

## 🔗 컨테이너 간 통신

### 📡 **내부 네트워크 통신**

```mermaid
sequenceDiagram
    participant App as maruni-app<br/>(Spring Boot)
    participant DB as postgres-db<br/>(PostgreSQL)
    participant Cache as redis<br/>(Redis)

    Note over App, Cache: Docker Backend Network (bridge)

    App->>DB: JDBC Connection<br/>jdbc:postgresql://postgres-db:5432/maruni-db
    DB-->>App: SQL Result Set

    App->>Cache: Lettuce Connection<br/>redis://redis:6379
    Cache-->>App: Cached Data

    Note over App: Health Check 의존성
    App->>DB: Health Check (pg_isready)
    App->>Cache: Health Check (redis-cli ping)
```

#### **네트워크 설정**
```yaml
networks:
  backend:
    driver: bridge
```

- **네트워크명**: `backend`
- **드라이버**: `bridge` (기본 Docker 네트워크)
- **통신 방식**: 컨테이너명으로 내부 DNS 해결

### 🌐 **외부 서비스 통신**

```mermaid
graph LR
    subgraph "MARUNI 시스템"
        App[Spring Boot App<br/>maruni-app:8080]
    end

    subgraph "외부 서비스"
        Firebase[Firebase FCM<br/>fcm.googleapis.com]
        OpenAI[OpenAI API<br/>api.openai.com]
    end

    subgraph "클라이언트"
        Mobile[모바일 앱]
        Web[웹 브라우저]
        Guardian[보호자 앱]
    end

    App -->|HTTPS| Firebase
    App -->|HTTPS| OpenAI

    Mobile -->|HTTP/HTTPS| App
    Web -->|HTTP/HTTPS| App
    Guardian -->|HTTP/HTTPS| App

    Firebase -->|Push Notification| Mobile
    Firebase -->|Push Notification| Guardian
```

---

## 📊 도메인별 컨테이너 매핑

### 🏗️ **Spring Boot 애플리케이션 내부 구조**

```mermaid
graph TB
    subgraph "maruni-app Container"
        subgraph "Presentation Layer"
            API[REST API Controllers<br/>25+ Endpoints]
            Swagger[Swagger UI<br/>API Documentation]
        end

        subgraph "Application Layer"
            Auth[Auth Service<br/>JWT 인증/인가]
            Member[Member Service<br/>회원 관리]
            Conversation[Conversation Service<br/>AI 대화]
            DailyCheck[DailyCheck Service<br/>일일 안부 확인]
            Guardian[Guardian Service<br/>보호자 관리]
            AlertRule[AlertRule Service<br/>이상징후 감지]
            Notification[Notification Service<br/>알림 발송]
        end

        subgraph "Infrastructure Layer"
            JPA[Spring Data JPA<br/>PostgreSQL 연동]
            Redis_Client[Redis Client<br/>Lettuce]
            Scheduler[Spring Scheduler<br/>@Scheduled]
            Firebase_SDK[Firebase SDK<br/>FCM 연동]
            OpenAI_Client[OpenAI Client<br/>Spring AI]
        end
    end

    API --> Auth
    API --> Member
    API --> Conversation
    API --> Guardian
    API --> AlertRule

    Auth --> Redis_Client
    Member --> JPA
    Conversation --> JPA
    Conversation --> OpenAI_Client
    DailyCheck --> JPA
    DailyCheck --> Notification
    Guardian --> JPA
    AlertRule --> JPA
    AlertRule --> Notification
    Notification --> Firebase_SDK

    Scheduler --> DailyCheck
```

### 📋 **데이터 저장소 매핑**

| 도메인 | PostgreSQL 테이블 | Redis 키 | 용도 |
|--------|-------------------|----------|------|
| **Member** | `member_table` | - | 회원 정보, 프로필 |
| **Auth** | `refresh_token` | `refreshToken:{memberId}`<br/>`blacklist:token:{token}` | JWT 토큰 관리 |
| **Conversation** | `conversations`<br/>`messages` | - | AI 대화 기록 |
| **DailyCheck** | `daily_check_records`<br/>`retry_records` | - | 안부 확인 기록 |
| **Guardian** | `guardian` | - | 보호자 정보, 관계 |
| **AlertRule** | `alert_rule`<br/>`alert_history` | - | 알림 규칙, 이력 |
| **Notification** | `notification_history` | - | 알림 발송 기록 |

---

## ⚙️ 환경별 컨테이너 설정

### 🔧 **개발 환경 (dev)**

```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SWAGGER_SERVER_URL: http://localhost:8080
    ports:
      - "8080:8080"
```

**설정 특징:**
- **AI 서비스**: Mock 서비스 (OpenAI API 절약)
- **알림 서비스**: Mock 푸시 알림
- **데이터베이스**: 로컬 PostgreSQL
- **Swagger**: 활성화 (`/swagger-ui.html`)

### 🚀 **운영 환경 (prod)**

```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  app:
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SWAGGER_SERVER_URL: https://api.maruni.com
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1.0'
          memory: 2G
```

**설정 특징:**
- **AI 서비스**: 실제 OpenAI GPT-4o 연동
- **알림 서비스**: Firebase FCM 실제 연동
- **보안**: HTTPS, JWT 보안 강화
- **Swagger**: 비활성화 (보안)
- **스케일링**: 다중 인스턴스 배포

---

## 🔒 보안 및 네트워킹

### 🛡️ **보안 설정**

```mermaid
graph TB
    subgraph "보안 계층"
        subgraph "네트워크 보안"
            A[Docker Bridge Network<br/>내부 통신 격리]
            B[환경변수 암호화<br/>.env 파일]
        end

        subgraph "애플리케이션 보안"
            C[JWT 이중 토큰<br/>Access + Refresh]
            D[Spring Security<br/>인증/인가 필터]
            E[HTTPS 강제<br/>SSL/TLS]
        end

        subgraph "데이터 보안"
            F[PostgreSQL 인증<br/>사용자/비밀번호]
            G[Redis 인증<br/>requirepass]
            H[비밀번호 암호화<br/>BCrypt]
        end
    end
```

#### **환경변수 보안**
```bash
# .env (환경별 분리)
DB_USERNAME=secure_db_user
DB_PASSWORD=secure_db_password_32_chars
REDIS_PASSWORD=secure_redis_password
JWT_SECRET_KEY=jwt_secret_key_at_least_32_characters
OPENAI_API_KEY=sk-...
```

#### **네트워크 격리**
- **내부 통신**: Docker 내부 네트워크만 허용
- **외부 노출**: 애플리케이션 포트(8080)만 외부 노출
- **데이터베이스**: 내부 네트워크에서만 접근 가능

### 🌐 **포트 매핑**

| 컨테이너 | 내부 포트 | 외부 포트 | 용도 |
|----------|-----------|-----------|------|
| `maruni-app` | 8080 | 8080 | REST API, Swagger UI |
| `postgres-db` | 5432 | 5432 | 개발용 DB 접근 |
| `redis` | 6379 | 6379 | 개발용 캐시 접근 |

**⚠️ 운영 환경**: 데이터베이스 포트는 외부 노출하지 않음

---

## 📈 헬스체크 및 모니터링

### 💊 **컨테이너 헬스체크**

```mermaid
sequenceDiagram
    participant Docker as Docker Engine
    participant App as maruni-app
    participant DB as postgres-db
    participant Redis as redis

    Note over Docker: 30초마다 헬스체크

    Docker->>App: curl -f http://localhost:8080/actuator/health
    App-->>Docker: 200 OK {"status":"UP"}

    Docker->>DB: pg_isready -U username -d maruni-db
    DB-->>Docker: accepting connections

    Docker->>Redis: redis-cli -a password ping
    Redis-->>Docker: PONG

    alt 헬스체크 실패
        Docker->>Docker: 3회 재시도 후 컨테이너 재시작
    end
```

#### **헬스체크 설정**
```yaml
healthcheck:
  interval: 30s      # 체크 간격
  timeout: 10s       # 타임아웃
  retries: 3         # 재시도 횟수
  start_period: 60s  # 시작 유예 시간
```

### 📊 **모니터링 엔드포인트**

```http
GET /actuator/health          # 전체 헬스체크
GET /actuator/health/db       # 데이터베이스 상태
GET /actuator/health/redis    # Redis 상태
GET /actuator/metrics         # 애플리케이션 메트릭
GET /actuator/info           # 애플리케이션 정보
```

---

## 🚀 배포 및 스케일링

### 📦 **배포 프로세스**

```mermaid
graph LR
    A[Git Push] --> B[CI/CD Pipeline]
    B --> C[Docker Build]
    C --> D[Docker Push]
    D --> E[Docker Compose Up]
    E --> F[Health Check]
    F --> G[Service Ready]

    style A fill:#e1f5fe
    style G fill:#e8f5e8
```

#### **배포 명령어**
```bash
# 개발 환경
docker-compose up -d

# 운영 환경 (스케일링)
docker-compose -f docker-compose.prod.yml up -d --scale app=2

# 무중단 배포
docker-compose -f docker-compose.prod.yml up -d --no-deps app
```

### 📈 **수평 스케일링**

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[Nginx/HAProxy]
    end

    subgraph "Application Instances"
        App1[maruni-app:8080<br/>Instance 1]
        App2[maruni-app:8080<br/>Instance 2]
        App3[maruni-app:8080<br/>Instance 3]
    end

    subgraph "Shared Data Stores"
        DB[PostgreSQL<br/>Single Instance]
        Cache[Redis Cluster<br/>Multi Instance]
    end

    LB --> App1
    LB --> App2
    LB --> App3

    App1 --> DB
    App2 --> DB
    App3 --> DB

    App1 --> Cache
    App2 --> Cache
    App3 --> Cache
```

---

## 🔧 운영 관리

### 📊 **로그 관리**

```bash
# 컨테이너별 로그 조회
docker-compose logs -f app
docker-compose logs -f db
docker-compose logs -f redis

# 전체 로그
docker-compose logs -f

# 로그 크기 제한
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

### 🔄 **백업 및 복구**

```bash
# PostgreSQL 백업
docker exec postgres-db pg_dump -U ${DB_USERNAME} maruni-db > backup.sql

# Redis 백업
docker exec redis redis-cli -a ${REDIS_PASSWORD} BGSAVE

# 볼륨 백업
docker run --rm -v postgres-data:/source -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz -C /source .
```

### 🔍 **트러블슈팅**

#### **일반적인 문제들**

1. **컨테이너 시작 실패**
   ```bash
   # 헬스체크 상태 확인
   docker-compose ps

   # 로그 확인
   docker-compose logs app
   ```

2. **데이터베이스 연결 실패**
   ```bash
   # PostgreSQL 연결 테스트
   docker exec -it postgres-db psql -U ${DB_USERNAME} -d maruni-db

   # 네트워크 확인
   docker network ls
   docker network inspect maruni_backend
   ```

3. **Redis 연결 실패**
   ```bash
   # Redis 연결 테스트
   docker exec -it redis redis-cli -a ${REDIS_PASSWORD} ping

   # 메모리 사용량 확인
   docker exec redis redis-cli -a ${REDIS_PASSWORD} info memory
   ```

---

## 🎯 확장 계획

### 📱 **Phase 3: 마이크로서비스 확장**

```mermaid
graph TB
    subgraph "API Gateway"
        Gateway[Spring Cloud Gateway<br/>:8080]
    end

    subgraph "Core Services"
        Auth[Auth Service<br/>:8081]
        User[User Service<br/>:8082]
        Chat[Chat Service<br/>:8083]
        Alert[Alert Service<br/>:8084]
    end

    subgraph "Support Services"
        Config[Config Server<br/>:8888]
        Discovery[Service Discovery<br/>:8761]
        Monitor[Monitoring<br/>:9090]
    end

    subgraph "Data Layer"
        DB[(PostgreSQL Cluster)]
        Cache[(Redis Cluster)]
        MQ[Message Queue<br/>RabbitMQ]
    end

    Gateway --> Auth
    Gateway --> User
    Gateway --> Chat
    Gateway --> Alert

    Auth --> DB
    User --> DB
    Chat --> DB
    Alert --> DB

    Auth --> Cache
    User --> Cache
    Chat --> MQ
    Alert --> MQ
```

### 🔮 **향후 컨테이너 추가 계획**

| 서비스 | 기술 스택 | 포트 | 용도 |
|--------|-----------|------|------|
| **API Gateway** | Spring Cloud Gateway | 8080 | 라우팅, 로드밸런싱 |
| **Service Discovery** | Eureka | 8761 | 서비스 등록/발견 |
| **Config Server** | Spring Cloud Config | 8888 | 중앙 설정 관리 |
| **Message Queue** | RabbitMQ | 5672 | 비동기 메시징 |
| **Monitoring** | Prometheus + Grafana | 9090 | 메트릭 수집/시각화 |
| **Mobile API** | Flutter Backend | 8090 | 모바일 전용 API |

---

## 📋 문서 연관 관계

### 🔗 **관련 문서**
- **[유저 플로우 다이어그램](./user_flow_diagram.md)**: 사용자 여정 및 비즈니스 플로우
- **[전체 프로젝트 가이드](./README.md)**: 프로젝트 개요 및 현황
- **[도메인 구조](./domains/README.md)**: 비즈니스 도메인 아키텍처
- **[기술 스택](./specifications/tech-stack.md)**: 상세 기술 정보

### 🛠️ **인프라 문서**
- **[Docker 설정](../docker-compose.yml)**: 실제 컨테이너 구성
- **[Dockerfile](../Dockerfile)**: 애플리케이션 빌드 설정
- **[환경 설정](../src/main/resources/application.yml)**: Spring Boot 설정

---

**MARUNI는 Docker 기반의 마이크로서비스 지향 아키텍처로 구축된 확장 가능하고 안정적인 노인 돌봄 플랫폼입니다. 컨테이너화를 통해 개발/운영 환경의 일관성을 보장하고, 향후 클라우드 네이티브 확장을 위한 기반을 마련했습니다.** 🚀