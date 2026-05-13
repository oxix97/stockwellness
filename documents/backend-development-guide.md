# Backend Development Guide

## 목적
이 문서는 `stockwellness` 백엔드 개발자가 기능 추가, 버그 수정, 성능 점검을 시작할 때 바로 참고할 수 있는 작업 가이드다. 아키텍처 원칙 자체는 [architecture.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/docs/architecture.md), 테스트 규칙은 [testing.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/docs/testing.md), 코드 스타일은 [code-style.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/docs/code-style.md)를 기준으로 한다.

## 먼저 이해해야 할 것
이 프로젝트는 멀티 모듈 구조다.

- `stockwellness-core`
  도메인, 애플리케이션 서비스, 포트, 영속성/외부 연동 어댑터의 중심이다.
- `stockwellness-api`
  REST 컨트롤러, 인증/보안, 웹 어댑터를 담당한다.
- `stockwellness-batch`
  EOD 수집, 기술 지표 계산, 배치 스텝을 담당한다.

작업 기준은 단순하다.

- 비즈니스 규칙이 바뀌면 `core`
- HTTP 입력/출력이나 인증 흐름이 바뀌면 `api`
- 데이터 수집/스케줄/지표 계산이 바뀌면 `batch`

## 로컬 개발 시작 순서
### 1. 인프라 실행
```bash
cd /Users/chan/Desktop/gongbu/stockwellness-project/stockwellness
docker compose up -d
```

### 2. 환경변수 확인
루트 `.env`에 최소 아래 값이 필요하다.

```bash
DB_URL=
DB_USERNAME=
DB_PASSWORD=
REDIS_HOST=
KAFKA_BOOTSTRAP_SERVERS=
JWT_SECRET=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
OPENAI_API_KEY=
KIS_APP_KEY=
KIS_APP_SECRET=
```

### 3. 서버 실행
```bash
./gradlew :stockwellness-api:bootRun
./gradlew :stockwellness-batch:bootRun
```

### 4. 기본 검증
```bash
./gradlew :stockwellness-api:test
./gradlew :stockwellness-core:test
./gradlew :stockwellness-batch:test
```

## 기능 개발 기본 흐름
### API 추가
1. `core`에 UseCase 또는 Facade 진입점 정의
2. `core` 서비스에서 도메인 규칙 구현
3. 필요하면 `adapter/out`에 Port 구현 추가
4. `api` 컨트롤러에서 Request -> Command 변환
5. REST Docs 테스트 작성

### 조회 API 수정
1. QueryDSL 또는 Repository 조회 경로 확인
2. N+1 여부 확인
3. 캐시 대상인지 검토
4. 응답 DTO가 프론트 계약과 맞는지 확인
5. k6 또는 최소 MockMvc/통합 테스트로 회귀 확인

### 배치 로직 수정
1. 배치 Step/Reader/Processor/Writer 경계 확인
2. 외부 KIS 호출, 재시도, 타임아웃 값 점검
3. 저장 단위와 트랜잭션 범위 확인
4. 기존 데이터 적재 및 중복 처리 방식 확인
5. 배치 통합 테스트로 검증

## 변경 전 체크리스트
- 어느 모듈 책임인지 명확한가
- Controller가 Service를 직접 주입하지 않는가
- 도메인 규칙이 Controller나 DTO에 새지 않는가
- ErrorCode와 예외 처리 방식이 기존 규칙과 일치하는가
- QueryDSL 조회가 count 쿼리, fetch join, slice 처리 측면에서 적절한가
- 모바일/프론트 계약에 영향이 있으면 OpenAPI 또는 REST Docs가 갱신되는가

## 테스트 전략
### 가장 먼저 돌릴 것
```bash
./gradlew :stockwellness-core:test
./gradlew :stockwellness-api:test
```

### API 계약 변경 시
```bash
./gradlew updateOpenApiSpec
```

### 특정 테스트만 빠르게 확인
```bash
./gradlew :stockwellness-api:test --tests "org.stockwellness.adapter.in.web.auth.AuthControllerTest"
```

