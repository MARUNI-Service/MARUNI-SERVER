# MARUNI 로그인 페이지 UI 설계

**간편하고 안전한 로그인** - 노인분들을 위한 최적화된 인증 화면

## 🎯 페이지 목적 및 사용자 플로우

### **기능 개요**
- 전화번호 기반 간편 로그인
- 생년월일로 본인 확인
- 자동 로그인 기능 지원
- 비밀번호 찾기 및 회원가입 안내

### **사용자 여정**
```
📱 앱 실행 → 🔐 로그인 화면
    ↓
📞 전화번호 입력
    ↓
🎂 생년월일 입력
    ↓
✅ 로그인 버튼 터치
    ↓
🔄 서버 인증 중... (로딩)
    ↓
🏠 메인 대시보드로 이동
```

## 📱 페이지 레이아웃 설계

### **전체 화면 구성**
```
┌─────────────────────────────────────┐
│                                     │
│          🤖 MARUNI 로고               │ ← 상단 (200px)
│                                     │
├─────────────────────────────────────┤
│                                     │
│                                     │
│         로그인 폼 영역                 │ ← 중앙 (400px)
│                                     │
│                                     │
├─────────────────────────────────────┤
│                                     │
│        도움말 & 회원가입               │ ← 하단 (160px)
│                                     │
└─────────────────────────────────────┘
```

## 📋 구역별 상세 설계

### **1. 상단 브랜딩 영역 (200px)**
```tsx
// 디자인 명세
높이: 200px
배경: 연한 파란색 그라데이션 (#f0f9ff → #e0f2fe)
패딩: 40px 20px

// 내용 구성
🤖 MARUNI 로고 (80px × 80px)
   원형 아바타, 따뜻한 파란색 배경
   중앙 정렬

"마루니와 함께하는 안부"
폰트: 24px, 굵게, 검정색 (#1f2937)
중앙 정렬

"안전하고 편리한 로그인"
폰트: 16px, 보통, 회색 (#6b7280)
중앙 정렬
```

### **2. 로그인 폼 영역 (400px)**
```tsx
// 디자인 명세
높이: 400px
배경: 흰색
패딩: 30px
모서리: 둥근 모서리 16px (상단만)
그림자: 미묘한 상단 그림자

// 폼 컨테이너
최대 너비: 320px
중앙 정렬
```

#### **전화번호 입력 필드**
```tsx
// 레이블
📞 전화번호
폰트: 18px, 굵게, 검정색
여백: 하단 8px

// 입력 필드
<input type="tel">
  placeholder: "010-1234-5678"
  크기: 전체 너비 × 56px
  폰트: 20px, 보통
  패딩: 16px
  테두리: 2px solid #d1d5db
  모서리: 둥근 모서리 8px

// 포커스 상태
테두리: 2px solid #2563eb
배경: #f8fafc
그림자: 0 0 0 3px rgba(37, 99, 235, 0.1)

// 에러 상태
테두리: 2px solid #dc2626
배경: #fef2f2
```

#### **생년월일 입력 필드**
```tsx
// 레이블
🎂 생년월일
폰트: 18px, 굵게, 검정색
여백: 상단 20px, 하단 8px

// 입력 필드
<input type="date">
  placeholder: "1950-01-01"
  크기: 전체 너비 × 56px
  폰트: 20px, 보통
  패딩: 16px
  테두리: 2px solid #d1d5db
  모서리: 둥근 모서리 8px

// 포커스/에러 상태 (전화번호와 동일)
```

#### **자동 로그인 체크박스**
```tsx
// 체크박스 컨테이너
여백: 상단 20px
수평 정렬: 왼쪽 정렬

// 체크박스
☑️ 자동 로그인
크기: 24px × 24px
폰트: 16px, 보통, 회색 (#6b7280)
간격: 체크박스와 텍스트 사이 12px

// 체크된 상태
배경: #2563eb
체크마크: 흰색
```

