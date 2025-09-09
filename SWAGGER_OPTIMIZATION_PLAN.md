# Swagger 최적화 플랜

## 📋 개요
MARUNI 프로젝트의 Swagger 관련 로직을 단순하고 실용적으로 최적화하는 계획입니다.
복잡한 패턴 도입 없이 기존 구조를 유지하면서 성능과 가독성을 개선합니다.

## 🎯 최적화 목표
- **성능 개선**: 캐싱을 통한 30-50% 응답시간 단축
- **코드 품질**: 중복 코드 50% 감소
- **유지보수성**: 하드코딩 제거로 확장성 향상
- **메모리 효율**: 중복 객체 생성 방지

## 📊 현재 문제점 분석

### 1. 코드 중복
```java
// 현재: 두 개의 유사한 메소드 존재
private Example getSwaggerExample(ErrorCode errorCode) { ... }
private Example getSuccessSwaggerExample(SuccessCode successCode) { ... }
```

### 2. 성능 비효율
- 매 요청마다 동일한 예제 객체 재생성
- Stream API로 인한 불필요한 연산 반복

### 3. 하드코딩 문제
```java
// SwaggerConfig.java:133-136
if (Objects.equals(errorCode.getCode(), ErrorCode.PARAMETER_VALIDATION_ERROR.getCode())) {
    ParameterData parameterData = new ParameterData("memberEmail", "invalid-email", "이메일 형식이 올바르지 않습니다");
    details = List.of(parameterData);
}
```

### 4. 불필요한 복잡성
- `SwaggerResponseDescription.ALL_ERROR`의 중복 정의
- 과도한 enum 값 중복

## 🚀 최적화 플랜

### Phase 1: 기본 리팩토링 (45분)

#### 1.1 Example 생성 메소드 통합
**파일**: `SwaggerConfig.java`

**Before**:
```java
private Example getSwaggerExample(ErrorCode errorCode) { ... }
private Example getSuccessSwaggerExample(SuccessCode successCode) { ... }
```

**After**:
```java
private <T> Example createExample(T type, String description, Object details) {
    CommonApiResponse<?> response;
    if (type instanceof ErrorCode) {
        response = details != null 
            ? CommonApiResponse.failWithDetails((ErrorCode) type, details)
            : CommonApiResponse.fail((ErrorCode) type);
    } else {
        response = CommonApiResponse.success((SuccessCode) type);
    }
    
    Example example = new Example();
    example.description(description);
    example.setValue(response);
    return example;
}
```

#### 1.2 하드코딩 제거
**파일**: `SwaggerConfig.java`

**Before**:
```java
// 하드코딩된 ParameterData 생성
if (Objects.equals(errorCode.getCode(), ErrorCode.PARAMETER_VALIDATION_ERROR.getCode())) {
    ParameterData parameterData = new ParameterData("memberEmail", "invalid-email", "이메일 형식이 올바르지 않습니다");
    details = List.of(parameterData);
}
```

**After**:
```java
private Object getErrorDetails(ErrorCode errorCode) {
    if (errorCode == ErrorCode.PARAMETER_VALIDATION_ERROR) {
        return List.of(new ParameterData("field", "invalid-value", "검증 실패 예시"));
    }
    return null;
}
```

#### 1.3 기존 메소드들을 새로운 통합 메소드로 대체
**Before**:
```java
private Example getSwaggerExample(ErrorCode errorCode) { ... }
private Example getSuccessSwaggerExample(SuccessCode successCode) { ... }
```

**After**:
```java
private Example getSwaggerExample(ErrorCode errorCode) {
    Object details = getErrorDetails(errorCode);
    return createExample(errorCode, errorCode.getMessage(), details);
}

private Example getSuccessSwaggerExample(SuccessCode successCode) {
    return createExample(successCode, successCode.getMessage(), null);
}
```

### Phase 2: 코드 정리 (30분)

#### 2.1 SwaggerResponseDescription 정리
**파일**: `SwaggerResponseDescription.java`

**Before**:
```java
ALL_ERROR(Set.of(
    ErrorCode.MEMBER_NOT_FOUND,
    ErrorCode.DUPLICATE_EMAIL,  // 중복
    ErrorCode.INVALID_TOKEN,    // 중복
    ...
))
```

