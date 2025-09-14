# MARUNI 백엔드 우선 개발 계획 (Backend-First Strategy)

## 📋 문서 개요

**MARUNI 백엔드 우선 개발 전략**은 모든 비즈니스 로직과 API를 완전히 구축한 후,
완성된 백엔드 시스템을 기반으로 모바일 앱을 개발하는 체계적 접근법입니다.

### 🎯 백엔드 우선 전략의 장점
- **안정적인 API 설계**: 프론트엔드 요구사항 고려한 완전한 API
- **독립적인 테스트**: 백엔드 로직을 완전히 검증 후 앱 개발
- **확장 가능성**: 웹, 앱, API 연동 등 다양한 클라이언트 지원
- **비즈니스 로직 집중**: UI에 방해받지 않는 핵심 기능 완성

---

## 🏗️ 현재 상태 및 목표 시스템

### ✅ Phase 1 완료 (Foundation)
```yaml
완성된 시스템:
  - 🔐 JWT 인증/보안 시스템
  - 👥 Member 도메인 (회원가입, 로그인)
  - 🧠 AI 대화 시스템 (OpenAI GPT-4o 연동)
  - 💬 Conversation 도메인 (AI 채팅 완성)
  - 🗄️ PostgreSQL + Redis 인프라
  - 📚 Swagger API 문서화

기술 스택:
  - Spring Boot 3.5.x + Java 21
  - Spring Security 6 (JWT)
  - Spring Data JPA + PostgreSQL
  - Redis (캐싱)
  - OpenAI GPT-4o API
```

### 🎯 목표 완성 시스템 (백엔드)
```yaml
Phase 2-4 목표:
  - 📅 정기 안부 메시지 스케줄링 시스템
  - 👨‍⚕️ 건강 상태 모니터링 및 분석
  - 🚨 이상징후 감지 및 보호자 알림
  - 📊 대화 분석 및 리포팅 시스템
  - 👨‍💼 관리자 대시보드 API
  - 📱 모바일 앱 전용 API 최적화

지원할 클라이언트:
  - Flutter 모바일 앱 (주력)
  - 관리자 웹 대시보드
  - 보호자 알림 시스템
  - 외부 시스템 연동 API
```

---

## 🗺️ 백엔드 우선 개발 로드맵 (12주 계획)

### Phase 2: 스케줄링 & 알림 시스템 (4주)

#### **Week 5-6: 정기 안부 메시지 스케줄링 도메인**
```yaml
목표: 매일 정시에 사용자별 맞춤 안부 메시지 생성 시스템

구현 내용:
🔴 TDD Red (Day 1-2):
  - DailyCheckService 테스트 작성
  - 스케줄링 로직 테스트
  - 사용자별 메시지 생성 테스트

🟢 TDD Green (Day 3-5):
  - @Scheduled 기반 배치 작업
  - 개인화된 안부 메시지 생성 알고리즘
  - 발송 실패 재시도 메커니즘

🔵 TDD Refactor (Day 6-7):
  - 성능 최적화 (대량 사용자 처리)
  - 스케줄링 설정 동적 관리
  - 모니터링 및 로깅 강화

Domain Design:
- DailyCheckEntity: 개인별 안부 일정 관리
- CheckMessageEntity: 생성된 안부 메시지 이력
- ScheduleConfigEntity: 스케줄링 설정 관리
```

#### **Week 7-8: 보호자 알림 시스템**
```yaml
목표: 이상징후 감지 시 다채널 보호자 알림 시스템

구현 내용:
🔴 TDD Red (Day 1-2):
  - Guardian 도메인 테스트
  - 알림 발송 로직 테스트
  - 에스컬레이션 규칙 테스트

🟢 TDD Green (Day 3-5):
  - Guardian 엔티티 및 관계 설정
  - 알림 채널별 발송 구현 (이메일, SMS 준비)
  - 긴급도별 알림 규칙 엔진

🔵 TDD Refactor (Day 6-7):
  - 알림 템플릿 시스템
  - 발송 이력 및 상태 추적
  - 스팸 방지 및 빈도 제한

Domain Design:
- GuardianEntity: 보호자 정보 및 연락처
- AlertRuleEntity: 알림 발송 조건 설정
- NotificationEntity: 알림 발송 이력
- AlertChannelEntity: 알림 채널별 설정
```

