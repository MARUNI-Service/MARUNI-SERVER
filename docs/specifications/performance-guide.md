# 성능 최적화 가이드

**MARUNI 성능 튜닝 및 최적화 표준**

---

## 🚀 JPA 성능 최적화

### **N+1 쿼리 문제 해결**
```java
// ❌ N+1 문제 발생 코드
@Service
public class BadMemberService {
    public List<MemberResponseDto> findAllMembers() {
        return memberRepository.findAll().stream()
            .map(member -> {
                // 각 member마다 guardian 조회 쿼리 실행 (N+1)
                String guardianName = member.getGuardian() != null ?
                    member.getGuardian().getName() : null;
                return MemberResponseDto.from(member, guardianName);
            })
            .collect(Collectors.toList());
    }
}

// ✅ Fetch Join으로 해결
@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    @Query("SELECT m FROM MemberEntity m LEFT JOIN FETCH m.guardian")
    List<MemberEntity> findAllWithGuardian();

    @Query("SELECT m FROM MemberEntity m " +
           "LEFT JOIN FETCH m.guardian " +
           "LEFT JOIN FETCH m.conversations c " +
           "LEFT JOIN FETCH c.messages")
    List<MemberEntity> findAllWithDetails();
}

// ✅ @EntityGraph 사용
@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    @EntityGraph(attributePaths = {"guardian", "conversations"})
    List<MemberEntity> findAll();

    @EntityGraph(value = "Member.withGuardian", type = EntityGraph.EntityGraphType.LOAD)
    Optional<MemberEntity> findById(Long id);
}

// Entity에 @NamedEntityGraph 정의
@Entity
@NamedEntityGraph(
    name = "Member.withGuardian",
    attributeNodes = @NamedAttributeNode("guardian")
)
public class MemberEntity extends BaseTimeEntity {
    // 구현
}
```

### **Fetch Type 최적화**
```java
// 관계별 최적 FetchType 설정
@Entity
public class ConversationEntity extends BaseTimeEntity {
    // 항상 필요한 관계 - EAGER (신중히 사용)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    // 선택적으로 필요한 관계 - LAZY (기본값)
    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY)
    private List<MessageEntity> messages = new ArrayList<>();

    // 컬렉션은 항상 LAZY
    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY)
    private List<AlertHistoryEntity> alertHistories = new ArrayList<>();
}

// Batch Size로 성능 개선
@Entity
@BatchSize(size = 10)
public class ConversationEntity extends BaseTimeEntity {
    @OneToMany(mappedBy = "conversation")
    @BatchSize(size = 20)
    private List<MessageEntity> messages;
}
```

### **Projection 활용**
```java
// DTO 직접 조회로 불필요한 데이터 제거
@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    @Query("SELECT new com.anyang.maruni.domain.member.application.dto.MemberSummaryDto(" +
           "m.id, m.memberName, m.memberEmail, m.createdAt) " +
           "FROM MemberEntity m")
    List<MemberSummaryDto> findAllSummary();

    @Query("SELECT m.id as id, m.memberName as name, m.memberEmail as email " +
           "FROM MemberEntity m")
    List<MemberProjection> findAllProjection();
}

// Interface-based Projection
public interface MemberProjection {
    Long getId();
    String getName();
    String getEmail();
    LocalDateTime getCreatedAt();
}

// Class-based Projection (DTO)
@Getter
@AllArgsConstructor
public class MemberSummaryDto {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}
```

---

## 📊 페이징 및 정렬 최적화

