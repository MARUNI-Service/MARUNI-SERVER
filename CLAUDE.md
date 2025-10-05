# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 📋 문서 참조 가이드

**MARUNI 프로젝트는 체계적인 문서화가 완료되어 있습니다. 작업 전 반드시 관련 문서를 먼저 확인하세요.**

### 📚 **핵심 문서 구조**
```
docs/
├── README.md                   # 📋 전체 프로젝트 가이드 (필수 읽기)
├── domains/                    # 🏗️ 도메인별 구현 가이드
│   ├── README.md               # 도메인 아키텍처 개요 및 의존 관계
│   ├── conversation.md         # AI 대화 시스템 (OpenAI GPT-4o)
│   ├── dailycheck.md           # 스케줄링 시스템 (TDD 완전 사이클)
│   ├── alertrule.md            # 이상징후 감지 (3종 알고리즘)
│   ├── guardian.md             # 보호자 관리 시스템
│   ├── member.md               # 회원 관리 시스템
│   ├── auth.md                 # JWT 인증/인가 시스템
│   └── notification.md         # 알림 시스템
├── roadmap/                    # 🚀 발전 계획
│   ├── README.md               # 전체 로드맵 및 Phase별 현황
│   └── phase3.md               # 고도화 & 모바일 연동 계획
└── specifications/             # 📝 기술 규격서 (9개 문서)
    ├── README.md               # 기술 규격서 통합 가이드
    ├── coding-standards.md     # ⭐⭐⭐ Java 코딩 컨벤션 (매일 참조)
    ├── quick-reference.md      # ⭐⭐⭐ 빠른 참조 템플릿 (일상 작업)
    ├── architecture-guide.md   # ⭐⭐ DDD 아키텍처 설계 (새 기능 개발)
    ├── api-design-guide.md     # ⭐⭐ REST API 설계 (API 개발)
    ├── database-design-guide.md # ⭐ Entity 설계 패턴 (DB 작업)
    ├── testing-guide.md        # ⭐ TDD 테스트 작성법 (테스트 작성)
    ├── security-guide.md       # 🔒 JWT 보안 구현 (보안 설정)
    ├── performance-guide.md    # ⚡ JPA 성능 최적화 (성능 작업)
    └── tech-stack.md           # 🛠️ 기술 스택 전체 (환경 구성)
```

### ⚠️ **작업 시 필수 순서**
1. **`docs/README.md`** 전체 현황 파악
2. **`docs/domains/README.md`** 도메인 구조 이해
3. **`docs/specifications/README.md`** 기술 규격서 확인
4. **해당 도메인 가이드 숙지** (예: `domains/conversation.md`)
5. **관련 기술 규격 확인** (예: `specifications/coding-standards.md`)
6. **실제 코드 확인 후 작업 진행**

## Project Overview

**MARUNI**는 노인들의 외로움과 우울증 문제 해결을 위한 **완성된** 노인 돌봄 플랫폼입니다.

### 🎯 **서비스 핵심 플로우**
```
매일 오전 9시 안부 메시지 자동 발송
         ↓
사용자 응답 → AI 분석 (OpenAI GPT-4o)
         ↓
이상징후 감지 (3종 알고리즘)
         ↓
보호자 알림 발송 (실시간)
```

### 🏆 **현재 상태: Phase 2 MVP 100% 완성** (2025-09-16)
- ✅ **6개 핵심 도메인 완성**: TDD + DDD 완전 적용
- ✅ **25+ REST API**: JWT 인증, Swagger 문서화 완료
- ✅ **실제 운영 준비**: 상용 서비스 수준 달성
- ✅ **AI 시스템**: OpenAI GPT-4o 실제 연동 완성
- ✅ **자동화**: 스케줄링, 알림, 감지 시스템 완전 자동화

### 🛠️ **기술 스택**
```
Backend: Spring Boot 3.5.x + Java 21
Database: PostgreSQL + Redis
AI: OpenAI GPT-4o (Spring AI)
Auth: JWT (Access/Refresh Token)
Testing: TDD Red-Green-Blue 사이클
Architecture: DDD (Domain-Driven Design)
Docs: Swagger/OpenAPI 3.0
Deployment: Docker + Docker Compose
```

## Quick Start

