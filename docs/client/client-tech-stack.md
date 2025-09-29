# MARUNI 클라이언트 기술 스택

**MARUNI 노인 돌봄 서비스** 클라이언트 구현을 위한 기술 스택 가이드

## 🎯 프로젝트 배경

### 서비스 특성
- **주 사용자**: 노인분들 (단순하고 직관적한 UI 필요)
- **플랫폼**: 안드로이드 중심 (iOS 고려 불필요)
- **핵심 기능**: 매일 안부 메시지 + AI 대화 + 실시간 알림
- **백엔드**: Spring Boot + JWT + 25+ REST API 완성

### 개발 목표
- ✅ **빠른 MVP 구현**: 2-3주 내 핵심 기능 완성
- ✅ **홈 화면 앱 경험**: 네이티브 앱과 유사한 UX
- ✅ **점진적 확장**: PWA → 네이티브 앱 업그레이드 경로
- ✅ **노인 친화적**: 큰 버튼, 명확한 인터페이스

## 🛠️ 선택된 기술 스택

### **핵심 프레임워크**
```javascript
React 18 + TypeScript
- 컴포넌트 기반 개발
- 타입 안정성
- 풍부한 생태계
```

### **PWA 구현**
```javascript
Vite + vite-plugin-pwa
- 빠른 개발 서버
- PWA 자동 설정
- 최적화된 빌드
```

### **상태 관리**
```javascript
// 서버 상태
TanStack Query (React Query)
- API 캐싱 및 동기화
- 오프라인 지원
- 백그라운드 업데이트

// 클라이언트 상태
Zustand (가벼운 전역 상태)
- 인증 상태 관리
- UI 상태 관리
```

### **라우팅 & 네비게이션**
```javascript
React Router v6
- SPA 라우팅
- 중첩 라우팅
- 보호된 라우트 (JWT 인증)
```

### **HTTP 클라이언트**
```javascript
Axios
- 인터셉터 (JWT 자동 첨부)
- 요청/응답 변환
- 에러 처리
```

### **UI/UX 라이브러리**
```javascript
// 스타일링
Tailwind CSS
- 유틸리티 우선 CSS
- 반응형 디자인
- 빠른 프로토타이핑

// 컴포넌트 (선택적)
Headless UI 또는 Radix UI
- 접근성 최적화
- 커스터마이징 용이
- 노인 친화적 큰 터치 영역
```

### **PWA 필수 기능**
```javascript
// Service Worker
Workbox (vite-plugin-pwa 내장)
- 오프라인 캐싱
- 백그라운드 동기화
- 업데이트 알림

// 푸시 알림
Web Push API
- 실시간 알림 수신
- 백그라운드 알림
- 안부 메시지 알림
```

## 📱 PWA 핵심 특징

### **Android 최적화**
- **홈 화면 추가**: "앱 설치" 경험 제공
- **풀스크린 모드**: 브라우저 UI 숨김
- **스플래시 스크린**: 네이티브 앱과 동일한 로딩 경험
- **오프라인 지원**: 네트워크 없이도 기본 기능 사용 가능

### **성능 최적화**
- **코드 스플리팅**: 라우트별 지연 로딩
- **이미지 최적화**: WebP 포맷, 지연 로딩
- **번들 최적화**: Tree shaking, 압축

## 🚀 개발 환경 설정

### **프로젝트 초기화**
```bash
# Vite React PWA 프로젝트 생성
npm create vite@latest maruni-client -- --template react-ts
cd maruni-client

# 핵심 의존성 설치
npm install

# PWA 및 상태 관리
npm install -D vite-plugin-pwa
npm install @tanstack/react-query zustand

# 라우팅 및 HTTP
npm install react-router-dom axios

# UI 라이브러리
npm install tailwindcss @headlessui/react
npx tailwindcss init -p
```

### **PWA 설정 (vite.config.ts)**
```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg}']
      },
      manifest: {
        name: 'MARUNI - 마음이 닿는 안부',
        short_name: 'MARUNI',
        description: '노인 돌봄을 위한 AI 기반 소통 서비스',
        theme_color: '#ffffff',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'portrait',
        scope: '/',
        start_url: '/',
        icons: [
          {
            src: 'icons/icon-192x192.png',
            sizes: '192x192',
            type: 'image/png'
          },
          {
            src: 'icons/icon-512x512.png',
            sizes: '512x512',
            type: 'image/png'
          }
        ]
      }
    })
  ]
})
```

