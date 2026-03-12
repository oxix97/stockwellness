# API 표준 응답 및 에러 처리 규격

본 문서는 Stockwellness 프로젝트의 API 응답 포맷과 전역 예외 처리 메커니즘에 대한 표준 규격을 정의합니다.

## 1. 공통 응답 구조

모든 API 응답은 일관된 JSON 래퍼를 사용합니다.

### 1.1. 성공 응답 (`ApiResponse<T>`)

성공 시 `200 OK` (또는 `201 Created` 등) 상태 코드와 함께 다음 구조를 반환합니다.

```json
{
  "data": { ... },
  "timestamp": "2026-03-12T17:00:00.000000"
}
```

- **data**: 실제 응답 데이터 (객체, 리스트 또는 null)
- **timestamp**: 서버 응답 생성 일시

### 1.2. 에러 응답 (`ErrorResponse`)

에러 발생 시 해당 HTTP 상태 코드와 함께 다음 구조를 반환합니다.

```json
{
  "status": 400,
  "code": "G001",
  "message": "잘못된 입력값입니다.",
  "timestamp": "2026-03-12T17:00:00.000000",
  "traceId": "e4e4d65f",
  "errors": [
    {
      "field": "name",
      "value": "",
      "reason": "공백일 수 없습니다."
    }
  ]
}
```

- **status**: HTTP 상태 코드
- **code**: 서비스 내부 관리용 에러 코드 (기계 판독 가능)
- **message**: 사용자 또는 개발자용 에러 메시지
- **timestamp**: 에러 발생 일시
- **traceId**: 로그 추적을 위한 고유 ID (앞 8자리)
- **errors**: 상세 필드 에러 목록 (주로 입력값 검증 실패 시 사용)

## 2. 에러 코드 체계

에러 코드는 다음과 같은 규칙으로 명명됩니다.

- **G***: 공통 에러 (Common)
- **A***: 인증/인가 에러 (Auth)
- **M***: 회원 관련 에러 (Member)
- **P***: 포트폴리오 관련 에러 (Portfolio)
- **S***: 주식/섹터 관련 에러 (Stock/Sector)
- **B***: 배치 작업 관련 에러 (Batch)

상세 코드는 `org.stockwellness.global.error.ErrorCode` Enum을 참조하십시오.

## 3. 예외 처리 가이드 (Back-end)

### 3.1. 비즈니스 예외 던지기

비즈니스 로직 수준에서 예외를 던질 때는 `BusinessException`을 상속받은 구체 예외 또는 `GlobalException`을 사용하십시오.

```java
if (portfolio == null) {
    throw new GlobalException(ErrorCode.PORTFOLIO_NOT_FOUND);
}
```

### 3.2. 전역 핸들러 로직

`GlobalExceptionHandler`는 Java 21의 `switch pattern matching`을 사용하여 예외 타입을 분류하고 표준 `ErrorResponse`로 변환합니다. 새로운 공통 예외가 추가될 경우 핸들러의 `switch` 문에 케이스를 추가하십시오.