### 필수 환경 변수 (`.env` 파일)
```bash
# Database
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Redis  
REDIS_PASSWORD=your_redis_password

# JWT (필수)
JWT_SECRET_KEY=your_jwt_secret_key_at_least_32_characters
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000

# OpenAI API (Phase 1: AI 대화 시스템)
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o
OPENAI_MAX_TOKENS=100
OPENAI_TEMPERATURE=0.7
```

### 개발 명령어
```bash
# Docker로 전체 환경 실행
docker-compose up -d

# 로컬에서 애플리케이션만 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

## 🏗️ Architecture

### 📊 **도메인 계층 구조** (DDD 완전 적용)
```
🔐 Foundation Layer (기반 시스템) ✅ 완성
├── Member (회원 관리) ✅
└── Auth (JWT 인증) ✅

💬 Core Service Layer (핵심 서비스) ✅ 완성
├── Conversation (AI 대화) ✅     # OpenAI GPT-4o 연동
├── DailyCheck (스케줄링) ✅      # 매일 안부 메시지 자동화
└── Guardian (보호자 관리) ✅      # 7개 REST API

🚨 Integration Layer (통합/알림) ✅ 완성
├── AlertRule (이상징후 감지) ✅   # 3종 알고리즘
└── Notification (알림 서비스) ✅  # Mock 기반 확장 준비
```

### 🔄 **도메인간 핵심 데이터 플로우**
```
📱 안부 확인: DailyCheck → Notification → Member
💬 대화 분석: Conversation → AlertRule → Guardian → Notification
🚨 긴급 상황: AlertRule → Guardian → Notification (즉시 발송)
```

### 🎯 **완성된 핵심 시스템들**

#### ✅ **AI 대화 시스템** (Conversation 도메인)
- **OpenAI GPT-4o 실제 연동**: Spring AI 기반 완전 구현
- **키워드 기반 감정분석**: POSITIVE/NEGATIVE/NEUTRAL 3단계
- **대화 세션 관리**: 자동 생성/조회, 영속성 보장
- **REST API**: POST /api/conversations/messages + JWT 인증

#### ✅ **스케줄링 시스템** (DailyCheck 도메인)
- **매일 정시 발송**: Cron "0 0 9 * * *" 오전 9시 자동화
- **중복 방지**: DB 제약 조건으로 일일 중복 완전 차단
- **자동 재시도**: 실패 시 점진적 지연 (5분 간격, 최대 3회)
- **83% 코드 개선**: TDD Blue 단계로 품질 향상

#### ✅ **이상징후 감지** (AlertRule 도메인)
- **3종 감지 알고리즘**: 감정패턴/무응답/키워드 분석
- **실시간 키워드 감지**: 긴급 키워드 즉시 알림
- **보호자 연동**: Guardian 시스템과 완전 통합
- **8개 REST API**: 규칙 관리 + 이력 조회 완성

#### ✅ **보호자 관리** (Guardian 도메인)
- **관계 설정**: GuardianRelation enum 기반 체계적 관리
- **알림 설정**: NotificationPreference로 개인화
- **권한 관리**: 접근 제어 및 데이터 보호
- **7개 REST API**: CRUD + 관계 관리 완성

#### ✅ **인증/보안 시스템** (Member + Auth 도메인)
- **JWT 이중 토큰**: Access(1시간) + Refresh(24시간) 분리
- **Redis 기반 저장**: 토큰 무효화 및 블랙리스트 관리
- **DDD 의존성 역전**: 도메인 → Global 구현체 구조
- **Spring Security**: 필터 체인 기반 완전 보안

### Package Structure
```
com.anyang.maruni/
├── global/                          # 완성됨 - 수정 지양
│   ├── config/                     # 설정 (Swagger, Security, Redis, JWT)
│   ├── response/                   # 표준화된 API 응답 시스템
│   │   ├── annotation/            # @AutoApiResponse, @SuccessCodeAnnotation
│   │   ├── dto/CommonApiResponse  # 공통 응답 DTO
│   │   ├── success/               # 성공 코드 정의
│   │   └── error/                 # 에러 코드 정의
│   ├── exception/                 # 글로벌 예외 처리
│   ├── swagger/                   # Swagger 커스터마이징
│   ├── advice/                    # 컨트롤러 조언
│   ├── security/                  # Spring Security 필터 및 JWT 유틸
│   │   ├── JWTUtil.java          # JWT 토큰 생성/검증 (TokenManager 구현)
│   │   ├── JwtTokenService.java  # 토큰 발급 서비스 (TokenService 구현)
│   │   ├── JwtAuthenticationFilter.java  # JWT 인증 필터
│   │   ├── LoginFilter.java      # 로그인 처리 필터
│   │   └── AuthenticationEventHandler.java  # 인증 이벤트 인터페이스
│   └── entity/BaseTimeEntity      # JPA 감사 기본 엔티티
├── domain/                        # 비즈니스 도메인들 (DDD 구조)
│   ├── member/                   # 회원 관리 도메인 ✅
│   │   ├── application/          # Application Layer
│   │   │   ├── dto/             # Request/Response DTO
│   │   │   ├── service/         # Application Service (MemberService)
│   │   │   └── mapper/          # DTO ↔ Entity 매핑
│   │   ├── domain/              # Domain Layer
│   │   │   ├── entity/         # MemberEntity (도메인 엔티티)
│   │   │   └── repository/     # MemberRepository (인터페이스)
│   │   ├── infrastructure/      # Infrastructure Layer
│   │   │   └── security/       # Spring Security 구현체
│   │   │       ├── CustomUserDetails.java
│   │   │       └── CustomUserDetailsService.java
│   │   └── presentation/        # Presentation Layer
│   │       └── controller/     # REST API 컨트롤러
│   ├── auth/                    # 인증/권한 도메인 ✅
│   │   ├── application/         # AuthenticationService (이벤트 핸들러 구현)
│   │   ├── domain/              # 토큰 관련 도메인 서비스 및 VO
│   │   │   ├── service/        # TokenValidator, RefreshTokenService 등
│   │   │   ├── vo/             # MemberTokenInfo (Value Object)
│   │   │   ├── entity/         # RefreshToken Entity
│   │   │   └── repository/     # 토큰 저장소 인터페이스
│   │   ├── infrastructure/      # Redis 기반 토큰 저장소 구현
│   │   └── presentation/        # 토큰 재발급 API 등
│   ├── conversation/             # AI 대화 도메인 ✅ (100% 완료)
│   │   ├── application/          # Application Layer
│   │   │   ├── dto/             # ConversationRequestDto, ConversationResponseDto, MessageDto
│   │   │   └── service/         # SimpleConversationService ✅ 완성
│   │   ├── domain/              # Domain Layer
│   │   │   ├── entity/         # ConversationEntity, MessageEntity, EmotionType, MessageType
│   │   │   └── repository/     # ConversationRepository, MessageRepository
│   │   ├── infrastructure/      # Infrastructure Layer
│   │   │   └── SimpleAIResponseGenerator.java  # ✅ OpenAI GPT-4o 연동, 감정분석 완성
│   │   └── presentation/        # Presentation Layer
│   │       └── controller/     # ConversationController ✅ REST API 완성
│   ├── dailycheck/              # 스케줄링 시스템 도메인 ✅ (100% 완료)
│   │   ├── application/         # Application Layer
│   │   │   └── service/        # DailyCheckService ✅ TDD 완전 구현
│   │   ├── domain/             # Domain Layer
│   │   │   ├── entity/        # DailyCheckRecord, RetryRecord ✅
│   │   │   └── repository/    # DailyCheckRecordRepository, RetryRecordRepository ✅
│   │   └── infrastructure/     # (향후 확장 대비)
│   ├── guardian/                # 보호자 관리 도메인 ✅ (100% 완료)
│   │   ├── application/         # Application Layer
│   │   │   ├── dto/            # GuardianRequestDto, GuardianResponseDto 등
│   │   │   └── service/        # GuardianService ✅ TDD 완전 구현
│   │   ├── domain/             # Domain Layer
│   │   │   ├── entity/        # GuardianEntity, GuardianRelation, NotificationPreference ✅
│   │   │   └── repository/    # GuardianRepository ✅
│   │   └── presentation/       # Presentation Layer
│   │       └── controller/    # GuardianController ✅ REST API 완성
│   ├── alertrule/              # 이상징후 감지 도메인 ✅ (100% 완료)
│   │   ├── application/        # Application Layer
│   │   │   ├── dto/           # AlertRuleRequestDto, AlertRuleResponseDto 등 ✅
│   │   │   ├── service/       # AlertRuleService ✅ TDD 완전 구현
│   │   │   └── analyzer/      # EmotionPatternAnalyzer, NoResponseAnalyzer, KeywordAnalyzer ✅
│   │   ├── domain/            # Domain Layer
│   │   │   ├── entity/       # AlertRule, AlertHistory, AlertCondition, AlertType, AlertLevel ✅
│   │   │   └── repository/   # AlertRuleRepository, AlertHistoryRepository ✅
│   │   └── presentation/      # Presentation Layer
│   │       └── controller/   # AlertRuleController ✅ 8개 REST API 완성
│   ├── notification/           # 알림 시스템 도메인 ✅ (100% 완료)
│   │   ├── domain/service/    # NotificationService 인터페이스 ✅
│   │   └── infrastructure/    # MockPushNotificationService ✅
└── MaruniApplication
```

### 핵심 아키텍처 컴포넌트

#### 1. 글로벌 응답 시스템
- **@AutoApiResponse**: 자동 응답 래핑 (컨트롤러 클래스/메소드 레벨)
- **ApiResponseAdvice**: 모든 컨트롤러 응답을 `CommonApiResponse<T>` 구조로 래핑
- **@SuccessCodeAnnotation**: 메소드별 성공 코드 지정

#### 2. 예외 처리 시스템  
- **GlobalExceptionHandler**: 모든 예외를 일관된 응답으로 변환
- **BaseException**: 모든 비즈니스 예외의 기본 클래스
- **자동 처리**: Bean Validation 오류, Enum 변환 오류 등

#### 3. API 문서화 (Swagger)
- **자동 문서 생성**: `@CustomExceptionDescription`, `@SuccessResponseDescription`
- **JWT 인증 지원**: Bearer 토큰 인증 스키마 자동 적용
- **동적 서버 URL**: 환경별 서버 URL 자동 설정

#### 4. 인증/보안 시스템 (JWT 기반)
- **의존성 역전**: 도메인 인터페이스 → Global 구현체 구조로 DDD 원칙 준수
- **토큰 관리**: Access/Refresh 토큰 분리, Redis 기반 저장
- **Spring Security**: 필터 체인을 통한 JWT 인증/인가 처리
- **계층 분리**: Infrastructure → Application Service → Domain Repository 의존성 구조

## 🎯 Claude Code 작업 가이드라인

### 📚 **문서 기반 작업 원칙 (최우선)**

#### 🔍 **작업 전 필수 확인 순서**
```
1. docs/README.md → 전체 프로젝트 현황 파악
2. docs/domains/README.md → 도메인 아키텍처 이해
3. docs/specifications/README.md → 기술 규격서 통합 가이드 확인
4. docs/domains/{domain}.md → 해당 도메인 구현 가이드 숙지
5. docs/specifications/{관련규격}.md → 해당 기술 규격 숙지
   ├── coding-standards.md (매일 참조)
   ├── architecture-guide.md (새 기능 개발)
   ├── api-design-guide.md (API 작업)
   └── database-design-guide.md (Entity 작업)