### Phase 3: 분석 & 모니터링 시스템 (4주)

#### **Week 9-10: 건강 상태 분석 도메인**
```yaml
목표: 대화 패턴 분석을 통한 건강 상태 모니터링

구현 내용:
🔴 TDD Red (Day 1-2):
  - HealthAnalysisService 테스트
  - 패턴 감지 알고리즘 테스트
  - 건강 지표 계산 테스트

🟢 TDD Green (Day 3-5):
  - 대화 감정 분석 고도화
  - 건강 상태 점수 계산 시스템
  - 이상 패턴 감지 알고리즘

🔵 TDD Refactor (Day 6-7):
  - ML 모델 연동 준비
  - 분석 결과 캐싱 최적화
  - 배치 분석 시스템

Domain Design:
- HealthMetricEntity: 건강 지표 데이터
- ConversationAnalysisEntity: 대화 분석 결과
- HealthTrendEntity: 건강 상태 변화 추이
- RiskAssessmentEntity: 위험도 평가 결과
```

#### **Week 11-12: 리포팅 & 대시보드 API**
```yaml
목표: 관리자 및 보호자를 위한 종합 리포팅 시스템

구현 내용:
🔴 TDD Red (Day 1-2):
  - ReportService 테스트
  - 통계 데이터 집계 테스트
  - 대시보드 API 테스트

🟢 TDD Green (Day 3-5):
  - 일/주/월 통계 리포트 생성
  - 실시간 대시보드 데이터 API
  - 사용자별 맞춤 리포트 생성

🔵 TDD Refactor (Day 6-7):
  - 대용량 데이터 처리 최적화
  - 리포트 캐싱 전략
  - API 응답 속도 최적화

Domain Design:
- ReportEntity: 생성된 리포트 메타데이터
- StatisticsEntity: 통계 데이터 스냅샷
- DashboardConfigEntity: 대시보드 설정
- MetricAggregationEntity: 지표 집계 결과
```

### Phase 4: 시스템 완성 & 최적화 (4주)

#### **Week 13-14: API 통합 및 최적화**
```yaml
목표: 모바일 앱 개발을 위한 완벽한 API 시스템

구현 내용:
🔴 TDD Red (Day 1-2):
  - 통합 API 테스트
  - 성능 테스트
  - 부하 테스트

🟢 TDD Green (Day 3-5):
  - 모바일 친화적 API 엔드포인트 추가
  - 페이징 및 필터링 고도화
  - API 버전 관리 시스템

🔵 TDD Refactor (Day 6-7):
  - 응답 시간 최적화
  - 데이터베이스 쿼리 최적화
  - 캐싱 전략 고도화

API 완성도:
- 모든 비즈니스 로직 API 완성
- Swagger 문서 완전 업데이트
- 모바일 앱 요구사항 100% 지원
```

#### **Week 15-16: 보안 강화 및 운영 준비**
```yaml
목표: 프로덕션 환경 배포 준비 완료

구현 내용:
🔴 TDD Red (Day 1-2):
  - 보안 테스트
  - 장애 복구 테스트
  - 모니터링 테스트

🟢 TDD Green (Day 3-5):
  - API Rate Limiting
  - 보안 헤더 및 CORS 설정
  - 헬스체크 및 모니터링 강화

🔵 TDD Refactor (Day 6-7):
  - 로깅 시스템 완성
  - 장애 대응 자동화
  - 백업 및 복구 시스템

운영 준비:
- Docker 컨테이너화 완성
- CI/CD 파이프라인 구축
- 모니터링 및 알람 시스템
- 데이터베이스 백업 전략
```

---

## 📊 Phase별 도메인 아키텍처

