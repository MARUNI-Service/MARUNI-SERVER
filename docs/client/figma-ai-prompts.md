# MARUNI Figma AI 프롬프트 가이드

**노인 친화적 PWA 앱을 위한 Figma AI 프롬프트 모음집**

## 🎯 기본 컨텍스트 (모든 프롬프트에 포함)

```
Context: Korean elderly care mobile PWA app "MARUNI"
Target Users: Korean elderly (65+ years old)
Platform: Android PWA (Progressive Web App)
Brand: Warm, trustworthy, simple

Accessibility Requirements:
- Minimum touch targets: 56px height
- Large readable fonts: 16px minimum, prefer 18px+
- High contrast colors (WCAG 2.1 AA compliance)
- Simple navigation patterns
- Clear visual hierarchy

Brand Colors:
- Primary Blue: #2563eb
- Secondary Green: #059669
- Warning Orange: #ea580c
- Neutral Gray: #6b7280
- Background White: #ffffff
- Light Blue: #f0f9ff
- Light Gray: #f9fafb

Typography Scale:
- H1: 28px Bold (Main titles)
- H2: 24px Bold (Page titles)
- H3: 20px SemiBold (Section headers)
- Body Large: 18px Medium (Important text)
- Body: 16px Regular (General text)
- Caption: 14px Regular (Secondary info)
- Small: 12px Regular (Timestamps, status)

Mobile Frame: 390×844px (iPhone 14 equivalent)
```

## 📱 1. 디자인 시스템 프롬프트

```
Create a comprehensive design system for MARUNI elderly care app with these specifications:

DESIGN TOKENS:
- Color palette with primary blue #2563eb, success green #059669, warning orange #ea580c
- Typography system with 7 text styles (28px-12px range)
- Spacing system based on 8px grid (8, 16, 24, 32, 48px)
- Border radius: 8px (small), 12px (medium), 16px (large)

CORE COMPONENTS:
1. Buttons:
   - Primary: Blue #2563eb, 56px height, white text, bold
   - Secondary: Gray #f3f4f6, 56px height, black text
   - Large action: 280×100px for main dashboard
   - States: default, hover, active, disabled

2. Input Fields:
   - Height: 56px
   - Border: 2px solid #d1d5db
   - Focus: 2px solid #2563eb with blue glow
   - Error: 2px solid #dc2626 with red background
   - Padding: 16px horizontal

3. Cards:
   - Background: white
   - Border: 1px solid #e5e7eb
   - Border radius: 12px
   - Shadow: 0 2px 8px rgba(0,0,0,0.1)
   - Padding: 16px

4. Modal/Dialog:
   - Bottom sheet style
   - Overlay: rgba(0,0,0,0.5)
   - Container: white, top rounded corners 20px
   - Drag handle: 40×4px gray bar

ACCESSIBILITY:
- All interactive elements minimum 48×48px
- Color contrast ratio minimum 4.5:1
- Focus indicators: 3px blue outline
- Large touch targets with adequate spacing

Create as reusable components with variants and auto-layout.
```

## 🔐 2. 로그인 페이지 프롬프트

```
Design a login page for MARUNI elderly care app with these exact specifications:

LAYOUT STRUCTURE (390×844px mobile frame):
1. Header Area (200px height):
   - Gradient background: #f0f9ff to #e0f2fe
   - MARUNI logo: 80×80px circular avatar with blue background
   - App name: "마루니와 함께하는 안부" (24px bold, black)
   - Tagline: "안전하고 편리한 로그인" (16px regular, gray)
   - All centered vertically and horizontally

2. Form Area (400px height):
   - White background with top rounded corners 16px
   - Subtle top shadow
   - Padding: 30px
   - Max width: 320px, centered

   Phone Input Field:
   - Label: "📞 전화번호" (18px bold, black, 8px bottom margin)
   - Input: 56px height, placeholder "010-1234-5678"
   - Border: 2px solid #d1d5db, radius 8px
   - Font: 20px, padding 16px

   Birth Date Input Field:
   - Label: "🎂 생년월일" (18px bold, black, 20px top margin, 8px bottom)
   - Input: 56px height, type="date"
   - Same styling as phone input

   Auto-login Checkbox:
   - 20px top margin, left aligned
   - 24×24px checkbox + "자동 로그인" text (16px gray)
   - 12px gap between checkbox and text

   Login Button:
   - 56px height, full width
   - Primary blue #2563eb background
   - "로그인" text (20px bold, white)
   - 8px border radius
   - 30px top margin
   - Disabled state: gray #d1d5db when fields empty

3. Help Area (160px height):
   - Light gray background #f9fafb
   - Padding: 20px
   - Center aligned text

   Help Links:
   - "❓ 로그인에 어려움이 있으신가요?" (16px gray)
   - "📞 고객센터: 1588-1234" (18px bold blue)
   - "👥 처음 사용하시나요? 회원가입" (16px gray)
   - 20px spacing between links

STATES TO INCLUDE:
- Default state (empty fields)
- Typing state (focused input with blue border)
- Error state (red border + error message)
- Loading state (spinner in button)

Make it warm, trustworthy, and very easy to use for elderly users.
```

