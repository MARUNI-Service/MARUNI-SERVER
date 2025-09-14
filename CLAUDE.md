# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MARUNI**는 노인들의 외로움과 우울증 문제 해결을 위한 문자 기반 소통 서비스입니다. 매일 정기적으로 안부 문자를 보내고, AI를 통해 응답을 분석하여 이상징후를 감지하며, 필요시 보호자에게 알림을 전송하는 노인 돌봄 플랫폼입니다.

### 서비스 특징
- **능동적 소통**: 매일 아침 9시 안부 문자 자동 발송
- **AI 기반 분석**: 문자 응답을 통한 감정 상태 및 이상징후 감지  
- **보호자 연동**: 긴급 상황 시 보호자/관리자에게 실시간 알림
- **건강 모니터링**: 지속적인 대화를 통한 건강 상태 추적

### 기술 스택
Spring Boot 3.5.x + Java 21, JWT 인증, Redis 캐싱, PostgreSQL, Docker, Swagger/OpenAPI

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

## Architecture

### 프로젝트 현재 상태
- ✅ **Global 아키텍처 완성**: 응답 래핑, 예외 처리, Swagger 문서화 시스템 구축
- ✅ **인프라 설정 완료**: Docker, PostgreSQL, Redis 환경 구성
- ✅ **인증/보안 시스템 구축**: JWT 토큰 기반 인증, DDD 원칙 준수한 Security Layer 구현
- ✅ **Member 도메인 구현**: 회원 가입, 인증, 관리 기능 완성
- ✅ **Auth 도메인 구현**: 토큰 발급/검증, 로그인/로그아웃 기능 완성
- ✅ **Conversation 도메인 구현 완료** (100%): AI 대화 시스템 MVP 완성
  - ✅ SimpleAIResponseGenerator 완성 (OpenAI GPT-4o 연동, 키워드 기반 감정분석)
  - ✅ Entity 설계 완성 (ConversationEntity, MessageEntity, EmotionType 등)
  - ✅ SimpleConversationService 핵심 로직 구현 완료 (대화 생성, 메시지 처리, AI 응답 생성)
  - ✅ Repository 패턴 구현 (ConversationRepository, MessageRepository)
  - ✅ REST API Controller 구현 (POST /api/conversations/messages)
  - ✅ TDD 테스트 코드 작성 (실제 비즈니스 로직 검증)
- ⏳ **비즈니스 로직**: SMS 발송, 보호자 알림 등 추가 도메인 구현 필요

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
│   └── ...                      # 추가 도메인들 (SMS, 안부메시지, 알림 등)
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

## Claude 작업 가이드라인

### 🚫 절대 금지사항
- **추론/추측 금지**: 불확실한 내용에 대해 임의로 추론하거나 가정하지 않음
- **할루시네이션 방지**: 존재하지 않는 API, 라이브러리, 설정값 등을 만들어내지 않음
- **무단 결정 금지**: 비즈니스 로직이나 아키텍처 결정을 사용자 확인 없이 진행하지 않음

### ✅ 반드시 지켜야 할 원칙
1. **질문 우선**: 불확실한 내용은 반드시 사용자에게 먼저 질문
2. **확인 후 진행**: 중요한 구현 결정 전 사용자 승인 필수
3. **문서 기반 작업**: 기존 코드와 이 문서를 최우선 참고
4. **단계적 접근**: 복잡한 작업은 단계별로 소통하며 진행
5. **문서 업데이트**: 주요 작업 완료 후 반드시 CLAUDE.md 업데이트

### 💬 올바른 질문 예시
❌ "SMS API는 Twilio를 사용하겠습니다"  
✅ "SMS 발송을 위해 어떤 서비스를 사용하실 계획인가요? (Twilio, AWS SNS, 국내 서비스 등)"

❌ "JWT 토큰 만료시간을 1시간으로 설정하겠습니다"  
✅ "JWT 액세스 토큰의 만료시간은 어떻게 설정하시겠어요? 보안과 사용성을 고려해야 합니다"

## 개발 워크플로우

### 새 도메인 개발 순서
```
1. 요구사항 분석 및 사용자 확인
2. Entity 설계 (BaseTimeEntity 상속)
3. Repository 생성 (JpaRepository 상속)
4. Service 구현 (@Transactional, BaseException 활용)
5. DTO 정의 (Bean Validation 적용)
6. Controller 생성 (@AutoApiResponse, Swagger 어노테이션)
7. ErrorCode/SuccessCode 추가
8. 테스트 작성
9. CLAUDE.md 업데이트
```

### 코드 생성 필수 체크리스트
- [ ] **Entity**: BaseTimeEntity 상속
- [ ] **Service**: @Transactional 적절히 적용
- [ ] **Controller**: @AutoApiResponse 적용  
- [ ] **DTO**: Bean Validation 어노테이션
- [ ] **Exception**: BaseException 상속
- [ ] **Swagger**: 문서화 어노테이션 적용
- [ ] **DDD 구조**: Domain/Application/Infrastructure/Presentation 계층 분리
- [ ] **의존성 방향**: Infrastructure → Application → Domain 순서 준수

