# k6 Load Test Auth & Usage Guide

이 문서는 StockWellness 프로젝트의 부하 테스트(k6) 환경 설정 및 실행 가이드를 제공합니다.

## 1. 인증 시스템 (Auth)

부하 테스트 시 모든 인증 기반 API(`Authenticated Read APIs`)는 `ACCESS_TOKEN` 환경 변수를 필요로 합니다.

### 인증 문제 해결 (`A003` 에러)
테스트 실행 시 `401 Unauthorized (code: A003)` 에러가 발생하면 토큰이 만료된 것입니다.

**해결 방법:**
아래 스크립트를 실행하여 로컬 API 서버로부터 신규 토큰을 발급받고 `.env` 파일을 자동으로 갱신합니다.
```bash
./k6/refresh-token.sh
```

> **참고:** 이 스크립트는 내부적으로 `auth-login.js` 시나리오를 1회 실행하여 토큰을 추출합니다.

## 2. 테스트 모드 (Execution Modes)

`run-all.sh`는 두 가지 주요 모드를 지원합니다.

### 2.1 Standard 모드 (실제 부하 측정)
시나리오당 약 8분간 실행하며, 실제 서비스 부하 상황을 시뮬레이션합니다.
```bash
./k6/run-all.sh standard
```

### 2.2 Quick 모드 (기능 및 인증 검증)
시나리오당 **10초**만 실행하여 인증 여부 및 API의 정상 동작을 빠르게 확인합니다.
```bash
./k6/run-all.sh quick
```

## 3. 결과 확인 (Results)

테스트 결과는 `k6/results` 디렉토리에 저장됩니다.

- **전체 요약:** `run-all-summary-YYYYMMDD-HHMMSS.md` (Markdown 테이블 형식)
- **상세 로그:** `logs/YYYYMMDD-HHMMSS/*.log`
- **메트릭 데이터:** `*.json`

## 4. 로컬 네트워크 설정

Docker 컨테이너 내부의 k6가 로컬 호스트(8080)에 접근하기 위해 `k6/.env`의 `BASE_URL`은 다음과 같이 설정되어야 합니다:
```env
BASE_URL=http://host.docker.internal:8080
```

## 5. 성능 병목 지점 최적화 (Reference)

- **Market Indexes (p95 3.9s):** Redis 캐싱(`marketDashboard:v1`)이 적용되어 있습니다. (TTL 5분)
- **HikariCP Timeout (20s):** `connection-timeout` 초과 발생 시 커넥션 풀 크기 조절 및 슬로우 쿼리 최적화가 필요합니다.
