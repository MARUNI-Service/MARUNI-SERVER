# 데모 데이터 실행 가이드

## 📋 개요

`demo-data.sql`은 MARUNI 프로젝트의 데모 시연을 위한 샘플 데이터입니다.

## 🎭 데모 시나리오

### 👥 등장인물 (6명)

#### 노인 회원 (3명)
| 이름 | 이메일 | 상태 | 보호자 |
|------|--------|------|--------|
| 김철수 (80세) | chulsoo.kim@example.com | 부정 감정 패턴 | 김영수 (아들) |
| 이순자 (75세) | soonja.lee@example.com | 긍정적 상태 | 이민정 (딸) |
| 박민수 (70세) | minsoo.park@example.com | 3일 무응답 | 요청 대기중 |

#### 보호자 회원 (3명)
| 이름 | 이메일 | 역할 |
|------|--------|------|
| 김영수 | youngsoo.kim@example.com | 김철수의 보호자 (아들) |
| 이민정 | minjung.lee@example.com | 이순자의 보호자 (딸) |
| 장수연 | sooyeon.jang@example.com | 박민수의 보호자 요청 대기 |

**공통 비밀번호**: `password123`

### 📊 데이터 구성

```
✅ 회원: 6명 (노인 3 + 보호자 3)
✅ 보호자 관계: 2건 (설정 완료)
✅ 보호자 요청: 1건 (PENDING)
✅ 대화: 5건 (김철수 3건, 이순자 2건)
✅ 메시지: 12건 (부정/긍정 패턴 포함)
✅ 이상징후 규칙: 9건 (각 노인별 3종)
✅ 감지 이력: 2건 (부정 감정 + 무응답)
✅ 안부 확인: 17건 (최근 7일)
✅ 알림 이력: 4건 (안부 + 이상징후 + 보호자 요청)
```

## 🚀 실행 방법

### 방법 1: PostgreSQL 클라이언트에서 직접 실행

```bash
# Docker 컨테이너에 접속
docker-compose exec db psql -U postgres -d maruni_db

# SQL 파일 실행
\i /path/to/demo-data.sql

# 또는 호스트에서 직접 실행
psql -h localhost -p 5432 -U postgres -d maruni_db -f src/main/resources/db/demo-data.sql
```

### 방법 2: DBeaver, pgAdmin 등 GUI 툴 사용

1. DB 연결 설정:
   - Host: `localhost`
   - Port: `5432`
   - Database: `maruni_db`
   - Username: `postgres`
   - Password: `.env` 파일 참조

2. `demo-data.sql` 파일 열기
3. 전체 스크립트 실행 (Ctrl+Enter 또는 Execute)

### 방법 3: Spring Boot 애플리케이션에서 자동 실행

**주의**: 이 방법은 매번 애플리케이션 시작 시 실행되므로 개발 환경에서만 사용하세요.

`application.yml` 또는 `application-dev.yml`에 추가:

```yaml
spring:
  sql:
    init:
      mode: always
      data-locations: classpath:db/demo-data.sql
```

## 📝 데이터 확인

### 회원 조회
```sql
-- 모든 회원 조회
SELECT id, member_name, member_email, daily_check_enabled, guardian_member_id
FROM member_table
ORDER BY id;

-- 보호자 관계 조회
SELECT
    m.id,
    m.member_name as 노인,
    g.member_name as 보호자,
    m.guardian_relation as 관계
FROM member_table m
LEFT JOIN member_table g ON m.guardian_member_id = g.id
WHERE m.guardian_member_id IS NOT NULL;
```

### 대화 및 메시지 조회
```sql
-- 김철수의 최근 대화
SELECT
    c.id as 대화ID,
    m.content as 내용,
    m.emotion as 감정,
    m.type as 타입,
    m.created_at as 시간
FROM conversations c
JOIN messages m ON c.id = m.conversation_id
WHERE c.member_id = 1
ORDER BY m.created_at DESC;
```

