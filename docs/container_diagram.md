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
    subgraph "maruni-app Container (Port: 8080)"
        subgraph "Presentation Layer"
            API[REST API Controllers<br/>25+ Endpoints<br/>JWT + @AutoApiResponse]
            Swagger[Swagger UI<br/>/swagger-ui.html<br/>OpenAPI 3.0 문서화]
            Security[Spring Security<br/>JWT Filter Chain<br/>Authentication/Authorization]
        end

        subgraph "Application Layer (6개 도메인)"
            subgraph "Foundation Services"
                Auth[Auth Service<br/>- Token 발급/검증<br/>- Refresh Token 관리<br/>- Redis 기반 Blacklist]
                Member[Member Service<br/>- 회원 가입/조회<br/>- 프로필 관리<br/>- BCrypt 암호화]
            end

            subgraph "Core Services"
                Conversation[Conversation Service<br/>- OpenAI GPT-4o 연동<br/>- 감정 분석 (3단계)<br/>- 대화 세션 관리]
                DailyCheck[DailyCheck 시스템<br/>- DailyCheckScheduler<br/>- DailyCheckOrchestrator<br/>- RetryService (재시도)]
                Guardian[Guardian Service<br/>- 보호자 관계 관리<br/>- 알림 설정<br/>- 권한 제어]
            end

            subgraph "Integration Services"
                AlertRule[AlertRule Service<br/>- 3종 감지 알고리즘<br/>- 실시간 키워드 분석<br/>- 이상징후 판정]
                Notification[Notification Service<br/>- FCM 푸시 알림<br/>- Mock/Real 환경 분리<br/>- 발송 이력 관리]
            end
        end

        subgraph "Infrastructure Layer"
            subgraph "Data Access"
                JPA[Spring Data JPA<br/>- PostgreSQL 연동<br/>- Custom Repository<br/>- Query Method]
                Redis_Client[Redis Client (Lettuce)<br/>- JWT Token Store<br/>- Session Cache<br/>- Connection Pool]
            end

            subgraph "External Integrations"
                Scheduler[Spring Scheduler<br/>- @Scheduled Cron<br/>- Async Task<br/>- Error Handling]
                Firebase_SDK[Firebase SDK<br/>- FCM Push Service<br/>- Token Management<br/>- Batch Notification]
                OpenAI_Client[OpenAI Client<br/>- Spring AI Framework<br/>- GPT-4o Integration<br/>- Emotion Analysis]
            end
        end
    end

    %% API Layer Connections
    API -.-> Security
    Security -.-> Auth
    API --> Member
    API --> Conversation
    API --> Guardian
    API --> AlertRule

    %% Service Layer Connections
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

    %% Scheduler Connections
    Scheduler --> DailyCheck

    %% Cross-Domain Dependencies
    AlertRule -.-> Guardian
    DailyCheck -.-> Member
    Notification -.-> Guardian

    %% Styling
    classDef foundation fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef core fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef integration fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef infrastructure fill:#fff3e0,stroke:#f57c00,stroke-width:2px

    class Auth,Member foundation
    class Conversation,DailyCheck,Guardian core
    class AlertRule,Notification integration
    class JPA,Redis_Client,Scheduler,Firebase_SDK,OpenAI_Client infrastructure