6. 실제 코드 확인 → 문서와 코드 일치성 검증
7. 작업 진행 → 문서 기반 패턴 준수
```

#### ⚠️ **중요: MARUNI는 완성된 프로젝트**
- **Phase 2 MVP 100% 완료**: 6개 도메인 모두 TDD + DDD 적용 완성
- **25+ REST API 완성**: 실제 운영 가능한 상용 서비스 수준
- **기존 패턴 준수**: 새 기능 추가 시 기존 구조와 일관성 유지
- **문서 우선**: 추측하지 말고 반드시 문서 확인 후 작업

### 🚫 **절대 금지사항**
- **문서 무시**: 문서 확인 없이 임의 추론하거나 가정하지 않음
- **패턴 파괴**: 기존 TDD + DDD 구조를 무시한 임의 구현
- **무단 결정**: 완성된 시스템의 아키텍처 변경을 사용자 확인 없이 진행
- **할루시네이션**: 존재하지 않는 API, 엔티티, 서비스 등을 만들어내지 않음

### ✅ **반드시 지켜야 할 원칙**

#### 1. **문서 기반 작업**
```
❌ "UserService를 추가하겠습니다"
✅ "docs/domains/member.md를 확인하니 MemberService가 이미 완성되어 있습니다"

❌ "JWT 설정을 새로 하겠습니다"
✅ "docs/specifications/security-guide.md에 JWT 시스템이 완전히 구현되어 있어 기존 패턴을 사용하겠습니다"