### Phase 2: 스케줄링 & 알림 도메인
```
domain/
├── scheduling/              # 스케줄링 도메인
│   ├── application/
│   │   ├── dto/
│   │   │   ├── DailyCheckRequestDto
│   │   │   ├── ScheduleConfigDto
│   │   │   └── CheckMessageDto
│   │   ├── service/
│   │   │   ├── DailyCheckService        # 정기 안부 메시지
│   │   │   ├── SchedulingService        # 스케줄 관리
│   │   │   └── MessageGenerationService # 메시지 생성
│   │   └── mapper/
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── DailyCheckEntity
│   │   │   ├── CheckMessageEntity
│   │   │   └── ScheduleConfigEntity
│   │   ├── service/
│   │   │   ├── PersonalizedMessageService
│   │   │   └── ScheduleValidationService
│   │   └── repository/
│   ├── infrastructure/
│   │   ├── scheduler/
│   │   │   └── SpringSchedulerConfig
│   │   └── repository/
│   └── presentation/
│       └── controller/
│           ├── SchedulingController
│           └── DailyCheckController

├── notification/            # 알림 도메인
│   ├── application/
│   │   ├── dto/
│   │   │   ├── GuardianDto
│   │   │   ├── AlertRuleDto
│   │   │   └── NotificationDto
│   │   ├── service/
│   │   │   ├── GuardianService          # 보호자 관리
│   │   │   ├── AlertService             # 알림 발송
│   │   │   └── NotificationService      # 알림 이력
│   │   └── mapper/
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── GuardianEntity
│   │   │   ├── AlertRuleEntity
│   │   │   ├── NotificationEntity
│   │   │   └── AlertChannelEntity
│   │   ├── service/
│   │   │   ├── AlertRuleEngine
│   │   │   └── NotificationChannelService
│   │   └── repository/
│   ├── infrastructure/
│   │   ├── channel/
│   │   │   ├── EmailNotificationClient
│   │   │   ├── SmsNotificationClient
│   │   │   └── PushNotificationClient
│   │   └── repository/
│   └── presentation/
│       └── controller/
│           ├── GuardianController
│           └── NotificationController
```

### Phase 3: 분석 & 모니터링 도메인
```
domain/
├── health/                  # 건강 분석 도메인
│   ├── application/
│   │   ├── service/
│   │   │   ├── HealthAnalysisService    # 건강 상태 분석
│   │   │   ├── PatternDetectionService  # 패턴 감지
│   │   │   └── RiskAssessmentService    # 위험도 평가
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── HealthMetricEntity
│   │   │   ├── ConversationAnalysisEntity
│   │   │   └── RiskAssessmentEntity
│   │   └── service/
│   │       ├── HealthScoreCalculator
│   │       └── AnomalyDetectionService
│   └── infrastructure/
│       ├── ml/
│       │   └── HealthPredictionModel
│       └── repository/

├── analytics/              # 분석 리포팅 도메인
│   ├── application/
│   │   ├── service/
│   │   │   ├── ReportService           # 리포트 생성
│   │   │   ├── StatisticsService       # 통계 계산
│   │   │   └── DashboardService        # 대시보드 데이터
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── ReportEntity
│   │   │   ├── StatisticsEntity
│   │   │   └── MetricAggregationEntity
│   │   └── service/
│   │       ├── DataAggregationService
│   │       └── TrendAnalysisService
│   └── infrastructure/
│       ├── report/
│       │   └── ReportGenerator
│       └── repository/
```

---

## 🛠️ 기술 스택 완성도

### 백엔드 Core Technologies
```yaml
✅ 이미 완성:
  - Spring Boot 3.5.x + Java 21
  - Spring Security 6 (JWT 인증)
  - Spring Data JPA + PostgreSQL
  - Redis (캐싱)
  - OpenAI GPT-4o API 연동
  - Swagger/OpenAPI 문서화

🆕 Phase 2-4에서 추가:
  - Spring Scheduler (배치 작업)
  - Spring Events (도메인 이벤트)
  - Spring Mail (이메일 알림)
  - Spring Cache (성능 최적화)
  - Micrometer (메트릭)
  - Actuator (모니터링)
```

### 외부 연동 서비스
```yaml
현재 연동: OpenAI GPT-4o

추가 연동 예정:
  - 이메일 서비스 (Gmail SMTP 또는 AWS SES)
  - SMS 서비스 (CoolSMS 또는 NHN Cloud SMS)
  - 푸시 알림 (Firebase Cloud Messaging)
  - 파일 스토리지 (AWS S3 또는 MinIO)
```

