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
- ⏳ **비즈니스 로직**: 도메인별 Controller, Service, Repository 구현 필요
- ⏳ **인증/보안**: JWT 토큰 기반 인증 시스템 구현 필요

### Package Structure
```
com.anyang.maruni/
├── global/                          # 완성됨 - 수정 지양
│   ├── config/                     # 설정 (Swagger, Security, Redis)
│   ├── response/                   # 표준화된 API 응답 시스템
│   │   ├── annotation/            # @AutoApiResponse, @SuccessCodeAnnotation
│   │   ├── dto/CommonApiResponse  # 공통 응답 DTO
│   │   ├── success/               # 성공 코드 정의
│   │   └── error/                 # 에러 코드 정의
│   ├── exception/                 # 글로벌 예외 처리
│   ├── swagger/                   # Swagger 커스터마이징
│   ├── advice/                    # 컨트롤러 조언
│   └── entity/BaseTimeEntity      # JPA 감사 기본 엔티티
├── domain/                        # 새로 개발할 비즈니스 도메인들
│   ├── member/                   # 사용자 관리
│   ├── auth/                     # 인증/권한
│   └── ...                       # 기타 도메인들
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

이제 Claude는 이 최적화된 가이드를 바탕으로 MARUNI 프로젝트에서 효율적이고 일관된 개발을 진행할 수 있습니다!