❌ "Entity를 만들어야겠습니다"
✅ "docs/specifications/database-design-guide.md의 BaseTimeEntity 상속 패턴을 따르겠습니다"
```

#### 2. **기존 패턴 준수**
```
❌ "새로운 방식으로 Entity를 만들겠습니다"
✅ "docs/specifications/database-design-guide.md의 BaseTimeEntity 상속 + 정적 팩토리 메서드 패턴을 따르겠습니다"

❌ "Controller를 간단하게 만들겠습니다"
✅ "docs/specifications/api-design-guide.md의 @AutoApiResponse + Swagger 어노테이션 패턴을 적용하겠습니다"

❌ "테스트를 대충 만들겠습니다"
✅ "docs/specifications/testing-guide.md의 TDD Red-Green-Blue 사이클을 적용하겠습니다"
```

#### 3. **TDD 사이클 준수**
```
완성된 도메인 확장 시:
1. docs/specifications/testing-guide.md의 테스트 패턴 분석
2. Red: 기존 스타일로 실패 테스트 작성
3. Green: 기존 패턴 준수한 최소 구현
4. Blue: 리팩토링 + 50% 이상 코드 품질 향상
```

## 🔄 개발 워크플로우

### 🆕 **새 도메인 개발 시** (Phase 3+ 확장)
```
⚠️ 주의: 6개 핵심 도메인은 모두 완성됨. 새 도메인은 Phase 3 계획 참조

