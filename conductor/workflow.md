# Workflow

## 작업 라이프사이클

1. GitHub Issue에서 다음 태스크 선택 (`gh issue list --state open`)
2. 브랜치 생성: `feature/#<이슈번호>-<설명>`
3. 실패하는 테스트 작성 (Red)
4. 테스트를 통과하는 최소 구현 (Green)
5. 리팩터링 후 커버리지 확인 (>80%)
6. 커밋 메시지는 **한글**로 작성, 커밋 전 메시지 보고 후 승인 받고 진행
7. PR → 코드 리뷰 → `develop` 머지 (`--no-ff`)

## 핵심 원칙

- **GitHub Issues가 단일 진실 공급원**: 모든 작업은 Issue에서 추적
- **기술 스택 변경 시 선 문서화**: `docs/tech-stack.md` 먼저 업데이트 후 구현
- **Non-Interactive 명령 선호**: watch 모드 도구는 `CI=true` 플래그 사용

## 개발 명령어

```bash
# 인프라 기동
docker compose up -d

# API 서버 실행
./gradlew :stockwellness-api:bootRun

# 배치 서버 실행
./gradlew :stockwellness-batch:bootRun

# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :stockwellness-api:test
./gradlew :stockwellness-core:test

# REST Docs 생성
./gradlew :stockwellness-api:openapi3
```

## 완료 기준 (Definition of Done)

- [ ] 테스트 통과
- [ ] 커버리지 80% 이상
- [ ] 커스텀 예외 사용 (Raw Exception 금지)
- [ ] REST Docs 문서화 (API 변경 시)
- [ ] 커밋 메시지 컨벤션 준수 (한글)
- [ ] GitHub Issue close 및 PR 머지

## 배포

```bash
# API 서버
./deploy/scripts/deploy-api.sh

# 배치 서버
./deploy/scripts/deploy-batch.sh
```

환경 변수: `deploy/.env` (`.env.example` 참고)