```

### 📋 **상세 데이터 저장소 매핑**

#### 🗄️ **PostgreSQL 테이블 구조 (실제 코드 기준)**

| 도메인 | 테이블명 | 주요 컬럼 | 관계 | 용도 |
|--------|----------|-----------|------|------|
| **Member** | `member_table` | `id`, `memberEmail`, `memberName`, `memberPassword`, `guardian_id`, `push_token` | `guardian.id` (FK) | 회원 정보 + FCM |
| **Auth** | `refresh_token` | `id`, `member_id`, `token`, `expires_at`, `created_at` | `member_table.id` (FK) | JWT Refresh Token 저장 |
| **Conversation** | `conversations` | `id`, `member_id`, `session_id`, `status`, `created_at` | `member_table.id` (FK) | AI 대화 세션 |
| **Conversation** | `messages` | `id`, `conversation_id`, `content`, `message_type`, `emotion_type`, `ai_response` | `conversations.id` (FK) | 대화 메시지 내역 |
| **DailyCheck** | `daily_check_records` | `id`, `memberId`, `checkDate`, `message`, `success`, `created_at`, `updated_at` | `member_table.id` (FK) | 일일 안부 확인 기록 |
| **DailyCheck** | `retry_records` | `id`, `daily_check_id`, `retry_attempt`, `retry_at`, `error_message`, `success` | `daily_check_records.id` (FK) | 재시도 이력 |
| **Guardian** | `guardian` | `id`, `guardianName`, `guardianEmail`, `guardianPhone`, `relation`, `notificationPreference`, `isActive` | - | 보호자 기본 정보 |
| **AlertRule** | `alert_rule` | `id`, `member_id`, `rule_type`, `conditions`, `alert_level`, `is_active` | `member_table.id` (FK) | 이상징후 감지 규칙 |
| **AlertRule** | `alert_history` | `id`, `member_id`, `alert_rule_id`, `triggered_at`, `alert_content`, `resolved_at` | `member_table.id`, `alert_rule.id` (FK) | 알림 발생 이력 |

#### 🔄 **Redis 캐시 구조 (5개 키 패턴)**

| 도메인 | Redis 키 패턴 | TTL | 데이터 구조 | 용도 |
|--------|---------------|-----|-------------|------|
| **Auth** | `refreshToken:{memberId}` | 24시간 | String | Refresh Token 저장 |
| **Auth** | `blacklist:token:{tokenHash}` | Access Token TTL | String | 무효화된 토큰 관리 |
| **Auth** | `loginAttempt:{email}` | 15분 | Counter | 로그인 시도 횟수 제한 |
| **Conversation** | `conversation:session:{sessionId}` | 1시간 | Hash | 대화 세션 임시 저장 |
| **DailyCheck** | `dailycheck:lock:{memberId}:{date}` | 24시간 | String | 중복 발송 방지 |

### 🔗 **도메인 간 의존성 매핑**

```mermaid
graph TB
    subgraph "Container: maruni-app"
        subgraph "Foundation Layer (기반 시스템)"
            Member[👤 Member Domain<br/>- MemberEntity<br/>- MemberService<br/>- CustomUserDetailsService]
            Auth[🔐 Auth Domain<br/>- RefreshToken Entity<br/>- TokenValidator<br/>- JwtAuthenticationFilter]
        end

        subgraph "Core Service Layer (핵심 서비스)"
            Conversation[💬 Conversation Domain<br/>- ConversationEntity<br/>- MessageEntity<br/>- SimpleAIResponseGenerator]
            DailyCheck[📅 DailyCheck Domain<br/>- DailyCheckRecord<br/>- RetryRecord<br/>- DailyCheckScheduler<br/>- DailyCheckOrchestrator]
            Guardian[👥 Guardian Domain<br/>- GuardianEntity<br/>- GuardianRelation<br/>- NotificationPreference]
        end

        subgraph "Integration Layer (통합/알림)"
            AlertRule[🚨 AlertRule Domain<br/>- AlertRule Entity<br/>- AlertHistory<br/>- 3종 Analyzer]
            Notification[📢 Notification Domain<br/>- NotificationService<br/>- MockPushNotificationService<br/>- FCM Integration]
        end
    end

    subgraph "External Container: postgres-db"
        DB[(PostgreSQL 15<br/>15개 테이블<br/>관계형 데이터)]
    end

    subgraph "External Container: redis"
        Cache[(Redis 7<br/>5개 키 패턴<br/>캐시 + 세션)]
    end

    subgraph "External Services"
        OpenAI[🤖 OpenAI GPT-4o<br/>api.openai.com<br/>AI 대화 생성]
        FCM[🔥 Firebase FCM<br/>fcm.googleapis.com<br/>푸시 알림]
    end

    %% Foundation Dependencies
    Auth -.-> Member
    Auth --> Cache

    %% Core Service Dependencies
    Conversation --> Member
    Conversation --> DB
    Conversation --> OpenAI

    DailyCheck --> Member
    DailyCheck --> DB
    DailyCheck --> Notification

    Guardian --> Member
    Guardian --> DB

    %% Integration Dependencies
    AlertRule --> Member
    AlertRule --> Guardian
    AlertRule --> DB
    AlertRule --> Notification

    Notification --> Guardian
    Notification --> FCM

    %% Data Layer Connections
    Member --> DB
    Auth --> DB

    %% Styling
    classDef foundation fill:#e3f2fd,stroke:#1976d2
    classDef core fill:#f3e5f5,stroke:#7b1fa2
    classDef integration fill:#e8f5e8,stroke:#388e3c
    classDef external fill:#fff3e0,stroke:#f57c00
    classDef service fill:#fce4ec,stroke:#c2185b

    class Member,Auth foundation
    class Conversation,DailyCheck,Guardian core
    class AlertRule,Notification integration
    class DB,Cache,OpenAI,FCM external
```

### 🏗️ **컨테이너별 리소스 할당**

#### 📊 **maruni-app Container (Spring Boot)**

```yaml
# docker-compose.yml 리소스 설정
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '1.0'          # CPU 1코어
          memory: 2G           # 메모리 2GB
        reservations:
          cpus: '0.5'          # CPU 최소 0.5코어
          memory: 1G           # 메모리 최소 1GB
    environment:
      # JVM 힙 설정
      JAVA_OPTS: >
        -Xms1g -Xmx1.5g
        -XX:+UseG1GC
        -XX:MaxGCPauseMillis=200
        -XX:+UnlockExperimentalVMOptions
        -XX:+UseContainerSupport
      # 도메인별 스레드 풀 설정
      SPRING_TASK_EXECUTION_POOL_CORE_SIZE: 10
      SPRING_TASK_EXECUTION_POOL_MAX_SIZE: 20
      SPRING_TASK_SCHEDULING_POOL_SIZE: 5
