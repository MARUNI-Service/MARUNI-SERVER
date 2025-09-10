# Auth 도메인 개선 플랜

## 📊 현상 분석

### 발견된 문제점

#### 1. 도메인 경계 위반 ⚠️
- **위치**: `domain/vo/MemberTokenInfo.java:36`
- **문제**: Auth 도메인이 Member 도메인의 `MemberEntity`에 직접 의존
- **영향도**: 높음 - 도메인 간 강결합 발생

```java
// 문제가 되는 코드
public static MemberTokenInfo from(MemberEntity member) {
    return new MemberTokenInfo(
        member.getId().toString(),
        member.getMemberEmail()  // Member 도메인 직접 참조
    );
}
```

#### 2. 설정값 하드코딩 🔧
- **위치**: `domain/entity/RefreshToken.java:15`
- **문제**: TTL 값이 하드코딩되어 설정 변경 시 코드 수정 필요
- **영향도**: 중간 - 운영 유연성 저하

```java
@RedisHash(value = "refreshToken", timeToLive = 1209600) // 14일 하드코딩
```

#### 3. Infrastructure 계층 직접 의존 🏗️
- **위치**: `domain/service/TokenValidator.java:7`
- **문제**: 도메인 서비스가 Infrastructure 구현체에 직접 의존
- **영향도**: 중간 - 테스트 어려움, 의존성 역전 원칙 위반

## 🎯 개선 목표

### 1. 도메인 순수성 확보
- 각 도메인의 독립성 보장
- 도메인 간 의존성 최소화
- Clean Architecture 원칙 준수

### 2. 설정 관리 개선
- 환경별 설정 분리
- 중앙화된 JWT 설정 관리
- 런타임 설정 변경 지원

### 3. 테스트 용이성 향상
- 의존성 주입을 통한 Mock 테스트 지원
- 격리된 단위 테스트 환경 구축

## 📋 개선 로드맵

### Phase 1: 도메인 경계 위반 해결 (우선순위: 🔥 높음)

#### 작업 내용
1. `MemberTokenInfo.from()` 메소드 제거
2. Application Service에서 매핑 로직 처리
3. Member 도메인 의존성 완전 제거

#### 변경 대상 파일
- `domain/vo/MemberTokenInfo.java` - `from()` 메소드 제거
- `application/service/AuthenticationService.java` - 매핑 로직 추가 (필요 시)

#### 구현 예시
```java
// Before - 도메인 경계 위반
public static MemberTokenInfo from(MemberEntity member) {
    return new MemberTokenInfo(member.getId().toString(), member.getMemberEmail());
}

// After - Application Service에서 처리
public void issueTokensOnLogin(HttpServletResponse response, Long memberId, String email) {
    MemberTokenInfo tokenInfo = MemberTokenInfo.of(memberId, email);
    tokenService.issueTokens(response, tokenInfo);
}
```

#### 검증 기준
- [ ] `MemberTokenInfo`에서 `MemberEntity` import 완전 제거
- [ ] 컴파일 에러 없음
- [ ] 기존 토큰 발급 기능 정상 동작
- [ ] 도메인 의존성 그래프에서 Auth → Member 의존성 제거 확인

### Phase 2: JWT 설정 외부화 (우선순위: ⚡ 중간)

#### 작업 내용
1. JWT 관련 설정을 `application.yml`로 이동
2. `@ConfigurationProperties`를 활용한 설정 클래스 생성
3. RefreshToken 엔티티에서 동적 TTL 설정

#### 새로 생성할 파일
- `global/config/JwtProperties.java` - JWT 설정 프로퍼티 클래스

```java
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private AccessToken accessToken = new AccessToken();
    private RefreshToken refreshToken = new RefreshToken();
    
    @Data
    public static class AccessToken {
        private long expiration = 3600000; // 1시간 (기본값)
    }
    
    @Data
    public static class RefreshToken {
        private long expiration = 1209600000; // 14일 (기본값)
    }
}
```

#### 변경 대상 파일
- `src/main/resources/application.yml` - JWT 설정 추가

```yaml
jwt:
  access-token:
    expiration: ${JWT_ACCESS_EXPIRATION:3600000}  # 1시간
  refresh-token:
    expiration: ${JWT_REFRESH_EXPIRATION:1209600000}  # 14일
```

- `domain/entity/RefreshToken.java` - 동적 TTL 적용

```java
@RedisHash(value = "refreshToken")
public class RefreshToken {
    @TimeToLive
    private Long ttl;
    
    // TTL을 매개변수로 받는 생성자 추가
    public RefreshToken(String memberId, String token, Long ttl) {
        this.memberId = memberId;
        this.token = token;
        this.ttl = ttl;
    }
}
```

#### 검증 기준
- [ ] `application.yml`에 JWT 설정 추가됨
- [ ] 환경 변수를 통한 설정 변경 가능
- [ ] RefreshToken TTL이 설정값에 따라 동적으로 설정됨
- [ ] 기존 토큰 만료 로직 정상 동작

### Phase 3: Infrastructure 인터페이스 분리 (우선순위: 🔧 낮음)

#### 작업 내용
1. 도메인 계층에 `TokenBlacklistRepository` 인터페이스 생성
2. Infrastructure 계층에서 인터페이스 구현
3. `TokenValidator`에서 인터페이스 의존으로 변경

