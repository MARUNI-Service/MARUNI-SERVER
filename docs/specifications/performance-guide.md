# 성능 최적화 가이드

**MARUNI JPA 및 Database 성능 최적화**

## 🚀 핵심 최적화 원칙

### 1. N+1 쿼리 문제 방지
```java
// ❌ N+1 문제 발생
@OneToMany(mappedBy = "guardian")
private List<MemberEntity> members;  // LAZY 로딩 시 N+1 발생

// ✅ Fetch Join 사용
@Query("SELECT g FROM GuardianEntity g " +
       "LEFT JOIN FETCH g.members " +
       "WHERE g.id = :guardianId")
GuardianEntity findByIdWithMembers(@Param("guardianId") Long guardianId);
```

### 2. @Transactional 적절한 사용
```java
// ✅ 조회 전용: readOnly = true
@Transactional(readOnly = true)
public MemberResponse getMyInfo(String email) {
    // SELECT 쿼리만 실행, 변경 감지 비활성화
}

// ✅ 수정/삭제: readOnly 없음
@Transactional
public void update(MemberUpdateRequest request) {
    // UPDATE/DELETE 쿼리 실행
}
```

### 3. Batch Size 설정
```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100  # IN 절로 한 번에 조회
```

## 🔍 JPA 성능 최적화

### Entity Fetch 전략
```java
// 기본 전략
@OneToMany: LAZY (지연 로딩)  // 기본값, 권장
@ManyToOne: EAGER (즉시 로딩)  // 기본값, 주의 필요

// 권장 설정
@ManyToOne(fetch = FetchType.LAZY)  // 명시적으로 LAZY 설정
@JoinColumn(name = "guardian_id")
private GuardianEntity guardian;
```

### SELECT 쿼리 최적화
```java
// ❌ 전체 Entity 조회
List<MemberEntity> members = memberRepository.findAll();

// ✅ 필요한 컬럼만 조회 (DTO Projection)
@Query("SELECT new com.anyang.maruni.dto.MemberSimpleDto(m.id, m.memberName) " +
       "FROM MemberEntity m")
List<MemberSimpleDto> findAllSimple();
```

### Bulk 연산 사용
```java
// ❌ 개별 UPDATE (N번 쿼리)
members.forEach(m -> m.updateStatus(newStatus));

// ✅ Bulk UPDATE (1번 쿼리)
@Modifying
@Query("UPDATE MemberEntity m SET m.status = :status WHERE m.id IN :ids")
void bulkUpdateStatus(@Param("status") Status status, @Param("ids") List<Long> ids);
```

## 💾 Database 최적화

### 인덱스 설정
```java
// 자주 조회되는 컬럼에 인덱스
@Table(name = "member_table", indexes = {
    @Index(name = "idx_member_email", columnList = "memberEmail"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class MemberEntity extends BaseTimeEntity {
    @Column(unique = true)
    private String memberEmail;  // 자동 인덱스 생성
}
```

### Connection Pool 설정
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # 최대 연결 수
      minimum-idle: 5            # 최소 유휴 연결
      connection-timeout: 30000  # 연결 대기 시간 (30초)
      idle-timeout: 600000       # 유휴 연결 유지 시간 (10분)
```

### 쿼리 로그 최적화
```yaml
# 개발 환경
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE  # 파라미터 값 출력

# 운영 환경
spring:
  jpa:
    show-sql: false  # SQL 로그 비활성화

logging:
  level:
    org.hibernate.SQL: WARN
```

## 📊 성능 측정 및 모니터링

### Hibernate Statistics 활성화
```yaml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true  # 통계 활성화
```

```java
// 통계 확인
@Autowired
private EntityManagerFactory emf;

public void printStatistics() {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    log.info("Query Count: {}", stats.getQueryExecutionCount());
    log.info("Entity Fetch Count: {}", stats.getEntityFetchCount());
}
```

### Spring Boot Actuator
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## ⚡ 실전 최적화 패턴

### 1. Guardian 회원 목록 조회
```java
// ❌ N+1 문제
public List<MemberResponse> getMembersByGuardian(Long guardianId) {
    GuardianEntity guardian = guardianRepository.findById(guardianId);
    return guardian.getMembers().stream()  // N+1 발생!
        .map(MemberResponse::from)
        .toList();
}

// ✅ Fetch Join 사용
@Query("SELECT g FROM GuardianEntity g " +
       "LEFT JOIN FETCH g.members " +
       "WHERE g.id = :guardianId")
GuardianEntity findByIdWithMembers(@Param("guardianId") Long guardianId);

public List<MemberResponse> getMembersByGuardian(Long guardianId) {
    GuardianEntity guardian = guardianRepository.findByIdWithMembers(guardianId);
    return guardian.getMembers().stream()
        .map(MemberResponse::from)
        .toList();
}
```

### 2. 대화 내역 페이징
```java
// ✅ Pageable 사용
@Query("SELECT m FROM MessageEntity m " +
       "WHERE m.conversation.member.id = :memberId " +
       "ORDER BY m.createdAt DESC")
Page<MessageEntity> findByMemberIdOrderByCreatedAtDesc(
    @Param("memberId") Long memberId,
    Pageable pageable
);

// 사용
Pageable pageable = PageRequest.of(0, 20);  // 20개씩 페이징
Page<MessageEntity> messages = messageRepository.findByMemberId...(..., pageable);
```

### 3. 통계 조회 최적화
```java
// ✅ COUNT 쿼리 사용
@Query("SELECT COUNT(d) FROM DailyCheckRecord d " +
       "WHERE d.memberId = :memberId AND d.success = true")
long countSuccessfulChecks(@Param("memberId") Long memberId);
```

## 🎯 체크리스트

### 개발 시 확인사항
- [ ] @Transactional(readOnly = true) 적용 (조회 메서드)
- [ ] Fetch Join 사용 (연관 엔티티 조회 시)
- [ ] DTO Projection 사용 (필요한 컬럼만 조회)
- [ ] Batch Size 설정 확인
- [ ] 인덱스 설정 (자주 조회되는 컬럼)

### 운영 전 확인사항
- [ ] SQL 로그 비활성화 (show-sql: false)
- [ ] Connection Pool 설정 최적화
- [ ] 쿼리 성능 테스트 (대용량 데이터)
- [ ] Slow Query 모니터링 설정

---

**더 자세한 JPA 최적화는 Hibernate 공식 문서를 참조하세요.**