### **효율적인 페이징**
```java
// Slice 사용 (totalCount 불필요 시)
@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    Slice<ConversationEntity> findByMemberIdOrderByCreatedAtDesc(
        Long memberId, Pageable pageable);
}

// 커서 기반 페이징 (대용량 데이터)
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    @Query("SELECT m FROM MessageEntity m WHERE m.conversation.id = :conversationId " +
           "AND m.id < :cursor ORDER BY m.id DESC")
    List<MessageEntity> findByConversationIdAndIdLessThanOrderByIdDesc(
        @Param("conversationId") Long conversationId,
        @Param("cursor") Long cursor,
        Pageable pageable);
}

// 정렬 최적화
@Service
public class ConversationService {
    public Page<ConversationResponseDto> findConversations(
            Long memberId, int page, int size, String sortBy) {

        // 인덱스가 있는 컬럼으로만 정렬
        Sort sort = Sort.by(Sort.Direction.DESC,
            sortBy.equals("recent") ? "createdAt" : "updatedAt");

        Pageable pageable = PageRequest.of(page, size, sort);
        return conversationRepository.findByMemberId(memberId, pageable)
            .map(ConversationResponseDto::from);
    }
}
```

### **벌크 연산 최적화**
```java
@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    // 벌크 업데이트 (단일 쿼리로 처리)
    @Modifying
    @Query("UPDATE MemberEntity m SET m.lastLoginAt = :loginTime " +
           "WHERE m.id IN :ids")
    void updateLastLoginTimes(@Param("ids") List<Long> ids,
                             @Param("loginTime") LocalDateTime loginTime);

    // 조건부 벌크 삭제
    @Modifying
    @Query("DELETE FROM DailyCheckRecord d WHERE d.checkDate < :cutoffDate")
    void deleteOldDailyCheckRecords(@Param("cutoffDate") LocalDate cutoffDate);

    // Native Query 사용 (복잡한 업데이트)
    @Modifying
    @Query(value = "UPDATE conversation_table SET updated_at = NOW() " +
                   "WHERE member_id = ?1 AND created_at > ?2",
           nativeQuery = true)
    void updateConversationTimestamp(Long memberId, LocalDateTime since);
}
```

---

## ⚡ 캐싱 전략

### **Redis 캐싱**
```java
@Service
@EnableCaching
public class MemberService {

    // 단순 캐싱
    @Cacheable(value = "members", key = "#id")
    public MemberResponseDto findById(Long id) {
        return memberRepository.findById(id)
            .map(MemberResponseDto::from)
            .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
    }

    // 조건부 캐싱
    @Cacheable(value = "memberStats", key = "#memberId",
               condition = "#memberId > 0", unless = "#result == null")
    public MemberStatsDto getMemberStats(Long memberId) {
        // 복잡한 통계 계산
        return calculateMemberStats(memberId);
    }

    // 캐시 무효화
    @CacheEvict(value = "members", key = "#id")
    public void updateMember(Long id, MemberUpdateRequest request) {
        // 업데이트 로직
    }

    // 캐시 갱신
    @CachePut(value = "members", key = "#result.id")
    public MemberResponseDto saveMember(MemberSaveRequest request) {
        // 저장 로직
        return MemberResponseDto.from(savedMember);
    }
}

// 캐시 설정
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .transactionAware()
            .build();
    }
}
```

### **Application Level 캐싱**
```java
@Component
public class ConversationStatsCache {
    private final Map<Long, ConversationStats> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void initCache() {
        // 5분마다 캐시 갱신
        scheduler.scheduleAtFixedRate(this::refreshCache, 0, 5, TimeUnit.MINUTES);
    }

    public ConversationStats getStats(Long memberId) {
        return cache.computeIfAbsent(memberId, this::calculateStats);
    }

    private void refreshCache() {
        cache.clear();
        // 인기 있는 회원들의 통계 미리 계산
        popularMemberIds.forEach(this::getStats);
    }
}
```

---

## 💾 데이터베이스 최적화

