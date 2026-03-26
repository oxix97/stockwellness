# Implementation Plan: CI/CD 워크플로우 태그 트리거 규칙 확장

## 1. Objective (목표)
범용 버전 태그(`v*.*.*`) 푸시 시에도 API와 Batch 서버가 동시에 자동 배포되도록 CI/CD 워크플로우의 트리거 규칙을 확장합니다.

## 2. Key Files & Context (대상 및 컨텍스트)
- **대상 파일**:
    - `.github/workflows/deploy-api.yaml`
    - `.github/workflows/deploy-batch.yaml`
- **변경 사항**: `on.push.tags` 하위에 `- 'v*.*.*'` 패턴 추가.

## 3. Implementation Steps (구현 단계)

### 단계 1: 배포 워크플로우 수정 (API)
- `.github/workflows/deploy-api.yaml` 파일의 `on.push.tags` 섹션을 다음과 같이 수정합니다.
    ```yaml
    on:
      push:
        tags:
          - 'v*.*.*'
          - 'api-v*.*.*'
    ```

### 단계 2: 배포 워크플로우 수정 (Batch)
- `.github/workflows/deploy-batch.yaml` 파일의 `on.push.tags` 섹션을 다음과 같이 수정합니다.
    ```yaml
    on:
      push:
        tags:
          - 'v*.*.*'
          - 'batch-v*.*.*'
    ```

### 단계 3: 커밋 및 푸시
- 변경 사항을 `develop` 브랜치에 커밋하고 푸시합니다.
- `main` 브랜치로 병합하여 배포 설정이 실제 배포 환경에 반영되도록 합니다.

## 4. Verification & Testing (검증 및 테스트)
- **검증**: `git push origin v*.*.*` 형태의 태그 푸시 시 GitHub Actions에서 워크플로우가 정상적으로 트리거되는지 확인합니다.
- **결과**: API와 Batch 서버가 모두 빌드 및 배포되어야 합니다.
