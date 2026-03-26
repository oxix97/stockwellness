# Implementation Plan: .gitignore 규칙에 따른 추적 파일 정리

## 1. Objective (목표)
`.gitignore` 파일에 정의되어 있으나 현재 Git 인덱스에서 추적(tracked) 중인 파일들을 모두 제외하여, 로컬 환경과 Git 저장소의 상태를 일치시킵니다.

## 2. Key Files & Context (대상 및 컨텍스트)
- **주요 대상**:
    - `/conductor/` 하위 모든 파일 (`index.md`, `tracks.md`, `archive/` 등)
    - `/.gemini/` 하위 모든 파일 (`settings.json` 등)
    - `/docs/` 하위 모든 파일 (`superpowers/` 등)
    - `stockwellness-api/logs/`, `stockwellness-core/logs/` 하위 아카이브 파일
- **Git 브랜치 전략**: `GEMINI.md` 규정에 따라 상위 이슈(`refactor`)와 하위 작업(`task`) 이슈를 생성하여 작업합니다.

## 3. Implementation Steps (구현 단계)

### 단계 1: GitHub 이슈 생성
- 상위 이슈 생성: `refactor: .gitignore 규칙에 따른 추적 파일 정리`
- 하위 이슈 생성:
    1. `task: conductor/ 디렉토리 추적 제외`
    2. `task: .gemini/ 및 docs/ 디렉토리 추적 제외`
    3. `task: 로그 아카이브 파일 추적 제외`

### 단계 2: Git 브랜치 생성
- 상위 브랜치 생성: `refactor/#[상위이슈번호]-cleanup-ignored-files`
- 하위 브랜치 생성: `task/#[하위이슈번호]-remove-ignored-files` (각 작업별)

### 단계 3: 파일 추적 제외 실행 (git rm --cached)
- 각 하위 브랜치에서 다음 명령어를 실행하여 로컬 파일은 보존하되 Git 인덱스에서만 제거합니다.
    1. `git rm -r --cached conductor/`
    2. `git rm -r --cached .gemini/`
    3. `git rm -r --cached docs/`
    4. `git rm --cached stockwellness-api/logs/archive/*.log.gz`
    5. `git rm --cached stockwellness-core/logs/archive/*.log.gz`

### 단계 4: 검증 및 커밋
- `git ls-files -c -i --exclude-standard` 명령으로 더 이상 추적 중인 파일이 없는지 확인합니다.
- 하위 브랜치 작업을 완료하고 상위 브랜치로 병합(Merge)합니다.

## 4. Verification & Testing (검증 및 테스트)
- **검증 명령**: `git ls-files -c -i --exclude-standard` 실행 시 출력이 없어야 합니다.
- **최종 결과**: `develop` 브랜치로 Pull Request를 생성하여 최종 병합을 준비합니다.