### 이상징후 감지 확인
```sql
-- 감지된 이상징후
SELECT
    ah.id,
    m.member_name as 회원명,
    ah.alert_type as 감지유형,
    ah.alert_level as 위험도,
    ah.alert_message as 메시지,
    ah.is_notification_sent as 알림발송,
    ah.created_at as 감지시간
FROM alert_history ah
JOIN member_table m ON ah.member_id = m.id
ORDER BY ah.created_at DESC;
```

### 알림 이력 확인
```sql
-- 모든 알림 이력
SELECT
    nh.id,
    m.member_name as 수신자,
    nh.notification_type as 알림타입,
    nh.title as 제목,
    nh.is_read as 읽음여부,
    nh.created_at as 발송시간
FROM notification_history nh
JOIN member_table m ON nh.member_id = m.id
ORDER BY nh.created_at DESC;
```

## 🎯 데모 테스트 시나리오

### 1️⃣ 로그인 테스트
```bash
# 김철수로 로그인
POST /api/auth/login
{
  "memberEmail": "chulsoo.kim@example.com",
  "memberPassword": "password123"
}

# 김영수(보호자)로 로그인
POST /api/auth/login
{
  "memberEmail": "youngsoo.kim@example.com",
  "memberPassword": "password123"
}
```

### 2️⃣ 대화 조회
```bash
# 김철수의 대화 이력 조회
GET /api/conversation?memberId=1
Authorization: Bearer {token}
```

### 3️⃣ 이상징후 확인
```bash
# 김철수의 이상징후 이력 조회
GET /api/alert/history?memberId=1
Authorization: Bearer {token}
```

### 4️⃣ 보호자 관계 확인
```bash
# 김철수의 보호자 조회
GET /api/guardian?memberId=1
Authorization: Bearer {token}
```

### 5️⃣ 알림 조회
```bash
# 김영수(보호자)가 받은 알림 조회
GET /api/notification/history?memberId=4
Authorization: Bearer {token}
```

## 🔄 데이터 초기화

전체 데이터를 삭제하고 다시 삽입하려면:

```sql
-- 스크립트 재실행 (스크립트 내부에 DELETE 문 포함됨)
\i /path/to/demo-data.sql
```

또는 개별 테이블 초기화:

```sql
-- 1. 모든 데이터 삭제 (역순)
DELETE FROM notification_history;
DELETE FROM daily_check_records;
DELETE FROM alert_history;
DELETE FROM alert_rule;
DELETE FROM messages;
DELETE FROM conversations;
DELETE FROM guardian_request;
UPDATE member_table SET guardian_member_id = NULL;
DELETE FROM member_table;

-- 2. 시퀀스 초기화
ALTER SEQUENCE member_table_id_seq RESTART WITH 1;
ALTER SEQUENCE guardian_request_id_seq RESTART WITH 1;
ALTER SEQUENCE conversations_id_seq RESTART WITH 1;
ALTER SEQUENCE messages_id_seq RESTART WITH 1;
ALTER SEQUENCE alert_rule_id_seq RESTART WITH 1;
ALTER SEQUENCE alert_history_id_seq RESTART WITH 1;
ALTER SEQUENCE daily_check_records_id_seq RESTART WITH 1;
ALTER SEQUENCE notification_history_id_seq RESTART WITH 1;
```

## ⚠️ 주의사항

1. **프로덕션 환경에서 실행 금지**: 이 스크립트는 데모용입니다.
2. **기존 데이터 삭제**: 스크립트는 기존 데이터를 모두 삭제합니다.
3. **비밀번호 보안**: 실제 서비스에서는 더 강력한 비밀번호를 사용하세요.
4. **시퀀스 충돌**: AUTO_INCREMENT ID가 수동 삽입된 ID와 충돌할 수 있으므로 시퀀스를 재설정했습니다.

## 📚 관련 문서

- [프로젝트 전체 문서](../../../../docs/README.md)
- [도메인 아키텍처](../../../../docs/domains/README.md)
- [API 설계 가이드](../../../../docs/specifications/api-design-guide.md)
- [데이터베이스 설계](../../../../docs/specifications/database-design-guide.md)

---

**작성일**: 2025-11-25
**버전**: 1.0.0
**MARUNI Phase 2 MVP**
