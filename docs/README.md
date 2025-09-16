# MARUNI 프로젝트 문서

**노인 돌봄을 위한 AI 기반 소통 서비스** - Claude Code 작업 가이드

## 🚀 빠른 시작 (Claude Code 작업자용)

### **1️⃣ 프로젝트 개요 파악**
- **서비스**: 매일 안부 문자 → AI 응답 분석 → 이상징후 감지 → 보호자 알림
- **아키텍처**: Spring Boot 3.5 + Java 21, DDD + TDD 완전 적용
- **상태**: Phase 2 MVP 100% 완성 (상용 서비스 준비 완료)

### **2️⃣ 기술 스택**
```
Backend: Spring Boot 3.5.x + Java 21
Database: PostgreSQL + Redis
AI: OpenAI GPT-4o (Spring AI)
Auth: JWT (Access/Refresh Token)
Testing: TDD Red-Green-Blue 사이클
Architecture: DDD (Domain-Driven Design)
```

### **3️⃣ 핵심 도메인 구조**
```
✅ Member (회원 관리) + Auth (JWT 인증)
✅ Conversation (AI 대화): OpenAI GPT-4o 연동 완성
✅ DailyCheck (스케줄링): 매일 안부 메시지 자동 발송
✅ Guardian (보호자): 관계 설정 및 알림 관리
✅ AlertRule (이상징후): 3종 알고리즘 자동 감지
✅ Notification (알림): Mock 서비스 (실제 구현 준비 완료)
```

## 📋 현재 완성 상태

### 🎉 **100% 완성된 시스템**
- ✅ **AI 대화 시스템**: OpenAI GPT-4o 연동, 감정 분석
- ✅ **스케줄링 시스템**: 매일 정시 안부 메시지 자동 발송
- ✅ **보호자 관리**: Guardian 등록 및 알림 설정
- ✅ **이상징후 감지**: 3종 알고리즘으로 위험 상황 자동 감지
- ✅ **알림 시스템**: Mock 기반 푸시 알림 (실제 구현 준비 완료)

### 🚀 **Phase 3 계획**
- 📊 고급 건강 분석 시스템 (ML 기반 패턴 분석)
- 📱 모바일 앱 연동 API (Flutter 최적화)
- 🎯 실시간 모니터링 대시보드 (관리자/보호자용)

## 📂 도메인별 구현 가이드

### 🏗️ **작업 순서 (Claude Code 가이드)**
```
1. 도메인 가이드 읽기 → 2. 실제 코드 확인 → 3. 테스트 작성 → 4. 구현 → 5. 문서 업데이트
```

### **핵심 도메인 ([domains/](./domains/))**
📋 **[도메인 종합 가이드](./domains/README.md)** - 아키텍처 개요, 도메인간 관계, 작업 순서

| 도메인 | 핵심 기능 | 상태 | 가이드 |
|--------|----------|------|--------|
| **[DailyCheck](./domains/dailycheck.md)** | 매일 정시 안부 메시지 자동 발송<br/>• Cron 스케줄링 • 중복 방지 • 자동 재시도 | ✅ 100% | TDD 완전 사이클 |
| **[Guardian](./domains/guardian.md)** | 보호자-회원 관계 관리<br/>• 관계 설정 • 알림 설정 • 권한 관리 | ✅ 100% | 7개 REST API |
| **[AlertRule](./domains/alertrule.md)** | 이상징후 자동 감지 시스템<br/>• 감정패턴 • 무응답 • 키워드 분석 | ✅ 100% | 3종 알고리즘 |
| **[Conversation](./domains/conversation.md)** | AI 대화 및 감정 분석<br/>• OpenAI GPT-4o • 키워드 감정분석 | ✅ 100% | Spring AI 연동 |
| **[Notification](./domains/notification.md)** | 푸시 알림 발송 시스템<br/>• Mock 구현 • 인터페이스 기반 확장 | ✅ 100% | 실제 구현 준비 |

## 🎯 확장 계획

### **다음 단계 개발** ([roadmap/](./roadmap/))
📋 **[로드맵 종합 가이드](./roadmap/README.md)** - 전체 발전 계획, Phase별 현황, 확장 전략

- **[Phase 3 계획](./roadmap/phase3.md)** - 고도화 & 모바일 연동 (8주 계획)
  - **Week 1-4**: 고급 건강 분석 (ML 패턴 분석, 리포팅 시스템)
  - **Week 5-8**: 모바일 연동 (Flutter API, 실시간 시스템)

## 🛠️ Claude Code 작업 가이드

### **새 도메인 추가 워크플로우**
```
1. 요구사항 분석 및 DDD 설계
2. TDD Red-Green-Blue 사이클 적용
3. Entity → Repository → Service → Controller 순서
4. 테스트 커버리지 90% 이상 확보
5. domains/ 폴더에 가이드 문서 추가
```

### **기존 도메인 확장 워크플로우**
```
1. 해당 도메인 가이드 숙지
2. 실제 코드와 테스트 확인
3. 확장 방향 섹션 참고
4. 호환성 유지하며 기능 추가
5. 가이드 문서 즉시 업데이트
```

### **코딩 표준**
- **DDD 구조**: Domain/Application/Infrastructure/Presentation 계층 분리
- **TDD 방법론**: Red(실패) → Green(성공) → Blue(리팩토링) 완전 사이클
- **Spring Boot**: @Transactional, Bean Validation, JWT 인증 적용
- **테스트**: Unit/Integration/Mock 테스트 조합으로 90% 커버리지

---

**🚀 MARUNI는 TDD + DDD로 구축된 완전한 노인 돌봄 플랫폼입니다.**