## 📂 프로젝트 구조

```
maruni-client/
├── public/
│   ├── icons/              # PWA 아이콘
│   └── manifest.json       # PWA 매니페스트
├── src/
│   ├── components/         # 재사용 컴포넌트
│   │   ├── common/        # 공통 컴포넌트
│   │   ├── layout/        # 레이아웃 컴포넌트
│   │   └── ui/            # UI 컴포넌트
│   ├── pages/             # 페이지 컴포넌트
│   │   ├── auth/          # 인증 관련
│   │   ├── conversation/  # 대화 기능
│   │   ├── daily-check/   # 안부 확인
│   │   └── settings/      # 설정
│   ├── hooks/             # 커스텀 훅
│   │   ├── api/           # API 훅
│   │   └── common/        # 공통 훅
│   ├── stores/            # Zustand 스토어
│   ├── services/          # API 서비스
│   ├── utils/             # 유틸리티 함수
│   ├── types/             # TypeScript 타입
│   └── styles/            # 글로벌 스타일
├── vite.config.ts
├── tailwind.config.js
└── package.json
```

## 🔧 핵심 기능 구현 계획

### **Phase 1: 핵심 기능 (2-3주)**
```
Week 1: 인증 & 메인 화면
- JWT 로그인/로그아웃
- 대화 목록 조회
- 메시지 전송/수신

Week 2: 안부 확인 & 보호자
- 안부 메시지 응답
- 보호자 정보 조회
- 알림 설정

Week 3: PWA 최적화
- 홈 화면 추가 가이드
- 오프라인 지원
- 푸시 알림 구현
```

### **Phase 2: UX 최적화 (1-2주)**
```
- 노인 친화적 UI 개선
- 접근성 향상
- 성능 최적화
- 에러 처리 강화
```

### **Phase 3: 확장 (선택적)**
```
- Capacitor 적용
- Play Store 배포
- 고급 푸시 알림
- 오프라인 동기화
```

## 🎨 UI/UX 가이드라인

### **노인 친화적 디자인**
```css
/* 큰 터치 영역 */
.touch-target {
  min-height: 48px;
  min-width: 48px;
}

/* 큰 폰트 사이즈 */
.text-primary { font-size: 18px; }
.text-large { font-size: 24px; }

/* 명확한 색상 대비 */
.high-contrast {
  color: #000000;
  background: #ffffff;
}

/* 간단한 네비게이션 */
.nav-simple {
  max-items: 4;
  bottom-navigation: true;
}
```

### **주요 화면 구성**
1. **로그인 화면**: 큰 버튼, 간단한 폼
2. **메인 화면**: 안부 메시지 + 대화 버튼
3. **대화 화면**: 큰 텍스트, 음성 입력 지원
4. **설정 화면**: 알림 설정, 보호자 정보

## 📊 성능 목표

### **PWA 성능 지표**
- **First Contentful Paint**: < 1.5초
- **Largest Contentful Paint**: < 2.5초
- **Time to Interactive**: < 3초
- **오프라인 지원**: 핵심 기능 100%

### **번들 크기 목표**
- **초기 번들**: < 500KB (gzipped)
- **총 번들**: < 2MB
- **이미지 최적화**: WebP, 지연 로딩

## 🔄 배포 및 업데이트

### **배포 전략**
```bash
# 개발 서버
npm run dev

# 프로덕션 빌드
npm run build

# PWA 유효성 검사
npm run preview
```

### **업데이트 전략**
- **자동 업데이트**: Service Worker 기반
- **사용자 알림**: 새 버전 알림 표시
- **점진적 롤아웃**: 단계별 배포

## 🚀 향후 확장 계획

### **네이티브 앱 전환 (Capacitor)**
```bash
# Capacitor 추가
npm install @capacitor/core @capacitor/cli
npx cap init maruni-client com.maruni.app

# Android 플랫폼 추가
npx cap add android
npx cap run android
```

### **고급 기능**
- **생체 인증**: 지문, 얼굴 인식
- **음성 인식**: Web Speech API
- **위치 기반 서비스**: 응급 상황 대응
- **오프라인 동기화**: 백그라운드 동기화

---

**💡 이 스택으로 MARUNI 클라이언트를 빠르고 안정적으로 구현할 수 있습니다!**