## 🏠 3. 메인 대시보드 프롬프트

```
Design the main dashboard for MARUNI elderly care app as a central hub:

LAYOUT STRUCTURE (390×844px mobile frame):
1. Header (60px height):
   - White background with bottom border #e5e7eb
   - Left: Notification bell icon (24px) with red dot for new alerts
   - Right: Profile photo (40px circular) + name
   - Subtle shadow

2. Greeting Area (120px height):
   - Gradient background: #f0f9ff to #e0f2fe
   - Padding: 20px
   - "👋 안녕하세요, [김영희]님!" (24px bold, black)
   - "📅 오늘은 2024년 9월 29일 일요일입니다" (18px gray)
   - Daily status: "💚 오늘 안부: 전송 완료 (9:15)" (16px green)
   - OR "아직 안부를 전하지 않았어요" (16px gray) if incomplete

3. Main Action Buttons (600px height):
   - White background, padding 20px
   - 4 large buttons: 280×100px each
   - 30px spacing between buttons
   - Rounded corners 12px, subtle shadow

   Button 1 - Daily Check:
   - Primary blue #2563eb background
   - "💬 안부 메시지 보내기" (20px bold white)
   - "마루니에게 오늘 기분을 알려주세요" (14px white, opacity 0.9)
   - If completed: "✅ 오늘 안부를 보냈어요" overlay

   Button 2 - AI Chat:
   - Green #059669 background
   - "🤖 마루니와 대화하기" (20px bold white)
   - "궁금한 것이 있으면 언제든 물어보세요" (14px white, opacity 0.9)
   - Red badge if new messages

   Button 3 - Guardian Contact:
   - Orange #ea580c background
   - "👥 보호자 연락하기" (20px bold white)
   - "가족에게 안부를 전하거나 도움을 요청하세요" (14px white, opacity 0.9)

   Button 4 - History:
   - Gray #6b7280 background
   - "📋 지난 기록 보기" (20px bold white)
   - "이전 대화와 안부 기록을 확인하세요" (14px white, opacity 0.9)

4. Quick Settings (80px height):
   - Light gray background #f9fafb
   - Top border #e5e7eb
   - 3 buttons equally spaced: "⚙️ 설정", "🆘 응급연락", "❓ 도움말"
   - Each 48×48px touch area, 24px icons + 12px text

INTERACTION STATES:
- Button press: scale(0.98) with slight shadow increase
- Completion badges and status indicators
- Clear visual hierarchy and touch-friendly spacing

Make it feel like a warm, welcoming home base for elderly users.
```

## 💬 4. 안부 메시지 페이지 프롬프트

