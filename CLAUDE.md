# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 📋 문서 참조 가이드

**MARUNI 프로젝트는 체계적인 문서화가 완료되어 있습니다. 작업 전 반드시 관련 문서를 먼저 확인하세요.**

### 📚 핵심 문서 구조
```
docs/
├── README.md                   # 📋 전체 프로젝트 가이드
├── domains/                    # 🏗️ 도메인별 구현 가이드 (7개)
│   ├── README.md               # 도메인 아키텍처 개요
│   ├── member.md               # 회원 관리
│   ├── auth.md                 # JWT Stateless 인증
│   ├── conversation.md         # AI 대화 (OpenAI GPT-4o)
│   ├── dailycheck.md           # 스케줄링 시스템
│   ├── guardian.md             # 보호자 관리
│   ├── alertrule.md            # 이상징후 감지
│   └── notification.md         # 알림 시스템
├── specifications/             # 📝 기술 규격서 (10개)
│   ├── README.md               # 규격서 통합 가이드
│   ├── coding-standards.md     # ⭐⭐⭐ Java 코딩 컨벤션
│   ├── quick-reference.md      # ⭐⭐⭐ 빠른 참조 템플릿
│   ├── architecture-guide.md   # ⭐⭐ DDD 아키텍처
│   ├── api-design-guide.md     # ⭐⭐ REST API 설계
│   ├── database-design-guide.md # ⭐ Entity 설계
│   ├── testing-guide.md        # ⭐ TDD 테스트
│   ├── security-guide.md       # 🔒 JWT 인증
│   ├── performance-guide.md    # ⚡ JPA 최적화
│   └── tech-stack.md           # 🛠️ 기술 스택
├── roadmap/                    # 🚀 발전 계획
│   └── phase3.md               # Phase 3 계획
└── new/
    └── user-journey.md         # 사용자 여정
```

### ⚠️ 작업 시 필수 순서
1. `docs/README.md` - 전체 현황 파악
2. `docs/domains/README.md` - 도메인 구조 이해
3. `docs/domains/{domain}.md` - 해당 도메인 가이드 숙지
4. `docs/specifications/{관련규격}.md` - 기술 규격 확인
5. 실제 코드 확인 후 작업 진행

## Project Overview

**MARUNI**는 노인 돌봄을 위한 완성된 AI 기반 소통 플랫폼입니다.

### 🎯 서비스 핵심 플로우
```
매일 오전 9시 안부 메시지 자동 발송
    ↓
사용자 응답 → AI 분석 (OpenAI GPT-4o)
    ↓
이상징후 감지 (3종 알고리즘)
    ↓
보호자 알림 발송 (알림 타입 시스템)
```

### 🏆 현재 상태: Phase 2 MVP 100% 완성 (2025-11-05)
- ✅ 7개 핵심 도메인 완성 (TDD + DDD)
- ✅ 25+ REST API (JWT 인증 + Swagger)
- ✅ OpenAI GPT-4o 실제 연동
- ✅ 알림 타입 시스템 (5종) + 이력 관리
- ✅ 상용 서비스 준비 완료

### 🛠️ 기술 스택
```
Backend: Spring Boot 3.5.x + Java 21
Database: PostgreSQL (단일 DB)
AI: OpenAI GPT-4o (Spring AI)
Auth: JWT Stateless (Access Token Only, 1시간)
Notification: 알림 타입 시스템 (5종) + Mock 구현체
Testing: TDD Red-Green-Blue
Architecture: DDD
Docs: Swagger/OpenAPI 3.0
Deployment: Docker + Docker Compose
```

## Quick Start

### 환경 변수 (.env)
```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT (Access Token Only)
JWT_SECRET_KEY=your_jwt_secret_key_at_least_32_characters
JWT_ACCESS_EXPIRATION=3600000  # 1시간

# OpenAI
OPENAI_API_KEY=your_openai_api_key
```

### 개발 명령어
```bash
docker-compose up -d    # 전체 환경 실행
./gradlew bootRun       # 로컬 실행
./gradlew test          # 테스트 실행
```

## 🏗️ Architecture