### 데이터베이스 설계 완성
```sql
-- Phase 2: 스케줄링 & 알림
CREATE TABLE daily_checks (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    scheduled_time TIME NOT NULL,
    scheduled_days INTEGER[] NOT NULL, -- 0=일요일, 6=토요일
    message_template TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE check_messages (
    id BIGSERIAL PRIMARY KEY,
    daily_check_id BIGINT NOT NULL REFERENCES daily_checks(id),
    member_id BIGINT NOT NULL REFERENCES members(id),
    content TEXT NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guardians (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id), -- 보호받는 사람
    name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50), -- 자녀, 배우자 등
    phone_number VARCHAR(20),
    email VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    notification_preferences JSONB, -- 알림 설정
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    condition_type VARCHAR(50) NOT NULL, -- EMOTION_NEGATIVE, NO_RESPONSE, EMERGENCY_KEYWORD
    condition_config JSONB NOT NULL, -- 조건 상세 설정
    severity_level VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    guardian_id BIGINT NOT NULL REFERENCES guardians(id),
    alert_rule_id BIGINT REFERENCES alert_rules(id),
    channel VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, FAILED
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Phase 3: 건강 분석 & 리포팅
CREATE TABLE health_metrics (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    metric_date DATE NOT NULL,
    conversation_count INTEGER DEFAULT 0,
    positive_emotion_ratio DECIMAL(5,2), -- 0.00 ~ 100.00
    negative_emotion_ratio DECIMAL(5,2),
    response_time_avg INTEGER, -- 평균 응답시간(분)
    health_score DECIMAL(5,2), -- 종합 건강 점수
    risk_level VARCHAR(20), -- LOW, MEDIUM, HIGH, CRITICAL
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE conversation_analysis (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id),
    overall_sentiment VARCHAR(20), -- POSITIVE, NEUTRAL, NEGATIVE
    emotional_intensity DECIMAL(3,2), -- 0.00 ~ 10.00
    concern_keywords TEXT[],
    health_indicators JSONB, -- 건강 관련 키워드 분석
    risk_flags TEXT[], -- 위험 신호
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    report_type VARCHAR(50) NOT NULL, -- DAILY, WEEKLY, MONTHLY
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    report_data JSONB NOT NULL, -- 리포트 내용
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🧪 TDD 전략 및 품질 기준

### Red-Green-Refactor 적용
```yaml
각 도메인별 TDD 사이클:
🔴 Red Phase (1-2일):
  - 실패하는 테스트 먼저 작성
  - 비즈니스 요구사항을 테스트로 명세
  - Edge Case 시나리오 포함

🟢 Green Phase (3-5일):
  - 테스트를 통과하는 최소 구현
  - 핵심 비즈니스 로직 완성
  - 도메인 엔티티 및 서비스 구현

🔵 Refactor Phase (6-7일):
  - 코드 품질 개선
  - 성능 최적화
  - 아키텍처 정리
```

### 테스트 커버리지 목표
```yaml
도메인별 테스트 커버리지:
- Domain Service: 95% 이상
- Application Service: 90% 이상
- Repository: 85% 이상
- Controller: 80% 이상

전체 프로젝트: 85% 이상 유지
```

### 테스트 종류별 비중
```
        /\
       /E2E\ (5%)
      /____\
     /      \
    /Integration\ (15%)
   /__________\
  /            \
 /Unit Tests    \ (80%)
/________________\
```

---

## 📈 각 Phase별 완료 기준

### Phase 2 완료 기준 (스케줄링 & 알림)
- [ ] 정기 안부 메시지 자동 발송 시스템 완성
- [ ] 보호자 등록 및 관리 API 완성
- [ ] 이상징후 감지 시 자동 알림 발송
- [ ] 알림 채널별 발송 이력 추적
- [ ] 스케줄링 관련 90% 테스트 커버리지
- [ ] API 문서화 완료 (Swagger)

### Phase 3 완료 기준 (분석 & 모니터링)
- [ ] 일일 건강 지표 자동 계산
- [ ] 대화 패턴 분석 시스템 완성
- [ ] 위험도 평가 알고리즘 구현
- [ ] 관리자용 대시보드 API 완성
- [ ] 보호자용 리포트 생성 기능
- [ ] 분석 관련 90% 테스트 커버리지

### Phase 4 완료 기준 (시스템 완성)
- [ ] 모든 API 응답시간 200ms 이내
- [ ] 동시 사용자 1000명 처리 가능
- [ ] 전체 시스템 85% 테스트 커버리지
- [ ] 프로덕션 배포 환경 완성
- [ ] 모니터링 및 알림 시스템 구축
- [ ] 데이터 백업 및 복구 시스템 완성

---

## 📱 Phase 5: 모바일 앱 개발 (백엔드 완료 후)

### 모바일 앱 개발 전략
```yaml
개발 플랫폼: Flutter (Android + iOS 동시)
개발 기간: 6-8주
주요 화면:
  - 로그인/회원가입
  - AI 채팅 화면 (메인)
  - 건강 상태 대시보드
  - 설정 및 프로필
  - 보호자 연결 관리

