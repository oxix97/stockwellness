# QA Result — stockwellness (Backend)

> 검사 일시: 2026-03-23
> 전체 판정: **REQUEST CHANGES**

---

## 심각도 기준

- 🔴 BLOCKER: 머지 전 반드시 수정
- 🟡 MAJOR: 강력 권고 수정
- 🟢 MINOR: 선택적 개선

---

## 검사 항목별 결과

### 1. Build & Compile — PASS

전체 빌드 (`./gradlew clean build`) 성공. 컴파일 에러 없음.

---

### 2. 테스트 실행 — PASS

| 모듈 | 테스트 수 | 결과 |
|------|----------|------|
| `stockwellness-core` | 126개 | 전원 통과 |
| `stockwellness-api` | 45개 | 전원 통과 |
| `stockwellness-batch` | 16개 | 전원 통과 |
| **합계** | **187개** | **전원 통과** |

---

### 3. 테스트 커버리지 — FAIL 🟡 MAJOR

JaCoCo 측정 결과, 세 모듈 모두 목표치(80%)에 미달.

| 모듈 | 커버리지 | 목표 | 판정 |
|------|----------|------|------|
| `stockwellness-core` | **44.2%** | 80% | ❌ |
| `stockwellness-api` | **59.5%** | 80% | ❌ |
| `stockwellness-batch` | **53.5%** | 80% | ❌ |

> 🟡 MAJOR: 세 모듈 모두 80% 기준 미달. 특히 도메인 로직을 담당하는 `core`의 44.2%는 심각한 수준. 금융 계산 로직(MDD, Sharpe, Beta, 백테스트)에 대한 단위 테스트 추가 필요.

---

### 4. 아키텍처 — 모듈 의존성 방향 — PASS

`core` 모듈에서 `api`·`batch` 모듈로의 import 없음. 헥사고날 아키텍처 의존성 방향 준수.

---

### 5. 코드 품질 — FQCN 인라인 사용 — PASS

`java.util.List` 등 FQCN 인라인 사용 없음. 모든 타입이 파일 상단 import로 처리됨.

---

### 6. 코드 품질 — Raw RuntimeException — FAIL 🟡 MAJOR

`GlobalException(ErrorCode.XYZ)` 대신 원시 예외를 직접 throw하는 코드 5건 발견:

| 파일 | 라인 | 내용 |
|------|------|------|
| `stockwellness-core/.../kis/adapter/KisTokenAdapter.java` | 68 | `throw new RuntimeException(...)` |
| `stockwellness-core/.../kis/client/KisMasterClient.java` | 57 | `throw new RuntimeException(...)` |
| `stockwellness-core/.../kis/converter/LeadingStocksConverter.java` | 29 | `throw new RuntimeException(...)` |
| `stockwellness-core/.../kis/converter/LeadingStocksConverter.java` | 42 | `throw new RuntimeException(...)` |
| `stockwellness-core/.../domain/stock/MarketIndex.java` | 31 | `throw new RuntimeException(...)` |

> 🟡 MAJOR: 위 5곳 모두 `GlobalException(ErrorCode.XYZ)` 패턴으로 교체 필요. 원시 예외는 `GlobalExceptionHandler`에서 포착되지 않아 표준 에러 응답 형식(`{ status, code, message, traceId }`)이 보장되지 않음.

---

### 7. API 계약 — 성공/에러 응답 형식 — PASS (WARN)

성공 응답: `{ "data": {...}, "timestamp": "..." }` 형식 준수됨. `global.common.response.ApiResponse` 래퍼가 통일되어 사용 중.

에러 응답: `GlobalException` → `GlobalExceptionHandler` → `{ status, code, message, traceId }` 흐름 확인됨.

단, **레거시 `global.common.ApiResponse`** (구형 단순 포맷)가 `global.common.response.ApiResponse` (통합 포맷)와 공존. 현재 활성 사용은 없으나 코드 혼동 유발 가능.

> 🟢 MINOR: 미사용 `global.common.ApiResponse` 레거시 클래스 제거 권장.

---

### 8. ErrorCode 프리픽스 — WARN 🟢 MINOR

ErrorCode enum에 `W*`(Watchlist), `T*`(Sector/Technical) 프리픽스가 실제로 사용되고 있으나 CLAUDE.md 명세(`A*`, `M*`, `P*`, `S*`, `B*`)에 문서화되지 않음.

> 🟢 MINOR: CLAUDE.md 에러코드 prefix 문서에 `W*`, `T*` 추가 필요.

---

### 9. 보안 — 하드코딩 시크릿 — PASS

소스 코드/로그에 하드코딩된 시크릿·인증 정보 없음. `.env` 및 환경 변수로 외부화되어 있음.

---

### 10. 입력값 검증 — PASS

컨트롤러 요청 파라미터에 `@Valid`, `@NotNull`, `@NotBlank`, `@RequestBody` 총 16건 사용 확인. API 경계 입력 검증 존재.

---

### 11. GlobalException 사용 패턴 — PASS

`GlobalException` 참조 총 65건. 도메인 및 서비스 레이어 전반에서 일관되게 사용됨.
단, 위 항목 6에서 발견된 KIS 어댑터/도메인 5곳은 미적용 상태.

---

### 12. 금융 도메인 테스트 — PASS

핵심 금융 계산 테스트 존재 확인:
- `BacktestEngineTest` — 백테스트 시뮬레이션 검증
- `PortfolioStatCalculatorTest` — MDD, Sharpe, Beta 계산 검증
- 기타 포트폴리오 도메인 테스트 존재

---

## 종합 결과

| # | 검사 항목 | 결과 | 심각도 |
|---|----------|------|--------|
| 1 | Build & Compile | PASS | — |
| 2 | 테스트 실행 (187개) | PASS | — |
| 3 | 테스트 커버리지 | **FAIL** | 🟡 MAJOR |
| 4 | 모듈 의존성 방향 | PASS | — |
| 5 | FQCN 인라인 사용 | PASS | — |
| 6 | Raw RuntimeException | **FAIL** | 🟡 MAJOR |
| 7 | API 응답 형식 | PASS (WARN) | 🟢 MINOR |
| 8 | ErrorCode 프리픽스 | WARN | 🟢 MINOR |
| 9 | 보안 (시크릿) | PASS | — |
| 10 | 입력값 검증 | PASS | — |
| 11 | GlobalException 패턴 | PASS | — |
| 12 | 금융 도메인 테스트 | PASS | — |

---

## 수정 필요 항목 (우선순위 순)

### 🟡 MAJOR (2건)

**1. Raw RuntimeException 5건 → GlobalException 교체**

| 파일 | 라인 |
|------|------|
| `KisTokenAdapter.java` | 68 |
| `KisMasterClient.java` | 57 |
| `LeadingStocksConverter.java` | 29, 42 |
| `MarketIndex.java` | 31 |

각 위치에 적절한 `ErrorCode`를 정의한 후 `throw new GlobalException(ErrorCode.XYZ)`로 교체.

**2. 테스트 커버리지 80% 미달**

- `core`: 44.2% → 우선 60% 이상 목표, 이후 80% 달성
- 금융 계산 로직(백테스트, MDD, Sharpe, Beta) 엣지케이스 테스트 집중 보강
- KIS 어댑터, Redis 캐시 어댑터 테스트 추가

### 🟢 MINOR (2건)

**3. 레거시 `global.common.ApiResponse` 클래스 제거**

**4. CLAUDE.md 에러코드 prefix 표에 `W*`(Watchlist), `T*`(Sector) 추가**