### 성능 영향이 있으면
`k6` 시나리오를 사용한다.

- 안내 문서: [k6/README.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/README.md)
- 명령어 모음: [k6/COMMANDS.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/COMMANDS.md)
- 전체 실행 스크립트: [k6/run-all.sh](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/run-all.sh:1)

## 최근 k6 실행에서 확인된 우선순위
기준 파일:
[run-all-summary-20260423-103755.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/results/run-all-summary-20260423-103755.md:1)

### 1. 순수 성능 병목
- `market-indexes`
- 증상: 실패율 `0%`, `p95 3.9s`, `p99 5.1s`
- 의미: 기능은 정상이고 조회 성능만 느리다

### 2. 부분 실패 + tail latency
- `stock-supply-ranking`
- 증상: 실패율 `1.44%`, `p99 20s`
- 의미: 일부 요청만 길게 지연되거나 실패한다

### 3. 20초 타임아웃 패턴
- `stock-price-history`
- `stock-returns`
- `sector-fluctuation-ranking`
- `sector-comparison`
- `sector-detail`
- 증상: 실패율 `100%`, `p95`가 약 `20.19s`
- 의미: 공통 서버 타임아웃, 외부 연동 타임아웃, 느린 쿼리 중 하나일 가능성이 높다

### 4. 인증 또는 테스트 데이터 전제 불일치
- `member-me`
- `member-notifications`
- `portfolio-*`
- `analysis-*`
- `watchlist-*`
- `stock-search-history`
- 증상: 응답은 빠르지만 상태코드 체크 실패, 실패율 `100%`
- 의미: 성능보다는 `ACCESS_TOKEN`, `PORTFOLIO_ID`, `GROUP_ID`, 실제 사용자 데이터 상태를 먼저 검증해야 한다

## 장애/성능 이슈 점검 순서
### 인증 API가 모두 실패할 때
1. `ACCESS_TOKEN`이 현재 유효한지 확인
2. 해당 토큰 소유자가 `PORTFOLIO_ID`, `GROUP_ID`를 실제로 보유하는지 확인
3. 실제 응답 상태코드가 `200`인지 확인
4. `data`는 오는데 상태코드만 다른지 확인

### 특정 조회 API가 20초로 떨어질 때
1. DB 쿼리 실행 시간 확인
2. Redis 캐시 hit/miss 확인
3. 외부 연동 KIS 또는 내부 HTTP 호출 여부 확인
4. 서버 타임아웃 설정값과 실제 지연 시간이 비슷한지 확인
5. 공통 Repository/QueryDSL 경로를 공유하는지 확인

### `p95`는 괜찮은데 `p99`만 튀는 경우
1. 일부 데이터 케이스만 느린지 확인
2. 정렬/페이징/집계 쿼리의 인덱스 사용 여부 확인
3. 캐시 미스 시 fallback 경로가 과도하게 느린지 확인
4. 외부 API 재시도 로직이 tail latency를 늘리는지 확인

## 자주 수정하는 위치
- 인증/인가: `stockwellness-api/src/main/java/.../auth`, `config`
- 포트폴리오 분석: `stockwellness-core/src/main/java/.../portfolio`
- 종목/섹터 조회: `stockwellness-core/src/main/java/.../stock`
- 배치 수집: `stockwellness-batch/src/main/java/.../batch`
- 공통 에러: `stockwellness-core/src/main/java/.../global/error`

## 작업 종료 전 체크리스트
- 관련 모듈 테스트를 돌렸는가
- 문서 또는 OpenAPI가 필요한 경우 갱신했는가
- 성능 영향이 있는 변경이면 최소 1개 k6 시나리오라도 재검증했는가
- 에러 응답이 표준 포맷을 유지하는가
- 금융 도메인 엣지 케이스를 확인했는가

## 추천 문서 순서
1. [README.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/README.md)
2. [architecture.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/docs/architecture.md)
3. [code-style.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/docs/code-style.md)
4. [testing.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/docs/testing.md)
5. [k6/README.md](/Users/chan/Desktop/gongbu/stockwellness-project/stockwellness/k6/README.md)
