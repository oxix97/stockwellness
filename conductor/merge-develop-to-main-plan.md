# PR 작성 계획: develop -> main 병합 및 모듈별 릴리스 (완료)

이 계획서는 `develop` 브랜치의 변경 사항을 `main`에 병합하고, API와 Batch 모듈을 각각 별도로 릴리스하는 절차를 정의하고 기록합니다.

## 1. 개요
- **목적**: v1.0.0 이후의 고도화 사항을 `main`에 반영하고 모듈별 독립 배포 수행.
- **대상 브랜치**: `develop` -> `main`
- **버전 정보**: v1.1.0 (공통) / `api-v1.1.0` / `batch-v1.1.0`

## 2. 주요 변경 사항 (Change Log)
### 2.1. API 모듈 (api-v1.1.0)
- **에러 모니터링**: API 서버 500 에러 발생 시 Slack 실시간 알림 연동 (#167)
- **인프라**: 비동기 실행 환경(`ApiAsyncConfig`) 및 관련 환경 변수 추가 (#168)
- **보안**: 인기 검색어 API 비로그인 접근 허용 등 보안 필터 정비

### 2.2. Batch 모듈 (batch-v1.1.0)
- **이벤트 연동**: 배치 작업 결과(성공/실패) Kafka 이벤트 발행 기능 추가 (#161)
- **안정성**: 배치 작업 결과 송신 관련 통합 테스트 및 의존성 이슈 해결 (#172)

## 3. 실행 결과 및 가이드
### 단계 1: 병합 완료 (완료)
- [x] PR #174 승인 및 `main` 브랜치 병합 완료 (Commit: `e73f6270`)

### 단계 2: 모듈별 태그 생성 및 배포 (대기)
- [ ] **API 배포**: `api-v1.1.0` 태그 생성 및 푸시
- [ ] **Batch 배포**: `batch-v1.1.0` 태그 생성 및 푸시

---
**릴리스 및 배포 트리거 명령**:
```bash
# API 모듈 배포
git checkout main && git pull origin main
git tag api-v1.1.0
git push origin api-v1.1.0

# Batch 모듈 배포
git tag batch-v1.1.0
git push origin batch-v1.1.0

# 공통 버전 관리용 태그 (선택)
git tag v1.1.0
git push origin v1.1.0
```

## 4. 기대 효과
- 모듈별 독립적인 릴리스 생명주기 관리 가능.
- 태그 푸시를 통한 자동화된 CI/CD 파이프라인(GitHub Actions -> n8n) 가동.