1. docs/roadmap/phase3.md 확인 → 계획된 도메인인지 검증
2. docs/domains/README.md → 기존 도메인과 의존 관계 분석
3. 사용자 승인 후 TDD Red-Green-Blue 사이클 적용:
   ├── Red: 실패 테스트 작성 (기존 패턴 참고)
   ├── Green: 최소 구현 (DDD 구조 준수)
   └── Blue: 리팩토링 + 품질 향상
4. docs/domains/{새도메인}.md 가이드 작성
5. CLAUDE.md 업데이트
```

### 🔧 **기존 도메인 확장 시** (현재 주요 작업)
```
1. docs/domains/{해당도메인}.md 가이드 완전 숙지
2. docs/specifications/ 관련 기술 규격 확인:
   ├── coding-standards.md (코딩 컨벤션)
   ├── architecture-guide.md (DDD 구조)
   ├── api-design-guide.md (API 패턴)
   ├── database-design-guide.md (Entity 패턴)
   └── testing-guide.md (TDD 패턴)
3. 실제 코드 확인 → 문서와 일치성 검증
4. TDD 사이클 준수:
   ├── Red: 기존 테스트 스타일로 실패 테스트 작성
   ├── Green: 기존 패턴 준수한 최소 구현
   └── Blue: 50% 이상 코드 품질 향상
5. 해당 도메인 가이드 문서 업데이트
```

### ✅ **코드 생성 필수 체크리스트** (완성된 패턴 준수)
```
DDD 계층:
- [ ] **Entity**: BaseTimeEntity 상속 + 정적 팩토리 메서드
- [ ] **Repository**: JpaRepository 상속 + 커스텀 쿼리 메서드
- [ ] **Service**: @Transactional + BaseException 활용
- [ ] **Controller**: @AutoApiResponse + Swagger 완전 문서화
- [ ] **DTO**: Bean Validation + 정적 from() 메서드

완성된 시스템 연동:
- [ ] **JWT 인증**: 기존 Spring Security 구조 활용
- [ ] **예외 처리**: 기존 BaseException 계층 구조 준수
- [ ] **API 응답**: 기존 CommonApiResponse 래핑 구조 활용
- [ ] **테스트**: 기존 @ExtendWith(MockitoExtension.class) 패턴
```

### 🏗️ **DDD 계층별 작업 순서**
```
Domain Layer (핵심):
1. Entity 설계 → BaseTimeEntity 상속 + 정적 팩토리
2. Repository 인터페이스 → 도메인 로직에 맞는 메서드

Application Layer:
3. DTO 정의 → Bean Validation + from() 메서드
4. Service 구현 → @Transactional + 비즈니스 로직

Infrastructure Layer:
5. Repository 구현체 → JPA 기반 (Spring Data JPA 자동 생성)