```

#### 🗄️ **postgres-db Container (PostgreSQL)**

```yaml
services:
  db:
    deploy:
      resources:
        limits:
          cpus: '0.5'          # CPU 0.5코어
          memory: 1G           # 메모리 1GB
        reservations:
          cpus: '0.25'         # CPU 최소 0.25코어
          memory: 512M         # 메모리 최소 512MB
    environment:
      # PostgreSQL 성능 튜닝
      POSTGRES_SHARED_BUFFERS: 256MB
      POSTGRES_EFFECTIVE_CACHE_SIZE: 512MB
      POSTGRES_WORK_MEM: 4MB
      POSTGRES_MAINTENANCE_WORK_MEM: 64MB
      POSTGRES_MAX_CONNECTIONS: 100
```

#### 🔄 **redis Container (Redis)**

```yaml
services:
  redis:
    deploy:
      resources:
        limits:
          cpus: '0.25'         # CPU 0.25코어
          memory: 512M         # 메모리 512MB
        reservations:
          cpus: '0.1'          # CPU 최소 0.1코어
          memory: 128M         # 메모리 최소 128MB
    environment:
      # Redis 메모리 최적화
      REDIS_MAXMEMORY: 256mb
      REDIS_MAXMEMORY_POLICY: allkeys-lru
      REDIS_SAVE: '900 1 300 10 60 10000'  # 스냅샷 설정
```

### 🔄 **도메인별 처리 플로우 매핑**

#### 💬 **AI 대화 처리 플로우**
```mermaid
sequenceDiagram
    participant Client as 모바일/웹 클라이언트
    participant App as maruni-app
    participant DB as postgres-db
    participant OpenAI as OpenAI API
    participant Redis as redis

    Client->>App: POST /api/conversations/messages<br/>{message: "안녕하세요"}

    App->>Redis: JWT 토큰 검증
    Redis-->>App: 토큰 유효성 확인

    App->>DB: Conversation 세션 조회/생성
    DB-->>App: 세션 정보 반환

    App->>DB: Message 엔티티 저장 (USER)

    App->>OpenAI: GPT-4o API 호출<br/>{"model": "gpt-4o", "messages": [...]}
    OpenAI-->>App: AI 응답 + 감정 분석

    App->>DB: Message 엔티티 저장 (AI)
    App->>DB: Conversation 상태 업데이트

    App-->>Client: ConversationResponseDto<br/>{aiResponse, emotionType}
```

#### 📅 **일일 안부 확인 플로우**
```mermaid
sequenceDiagram
    participant Scheduler as Spring Scheduler
    participant App as DailyCheck Service
    participant DB as postgres-db
    participant Redis as redis
    participant Notification as Notification Service
    participant FCM as Firebase FCM

    Note over Scheduler: 매일 오전 9시 (Cron: 0 0 9 * * *)

    Scheduler->>App: @Scheduled 메서드 트리거

    App->>Redis: Lock 확인<br/>dailycheck:lock:{memberId}:{date}
    alt Lock 존재하지 않음
        App->>Redis: Lock 설정 (TTL: 24시간)
        App->>DB: 활성 회원 목록 조회

        loop 각 회원별
            App->>DB: DailyCheckRecord 생성
            App->>Notification: 안부 메시지 발송 요청
            Notification->>FCM: 푸시 알림 발송

            alt 발송 실패
                App->>DB: RetryRecord 생성
                Note over App: 5분 후 재시도 (최대 3회)
            end
        end
    else Lock 이미 존재
        App->>App: 중복 실행 방지, 처리 중단
    end
```

#### 🚨 **이상징후 감지 플로우**
```mermaid
sequenceDiagram
    participant Trigger as 이벤트 트리거
    participant AlertRule as AlertRule Service
    participant Analyzer as 3종 Analyzer
    participant DB as postgres-db
    participant Guardian as Guardian Service
    participant Notification as Notification Service

    alt 대화 메시지 이벤트
        Trigger->>AlertRule: 새 메시지 이벤트
        AlertRule->>Analyzer: 감정 패턴 분석
        Analyzer-->>AlertRule: 분석 결과 (NEGATIVE/긴급키워드)
    else 무응답 이벤트
        Trigger->>AlertRule: 24시간 무응답 체크
        AlertRule->>Analyzer: 무응답 패턴 분석
        Analyzer-->>AlertRule: 무응답 기간 판정
    end

    alt 이상징후 감지됨
        AlertRule->>DB: AlertHistory 생성
        AlertRule->>Guardian: 담당 보호자 조회
        Guardian-->>AlertRule: 보호자 목록 + 알림 설정

        AlertRule->>Notification: 긴급 알림 발송 요청
        Notification->>DB: 알림 기록 저장
        Note over Notification: FCM/SMS/Email 발송
    end
```

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