#### **로그인 버튼**
```tsx
// 기본 상태
크기: 전체 너비 × 56px
배경: 주요 파란색 (#2563eb)
텍스트: "로그인", 20px, 굵게, 흰색
모서리: 둥근 모서리 8px
여백: 상단 30px

// 비활성 상태 (필드 미입력)
배경: 연한 회색 (#d1d5db)
텍스트: 회색 (#9ca3af)
터치 불가

// 활성 상태
배경: 진한 파란색 (#1d4ed8)
터치 시: 살짝 어두워짐 + 눌림 효과

// 로딩 상태
🔄 로그인 중...
스피너 애니메이션 + 텍스트
```

### **3. 하단 도움말 영역 (160px)**
```tsx
// 디자인 명세
높이: 160px
배경: 연한 회색 (#f9fafb)
패딩: 20px
중앙 정렬

// 도움말 링크들
❓ 로그인에 어려움이 있으신가요?
폰트: 16px, 보통, 회색 (#6b7280)
터치 시: 도움말 페이지로 이동

📞 고객센터: 1588-1234
폰트: 18px, 굵게, 파란색 (#2563eb)
터치 시: 전화 걸기

👥 처음 사용하시나요? 회원가입
폰트: 16px, 보통, 회색 (#6b7280)
터치 시: 회원가입 페이지로 이동

// 링크 간격: 20px
```

## 🔄 상태별 화면 변화

### **1. 초기 상태**
```
- 모든 입력 필드 비어있음
- 로그인 버튼 비활성 (회색)
- 자동 로그인 체크박스 해제
```

### **2. 입력 진행 중**
```
- 입력 필드 포커스 시 파란 테두리
- 유효성 검사 실시간 피드백
- 모든 필드 입력 시 로그인 버튼 활성화
```

### **3. 유효성 검사 에러**
```tsx
// 전화번호 형식 오류
"올바른 전화번호를 입력해 주세요"
- 입력 필드 빨간 테두리
- 에러 메시지 빨간색 표시

// 생년월일 형식 오류
"올바른 생년월일을 선택해 주세요"
- 날짜 선택기 빨간 테두리
- 에러 메시지 빨간색 표시
```

### **4. 로그인 중 (로딩)**
```tsx
┌─────────────────────────────────────┐
│                                     │
│        🔄 로그인 처리 중...           │
│                                     │
│      잠시만 기다려 주세요             │
│                                     │
│     [진행률 스피너 애니메이션]        │
│                                     │
│      * 입력 필드 모두 비활성화 *      │
└─────────────────────────────────────┘
```

### **5. 로그인 실패**
```tsx
// 인증 실패 메시지
🚫 "전화번호 또는 생년월일을 확인해 주세요"
- 빨간색 경고 박스로 상단에 표시
- 3초 후 자동 사라짐
- 입력 필드 초기화 또는 유지 (사용자 선택)

// 네트워크 오류
🌐 "인터넷 연결을 확인해 주세요"
- 재시도 버튼 제공
```

### **6. 로그인 성공**
```tsx
✅ "환영합니다!"
- 성공 메시지 짧게 표시
- 메인 대시보드로 자동 이동
- 부드러운 페이드 전환 효과
```

## 🎨 색상 및 스타일 가이드

### **색상 시스템**
```css
/* 주요 브랜드 색상 */
--primary-blue: #2563eb;
--primary-blue-dark: #1d4ed8;
--primary-blue-light: #3b82f6;

/* 배경 색상 */
--bg-gradient-start: #f0f9ff;
--bg-gradient-end: #e0f2fe;
--bg-form: #ffffff;
--bg-help: #f9fafb;

/* 입력 필드 */
--input-border: #d1d5db;
--input-border-focus: #2563eb;
--input-border-error: #dc2626;
--input-bg-focus: #f8fafc;
--input-bg-error: #fef2f2;

/* 텍스트 색상 */
--text-primary: #1f2937;
--text-secondary: #6b7280;
--text-disabled: #9ca3af;
--text-error: #dc2626;
--text-success: #059669;
```

