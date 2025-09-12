sequenceDiagram
participant User as 사용자
participant Web as Web Frontend
participant Auth as AuthController
participant UserService as UserService
participant SMS as SMSService
participant AI as AIService
participant Scheduler as TaskScheduler
participant DB as Database

    Note over User, DB: 1. 인증 관련 기능

    %% 1.1 기본 로그인
    User->>Web: 로그인 요청 (ID/PW)
    Web->>Auth: POST /api/auth/login
    Auth->>UserService: authenticate(username, password)
    UserService->>DB: 사용자 정보 조회
    DB-->>UserService: 사용자 데이터 반환
    UserService-->>Auth: 인증 결과
    Auth-->>Web: JWT 토큰 반환
    Web-->>User: 로그인 완료

    %% 1.2 소셜 로그인
    User->>Web: 소셜 로그인 클릭
    Web->>Auth: GET /api/auth/oauth/{provider}
    Auth->>외부OAuth: OAuth 인증 요청
    외부OAuth-->>Auth: 사용자 정보 반환
    Auth->>UserService: socialLogin(providerInfo)
    UserService->>DB: 소셜 계정 정보 저장/조회
    DB-->>UserService: 사용자 데이터
    UserService-->>Auth: 인증 결과
    Auth-->>Web: JWT 토큰 반환
    Web-->>User: 로그인 완료

    %% 1.3 기본 회원가입
    User->>Web: 회원가입 정보 입력
    Web->>Auth: POST /api/auth/register
    Auth->>UserService: registerUser(userDto)
    UserService->>DB: 사용자 정보 저장
    DB-->>UserService: 저장 완료
    UserService-->>Auth: 회원가입 완료
    Auth-->>Web: 성공 응답
    Web-->>User: 가입 완료 알림

    %% 1.4 Password 찾기
    User->>Web: 비밀번호 찾기 요청
    Web->>Auth: POST /api/auth/forgot-password
    Auth->>UserService: sendPasswordReset(email)
    UserService->>SMS: 인증번호 전송
    SMS-->>User: 인증번호 SMS
    User->>Web: 인증번호 입력
    Web->>Auth: POST /api/auth/verify-reset
    Auth->>UserService: verifyAndResetPassword()
    UserService->>DB: 비밀번호 업데이트
    DB-->>UserService: 업데이트 완료
    UserService-->>Auth: 재설정 완료
    Auth-->>Web: 성공 응답
    Web-->>User: 비밀번호 재설정 완료

    Note over User, DB: 2. 서비스 페이지 기능

    %% 2.1 서비스 소개 페이지
    User->>Web: 서비스 소개 페이지 접속
    Web->>User: 정적 페이지 렌더링

    %% 2.2 서비스 설정 페이지
    User->>Web: 설정 페이지 접속
    Web->>UserService: GET /api/user/settings
    UserService->>DB: 사용자 설정 조회
    DB-->>UserService: 설정 데이터
    UserService-->>Web: 설정 정보 반환
    Web-->>User: 설정 페이지 표시

    User->>Web: 설정 변경
    Web->>UserService: PUT /api/user/settings
    UserService->>DB: 설정 업데이트
    DB-->>UserService: 업데이트 완료
    UserService-->>Web: 성공 응답
    Web-->>User: 설정 변경 완료

    Note over User, DB: 3. 핵심 서비스 기능

    %% 2.1 매일 아침 9시 안부 문자
    Scheduler->>UserService: @Scheduled 매일 9시 실행
    UserService->>DB: 활성 사용자 목록 조회
    DB-->>UserService: 사용자 리스트
    loop 각 사용자별
        UserService->>SMS: 안부 문자 전송
        SMS-->>User: "안녕하세요! 오늘 하루도 화이팅!"
    end

    %% 2.2 대화 중 이상징후 감지
    User->>SMS: 문자 응답
    SMS->>AIService: 메시지 분석 요청
    AIService->>AI: 감정/이상징후 분석
    AI-->>AIService: 분석 결과
    alt 이상징후 감지됨
        AIService->>UserService: 알림 처리
        UserService->>DB: 이상징후 로그 저장
        UserService->>SMS: 보호자/관리자에게 알림
        SMS-->>보호자: 긴급 알림 문자
    else 정상
        AIService->>UserService: 정상 대화 처리
        UserService->>DB: 대화 로그 저장
    end