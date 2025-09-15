# MARUNI 프로젝트 문서

## 📚 문서 구조 가이드

MARUNI 프로젝트의 모든 문서가 체계적으로 정리되어 있습니다.

### 📂 폴더별 문서 구조

#### 🚀 [Phase 2: 스케줄링 & 알림 시스템](./phase2/)
**85% 완료** - 매일 안부 메시지 스케줄링 및 보호자 알림 시스템
- ✅ **완료**: Week 5 DailyCheck 도메인 TDD 완료
- ✅ **완료**: Week 6 Guardian 도메인 TDD 완전 사이클 완료 + REST API
- ⏳ **계획**: Week 7 AlertRule 도메인 (이상징후 감지)
- 🏗️ **구현**: 도메인 아키텍처, 알림/스케줄링 시스템 설계

#### 🤖 [Phase 1: AI 대화 시스템](./phase1/)
**100% 완료** - OpenAI 기반 AI 대화 및 감정 분석 시스템
- ✅ GPT-4o 연동 AI 응답 생성
- ✅ 키워드 기반 감정 분석
- ✅ DDD 아키텍처 완벽 적용

#### 🏗️ [Architecture](./architecture/)
아키텍처 설계 및 분석 문서
- 📊 보안 계층 분석 보고서
- 🏛️ DDD 구조 가이드라인

#### 📝 [General](./general/)
전체 프로젝트 계획 및 일반 문서
- 🗺️ 전체 개발 로드맵
- 🔧 백엔드 우선 개발 전략

### 🔗 주요 링크

#### 🎯 현재 작업 중
- **[Phase 2 현황](./phase2/README.md)** - 전체 진행 상황 및 다음 단계
- **[Week 7 AlertRule 계획](./phase2/planning/week7-alertrule.md)** - 다음 TDD 개발 단계

#### 📖 설계 문서
- **[도메인 아키텍처](./phase2/implementation/domain-architecture.md)** - DDD 기반 도메인 설계
- **[알림 시스템](./phase2/implementation/notification-system.md)** - Firebase FCM 기반 푸시 알림
- **[스케줄링 시스템](./phase2/implementation/scheduling-system.md)** - Spring Boot 스케줄링

#### ✅ 완료 기록
- **[Week 5 TDD 보고서](./phase2/completed/week5-tdd-report.md)** - DailyCheck 도메인 완벽한 TDD 사이클
- **[Week 6 TDD 보고서](./phase2/completed/week6-guardian-report.md)** - Guardian 도메인 TDD 완전 사이클 + REST API 구현
- **[환경 설정 완료](./phase2/completed/environment-setup.md)** - Phase 2 인프라 구축

### 📊 프로젝트 현황 요약

```yaml
전체 진행률: 85% 완료 🚀

Phase 1 (AI 대화): ✅ 100% 완료
- OpenAI GPT-4o 연동
- 감정 분석 시스템
- DDD 아키텍처 완성

Phase 2 (스케줄링 & 알림): ✅ 85% 완료
- DailyCheck 도메인: ✅ 100% 완료 (완벽한 TDD 사이클)
- Guardian 도메인: ✅ 100% 완료 (TDD 완전 사이클 + REST API)
- Notification 시스템: 🔄 20% 완료 (인터페이스)
- AlertRule 도메인: ⏳ 계획 중 (Week 7)

다음 목표: AlertRule 도메인 구현으로 Phase 2 MVP 완성
```

### 🔧 문서 이용 가이드

1. **개발자**: [Phase 2 README](./phase2/README.md)에서 현재 진행 상황 확인
2. **아키텍트**: [도메인 아키텍처](./phase2/implementation/domain-architecture.md)에서 설계 확인
3. **PM**: 각 Phase별 README에서 진행률 및 다음 단계 확인
4. **QA**: [완료 기록](./phase2/completed/) 폴더에서 테스트 커버리지 확인

### 📝 문서 업데이트 원칙

- **실시간 업데이트**: 작업 완료 시 즉시 문서 업데이트
- **구조화**: 관심사별로 문서 분리 유지
- **상호 링크**: README 파일을 통한 네비게이션 제공
- **버전 관리**: Git을 통한 문서 변경 이력 추적

**MARUNI 프로젝트는 체계적인 문서화를 통해 개발 효율성과 코드 품질을 동시에 추구합니다.** 🚀