### **애니메이션 효과**
```css
/* 입력 필드 포커스 애니메이션 */
.input-focus {
  transition: all 0.3s ease;
  transform-origin: center;
}

.input-focus:focus {
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* 버튼 터치 애니메이션 */
.login-button {
  transition: all 0.2s ease;
}

.login-button:active {
  transform: scale(0.98);
}

/* 로딩 스피너 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.loading-spinner {
  animation: spin 1s linear infinite;
}

/* 에러 메시지 등장 */
@keyframes slide-down {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.error-message {
  animation: slide-down 0.3s ease;
}
```

## 🧪 접근성 및 사용성

### **노인 친화적 고려사항**
```tsx
// 큰 터치 영역
.touch-target {
  min-height: 56px;
  min-width: 56px;
}

// 명확한 포커스 표시
.form-input:focus {
  outline: 3px solid #2563eb;
  outline-offset: 2px;
}

// 대비 높은 색상
.high-contrast-text {
  color: #1f2937;
  font-weight: 500;
}

// 읽기 쉬운 폰트 크기
.readable-text {
  font-size: 18px;
  line-height: 1.6;
}
```

### **입력 도움 기능**
```tsx
// 자동 완성 및 제안
<input
  type="tel"
  autoComplete="tel"
  inputMode="numeric"
  pattern="[0-9]{3}-[0-9]{4}-[0-9]{4}"
/>

// 음성 라벨링
aria-label="전화번호 입력, 하이픈 포함하여 입력해 주세요"
aria-describedby="phone-help-text"

// 에러 메시지 연결
aria-invalid="true"
aria-errormessage="phone-error"
```

### **에러 처리 및 피드백**
```tsx
// 실시간 유효성 검사
const validatePhone = (phone) => {
  const phoneRegex = /^010-\d{4}-\d{4}$/;
  return phoneRegex.test(phone);
};

// 사용자 친화적 에러 메시지
const errorMessages = {
  phoneEmpty: "전화번호를 입력해 주세요",
  phoneInvalid: "010-1234-5678 형식으로 입력해 주세요",
  birthEmpty: "생년월일을 선택해 주세요",
  birthInvalid: "올바른 생년월일을 선택해 주세요",
  loginFailed: "전화번호 또는 생년월일을 확인해 주세요",
  networkError: "인터넷 연결을 확인해 주세요"
};
```

## 📱 반응형 고려사항

### **다양한 화면 크기**
```css
/* 작은 화면 (320px~) */
.small-screen {
  .logo-area { height: 160px; }
  .form-area { padding: 20px; }
  .input-field { font-size: 18px; }
}

/* 큰 화면 (480px~) */
.large-screen {
  .container {
    max-width: 480px;
    margin: 0 auto;
  }
  .form-container {
    max-width: 360px;
  }
}

/* 가로 모드 */
@media (orientation: landscape) {
  .logo-area { height: 120px; }
  .form-area { height: auto; }
}
```

## 🔐 보안 고려사항

### **입력 데이터 보호**
```tsx
// 민감 데이터 마스킹
const maskBirthDate = (date) => {
  return date.replace(/(\d{4})-(\d{2})-(\d{2})/, '****-**-**');
};

// 자동 완성 제한
<input
  autoComplete="off"
  data-lpignore="true"  // LastPass 무시
  data-form-type="other"
/>

// 세션 관리
const clearSensitiveData = () => {
  // 로그인 실패 시 입력값 초기화
  // 페이지 이탈 시 메모리 정리
};
```

## 🚀 구현 우선순위

### **Phase 1: 기본 폼**
1. 입력 필드 및 레이아웃
2. 기본 유효성 검사
3. 로그인 API 연동

### **Phase 2: 사용성 개선**
1. 실시간 유효성 검사
2. 애니메이션 효과
3. 에러 처리 강화

### **Phase 3: 접근성 최적화**
1. 스크린 리더 지원
2. 키보드 네비게이션
3. 고대비 모드 지원

---

**🔐 이 로그인 페이지는 노인분들이 쉽고 안전하게 마루니 서비스에 접근할 수 있도록 설계되었습니다!**