```
Design daily check-in page for MARUNI elderly care app:

LAYOUT STRUCTURE (390×844px mobile frame):
1. Header (60px height):
   - White background, bottom border
   - Left: Back arrow (24px)
   - Center: "💬 안부 메시지" (20px bold)
   - Right: Progress indicator "✅" or "📝"

2. Question Area (150px height):
   - Light blue background #f0f9ff
   - Padding: 20px
   - Top rounded corners 12px

   Content:
   - "📅 2024년 9월 29일 일요일" (16px gray)
   - "안녕하세요! 오늘 기분은 어떠세요?" (24px bold black, line-height 1.4)
   - "💡 편안하게 오늘 하루 어떻게 지내셨는지 들려주세요" (16px gray)

3. Input Area (400px height):
   - White background, padding 20px

   TEXT MODE (default):
   - Large textarea: full width × 300px
   - Placeholder: "예: 오늘은 날씨가 좋아서 기분이 좋아요"
   - Border: 1px solid #e5e7eb, focus: 2px solid #2563eb
   - Font: 18px, padding 16px, radius 8px
   - Character count: "47자 입력됨" (14px gray, bottom right)

   Quick response buttons (optional):
   - "😊 좋아요", "😐 보통이에요", "😔 우울해요"
   - 80×40px each, rounded

   VOICE MODE (alternate state):
   - Large microphone icon: 120px circular, red #dc2626 background
   - White mic icon in center
   - "터치해서 말씀해 주세요" (20px bold, center)
   - When recording: "🔴 녹음 중... 말씀하고 계신 내용을 듣고 있어요"
   - Recognized text appears in gray box below

4. Action Bar (80px height):
   - White background, top border
   - Padding: 16px
   - 3 buttons with spacing:

   Voice Button:
   - "🎤 음성입력" 80×48px
   - Default: gray #f3f4f6, Active: red #dc2626 with white text

   Text Button:
   - "📝 텍스트" 80×48px
   - Default: gray #f3f4f6, Active: blue #2563eb with white text

   Send Button:
   - "📤 전송하기" 120×48px
   - Blue #2563eb background, white text (18px bold)
   - Disabled: gray #d1d5db when no content
   - Radius: 8px

STATES TO INCLUDE:
1. Default (text input mode)
2. Voice input mode (mic interface)
3. Voice recording (red mic + waveform animation)
4. Voice recognized (text populated)
5. Sending (loading overlay: "🔄 전송 중... AI가 메시지를 분석하고 있어요")
6. Success (full screen: "✅ 전송 완료! 오늘 안부를 잘 전달했어요")

Make inputs very large and clear for elderly users with potential vision/dexterity challenges.
```

## 🤖 5. AI 대화 페이지 프롬프트

```
Design AI conversation page for MARUNI elderly care app in chat interface style:

LAYOUT STRUCTURE (390×844px mobile frame):
1. Header (60px height):
   - White background, bottom border, subtle shadow
   - Left: Back arrow (24px)
   - Center: "🤖 마루니와 대화" (20px bold) + "AI 어시스턴트" (14px gray below)
   - Right: Menu dots (24px)

2. Chat Area (variable height):
   - Light gray background #f9fafb
   - Padding: 16px
   - Scroll to bottom with new messages

   EMPTY STATE (first conversation):
   - Center aligned welcome screen
   - Large MARUNI avatar: 80px circular, warm blue background
   - "안녕하세요! 저는 마루니예요" (24px bold, black)
   - "궁금한 것이 있으면 언제든 물어보세요\n건강, 날씨, 일상 대화 모두 좋아요!" (16px gray, center, line-height 1.5)
   - Suggestion: "💡 '오늘 날씨가 어때요?' 같은 질문으로 시작해보세요" (14px italic, light gray)

   USER MESSAGES (right aligned):
   - Max width: 80%, right margin 0, left margin 20%
   - Blue bubble #2563eb, white text
   - Padding: 12px 16px, border-radius: 16px (right-bottom 4px)
   - Font: 16px regular
   - Timestamp below: "오후 2:35" (12px gray, right aligned)
   - 8px vertical spacing

   AI MESSAGES (left aligned):
   - Max width: 80%, left margin 0, right margin 20%
   - Small avatar (32px) + message bubble
   - Avatar: fixed left, message 8px gap to right
   - White bubble #ffffff, black text #1f2937
   - Border: 1px solid #e5e7eb, border-radius: 16px (left-bottom 4px)
   - Padding: 12px 16px, font: 16px regular
   - Subtle shadow
   - Timestamp below: "오후 2:36" (12px gray, left aligned)

   TYPING INDICATOR (AI responding):
   - Avatar + typing bubble
   - White background, 3 dots animation ● ● ●
   - Dots scale up/down sequentially, 1.5s cycle
   - Padding: 12px 16px, rounded 16px

3. Input Bar (80px height):
   - White background, top border, top shadow
   - Padding: 16px
   - Layout: [Input field] [Voice] [Send]

   Message Input:
   - Height: 48px, flexible width
   - Gray background #f3f4f6, no border, radius 24px
   - Padding: 12px 16px, font: 16px
   - Placeholder: "마루니에게 메시지를 보내세요..."
   - Focus: white background, blue border 2px #2563eb

   Voice Button:
   - 48×48px circular
   - Default: gray #f3f4f6, gray icon #6b7280
   - Active (recording): red #dc2626, white icon, pulse animation

   Send Button:
   - 48×48px circular
   - Disabled: gray #d1d5db when empty
   - Active: blue #2563eb, white icon
   - Press: slightly darker

SPECIAL FEATURES:
- Emotion analysis badge: "😊 긍정적" (small, green background)
- Emergency detection: "🚨 도움이 필요하시나요? [예, 연락해주세요] [아니오, 괜찮아요]"
- Scroll behavior: auto-scroll to latest message
- Message appearance animation: slide up + fade in

Create multiple conversation states showing natural elderly-AI dialogue flow.
```

