-- ============================================
-- MARUNI 데모 더미 데이터 SQL 스크립트
-- ============================================
-- 작성일: 2025-11-25
-- 용도: 데모 시연용 샘플 데이터
-- 실행: PostgreSQL DB에서 직접 실행
-- ============================================

-- ============================================
-- 1. 기존 데이터 삭제 (역순으로 삭제)
-- ============================================
DELETE FROM notification_history;
DELETE FROM daily_check_records;
DELETE FROM alert_history;
DELETE FROM alert_rule;
DELETE FROM messages;
DELETE FROM conversations;
DELETE FROM guardian_request;
-- member_table은 자기참조가 있어서 guardian 관계를 먼저 NULL로 설정
UPDATE member_table SET guardian_member_id = NULL WHERE guardian_member_id IS NOT NULL;
DELETE FROM member_table;

-- ============================================
-- 2. 회원 데이터 (6명)
-- ============================================
-- 비밀번호: "password123" (BCrypt 인코딩됨)
-- $2a$10$MZEYpcty6wLbnne/P4m8qOU2F6iQwv0fqR.gW5EBu5k4MF71js98C

-- 노인 회원 (3명) - 안부 메시지 수신 활성화
INSERT INTO member_table (id, member_email, member_name, member_password, daily_check_enabled, guardian_member_id, guardian_relation, created_at, updated_at)
VALUES
    (1, 'chulsoo.kim@example.com', '김철수', '$2a$10$MZEYpcty6wLbnne/P4m8qOU2F6iQwv0fqR.gW5EBu5k4MF71js98C', true, NULL, NULL, NOW() - INTERVAL '30 days', NOW()),
    (2, 'soonja.lee@example.com', '이순자', '$2a$10$MZEYpcty6wLbnne/P4m8qOU2F6iQwv0fqR.gW5EBu5k4MF71js98C', true, NULL, NULL, NOW() - INTERVAL '25 days', NOW()),
    (3, 'minsoo.park@example.com', '박민수', '$2a$10$MZEYpcty6wLbnne/P4m8qOU2F6iQwv0fqR.gW5EBu5k4MF71js98C', true, NULL, NULL, NOW() - INTERVAL '20 days', NOW());

-- 보호자 회원 (3명) - 안부 메시지 수신 비활성화
INSERT INTO member_table (id, member_email, member_name, member_password, daily_check_enabled, guardian_member_id, guardian_relation, created_at, updated_at)
VALUES
    (4, 'youngsoo.kim@example.com', '김영수', '$2a$10$MZEYpcty6wLbnne/P4m8qOU2F6iQwv0fqR.gW5EBu5k4MF71js98C', false, NULL, NULL, NOW() - INTERVAL '30 days', NOW()),
    (5, 'minjung.lee@example.com', '이민정', '$2a$10$MZEYpcty6wLbnne/P4m8qOU2F6iQwv0fqR.gW5EBu5k4MF71js98C', false, NULL, NULL, NOW() - INTERVAL '25 days', NOW()),
    (6, 'sooyeon.jang@example.com', '장수연', '$2a$10$MZEYpcty6wLbnne/P4m8qOU2F6iQwv0fqR.gW5EBu5k4MF71js98C', false, NULL, NULL, NOW() - INTERVAL '20 days', NOW());

-- 시퀀스 재설정 (다음 ID를 7로 설정)
SELECT setval('member_table_id_seq', 6);

-- ============================================
-- 3. 보호자 관계 설정 (2건)
-- ============================================
-- 김철수(1) → 김영수(4) 보호자 관계 (가족)
UPDATE member_table SET guardian_member_id = 4, guardian_relation = 'FAMILY' WHERE id = 1;

-- 이순자(2) → 이민정(5) 보호자 관계 (가족)
UPDATE member_table SET guardian_member_id = 5, guardian_relation = 'FAMILY' WHERE id = 2;

-- 박민수(3)는 아직 보호자 없음 (요청 대기중)