### 도메인 계층 구조 (DDD)
```
🔐 Foundation Layer
├── Member (회원 관리) ✅
└── Auth (JWT 인증) ✅

💬 Core Service Layer
├── Conversation (AI 대화) ✅
├── DailyCheck (스케줄링) ✅
└── Guardian (보호자 관리) ✅

🚨 Integration Layer
├── AlertRule (이상징후 감지) ✅
└── Notification (알림 시스템) ✅
```

### 도메인간 데이터 플로우
```
안부 확인: DailyCheck → Notification → Member
대화 분석: Conversation → AlertRule → Guardian → Notification
긴급 상황: AlertRule → Guardian → Notification (즉시)
```

### Package Structure
```
com.anyang.maruni/
├── global/                     # 글로벌 설정
│   ├── config/                # Security, Swagger, JWT
│   ├── response/              # 표준화 API 응답
│   ├── exception/             # 글로벌 예외 처리
│   └── security/              # JWT 필터, 인증
├── domain/                    # 비즈니스 도메인 (DDD)
│   ├── member/               # 회원 관리
│   ├── auth/                 # 인증/인가
│   ├── conversation/         # AI 대화
│   ├── dailycheck/           # 스케줄링
│   ├── guardian/             # 보호자 관리
│   ├── alertrule/            # 이상징후 감지
│   └── notification/         # 알림 시스템
└── MaruniApplication
```

### DDD 계층별 구조
```
domain/{도메인}/
├── application/              # Application Layer
│   ├── dto/                 # Request/Response DTO
│   ├── service/             # Application Service
│   └── mapper/              # DTO ↔ Entity 매핑
├── domain/                  # Domain Layer
│   ├── entity/             # 도메인 엔티티
│   └── repository/         # Repository 인터페이스
├── infrastructure/          # Infrastructure Layer
│   └── (외부 연동 구현체)
└── presentation/            # Presentation Layer
    └── controller/         # REST API 컨트롤러
```

## 🎯 Claude Code 작업 가이드라인

### 🎯 기본 작업 원칙 (필수)
- **계획 요청 시**: 항상 코드 구현 여부를 먼저 물어볼 것
- **최소 구현**: 요청받은 것보다 더 많은 코드를 작성하지 않을 것
- **서버 실행 금지**: 구현 완료 후 서버를 자동으로 시작하지 않을 것
- **한국어 우선**: 모든 응답은 한국어를 기본으로 작성할 것
- **UTF-8 인코딩**: 파일 생성 시 항상 UTF-8 인코딩 사용
- **근본적 해결**: 문제 발견 시 임시방편이 아닌 근본적인 해결책 제시

### 📚 문서 기반 작업 원칙 (최우선)

#### 🔍 작업 전 필수 확인
1. `docs/README.md` → 전체 프로젝트 현황
2. `docs/domains/README.md` → 도메인 아키텍처
3. `docs/domains/{domain}.md` → 해당 도메인 가이드
4. `docs/specifications/{관련규격}.md` → 기술 규격
5. 실제 코드 확인 → 문서와 일치성 검증
6. 작업 진행 → 문서 기반 패턴 준수

#### ⚠️ 중요: MARUNI는 완성된 프로젝트
- Phase 2 MVP 100% 완료: 7개 도메인 TDD + DDD 완성
- 25+ REST API 완성: 실제 운영 가능한 수준
- 기존 패턴 준수: 새 기능 추가 시 일관성 유지
- 문서 우선: 추측 금지, 반드시 문서 확인

### 🚫 절대 금지사항
- 문서 무시: 문서 확인 없이 임의 추론하지 않음
- 패턴 파괴: 기존 TDD + DDD 구조 무시한 임의 구현
- 무단 결정: 아키텍처 변경을 사용자 확인 없이 진행
- 할루시네이션: 존재하지 않는 API/엔티티 만들어내지 않음

### ✅ 반드시 지켜야 할 원칙

#### 1. 문서 기반 작업
```
❌ "UserService를 추가하겠습니다"
✅ "docs/domains/member.md 확인 결과 MemberService가 이미 완성되어 있습니다"

❌ "JWT 설정을 새로 하겠습니다"
✅ "docs/domains/auth.md에 JWT Stateless 시스템이 완성되어 있어 기존 패턴 사용"
```

#### 2. 기존 패턴 준수
```
✅ BaseTimeEntity 상속 + 정적 팩토리 메서드 패턴
✅ @AutoApiResponse + Swagger 어노테이션
✅ TDD Red-Green-Blue 사이클
```