**After**:
```java
public enum SwaggerResponseDescription {
    MEMBER_ERROR(ErrorCode.MEMBER_NOT_FOUND, ErrorCode.DUPLICATE_EMAIL),
    MEMBER_JOIN_ERROR(ErrorCode.DUPLICATE_EMAIL, ErrorCode.PARAMETER_VALIDATION_ERROR, ErrorCode.INVALID_INPUT_VALUE),
    AUTH_ERROR(ErrorCode.INVALID_TOKEN, ErrorCode.REFRESH_TOKEN_NOT_FOUND, ErrorCode.LOGIN_FAIL),
    COMMON_ERROR(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INVALID_INPUT_VALUE);
    // ALL_ERROR 제거
    
    private final Set<ErrorCode> errorCodeList;
    
    SwaggerResponseDescription(ErrorCode... errorCodes) {
        this.errorCodeList = Set.of(errorCodes);
    }
}
```

#### 2.2 메소드명 개선
**파일**: `SwaggerConfig.java`

```java
// Before
private void generateErrorCodeResponseExample(Operation operation, SwaggerResponseDescription type)
private void generateSuccessResponseExample(Operation operation, SuccessCode successCode)

// After  
private void addErrorExamples(Operation operation, SwaggerResponseDescription type)
private void addSuccessExample(Operation operation, SuccessCode successCode)
```

## 📋 구현 체크리스트

### Phase 1: 기본 리팩토링 ✅
- [ ] `createExample()` 통합 메소드 구현
- [ ] `getErrorDetails()` 메소드 분리
- [ ] 기존 메소드들을 통합 메소드 사용하도록 수정
- [ ] 단위 테스트 실행 및 검증

### Phase 2: 코드 정리 ✅
- [ ] `SwaggerResponseDescription` enum 정리
- [ ] `ALL_ERROR` 제거 및 관련 코드 수정
- [ ] 메소드명 일관성 개선
- [ ] 코드 리뷰 및 문서화

## 🧪 테스트 계획

### 단위 테스트
```java
@Test
void should_create_error_example_with_details() {
    // Given
    SwaggerConfig config = new SwaggerConfig();
    
    // When
    Example example = config.getSwaggerExample(ErrorCode.PARAMETER_VALIDATION_ERROR);
    
    // Then
    assertThat(example.getDescription()).isEqualTo("파라미터 검증에 실패했습니다");
    assertThat(example.getValue()).isInstanceOf(CommonApiResponse.class);
}

@Test
void should_create_success_example() {
    // Given
    SwaggerConfig config = new SwaggerConfig();
    
    // When
    Example example = config.getSuccessSwaggerExample(SuccessCode.SUCCESS);
    
    // Then
    assertThat(example.getDescription()).isEqualTo("성공");
    assertThat(example.getValue()).isInstanceOf(CommonApiResponse.class);
}
```

## 📈 예상 개선 효과

### 코드 품질 메트릭
- **코드 중복**: 50% 감소 (중복 메소드 통합)
- **순환 복잡도**: 30% 감소 (로직 단순화)
- **가독성 점수**: 향상 (명확한 메소드 분리)
- **유지보수성**: 향상 (하드코딩 제거)

## 🔄 롤백 계획

각 Phase별로 Git 커밋을 분리하여 문제 발생시 단계별 롤백 가능:

1. **Phase 1 롤백**: 기본 리팩토링만 되돌리기
2. **Phase 2 롤백**: 캐싱 로직 제거
3. **Phase 3 롤백**: enum 정리 되돌리기

## 📚 참고사항

### 주의사항
- 기존 어노테이션 사용법 변경 없음 (`@CustomExceptionDescription`, `@SuccessResponseDescription`)
- API 응답 형식 변경 없음
- 하위 호환성 완전 보장

### 확장 가능성
- 향후 추가 ErrorCode에 대한 특별 케이스 처리 용이
- 캐싱 전략 확장 가능 (TTL, 크기 제한 등)
- 성능 모니터링 메트릭 추가 가능

---

**작성일**: 2025-09-09  
**작성자**: Claude Code  
**예상 소요시간**: 총 1시간 15분 (캐싱 제거로 단축)  
**우선순위**: Medium

---

## 📝 변경 사항 (2025-09-09)
- **캐싱 로직 제거**: 애플리케이션 시작시에만 실행되는 로직이므로 캐싱이 불필요
- **Phase 2 단순화**: 성능 최적화 → 코드 정리로 변경
- **소요시간 단축**: 2시간 → 1시간 15분