#### 새로 생성할 파일
- `domain/repository/TokenBlacklistRepository.java`

```java
/**
 * 토큰 블랙리스트 관리를 위한 도메인 인터페이스
 * Infrastructure 구현체와 도메인 로직을 분리
 */
public interface TokenBlacklistRepository {
    /**
     * Access Token을 블랙리스트에 추가
     * @param accessToken 블랙리스트에 추가할 토큰
     * @param expirationMillis 토큰 만료까지 남은 시간 (밀리초)
     */
    void addToBlacklist(String accessToken, long expirationMillis);
    
    /**
     * Access Token이 블랙리스트에 존재하는지 확인
     * @param accessToken 확인할 토큰
     * @return 블랙리스트 존재 여부
     */
    boolean isTokenBlacklisted(String accessToken);
}
```

#### 변경 대상 파일
- `domain/service/TokenValidator.java`

```java
@Component
public class TokenValidator {
    private final TokenBlacklistRepository tokenBlacklistRepository; // 인터페이스 의존
    
    public boolean isValidAccessToken(String accessToken) {
        if (!tokenManager.isAccessToken(accessToken)) {
            return false;
        }
        return !tokenBlacklistRepository.isTokenBlacklisted(accessToken);
    }
}
```

- `infrastructure/BlacklistTokenStorage.java`

```java
@Component
public class BlacklistTokenStorage implements TokenBlacklistRepository {
    // 기존 구현 유지, 인터페이스 구현만 추가
}
```

#### 검증 기준
- [ ] 도메인 계층에 Infrastructure 직접 의존성 없음
- [ ] `TokenValidator` 단위 테스트에서 Mock 객체 사용 가능
- [ ] 블랙리스트 기능 정상 동작
- [ ] 의존성 주입 정상 작동

## 🚀 구현 일정

### 1주차: Phase 1 (도메인 경계 위반 해결)
- **월요일**: 현재 의존성 분석 및 영향도 파악
- **화요일**: `MemberTokenInfo.from()` 메소드 제거
- **수요일**: Application Service 매핑 로직 구현
- **목요일**: 단위 테스트 및 통합 테스트
- **금요일**: 코드 리뷰 및 피드백 반영

### 2주차: Phase 2 (설정 외부화)
- **월요일**: `JwtProperties` 클래스 설계 및 구현
- **화요일**: `application.yml` 설정 추가
- **수요일**: `RefreshToken` 동적 TTL 적용
- **목요일**: 환경별 설정 테스트
- **금요일**: 성능 테스트 및 검증

### 3주차: Phase 3 (인터페이스 분리)
- **월요일**: `TokenBlacklistRepository` 인터페이스 설계
- **화요일**: 인터페이스 구현 및 의존성 주입 설정
- **수요일**: `TokenValidator` 리팩토링
- **목요일**: Mock 테스트 작성
- **금요일**: 전체 시스템 통합 테스트

## 📈 예상 효과

### 도메인 순수성 확보
- **독립성**: 각 도메인이 독립적으로 발전 가능
- **재사용성**: Auth 도메인을 다른 프로젝트에서 재사용 가능
- **유지보수성**: 도메인별 변경이 다른 도메인에 미치는 영향 최소화

### 설정 관리 개선
- **유연성**: 환경별(개발/스테이징/운영) 토큰 만료시간 설정 가능
- **운영편의성**: 코드 변경 없이 설정 조정 가능
- **표준화**: JWT 관련 설정의 중앙 관리

### 테스트 용이성 향상
- **격리 테스트**: 외부 의존성 없는 순수한 도메인 로직 테스트
- **Mock 테스트**: Infrastructure 계층 Mock을 통한 빠른 테스트
- **신뢰성**: 더 안정적이고 예측 가능한 테스트 환경

## 🔍 위험도 분석

### 높은 위험도
- **Phase 1**: 기존 토큰 발급 로직 변경으로 인한 부작용 가능성
  - **완화방안**: 충분한 테스트 케이스 작성, 단계적 배포

### 중간 위험도
- **Phase 2**: 설정 변경으로 인한 기존 토큰 호환성 이슈
  - **완화방안**: 기본값 설정, 하위 호환성 유지

### 낮은 위험도
- **Phase 3**: 인터페이스 분리는 내부 구조 변경으로 외부 영향 최소
  - **완화방안**: 기존 기능 유지하면서 인터페이스만 추가

## 📋 체크리스트

### Phase 1 완료 기준
- [ ] `MemberEntity` import 완전 제거
- [ ] 컴파일 에러 해결
- [ ] 기존 API 정상 동작
- [ ] 단위 테스트 통과
- [ ] 통합 테스트 통과

### Phase 2 완료 기준
- [ ] JWT 설정 외부화 완료
- [ ] 환경 변수 테스트
- [ ] TTL 동적 설정 확인
- [ ] 성능 영향 없음 확인

### Phase 3 완료 기준
- [ ] 도메인 인터페이스 생성
- [ ] Infrastructure 구현체 분리
- [ ] Mock 테스트 작성
- [ ] 전체 기능 정상 동작

## 📚 참고 자료

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties)

---

**문서 작성일**: 2025-09-10  
**작성자**: Claude Code  
**버전**: 1.0