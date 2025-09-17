# MARUNI 사용자 흐름 다이어그램

## 전체 사용자 플로우

```mermaid
graph TB
    %% 사용자 등록 및 인증
    Start([사용자 시작]) --> Register[회원가입]
    Register --> Login[로그인]
    Login --> JWT[JWT 토큰 발급]

    %% 보호자 설정
    JWT --> GuardianSetup[보호자 등록]
    GuardianSetup --> GuardianNotify[보호자 알림 설정]

    %% 일일 안부 시스템
    GuardianNotify --> DailySchedule[매일 오전 9시<br/>안부 메시지 자동 발송]
    DailySchedule --> UserResponse{사용자 응답}

    %% 응답 있음 - AI 대화 플로우
    UserResponse -->|응답 있음| ConversationStart[AI 대화 시작]
    ConversationStart --> AIAnalysis[OpenAI GPT-4o<br/>감정 분석]
    AIAnalysis --> EmotionType{감정 분석 결과}

    EmotionType -->|POSITIVE| NormalFlow[정상 상태 기록]
    EmotionType -->|NEUTRAL| NormalFlow
    EmotionType -->|NEGATIVE| AlertCheck[이상징후 감지]

    %% 응답 없음 - 무응답 플로우
    UserResponse -->|응답 없음| NoResponseAlert[무응답 알고리즘 실행]
    NoResponseAlert --> AlertCheck

    %% 이상징후 감지 시스템
    AlertCheck --> AlertAnalysis[3종 알고리즘 분석<br/>1. 감정패턴 분석<br/>2. 무응답 감지<br/>3. 키워드 분석]
    AlertAnalysis --> RiskLevel{위험도 평가}

    %% 위험도별 처리
    RiskLevel -->|LOW| NormalFlow
    RiskLevel -->|MEDIUM| GuardianAlert[보호자 일반 알림]
    RiskLevel -->|HIGH| EmergencyAlert[보호자 긴급 알림]

    %% 알림 발송 시스템
    GuardianAlert --> NotificationSend[푸시 알림 발송]
    EmergencyAlert --> NotificationSend
    NotificationSend --> GuardianResponse{보호자 응답}

    %% 보호자 대응
    GuardianResponse -->|확인| MonitoringContinue[모니터링 계속]
    GuardianResponse -->|추가 조치 필요| FollowUp[후속 조치]

    %% 데이터 축적 및 분석
    NormalFlow --> DataStorage[대화 데이터 저장]
    MonitoringContinue --> DataStorage
    FollowUp --> DataStorage
    DataStorage --> PatternAnalysis[장기 패턴 분석]

    %% 다음 날 순환
    PatternAnalysis --> NextDay[다음 날]
    NextDay --> DailySchedule

    %% 스타일링
    classDef userAction fill:#e1f5fe
    classDef aiProcess fill:#f3e5f5
    classDef alertProcess fill:#ffebee
    classDef guardianProcess fill:#e8f5e8
    classDef systemProcess fill:#fff3e0

    class Register,Login,UserResponse,GuardianResponse userAction
    class ConversationStart,AIAnalysis,EmotionType,AlertAnalysis aiProcess
    class AlertCheck,NoResponseAlert,RiskLevel,GuardianAlert,EmergencyAlert alertProcess
    class GuardianSetup,GuardianNotify,NotificationSend,MonitoringContinue,FollowUp guardianProcess
    class JWT,DailySchedule,NormalFlow,DataStorage,PatternAnalysis,NextDay systemProcess
```

## 도메인별 상세 플로우

### 1. 인증 및 회원 관리 플로우
```mermaid
sequenceDiagram
    participant U as 사용자
    participant M as Member Domain
    participant A as Auth Domain
    participant R as Redis

    U->>M: 회원가입 요청
    M->>M: 회원 정보 저장
    M->>U: 가입 완료

    U->>A: 로그인 요청
    A->>M: 사용자 검증
    M->>A: 검증 완료
    A->>R: Refresh Token 저장
    A->>U: Access Token + Refresh Token 발급
```