Presentation Layer:
6. Controller → @AutoApiResponse + Swagger 문서화
7. 통합 테스트 → MockMvc 기반
```

## 네이밍 컨벤션

### 패키지 & 클래스
- **도메인 패키지**: 단수형, 소문자 (`member`, `auth`)
- **Entity**: `{Domain}Entity`
- **Service**: `{Domain}Service`  
- **Controller**: `{Domain}Controller`
- **DTO**: `{Domain}{Action}RequestDto/ResponseDto`
- **API 경로**: `/api/{domain}` (RESTful)

## 문제 해결 가이드

### 자주 발생하는 문제들

**API 응답이 래핑되지 않을 때**
→ `@AutoApiResponse` 어노테이션 확인

**커스텀 예외가 처리되지 않을 때**  
→ `BaseException` 상속 및 `ErrorCode` 정의 확인

**Swagger 예시가 표시되지 않을 때**
→ `@CustomExceptionDescription` 어노테이션 확인

**Docker 환경에서 DB 연결 실패**
→ `.env` 파일 환경변수 및 `docker-compose up -d` 실행 확인

**JWT 토큰 인증 실패**  
→ Authorization 헤더 형식 확인 (`Bearer {token}`)  
→ 토큰 만료 시간 및 Secret Key 설정 확인  

**Security 관련 403/401 에러**
→ SecurityConfig의 permitAll() 경로 설정 확인  
→ JWT 필터 순서 및 CustomUserDetailsService Bean 등록 확인

### 디버깅
```bash
# 헬스 체크
curl http://localhost:8080/actuator/health

# 컨테이너 상태 확인
docker-compose ps

# 애플리케이션 로그 확인  
docker-compose logs -f app
```

## 성능 & 보안 원칙

### 보안
- 민감 정보는 반드시 환경 변수로 관리
- Bean Validation을 통한 입력 검증 필수
- JPA 쿼리 사용으로 SQL Injection 방지

### 성능  
- 조회 전용 메소드에 `@Transactional(readOnly = true)` 적용
- Redis 캐싱 전략적 활용
- N+1 쿼리 문제 방지를 위한 적절한 fetch 전략

## 작업 완료 후 문서 업데이트

### 반드시 업데이트해야 하는 경우
- 새 도메인/패키지 추가 → 패키지 구조 섹션 업데이트
- 새 환경변수 추가 → Quick Start 섹션 업데이트
- 새 개발 패턴 발견 → 표준 템플릿 섹션 업데이트
- 새 문제 해결법 → 문제 해결 가이드 업데이트

## 📋 프로젝트 현황 (2025-09-16 완성)

### 🎉 **Phase 2 MVP 100% 완성!**

**MARUNI는 TDD + DDD 방법론을 완전 적용하여 상용 서비스 수준으로 완성되었습니다.**

#### ✅ **완성된 핵심 지표**
- **6개 도메인 100% 완성**: Foundation → Core Service → Integration Layer
- **25+ REST API 엔드포인트**: JWT 인증 + Swagger 문서화 완료
- **TDD 완전 사이클**: Red-Green-Blue 모든 도메인 적용
- **실제 AI 연동**: OpenAI GPT-4o + Spring AI 완전 구현
- **자동화 시스템**: 스케줄링 + 알림 + 감지 완전 자동화
- **상용 서비스 준비**: 실제 운영 가능한 완성도

#### 🏆 **주요 성과**
```
💬 AI 대화 시스템: OpenAI GPT-4o 실제 연동 + 키워드 감정분석
📅 스케줄링 시스템: 매일 오전 9시 자동 발송 + 83% 코드 개선
🚨 이상징후 감지: 3종 알고리즘 + 50% 코드 품질 향상
👥 보호자 관리: 7개 REST API + 완전한 알림 연동
🔐 JWT 인증: Access/Refresh 토큰 + Redis 기반 완전 보안
```

### 📚 **상세 구현 내용은 문서 참조**

#### 🏗️ **도메인별 구현 가이드**
- **`docs/domains/conversation.md`**: AI 대화 시스템 (OpenAI GPT-4o)
- **`docs/domains/dailycheck.md`**: 스케줄링 시스템 (TDD 완전 사이클)
- **`docs/domains/alertrule.md`**: 이상징후 감지 (3종 알고리즘)
- **`docs/domains/guardian.md`**: 보호자 관리 시스템
- **`docs/domains/member.md`** + **`docs/domains/auth.md`**: 인증/보안

#### 🚀 **향후 계획**
- **`docs/roadmap/phase3.md`**: 고도화 & 모바일 연동 (8주 계획)
  - Week 1-4: 고급 건강 분석 (ML 기반)
  - Week 5-8: 모바일 앱 연동 (Flutter API)

---

**⚠️ 중요: 모든 상세 구현 내용과 코드 패턴은 docs/ 폴더의 해당 문서를 반드시 확인하세요.**