## 📋 6. 기록 조회 페이지 프롬프트

```
Design history/records page for MARUNI elderly care app:

LAYOUT STRUCTURE (390×844px mobile frame):
1. Header (60px height):
   - White background, bottom border, shadow
   - Left: Back arrow (24px)
   - Center: "📋 지난 기록" (20px bold)
   - Right: Search icon (24px)

2. Filter Bar (60px height):
   - Light gray background #f9fafb, bottom border
   - Padding: 12px 16px
   - Horizontal scrollable filter chips:

   Period filters: "📅 최근 7일", "📅 최근 30일", "📅 전체 기간"
   Category filters: "📂 전체", "💬 안부", "🤖 대화"
   Refresh: "🔄 새로고침" (circular button, right side)

   Filter chip styling:
   - 80×36px (period), 60×36px (category)
   - Unselected: white background, gray text, gray border
   - Selected: blue #2563eb background, white text
   - Radius: 18px, 8px spacing

3. Records List (variable height):
   - White background, padding: 16px
   - 12px spacing between cards

   EMPTY STATE:
   - Center aligned
   - Large document icon: 60px gray
   - "아직 기록이 없어요" (20px bold, gray)
   - "안부 메시지나 마루니와의 대화를\n시작하면 여기에 기록이 남아요" (16px gray, center, line-height 1.5)
   - "💡 메인 화면에서 대화를 시작해보세요" (14px italic, blue, clickable)

   RECORD CARDS (100px height each):
   - White background, border: 1px solid #e5e7eb
   - Border radius: 12px, padding: 16px
   - Subtle shadow, press animation: scale(0.98)
   - Layout: [Icon 56px] [Content flexible] [Time/Status 80px]

   Daily Check Record:
   - Icon: 💬 in blue circle #dbeafe (56px)
   - Title: "오늘 기분은 어떠세요?" (16px bold, black)
   - Preview: "좋아요! 날씨가 맑아서..." (14px gray, max 2 lines, ellipsis)
   - Time: "오전 9:15" (12px gray)
   - Status: ✅ (16px green) for completed

   AI Conversation Record:
   - Icon: 🤖 in green circle #dcfce7 (56px)
   - Title: "마루니와의 대화" (16px bold, black)
   - Preview: "안녕하세요! 오늘은..." (14px gray, max 2 lines)
   - Messages: "메시지 8개" (12px gray)
   - Time: "오후 2:30" (12px gray)

   Guardian Alert Record:
   - Icon: 🚨 in orange circle #fed7aa (56px)
   - Title: "보호자님께 알림을 보냈어요" (16px bold, black)
   - Reason: "감정 상태 변화가 감지되어..." (14px gray)
   - Priority badge: "⚠️ 중요" (red #dc2626, white text, 12px, radius 4px)
   - Time: "오후 3:45" (12px gray)

4. Pagination (60px height):
   - White background, top border
   - Padding: 16px, center aligned
   - "더 많은 기록 보기" button (200×40px, gray background, radius 20px)
   - Loading: "🔄 기록을 불러오는 중..." (spinner + text)
   - End: "모든 기록을 확인했어요" (14px gray)

DETAIL MODAL (bottom sheet):
- Overlay: rgba(0,0,0,0.5)
- Container: white, top rounded 20px, 80% height
- Drag handle: 40×4px gray bar, top center
- Header: Record type + date (18px bold)
- Content: Full conversation or detailed record
- Scrollable with proper spacing

Include various record types and time periods to show realistic usage patterns.
```