### 표준 템플릿

#### Entity
```java
@Entity
@Table(name = "table_name")  
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExampleEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String requiredField;
}
```

#### Service
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService {
    private final ExampleRepository repository;
    
    @Transactional
    public ExampleResponseDto create(ExampleRequestDto request) {
        // BaseException 상속 예외로 오류 처리
        return ExampleResponseDto.from(repository.save(entity));
    }
}
```

#### Controller
```java
@RestController
@RequestMapping("/api/examples")
@RequiredArgsConstructor
@AutoApiResponse
@CustomExceptionDescription(SwaggerResponseDescription.COMMON_ERROR)
public class ExampleController {
    private final ExampleService service;
    
    @PostMapping
    @SuccessResponseDescription(SuccessCode.SUCCESS)
    public ExampleResponseDto create(@Valid @RequestBody ExampleRequestDto request) {
        return service.create(request);
    }
}
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

## 📋 최근 완료 작업

### ✅ Security Layer DDD 구조 개선 완료 (2025-09-11)
- **CustomUserDetailsService/CustomUserDetails** → `domain/member/infrastructure/security`로 이동
- **Repository 직접 접근 제거**: Infrastructure → Application Service → Domain Repository 구조로 변경
- **DDD 원칙 준수도**: 85% → 95%로 향상
- **의존성 역전 완성**: 도메인 인터페이스 → Global 구현체 구조 확립

### ✅ Phase 1: AI 대화 시스템 MVP 완료 (2025-09-14)
**진행률: 100% 완료** 🎉

#### ✅ 완성된 모든 구성요소
- **SimpleAIResponseGenerator** (100%): OpenAI GPT-4o API 연동 완성
  - AI 응답 생성: `generateResponse()` 메서드 완전 구현
  - 감정 분석: `analyzeBasicEmotion()` 키워드 기반 3단계 분석 (POSITIVE/NEGATIVE/NEUTRAL)
  - 방어적 코딩: 예외 처리, 응답 길이 제한, 입력 검증
  - 테스트 커버리지: 5개 테스트 케이스 작성 (Mock 포함)

- **Entity 설계** (100%): DDD 구조 완성
  - `ConversationEntity`: BaseTimeEntity 상속, 정적 팩토리 메서드
  - `MessageEntity`: 타입별 정적 팩토리 메서드 구현
  - `EmotionType`, `MessageType` Enum 완성

- **DTO 계층** (100%): `ConversationRequestDto`, `ConversationResponseDto`, `MessageDto` 완성

- **SimpleConversationService** (100%): 핵심 비즈니스 로직 완전 구현
  - `processUserMessage()`: 전체 대화 플로우 관리
  - `findOrCreateActiveConversation()`: 대화 세션 자동 생성/조회
  - `saveUserMessage()`: 감정 분석 포함 사용자 메시지 저장
  - `saveAIMessage()`: AI 응답 메시지 저장
  - 완전한 트랜잭션 처리 및 데이터베이스 연동

- **Repository 패턴** (100%): JPA Repository 완전 활용
  - `ConversationRepository`: 회원별 활성 대화 조회 메서드 구현
  - `MessageRepository`: 대화별, 감정별 메시지 조회 메서드 구현

- **Controller 계층** (100%): REST API 완성
  - `ConversationController`: POST /api/conversations/messages 구현
  - Spring Security 인증 연동, Bean Validation 적용
  - Swagger API 문서화 완료

- **테스트 코드** (100%): 실제 비즈니스 로직 검증
  - 더미 구현 → 실제 비즈니스 로직 테스트로 전환
  - Mock 기반 3개 테스트 시나리오 (기존/신규 대화, 감정 분석)
  - 모든 테스트 통과 확인

#### 🚀 MVP 핵심 기능 구현 완료
- ✅ 사용자 메시지 → AI 응답 생성 플로우
- ✅ 대화 데이터 영속성 저장 (PostgreSQL)
- ✅ 키워드 기반 감정 분석 (긍정/중립/부정)
- ✅ REST API 제공 (JWT 인증 포함)
- ✅ DDD 아키텍처 완벽 준수
- ✅ TDD 접근 방식으로 개발 진행

### 📚 관련 문서
- **Phase 1 MVP 계획서**: `docs/phase1-ai-system-mvp.md`
- **아키텍처 분석 보고서**: `docs/architecture/security-layer-analysis.md`
- **구현된 도메인**: Member(회원), Auth(인증), Conversation(AI대화-완료) 도메인
- **JWT 인증 시스템**: Access/Refresh 토큰, Redis 저장소 구축 완료

이제 Claude는 이 최적화된 가이드를 바탕으로 MARUNI 프로젝트에서 효율적이고 일관된 개발을 진행할 수 있습니다!