-- ============================================
-- 4. 보호자 요청 (1건 - PENDING)
-- ============================================
INSERT INTO guardian_request (id, requester_id, guardian_id, relation, status, created_at, updated_at)
VALUES
    (1, 3, 6, 'FRIEND', 'PENDING', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

SELECT setval('guardian_request_id_seq', 1);

-- ============================================
-- 5. 대화 데이터 (김철수 3개, 이순자 2개)
-- ============================================
-- 김철수의 대화 (부정적 패턴)
INSERT INTO conversations (id, member_id, started_at, created_at, updated_at)
VALUES
    (1, 1, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (2, 1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (3, 1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- 이순자의 대화 (긍정적 패턴)
INSERT INTO conversations (id, member_id, started_at, created_at, updated_at)
VALUES
    (4, 2, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (5, 2, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

SELECT setval('conversations_id_seq', 5);

-- ============================================
-- 6. 메시지 데이터
-- ============================================
-- 김철수의 메시지 (부정적 감정 패턴)
INSERT INTO messages (id, conversation_id, type, content, emotion, created_at, updated_at)
VALUES
    -- 3일전 대화
    (1, 1, 'USER_MESSAGE', '몸이 아파요... 힘들어요', 'NEGATIVE', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (2, 1, 'AI_RESPONSE', '많이 힘드시군요. 어디가 불편하신가요? 필요하시면 보호자님께 연락드릴 수 있어요.', 'NEUTRAL', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),

    -- 2일전 대화
    (3, 2, 'USER_MESSAGE', '오늘도 기분이 안 좋아요', 'NEGATIVE', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (4, 2, 'AI_RESPONSE', '기분이 좋지 않으시다니 걱정되네요. 무슨 일이 있으신가요?', 'NEUTRAL', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (5, 2, 'USER_MESSAGE', '그냥 외로워요...', 'NEGATIVE', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

    -- 1일전 대화
    (6, 3, 'USER_MESSAGE', '잠을 잘 못 자서 너무 피곤해요', 'NEGATIVE', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (7, 3, 'AI_RESPONSE', '수면에 어려움을 겪고 계시는군요. 병원 방문이 필요할 수도 있겠습니다.', 'NEUTRAL', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- 이순자의 메시지 (긍정적 감정 패턴)
INSERT INTO messages (id, conversation_id, type, content, emotion, created_at, updated_at)
VALUES
    -- 2일전 대화
    (8, 4, 'USER_MESSAGE', '오늘 날씨가 너무 좋네요! 기분이 좋아요', 'POSITIVE', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (9, 4, 'AI_RESPONSE', '좋은 날씨에 기분도 좋으시다니 다행이에요! 산책이라도 다녀오시면 더 좋을 것 같아요.', 'NEUTRAL', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

    -- 1일전 대화
    (10, 5, 'USER_MESSAGE', '손주들이 다녀갔어요. 정말 행복했어요!', 'POSITIVE', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (11, 5, 'AI_RESPONSE', '손주분들과 좋은 시간을 보내셨군요! 가족들과 함께하는 시간이 소중하죠.', 'NEUTRAL', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (12, 5, 'USER_MESSAGE', '네, 정말 감사해요!', 'POSITIVE', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

SELECT setval('messages_id_seq', 12);

-- ============================================
-- 7. 이상징후 규칙 (각 노인별 3종 규칙)
-- ============================================
-- 김철수의 규칙
INSERT INTO alert_rule (id, member_id, alert_type, rule_name, rule_description, consecutive_days, threshold_count, target_emotion, keywords, alert_level, is_active, created_at, updated_at)
VALUES
    (1, 1, 'EMOTION_PATTERN', '연속 부정감정 감지', '3일 연속 부정적 감정 감지 시 알림', 3, 1, 'NEGATIVE', NULL, 'HIGH', true, NOW() - INTERVAL '30 days', NOW()),
    (2, 1, 'NO_RESPONSE', '무응답 감지', '3일 연속 무응답 시 알림', 3, 0, NULL, NULL, 'MEDIUM', true, NOW() - INTERVAL '30 days', NOW()),
    (3, 1, 'KEYWORD_DETECTION', '키워드 감지', '위험 키워드 감지 시 알림: 아프다,힘들다,죽고싶다', NULL, 1, NULL, '아프다,힘들다,죽고싶다,도와줘', 'EMERGENCY', true, NOW() - INTERVAL '30 days', NOW());

-- 이순자의 규칙
INSERT INTO alert_rule (id, member_id, alert_type, rule_name, rule_description, consecutive_days, threshold_count, target_emotion, keywords, alert_level, is_active, created_at, updated_at)
VALUES
    (4, 2, 'EMOTION_PATTERN', '연속 부정감정 감지', '3일 연속 부정적 감정 감지 시 알림', 3, 1, 'NEGATIVE', NULL, 'HIGH', true, NOW() - INTERVAL '25 days', NOW()),
    (5, 2, 'NO_RESPONSE', '무응답 감지', '3일 연속 무응답 시 알림', 3, 0, NULL, NULL, 'MEDIUM', true, NOW() - INTERVAL '25 days', NOW()),
    (6, 2, 'KEYWORD_DETECTION', '키워드 감지', '위험 키워드 감지 시 알림: 아프다,힘들다,죽고싶다', NULL, 1, NULL, '아프다,힘들다,죽고싶다,도와줘', 'EMERGENCY', true, NOW() - INTERVAL '25 days', NOW());

-- 박민수의 규칙
INSERT INTO alert_rule (id, member_id, alert_type, rule_name, rule_description, consecutive_days, threshold_count, target_emotion, keywords, alert_level, is_active, created_at, updated_at)
VALUES
    (7, 3, 'EMOTION_PATTERN', '연속 부정감정 감지', '3일 연속 부정적 감정 감지 시 알림', 3, 1, 'NEGATIVE', NULL, 'HIGH', true, NOW() - INTERVAL '20 days', NOW()),
    (8, 3, 'NO_RESPONSE', '무응답 감지', '3일 연속 무응답 시 알림', 3, 0, NULL, NULL, 'MEDIUM', true, NOW() - INTERVAL '20 days', NOW()),
    (9, 3, 'KEYWORD_DETECTION', '키워드 감지', '위험 키워드 감지 시 알림: 아프다,힘들다,죽고싶다', NULL, 1, NULL, '아프다,힘들다,죽고싶다,도와줘', 'EMERGENCY', true, NOW() - INTERVAL '20 days', NOW());

SELECT setval('alert_rule_id_seq', 9);

-- ============================================
-- 8. 이상징후 감지 이력
-- ============================================
-- 김철수: 연속 부정감정 감지됨 (알림 발송 완료)
INSERT INTO alert_history (id, alert_rule_id, member_id, alert_type, alert_level, alert_message, detection_details, is_notification_sent, notification_sent_at, notification_result, alert_date, created_at, updated_at)
VALUES
    (1, 1, 1, 'EMOTION_PATTERN', 'HIGH', '김철수님이 3일 연속 부정적인 감정을 보이고 있습니다.',
     '{"consecutiveDays": 3, "detectedDates": ["2025-11-22", "2025-11-23", "2025-11-24"], "emotions": ["NEGATIVE", "NEGATIVE", "NEGATIVE"]}',
     true, NOW() - INTERVAL '12 hours', 'SUCCESS: 보호자(김영수)에게 알림 발송 완료',
     NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '12 hours');

-- 박민수: 무응답 감지됨 (알림 미발송 - 보호자 미지정)
INSERT INTO alert_history (id, alert_rule_id, member_id, alert_type, alert_level, alert_message, detection_details, is_notification_sent, notification_sent_at, notification_result, alert_date, created_at, updated_at)
VALUES
    (2, 8, 3, 'NO_RESPONSE', 'MEDIUM', '박민수님이 3일 연속 응답하지 않고 있습니다.',
     '{"noResponseDays": 3, "lastResponseDate": "2025-11-21"}',
     false, NULL, '보호자 미지정으로 알림 발송 보류',
     NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

SELECT setval('alert_history_id_seq', 2);

-- ============================================
-- 9. 안부 확인 발송 기록 (최근 7일)
-- ============================================
-- 김철수 (매일 성공)
INSERT INTO daily_check_records (id, member_id, check_date, message, success, created_at, updated_at)
VALUES
    (1, 1, CURRENT_DATE - INTERVAL '6 days', '안녕하세요, 김철수님! 오늘 기분은 어떠세요?', true, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    (2, 1, CURRENT_DATE - INTERVAL '5 days', '안녕하세요, 김철수님! 오늘은 날씨가 좋네요.', true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (3, 1, CURRENT_DATE - INTERVAL '4 days', '안녕하세요, 김철수님! 잘 주무셨나요?', true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    (4, 1, CURRENT_DATE - INTERVAL '3 days', '안녕하세요, 김철수님! 오늘 하루는 어떠셨나요?', true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (5, 1, CURRENT_DATE - INTERVAL '2 days', '안녕하세요, 김철수님! 건강은 괜찮으신가요?', true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (6, 1, CURRENT_DATE - INTERVAL '1 day', '안녕하세요, 김철수님! 오늘도 좋은 하루 보내세요!', true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (7, 1, CURRENT_DATE, '안녕하세요, 김철수님! 오늘 기분은 어떠세요?', true, NOW(), NOW());

-- 이순자 (매일 성공)
INSERT INTO daily_check_records (id, member_id, check_date, message, success, created_at, updated_at)
VALUES
    (8, 2, CURRENT_DATE - INTERVAL '6 days', '안녕하세요, 이순자님! 오늘 기분은 어떠세요?', true, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    (9, 2, CURRENT_DATE - INTERVAL '5 days', '안녕하세요, 이순자님! 오늘은 날씨가 좋네요.', true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (10, 2, CURRENT_DATE - INTERVAL '4 days', '안녕하세요, 이순자님! 잘 주무셨나요?', true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    (11, 2, CURRENT_DATE - INTERVAL '3 days', '안녕하세요, 이순자님! 오늘 하루는 어떠셨나요?', true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (12, 2, CURRENT_DATE - INTERVAL '2 days', '안녕하세요, 이순자님! 건강은 괜찮으신가요?', true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (13, 2, CURRENT_DATE - INTERVAL '1 day', '안녕하세요, 이순자님! 오늘도 좋은 하루 보내세요!', true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (14, 2, CURRENT_DATE, '안녕하세요, 이순자님! 오늘 기분은 어떠세요?', true, NOW(), NOW());

-- 박민수 (4일전까지만 성공, 3일간 무응답)
INSERT INTO daily_check_records (id, member_id, check_date, message, success, created_at, updated_at)
VALUES
    (15, 3, CURRENT_DATE - INTERVAL '6 days', '안녕하세요, 박민수님! 오늘 기분은 어떠세요?', true, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    (16, 3, CURRENT_DATE - INTERVAL '5 days', '안녕하세요, 박민수님! 오늘은 날씨가 좋네요.', true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (17, 3, CURRENT_DATE - INTERVAL '4 days', '안녕하세요, 박민수님! 잘 주무셨나요?', true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days');

SELECT setval('daily_check_records_id_seq', 17);

-- ============================================
-- 10. 알림 이력 (Notification History)
-- ============================================
-- 김철수 안부 메시지 알림
INSERT INTO notification_history (id, member_id, title, message, channel_type, success, error_message, external_message_id,
                                  notification_type, source_type, source_entity_id, is_read, read_at, created_at, updated_at)
VALUES
    (1, 1, '오늘의 안부 메시지', '안녕하세요, 김철수님! 오늘 기분은 어떠세요?', 'PUSH', true, NULL, 'msg-001',
     'DAILY_CHECK', 'DAILY_CHECK', 7, true, NOW() - INTERVAL '6 hours', NOW(), NOW());

-- 이순자 안부 메시지 알림
INSERT INTO notification_history (id, member_id, title, message, channel_type, success, error_message, external_message_id,
                                  notification_type, source_type, source_entity_id, is_read, read_at, created_at, updated_at)
VALUES
    (2, 2, '오늘의 안부 메시지', '안녕하세요, 이순자님! 오늘 기분은 어떠세요?', 'PUSH', true, NULL, 'msg-002',
     'DAILY_CHECK', 'DAILY_CHECK', 14, true, NOW() - INTERVAL '4 hours', NOW(), NOW());

-- 김영수(보호자)에게 이상징후 알림
INSERT INTO notification_history (id, member_id, title, message, channel_type, success, error_message, external_message_id,
                                  notification_type, source_type, source_entity_id, is_read, read_at, created_at, updated_at)
VALUES
    (3, 4, '이상징후 감지', '김철수님이 3일 연속 부정적인 감정을 보이고 있습니다. 확인이 필요합니다.', 'PUSH', true, NULL, 'msg-003',
     'EMOTION_ALERT', 'ALERT_RULE', 1, false, NULL, NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours');

-- 장수연에게 보호자 요청 알림
INSERT INTO notification_history (id, member_id, title, message, channel_type, success, error_message, external_message_id,
                                  notification_type, source_type, source_entity_id, is_read, read_at, created_at, updated_at)
VALUES
    (4, 6, '보호자 등록 요청', '박민수님이 보호자 등록을 요청했습니다.', 'PUSH', true, NULL, 'msg-004',
     'GUARDIAN_REQUEST', 'GUARDIAN_REQUEST', 1, false, NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

SELECT setval('notification_history_id_seq', 4);

-- ============================================
-- 완료 메시지
-- ============================================
SELECT '✅ 데모 데이터 삽입 완료!' as status,
       (SELECT COUNT(*) FROM member_table) as 회원수,
       (SELECT COUNT(*) FROM guardian_request) as 보호자요청,
       (SELECT COUNT(*) FROM conversations) as 대화수,
       (SELECT COUNT(*) FROM messages) as 메시지수,
       (SELECT COUNT(*) FROM alert_rule) as 규칙수,
       (SELECT COUNT(*) FROM alert_history) as 감지이력,
       (SELECT COUNT(*) FROM daily_check_records) as 안부확인,
       (SELECT COUNT(*) FROM notification_history) as 알림이력;