## ⚙️ 7. 설정 페이지 프롬프트

```
Design settings page for MARUNI elderly care app with grouped menu structure:

LAYOUT STRUCTURE (390×844px mobile frame):
1. Header (60px height):
   - White background, bottom border, shadow
   - Left: Back arrow (24px)
   - Center: "⚙️ 설정" (20px bold)
   - Right: Profile photo (32px circular, clickable)

2. Settings List (variable height):
   - Light gray background #f9fafb
   - Padding: 16px
   - Vertical scroll

   GROUP STRUCTURE:
   Settings are organized in 5 groups with clear visual separation:

   GROUP 1: 👤 개인 설정 (24px top margin for first group)
   - Group header: "👤 개인 설정" (18px bold, black, 16px bottom padding)

   Menu Item - Personal Info:
   - 72×full width white card, 1px border #e5e7eb, radius 12px
   - Padding: 16px, 8px bottom margin
   - Layout: [Icon 24px] [Content flex] [Arrow 16px]
   - "📝 개인정보 수정" (18px bold, black)
   - "이름, 전화번호, 생년월일" (14px gray)
   - Right arrow ">" (16px gray)
   - Press animation: scale(0.98)

   Menu Item - Password:
   - Same styling as above
   - "🔒 비밀번호 변경"
   - "로그인 비밀번호 업데이트"

   GROUP 2: 🔔 알림 설정 (24px top margin)
   - Group header: "🔔 알림 설정" (18px bold, black)

   Menu Item - Daily Notification:
   - "💬 안부 메시지 알림" (18px bold)
   - "매일 오전 9시 알림" (14px gray)
   - Right: Toggle switch (52×32px)
   - Toggle ON: blue #2563eb, OFF: gray #d1d5db
   - Handle: 28×28px white circle with shadow

   Menu Item - Time Setting:
   - "⏰ 알림 시간 설정" + arrow
   - "현재: 오전 9:00" (14px gray)

   Menu Item - Guardian Alerts:
   - "👥 보호자 알림 설정" + arrow
   - "이상징후 감지 시 알림"

   GROUP 3: 👥 보호자 관리 (24px top margin)
   - Group header: "👥 보호자 관리"

   Guardian Cards (80×full width each):
   - White background, padding: 16px
   - "👤 김철수 (아들)" (16px bold, black)
   - "010-1234-5678" (14px gray)
   - "알림: 항상 받기" (14px gray)
   - Right buttons: 📞 (32×32px) + 🗑️ (32×32px, red when pressed)

   Add Guardian Button:
   - "➕ 새 보호자 추가" + arrow
   - Light blue background #f0f9ff, dashed border #93c5fd
   - "가족이나 지인을 등록하세요" (14px gray)

   GROUP 4: 📱 앱 설정 (24px top margin)
   - Group header: "📱 앱 설정"

   Font Size:
   - "🔤 글자 크기" + arrow
   - "현재: 보통" (14px gray)

   Theme:
   - "🎨 화면 테마" + arrow
   - "현재: 밝은 테마" (14px gray)

   Voice:
   - "🔊 음성 설정" + arrow
   - "음성 입력 및 안내"

   Auto Login:
   - "🔐 자동 로그인" + toggle
   - "앱 실행 시 자동으로 로그인"

   GROUP 5: ❓ 도움 및 지원 (24px top margin)
   - Group header: "❓ 도움 및 지원"

   Help Guide:
   - "📖 사용법 안내" + arrow
   - "마루니 이용 가이드"

   FAQ:
   - "❓ 자주 묻는 질문" + arrow
   - "궁금한 점을 확인하세요"

   Customer Service:
   - "📞 고객센터" + arrow
   - "1588-1234 (평일 9-18시)" (14px blue #2563eb)

   Feedback:
   - "📝 의견 보내기" + arrow
   - "개선 사항을 알려주세요"

3. Footer Info (80px height):
   - White background, top border
   - Padding: 16px, center aligned
   - "MARUNI v1.0.0" (14px gray)
   - "© 2024 안양대학교 SW융합대학" (12px light gray)
   - "개인정보처리방침 | 서비스 이용약관" (12px blue, clickable)

INTERACTION STATES:
- Menu item press: slight scale and background change
- Toggle animations: smooth slide with handle movement
- Group separation: 16px spacing with subtle dividers
- Large touch targets: minimum 72px height for all interactive elements

Create a clean, organized interface that elderly users can navigate confidently.
```