### **인덱스 설계**
```java
// 효과적인 인덱스 설계
@Entity
@Table(name = "conversation_table", indexes = {
    // 단일 컬럼 인덱스
    @Index(name = "idx_member_id", columnList = "member_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),

    // 복합 인덱스 (순서 중요 - 카디널리티 높은 순)
    @Index(name = "idx_member_created", columnList = "member_id, created_at"),

    // 유니크 인덱스
    @Index(name = "idx_member_date_unique", columnList = "member_id, check_date", unique = true),

    // 부분 인덱스 (PostgreSQL)
    @Index(name = "idx_active_conversations", columnList = "member_id",
           condition = "status = 'ACTIVE'")
})
public class ConversationEntity extends BaseTimeEntity {
    // 구현
}

// 쿼리에 맞는 인덱스 설계
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    // idx_member_created 인덱스 활용
    @Query("SELECT c FROM ConversationEntity c " +
           "WHERE c.member.id = :memberId AND c.createdAt >= :startDate " +
           "ORDER BY c.createdAt DESC")
    List<ConversationEntity> findRecentConversations(
        @Param("memberId") Long memberId,
        @Param("startDate") LocalDateTime startDate);
}
```

### **Connection Pool 설정**
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # 최대 연결 수
      minimum-idle: 10             # 최소 유휴 연결 수
      connection-timeout: 30000    # 연결 타임아웃 (30초)
      idle-timeout: 600000         # 유휴 연결 타임아웃 (10분)
      max-lifetime: 1800000        # 연결 최대 생존 시간 (30분)
      leak-detection-threshold: 60000  # 연결 누수 감지 (1분)

  jpa:
    properties:
      hibernate:
        # 배치 처리 최적화
        jdbc:
          batch_size: 25
          batch_versioned_data: true
        order_inserts: true
        order_updates: true

        # 쿼리 플랜 캐시
        query:
          plan_cache_max_size: 2048

        # 통계 수집
        generate_statistics: true
```

---

## 🔄 트랜잭션 최적화

### **트랜잭션 범위 최적화**
```java
@Service
public class OptimizedMemberService {

    // 읽기 전용 트랜잭션
    @Transactional(readOnly = true)
    public MemberResponseDto findById(Long id) {
        return memberRepository.findById(id)
            .map(MemberResponseDto::from)
            .orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));
    }

    // 트랜잭션 분리로 성능 향상
    public void processLargeData(List<MemberSaveRequest> requests) {
        // 검증은 트랜잭션 외부에서
        validateRequests(requests);

        // 실제 저장만 트랜잭션 내부에서
        saveMembersInBatch(requests);
    }

    @Transactional
    private void saveMembersInBatch(List<MemberSaveRequest> requests) {
        List<MemberEntity> entities = requests.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        memberRepository.saveAll(entities);
    }

    // 전파 속성 활용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog(AuditLogRequest request) {
        // 메인 트랜잭션과 독립적으로 실행
        auditLogRepository.save(AuditLogEntity.from(request));
    }
}
```

### **배치 처리 최적화**
```java
@Service
public class BatchProcessingService {

    private static final int BATCH_SIZE = 1000;

    @Transactional
    public void processBulkData(List<DataImportDto> dataList) {
        for (int i = 0; i < dataList.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, dataList.size());
            List<DataImportDto> batch = dataList.subList(i, endIndex);

            processBatch(batch);

            // 메모리 해제
            if (i % (BATCH_SIZE * 10) == 0) {
                entityManager.clear();
            }
        }
    }

    private void processBatch(List<DataImportDto> batch) {
        List<DataEntity> entities = batch.stream()
            .map(DataEntity::from)
            .collect(Collectors.toList());
        dataRepository.saveAll(entities);
    }
}
```

---

## 📡 API 응답 최적화

### **응답 크기 최적화**
```java
// DTO 필드 선택적 포함
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
public class MemberResponseDto {
    private Long id;
    private String memberName;
    private String memberEmail;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ConversationSummaryDto> recentConversations;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static MemberResponseDto from(MemberEntity member) {
        return MemberResponseDto.builder()
            .id(member.getId())
            .memberName(member.getMemberName())
            .memberEmail(member.getMemberEmail())
            .createdAt(member.getCreatedAt())
            .build();
    }

    // 상세 정보 포함 버전
    public static MemberResponseDto fromWithDetails(MemberEntity member) {
        MemberResponseDto dto = from(member);
        dto.recentConversations = member.getConversations().stream()
            .limit(5)
            .map(ConversationSummaryDto::from)
            .collect(Collectors.toList());
        return dto;
    }
}