백엔드 연동:
  - 완성된 REST API 100% 활용
  - JWT 토큰 인증
  - 푸시 알림 (Firebase)
  - 실시간 채팅 (추후 WebSocket 추가)
```

### 앱 UI/UX 설계
```yaml
노인 친화적 설계 원칙:
  - 큰 폰트 (최소 18pt)
  - 큰 버튼 (최소 44pt)
  - 명확한 색상 대비
  - 간단한 네비게이션
  - 음성 입력 지원
  - 오프라인 메시지 저장

보호자용 기능:
  - 실시간 건강 상태 모니터링
  - 대화 내역 확인
  - 알림 설정 관리
  - 응급 상황 대응 가이드
```

---

## ⚡ 즉시 실행 액션 아이템

### 이번 주 시작 (Phase 2 Week 1)
```yaml
Day 1 (오늘):
  ✅ 개발 계획 문서 완성
  🎯 다음: DailyCheckService TDD Red 테스트 작성

Day 2-3:
  📝 스케줄링 도메인 엔티티 설계
  🔴 정기 메시지 발송 테스트 작성
  🔴 개인화된 메시지 생성 테스트 작성

Day 4-5:
  🟢 Spring @Scheduled 구현
  🟢 DailyCheckService 기본 구현
  🟢 메시지 생성 알고리즘 구현

주말:
  🔵 코드 리뷰 및 리팩토링
  📚 다음 주 Guardian 도메인 설계 준비
```

### Phase별 마일스톤
```yaml
Week 6 (1월): 정기 안부 메시지 완성
Week 8 (2월): 보호자 알림 시스템 완성
Week 12 (3월): 건강 분석 시스템 완성
Week 16 (4월): 백엔드 시스템 완전 완성
Week 24 (6월): Flutter 모바일 앱 완성 및 배포
```

---

## 🎯 성공 지표 및 KPI

### 기술적 지표
- **API 응답 시간**: 평균 200ms 이내, 95th percentile 500ms 이내
- **시스템 가용성**: 99.9% 이상
- **테스트 커버리지**: 전체 85% 이상, 도메인 로직 95% 이상
- **동시 사용자**: 1000명 처리 가능
- **데이터 처리**: 일 10만건 메시지 처리 가능

### 비즈니스 지표
- **사용자 만족도**: 앱 스토어 평점 4.5 이상
- **재사용률**: 월 90% 이상 활성 사용자
- **알림 정확도**: 이상징후 감지 정확도 90% 이상
- **응답률**: 안부 메시지 응답률 80% 이상

---

## 📚 참고 문서 및 연관성

### 기존 문서와의 연관성
- `phase1-ai-system-mvp.md`: Phase 1 완성 기반
- `develop-plan.md`: 전체 프로젝트 비전 연계
- `CLAUDE.md`: 아키텍처 일관성 유지

### 새로 생성될 문서
- `phase2-scheduling-domain.md`: 스케줄링 상세 설계
- `phase3-health-analytics.md`: 건강 분석 상세 설계
- `phase4-production-ready.md`: 운영 환경 준비
- `phase5-mobile-app-plan.md`: 모바일 앱 개발 계획

---

**문서 작성일**: 2025-09-14
**최종 수정일**: 2025-09-14
**작성자**: Claude Code
**버전**: v1.0 (Backend-First Development Plan)
**개발 방법론**: Test-Driven Development (TDD)
**아키텍처**: Domain-Driven Design (DDD)

---

**🚀 다음 단계: Phase 2 Week 1 - 정기 안부 메시지 스케줄링 도메인 TDD 개발 시작!**