## 🎯 8. 모달 및 팝업 프롬프트

```
Design modal dialogs and popups for MARUNI elderly care app:

1. TIME PICKER MODAL (alarm time setting):
- Bottom sheet style: slide up from bottom
- Overlay: rgba(0,0,0,0.5)
- Container: white, top rounded 20px, 60% screen height
- Drag handle: 40×4px gray bar #d1d5db, top center, 12px margin
- Title: "⏰ 알림 시간 설정" (18px bold, center)
- Subtitle: "안부 메시지를 받을 시간을 선택하세요" (16px gray, center)
- Wheel picker: iOS style time selector
  - Hours: 오전/오후 + 01-12
  - Minutes: 00, 15, 30, 45
  - Currently selected: blue highlight
  - Large text: 20px for visibility
- Buttons: "취소" (gray) + "확인" (blue) at bottom, 48px height

2. FONT SIZE MODAL:
- Same bottom sheet structure
- Title: "🔤 글자 크기 설정"
- Preview text for each size:
  - ○ 작게: "마루니와 함께하는 안부" (14px)
  - ○ 보통: "마루니와 함께하는 안부" (16px)
  - ✅ 크게: "마루니와 함께하는 안부" (18px) - selected
  - ○ 매우 크게: "마루니와 함께하는 안부" (22px)
- Radio buttons: 24px, blue when selected
- Live preview: "현재 설정이 적용됩니다" (bottom)

3. DELETE CONFIRMATION DIALOG:
- Center modal: 320×200px
- White background, radius 16px, shadow
- Warning icon: ⚠️ (32px, orange, top center)
- Title: "확인" (20px bold, center)
- Message: "김철수님을 보호자에서\n삭제하시겠습니까?" (16px, center, line-height 1.5)
- Subtitle: "삭제하면 더 이상 알림을 받지 않습니다" (14px gray, center)
- Buttons: "취소" (gray, 140×48px) + "삭제" (red #dc2626, 140×48px)
- 16px gap between buttons

4. SUCCESS TOAST:
- Top slide-down notification
- Green background #059669, white text
- "✅ 설정이 저장되었습니다" (16px medium)
- Full width, 60px height, 16px padding
- Auto-dismiss after 2 seconds
- Entrance: slide down, Exit: fade out

5. LOADING OVERLAY:
- Full screen overlay: rgba(255,255,255,0.9)
- Center content:
  - Spinning icon: 40px blue circle
  - "처리 중..." (18px black, 16px top margin)
  - "잠시만 기다려 주세요" (14px gray, 8px top margin)

6. GUARDIAN ADD FORM (full page):
- Header: Back + "👥 보호자 추가" + Save (✅)
- Form fields (each 80px height, 16px spacing):
  - "👤 이름" + text input
  - "📞 전화번호" + tel input
  - "👨‍👩‍👧‍👦 관계" + dropdown selector
  - "🔔 알림 설정" + checkbox group:
    ✅ 이상징후 감지 시 알림
    ✅ 안부 메시지 공유
    ○ 대화 내용 요약 전송
  - "📧 이메일 (선택)" + email input
- Save button: full width, 56px, blue, bottom of form

All modals should have large touch targets, high contrast, and smooth animations suitable for elderly users.
```

---

## 🚀 사용 가이드

### **순서별 작업 방법:**

1. **먼저 디자인 시스템 생성** (프롬프트 #1)
2. **로그인 페이지부터 순차 진행** (프롬프트 #2-7)
3. **모달/팝업 요소 추가** (프롬프트 #8)
4. **프로토타입 연결 및 인터랙션 추가**

### **각 프롬프트 사용 시:**
- Figma AI에 복사/붙여넣기
- 생성 후 세부 조정
- 컴포넌트화 진행
- 다음 페이지로 연결

### **최적화 팁:**
- 한 번에 하나씩 생성
- 생성 후 즉시 세부 검토
- 노인 UX 기준으로 수정
- 일관성 유지 확인

**어떤 프롬프트부터 시작하시겠어요?**