// 압축 활성화
@Configuration
public class CompressionConfig {
    @Bean
    public EmbeddedServletContainerCustomizer servletContainerCustomizer() {
        return container -> {
            container.setCompression(Compression.ON);
            container.setCompressionMinResponseSize(1024);
        };
    }
}
```

### **비동기 처리**
```java
@Service
public class AsyncProcessingService {

    @Async("taskExecutor")
    public CompletableFuture<Void> sendNotificationAsync(Long memberId, String message) {
        try {
            notificationService.send(memberId, message);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public void processDataAsync(List<DataDto> dataList) {
        dataList.parallelStream()
            .forEach(this::processData);
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
```

---

## 🎯 모니터링 및 성능 측정

### **메트릭 수집**
```java
@Component
public class PerformanceMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter conversationCounter;
    private final Timer conversationTimer;

    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.conversationCounter = Counter.builder("conversation.created")
            .description("Number of conversations created")
            .register(meterRegistry);
        this.conversationTimer = Timer.builder("conversation.processing.time")
            .description("Time spent processing conversations")
            .register(meterRegistry);
    }

    public void recordConversationCreated() {
        conversationCounter.increment();
    }

    public Timer.Sample startConversationTimer() {
        return Timer.start(meterRegistry);
    }
}

@Service
public class ConversationService {
    private final PerformanceMetrics metrics;

    @Transactional
    public ConversationResponseDto processMessage(ConversationRequestDto request) {
        Timer.Sample sample = metrics.startConversationTimer();

        try {
            // 비즈니스 로직
            ConversationResponseDto response = doProcessMessage(request);
            metrics.recordConversationCreated();
            return response;
        } finally {
            sample.stop(metrics.getConversationTimer());
        }
    }
}
```

### **성능 로깅**
```java
@Component
@Slf4j
public class PerformanceLogger {

    @EventListener
    public void handleSlowQuery(SlowQueryEvent event) {
        log.warn("Slow query detected: {} ms - Query: {}",
                event.getExecutionTime(), event.getQuery());
    }

    @Scheduled(fixedRate = 60000) // 1분마다
    public void logPerformanceStats() {
        log.info("Current performance stats - " +
                "Active connections: {}, " +
                "Cache hit ratio: {}, " +
                "Average response time: {} ms",
                getActiveConnections(),
                getCacheHitRatio(),
                getAverageResponseTime());
    }
}

// 느린 쿼리 감지
@Configuration
public class HibernateConfig {
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (hibernateProperties) -> {
            hibernateProperties.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", 1000);
            hibernateProperties.put("hibernate.stats.fetch.profile", "true");
        };
    }
}
```

---

## 📋 성능 최적화 체크리스트

### **데이터베이스 최적화**
- [ ] N+1 쿼리 방지 (@Query, @EntityGraph 사용)
- [ ] 적절한 FetchType 설정 (LAZY 기본)
- [ ] 인덱스 설계 (조회 패턴 분석)
- [ ] 벌크 연산 활용 (대량 데이터 처리)
- [ ] Connection Pool 튜닝

### **캐싱 전략**
- [ ] Redis 캐싱 적용 (자주 조회되는 데이터)
- [ ] 캐시 TTL 설정 (데이터 특성에 맞게)
- [ ] 캐시 무효화 전략 수립
- [ ] 캐시 히트율 모니터링

### **API 성능**
- [ ] 응답 크기 최적화 (필요한 필드만 포함)
- [ ] 페이징 구현 (대량 데이터 조회)
- [ ] 비동기 처리 활용 (시간 소요 작업)
- [ ] 응답 압축 활성화

### **모니터링**
- [ ] 성능 메트릭 수집 (Micrometer)
- [ ] 느린 쿼리 감지 및 로깅
- [ ] 리소스 사용량 모니터링
- [ ] 응답 시간 추적

---

**Version**: v1.0.0 | **Updated**: 2025-09-16