### 2. 일일 안부 및 AI 대화 플로우
```mermaid
sequenceDiagram
    participant S as Scheduler
    participant D as DailyCheck Domain
    participant N as Notification Domain
    participant U as 사용자
    participant C as Conversation Domain
    participant AI as OpenAI GPT-4o
    participant AR as AlertRule Domain

    Note over S: 매일 오전 9시
    S->>D: 안부 메시지 발송 트리거
    D->>N: 안부 메시지 발송
    N->>U: 푸시 알림

    U->>C: 응답 메시지 전송
    C->>AI: 감정 분석 요청
    AI->>C: 감정 결과 (POSITIVE/NEUTRAL/NEGATIVE)
    C->>AR: 이상징후 분석 요청

    alt 이상징후 감지됨
        AR->>AR: 3종 알고리즘 실행
        AR->>N: 보호자 알림 요청
        N->>Guardian: 긴급 알림 발송
    else 정상 상태
        AR->>C: 정상 상태 기록
    end
```

### 3. 이상징후 감지 및 보호자 알림 플로우
```mermaid
graph LR
    subgraph "이상징후 감지 알고리즘"
        A1[감정패턴 분석<br/>연속 NEGATIVE 감지]
        A2[무응답 분석<br/>일정 시간 미응답]
        A3[키워드 분석<br/>위험 키워드 감지]
    end

    A1 --> Risk[위험도 계산]
    A2 --> Risk
    A3 --> Risk

    Risk --> Level{위험도 레벨}
    Level -->|LOW| Record[기록만 저장]
    Level -->|MEDIUM| Normal[일반 알림]
    Level -->|HIGH| Emergency[긴급 알림]

    Normal --> Guardian[보호자 알림 발송]
    Emergency --> Guardian
    Guardian --> Response{보호자 응답}
    Response -->|확인| Monitor[계속 모니터링]
    Response -->|조치 필요| Action[후속 조치]
```

### 4. 시스템 아키텍처 플로우
```mermaid
graph TB
    subgraph "Presentation Layer"
        C1[Member Controller]
        C2[Conversation Controller]
        C3[Guardian Controller]
        C4[AlertRule Controller]
    end

    subgraph "Application Layer"
        S1[Member Service]
        S2[Conversation Service]
        S3[Guardian Service]
        S4[AlertRule Service]
        S5[Daily Check Service]
    end

    subgraph "Domain Layer"
        E1[Member Entity]
        E2[Conversation Entity]
        E3[Guardian Entity]
        E4[AlertRule Entity]
        E5[DailyCheck Entity]
    end

    subgraph "Infrastructure Layer"
        DB[(PostgreSQL)]
        Redis[(Redis)]
        AI[OpenAI GPT-4o]
        Push[Push Notification]
    end

    C1 --> S1
    C2 --> S2
    C3 --> S3
    C4 --> S4

    S1 --> E1
    S2 --> E2
    S3 --> E3
    S4 --> E4
    S5 --> E5

    S1 --> DB
    S2 --> DB
    S2 --> AI
    S3 --> DB
    S4 --> DB
    S5 --> DB
    S1 --> Redis
    S4 --> Push
```

## 핵심 비즈니스 프로세스

### 매일 안부 확인 프로세스
1. **자동 스케줄링**: 매일 오전 9시 Cron 작업 실행
2. **메시지 발송**: 등록된 모든 사용자에게 안부 메시지 발송
3. **응답 대기**: 사용자 응답을 24시간 동안 대기
4. **AI 분석**: 응답이 오면 OpenAI GPT-4o로 감정 분석
5. **이상징후 감지**: 3종 알고리즘으로 위험 요소 분석
6. **보호자 알림**: 필요시 보호자에게 즉시 알림 발송

### 사용자 상태 모니터링
- **실시간 감지**: 대화 중 위험 키워드 즉시 감지
- **패턴 분석**: 장기간 감정 변화 추이 모니터링
- **무응답 추적**: 연속 무응답 시 자동 알림 증가
- **보호자 연동**: 다단계 알림으로 신속한 대응 지원

이 다이어그램들은 MARUNI 프로젝트의 완성된 시스템 플로우를 보여줍니다. Phase 3 확장 계획 시 참고하실 수 있습니다.