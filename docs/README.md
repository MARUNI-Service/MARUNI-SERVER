# MARUNI 프로젝트 문서

**노인 돌봄을 위한 AI 기반 소통 서비스**

## 🚀 빠른 시작

### 프로젝트 개요
- **서비스**: 매일 안부 메시지 → AI 응답 분석 → 이상징후 감지 → 보호자 알림
- **상태**: Phase 2 MVP 100% 완성 (상용 서비스 준비 완료)

### 기술 스택
```
Backend: Spring Boot 3.5.x + Java 21
Database: PostgreSQL (단일 DB)
AI: OpenAI GPT-4o (Spring AI)
Auth: JWT Stateless (Access Token Only, 1시간 유효)
Testing: TDD Red-Green-Blue 사이클
Architecture: DDD (Domain-Driven Design)
```

### 핵심 도메인
```
🔐 Foundation: Member + Auth (JWT 인증)
💬 Core Service: Conversation + DailyCheck + Guardian
🚨 Integration: AlertRule + Notification
```

## 📋 완성 현황

### ✅ 완성된 시스템
- **JWT 인증**: Access Token 단일 구조, Stateless, 클라이언트 로그아웃
- **AI 대화**: OpenAI GPT-4o 연동, 키워드 감정 분석
- **스케줄링**: 매일 오전 9시 안부 메시지 자동 발송 + 재시도
- **보호자 관리**: 관계 설정, 알림 설정, 7개 REST API
- **이상징후 감지**: 3종 알고리즘 (감정/무응답/키워드)
- **알림 시스템**: 알림 타입 시스템 (5종) + 이력 관리 + 조회 API

### 🚀 Phase 3 계획
- 고급 건강 분석 시스템 (ML 기반)
- 모바일 앱 연동 API (Flutter)
- 실시간 모니터링 대시보드

## 📂 문서 구조

### 도메인별 가이드 ([domains/](./domains/))
| 문서 | 내용 | 상태 |
|------|------|------|
| **[README](./domains/README.md)** | 도메인 아키텍처 개요 | ✅ |
| **[member](./domains/member.md)** | 회원 관리 시스템 | ✅ |
| **[auth](./domains/auth.md)** | JWT Stateless 인증 | ✅ |
| **[conversation](./domains/conversation.md)** | AI 대화 + 감정 분석 | ✅ |
| **[dailycheck](./domains/dailycheck.md)** | 스케줄링 시스템 | ✅ |
| **[guardian](./domains/guardian.md)** | 보호자 관리 | ✅ |
| **[alertrule](./domains/alertrule.md)** | 이상징후 감지 | ✅ |
| **[notification](./domains/notification.md)** | 알림 시스템 | ✅ |

### 기술 규격서 ([specifications/](./specifications/))
| 문서 | 용도 | 중요도 |
|------|------|--------|
| **[README](./specifications/README.md)** | 규격서 통합 가이드 | ⭐⭐⭐ |
| **[coding-standards](./specifications/coding-standards.md)** | Java 코딩 컨벤션 | ⭐⭐⭐ |
| **[quick-reference](./specifications/quick-reference.md)** | 빠른 참조 템플릿 | ⭐⭐⭐ |
| **[architecture-guide](./specifications/architecture-guide.md)** | DDD 아키텍처 | ⭐⭐ |
| **[api-design-guide](./specifications/api-design-guide.md)** | REST API 설계 | ⭐⭐ |
| **[database-design-guide](./specifications/database-design-guide.md)** | Entity 설계 패턴 | ⭐ |
| **[testing-guide](./specifications/testing-guide.md)** | TDD 테스트 작성 | ⭐ |
| **[security-guide](./specifications/security-guide.md)** | JWT 인증 구현 | 🔒 |
| **[performance-guide](./specifications/performance-guide.md)** | JPA 성능 최적화 | ⚡ |
| **[tech-stack](./specifications/tech-stack.md)** | 기술 스택 전체 | 🛠️ |

### 로드맵 ([roadmap/](./roadmap/))
| 문서 | 내용 |
|------|------|
| **[README](./roadmap/README.md)** | 전체 로드맵 현황 |
| **[phase3](./roadmap/phase3.md)** | 고도화 & 모바일 연동 (8주) |

### 사용자 여정 ([new/](./new/))
| 문서 | 내용 |
|------|------|
| **[user-journey](./new/user-journey.md)** | 사용자 시나리오 및 API 플로우 |

## 🛠️ 작업 가이드

### 새 도메인 추가
1. 도메인 계층 결정 (Foundation/Core/Integration)
2. TDD Red-Green-Blue 사이클 적용
3. Entity → Repository → Service → Controller 순서
4. domains/ 폴더에 가이드 문서 작성

### 기존 도메인 확장
1. 해당 도메인 가이드 숙지
2. 실제 코드 확인
3. 호환성 유지하며 기능 추가
4. 가이드 문서 즉시 업데이트

## 📈 시스템 아키텍처

```
클라이언트 (JWT Access Token)
    ↓
Spring Security Filter Chain
    ↓
7개 핵심 도메인 (DDD)
    ↓
PostgreSQL + OpenAI GPT-4o + Firebase FCM
```

### 인증 시스템
- **Stateless JWT**: 서버 세션 저장소 없음
- **Access Token Only**: 1시간 유효
- **클라이언트 로그아웃**: 토큰 삭제는 클라이언트 처리
- **DDD 의존성 역전**: Domain 인터페이스 → Global 구현체

---

**MARUNI는 TDD + DDD로 구축된 완전한 노인 돌봄 플랫폼입니다.** 🚀

**Updated**: 2025-11-05 | **Version**: 2.1.0
