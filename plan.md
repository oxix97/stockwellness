# QA Plan — stockwellness (Backend)

> 작성일: 2026-03-23

---

## 목표

Spring Boot 멀티모듈 백엔드의 코드 품질, 아키텍처 정합성, 테스트 커버리지, API 계약 준수를 검증한다.

---

## QA 범위

### 모듈 구성
| 모듈 | 역할 |
|---|---|
| `stockwellness-core` | 도메인 + 비즈니스 로직 + 영속성 어댑터 |
| `stockwellness-api` | REST API 컨트롤러 어댑터 |
| `stockwellness-batch` | 배치/스케줄링 어댑터 |

---

## 검사 항목 및 순서

### 1. 빌드 & 컴파일 검증
- [ ] `./gradlew clean build` 전체 빌드 통과 여부
- [ ] 컴파일 에러/경고 존재 여부

### 2. 테스트 실행 & 커버리지
- [ ] `./gradlew :stockwellness-core:test` 실행 및 결과 확인
- [ ] `./gradlew :stockwellness-api:test` 실행 및 결과 확인
- [ ] `./gradlew :stockwellness-batch:test` 실행 및 결과 확인
- [ ] JaCoCo 커버리지 목표: **> 80%** 달성 여부

### 3. 아키텍처 정합성
- [ ] 모듈 의존성 방향 위반 (`core` → `api`/`batch` 방향 의존 금지)
- [ ] 비즈니스 로직이 컨트롤러/배치 스텝에 누출되지 않았는지
- [ ] Hexagonal 패턴 준수 (Port interface 존재, Adapter 구현)

### 4. 코드 품질 안티패턴
- [ ] FQCN 인라인 사용 금지 (`java.util.List` 등 직접 사용 여부 grep)
- [ ] 원시 `RuntimeException` 직접 throw 금지 → `GlobalException(ErrorCode.XYZ)` 사용
- [ ] 코드 중복 — 유사 기능이 기존에 이미 존재하는지

### 5. API 계약 준수
- [ ] 성공 응답: `{ "data": {...}, "timestamp": "..." }` 형식 준수
- [ ] 에러 응답: `{ "status", "code", "message", "traceId" }` 형식 준수
- [ ] 에러코드 prefix 올바른지: `A*` 인증 / `M*` 회원 / `P*` 포트폴리오 / `S*` 주식 / `B*` 배치

### 6. 보안 점검
- [ ] 코드/로그에 시크릿·인증 정보 하드코딩 없음
- [ ] API 경계에서 입력값 검증 (`@Valid`, `@NotNull` 등) 존재
- [ ] SQL Injection 취약점 없음 (QueryDSL 파라미터 바인딩 사용)

### 7. 금융 도메인 로직 검증
- [ ] 백테스트 계산 로직 (MDD, CAGR, 수익률) 단위 테스트 존재
- [ ] Sharpe Ratio, Beta 계산 테스트 존재
- [ ] 0원 투자금, 음수 수익률, 빈 포트폴리오 엣지케이스 처리

### 8. 인프라 연동 패턴
- [ ] Redis 캐싱 어댑터 — 계층적 캐시 전략 구현 확인
- [ ] Kafka Transactional Outbox — DB 저장 + 이벤트 발행 원자성
- [ ] KIS API 클라이언트 — HTTP 에러 처리 및 재시도 로직

---

## 체크리스트 (CLAUDE.md 기준)

```
공통 안티패턴
- [ ] FQCN 인라인 사용 여부
- [ ] 오류 처리 근본 원인 추적 여부
- [ ] 코드 중복 여부
- [ ] 변수/함수명 명확성

아키텍처
- [ ] 모듈 의존성 방향 위반 없음
- [ ] 비즈니스 로직 컨트롤러 누출 없음
- [ ] GlobalException 사용

API 계약
- [ ] 성공/에러 응답 형식 준수
- [ ] 에러코드 prefix 올바름

보안
- [ ] 시크릿 하드코딩 없음
- [ ] API 입력값 검증 존재

QA
- [ ] 테스트 커버리지 > 80%
- [ ] 빈 상태/에러 상태/로딩 상태 처리
```

---

## 결과물

- `stockwellness/result.md` — 각 체크 항목의 통과/실패 및 발견된 이슈 목록