#### 3. TDD 사이클 준수
```
1. Red: 기존 스타일로 실패 테스트 작성
2. Green: 기존 패턴 준수한 최소 구현
3. Blue: 리팩토링 + 50% 이상 품질 향상
```

## 🔄 개발 워크플로우

### 기존 도메인 확장 시 (현재 주요 작업)
```
1. docs/domains/{해당도메인}.md 완전 숙지
2. docs/specifications/ 관련 규격 확인
3. 실제 코드 확인 → 문서 일치성 검증
4. TDD 사이클 적용
5. 해당 도메인 문서 업데이트
```

### 코드 생성 필수 체크리스트
```
DDD 계층:
- [ ] Entity: BaseTimeEntity 상속 + 정적 팩토리
- [ ] Repository: JpaRepository 상속
- [ ] Service: @Transactional + BaseException
- [ ] Controller: @AutoApiResponse + Swagger
- [ ] DTO: Bean Validation + from()

완성된 시스템 연동:
- [ ] JWT 인증: 기존 Security 구조 활용
- [ ] 예외 처리: BaseException 계층 준수
- [ ] API 응답: CommonApiResponse 래핑
- [ ] 테스트: @ExtendWith(MockitoExtension.class)
```

## 네이밍 컨벤션

```
패키지: 소문자 단수형 (member, auth)
Entity: {Domain}Entity
Service: {Domain}Service
Controller: {Domain}Controller
DTO: {Domain}{Action}RequestDto/ResponseDto
API 경로: /api/{domain}
```

## 문제 해결 가이드

### 자주 발생하는 문제들
- API 응답 미래핑 → `@AutoApiResponse` 확인
- 커스텀 예외 미처리 → `BaseException` 상속 확인
- Swagger 예시 미표시 → `@CustomExceptionDescription` 확인
- Docker DB 연결 실패 → `.env` 파일 확인
- JWT 인증 실패 → `Authorization: Bearer {token}` 헤더 확인

### 디버깅
```bash
curl http://localhost:8080/actuator/health  # 헬스 체크
docker-compose ps                           # 컨테이너 상태
docker-compose logs -f app                  # 로그 확인
```

## 성능 & 보안 원칙

### 보안
- 민감 정보는 환경 변수로 관리
- Bean Validation 입력 검증 필수
- JPA 쿼리로 SQL Injection 방지
- Stateless JWT로 CSRF 방어

### 성능
- `@Transactional(readOnly = true)` 조회용
- N+1 쿼리 방지 (fetch 전략)
- PostgreSQL 인덱스 활용

## 작업 완료 후 문서 업데이트

### 반드시 업데이트
- 새 도메인 추가 → Package Structure 섹션
- 새 환경변수 → Quick Start 섹션
- 새 개발 패턴 → 작업 가이드라인
- 새 문제 해결법 → 문제 해결 가이드

## 📋 프로젝트 현황 (2025-11-05)

### 🎉 Phase 2 MVP 100% 완성
- 7개 도메인 100% 완성 (TDD + DDD)
- 25+ REST API (JWT + Swagger)
- OpenAI GPT-4o 실제 연동
- 알림 타입 시스템 (5종) + 이력 관리
- PostgreSQL 단일 DB
- Stateless JWT (Access Token Only)

### 🏆 주요 성과
```
💬 AI 대화: OpenAI GPT-4o + 키워드 감정분석
📅 스케줄링: 매일 오전 9시 자동 발송 + 재시도
🚨 이상징후: 3종 알고리즘 + 50% 코드 품질 향상
👥 보호자: 7개 REST API + 알림 연동
🔐 JWT: Access Token Only, Stateless, 클라이언트 로그아웃
🔔 알림: 알림 타입 시스템 (5종) + 이력 관리 + 조회 API (4개)
```

### 📚 상세 구현 내용은 문서 참조
- `docs/domains/` - 각 도메인별 완전한 구현 가이드
- `docs/specifications/` - 기술 규격서 10개
- `docs/new/user-journey.md` - 사용자 시나리오

---

**⚠️ 중요: 모든 작업은 docs/ 폴더의 해당 문서를 반드시 확인하세요.**

**Updated**: 2025-11-05 | **Version**: 2.1.0 | **Status**: Phase 2 MVP Complete
