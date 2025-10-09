# 기술 규격서

**MARUNI 프로젝트 기술 문서 통합 가이드**

## 📚 문서 구조 (7개 문서)

### ⭐⭐⭐ 일상 참조 (매일 확인)
| 문서 | 용도 |
|------|------|
| **[coding-standards](./coding-standards.md)** | 코딩 컨벤션 + 전체 템플릿 (Entity, Service, Controller, DTO, Test) |

### ⭐⭐ 기능 개발 시
| 문서 | 용도 |
|------|------|
| **[api-design-guide](./api-design-guide.md)** | REST API 설계, Swagger 문서화, Controller 패턴 |

### ⭐ 특정 작업 시
| 문서 | 용도 |
|------|------|
| **[database-design-guide](./database-design-guide.md)** | JPA Entity 설계, 인덱스, 복합키, JPA 설정 |
| **[testing-guide](./testing-guide.md)** | TDD Red-Green-Blue 사이클, 테스트 패턴 |

### 🔒 시스템 설정
| 문서 | 용도 |
|------|------|
| **[security-guide](./security-guide.md)** | JWT Stateless 인증 구현 |
| **[performance-guide](./performance-guide.md)** | JPA 성능 최적화 (N+1, 캐시) |
| **[tech-stack](./tech-stack.md)** | 전체 기술 스택, 환경 변수, Docker 설정 |

---

## 📖 DDD 아키텍처 (핵심 개념)

### 계층 구조
```
Presentation Layer (Controller)
    ↓
Application Layer (Service, DTO)
    ↓
Domain Layer (Entity, Repository 인터페이스)
    ↓
Infrastructure Layer (외부 연동: OpenAI, Firebase)
```

### 계층별 역할
- **Presentation**: REST API 엔드포인트, 요청/응답 검증
- **Application**: 비즈니스 로직, @Transactional 관리
- **Domain**: 핵심 비즈니스 규칙, Entity, Repository 인터페이스
- **Infrastructure**: 외부 시스템 연동 (OpenAI, Firebase, JWT)

### 의존 방향
- ✅ **Presentation → Application → Domain** (상위 → 하위)
- ✅ **Infrastructure → Domain** (인터페이스 구현)
- ❌ **Domain → Application** (금지)
- ❌ **Domain → Infrastructure** (금지)

---

## 🎯 작업 시나리오별 가이드

### 1. 새 도메인 추가
```
1. coding-standards.md → 전체 템플릿 복사
2. database-design-guide.md → Entity 설계 패턴 확인
3. api-design-guide.md → REST API 설계
4. testing-guide.md → TDD 사이클 적용
5. docs/domains/{domain}.md → 도메인 가이드 작성
```

### 2. 기존 기능 확장
```
1. docs/domains/{해당도메인}.md → 기존 구현 파악
2. coding-standards.md → 템플릿 참조
3. testing-guide.md → 테스트 추가
```

### 3. 성능 문제 해결
```
1. performance-guide.md → N+1 쿼리, Fetch 전략 확인
2. database-design-guide.md → 인덱스 최적화
```

### 4. 보안 설정 변경
```
1. security-guide.md → JWT 인증 시스템 이해
2. docs/domains/auth.md → 인증 도메인 구현 확인
```

---

## 📊 문서별 핵심 내용

### coding-standards.md (필수 ⭐⭐⭐)
- 패키지 구조 및 네이밍 규칙
- **전체 템플릿** (Entity, Repository, Service, Controller, DTO, Test)
- BaseException 예외 처리
- 자주 쓰는 쿼리 패턴
- 필수/금지 어노테이션

### api-design-guide.md
- RESTful API 설계 원칙 (URL, HTTP 메서드, 상태 코드)
- CommonApiResponse 표준 구조
- Swagger 문서화 방법
- JWT 인증 헤더 사용법
- Bean Validation

### database-design-guide.md
- BaseTimeEntity 상속 패턴
- 정적 팩토리 메서드
- 연관관계 매핑 (@ManyToOne, Enum)
- 인덱스 및 복합키 설계

### testing-guide.md
- TDD Red-Green-Blue 사이클
- Unit/Integration/Controller 테스트 패턴
- AssertJ 사용법
- Mock 사용 패턴 (given-when-then)

### security-guide.md
- JWT Stateless 인증 시스템
- Access Token Only (1시간 유효)
- 주요 컴포넌트 (JWTUtil, Filter, SecurityConfig)
- 로그인/API 요청/로그아웃 플로우

### performance-guide.md
- N+1 쿼리 문제 방지 (Fetch Join)
- @Transactional(readOnly = true) 사용
- Batch Size 설정
- DTO Projection 및 Bulk 연산

### tech-stack.md
- 전체 기술 스택 (Spring Boot 3.5.x, Java 21, PostgreSQL)
- 핵심 의존성 (Spring AI, JWT, Firebase)
- 환경 변수 (.env)
- Docker 설정 및 개발 명령어

---

## 🔄 문서 업데이트 원칙

### 업데이트 시점
```
✅ 새 도메인 추가: coding-standards.md 업데이트
✅ 새 환경변수 추가: tech-stack.md 업데이트
✅ 새 개발 패턴 발견: coding-standards.md 업데이트
✅ 새 문제 해결법: performance-guide.md, security-guide.md 업데이트
```

### 업데이트 금지
```
❌ 완성된 시스템 구조 임의 변경
❌ 기존 패턴 무시한 새로운 방식 추가
❌ 실제 코드와 일치하지 않는 예시
```

---

## 🎓 학습 순서 (신규 개발자)

### 1단계: 프로젝트 이해
```
1. docs/README.md → 전체 프로젝트 개요
2. tech-stack.md → 기술 스택 확인
3. DDD 아키텍처 (이 문서 위 섹션) → 계층 구조 이해
```

### 2단계: 개발 준비
```
1. coding-standards.md → 코딩 컨벤션 + 템플릿 숙지
2. docs/domains/README.md → 도메인 구조 파악
3. api-design-guide.md → API 패턴 학습
```

### 3단계: 실습
```
1. 간단한 CRUD API 구현 (coding-standards.md 템플릿 사용)
2. TDD 사이클 적용 (testing-guide.md)
3. 실제 도메인 가이드 참조 (docs/domains/{domain}.md)
```

---

**프로젝트 전체 개요: `docs/README.md` 참조**
