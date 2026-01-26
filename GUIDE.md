# Gemini CLI 기반 개발 워크플로우 가이드

이 문서는 Gemini CLI를 활용하여 **Issue & Sub-issue 중심의 개발 프로세스**를 효율적으로 수행하기 위한 가이드입니다.

## 1. 작업 프로세스 개요

1.  **Issue (상위 작업)** 및 **Sub-issue (하위 작업)** 정의
2.  **Branch 생성** (계층 구조 활용)
3.  **개발 및 커밋**
4.  **Merge 전략**에 따른 병합 (Merge vs Squash & Merge)
5.  **CI/CD 자동화** 검증

---

## 2. 브랜치 전략 (Branching Strategy)

| 브랜치 종류 | 명명 규칙 | 생성 기준 | 설명 |
| :--- | :--- | :--- | :--- |
| **Develop** | `develop` | - | 개발 메인 브랜치. 배포 가능한 상태 유지. |
| **Feature** | `feature/#<상위이슈ID>-<기능명>` | `develop` | 상위 이슈(Epic/Feature) 단위 작업 공간. |
| **Task** | `task/#<하위이슈ID>-<작업명>` | `feature/...` | 하위 이슈(Sub-task) 단위 실제 구현 공간. |

---

## 3. 상세 워크플로우

### 단계 1: 상위 이슈 작업 시작
*   **상황:** 새로운 기능(예: 회원가입, 이슈 #10) 개발 시작.
*   **Gemini 명령:** "이슈 #10번 '회원가입' 작업할게. develop에서 `feature/#10-signup` 브랜치 생성해줘."
*   **Git 동작:** `git checkout develop` -> `git checkout -b feature/#10-signup`

### 단계 2: 하위 이슈 작업 시작
*   **상황:** 상위 이슈 내의 세부 작업(예: API 구현, 이슈 #11) 시작.
*   **Gemini 명령:** "하위 이슈 #11번 'API 구현' 할 거야. `task/#11-api-impl` 브랜치 만들어줘."
*   **Git 동작:** (feature 브랜치에서) `git checkout -b task/#11-api-impl`

### 단계 3: 개발 및 커밋
*   **상황:** 코드 작성 및 테스트.
*   **Gemini 명령:** "User 엔티티 만들고 커밋해줘."
*   **필수 절차 (Pre-Commit Review):**
    1.  `git status` 및 변경 내용 요약 제시.
    2.  **커밋 메시지 초안** 작성 및 사용자 검토 요청.
    3.  사용자 승인 후 `git commit` 실행.
*   **Git 동작:** `git add .` -> `git commit -m "feat: #11 User 엔티티 구현"`

### 단계 4: 하위 이슈 완료 (Task -> Feature)
*   **전략:** **일반 Merge (Merge Commit)** 또는 `--no-ff`
*   **이유:** Feature 브랜치 내에서 어떤 작업들이 수행되었는지 히스토리를 보존하기 위함.
*   **Gemini 명령:** "task #11번 작업 끝났어. feature 브랜치로 병합해줘."
*   **Git 동작:**
    1.  `git checkout feature/#10-signup`
    2.  `git merge --no-ff task/#11-api-impl`
    3.  `git branch -d task/#11-api-impl`

### 단계 5: 상위 이슈 완료 (Feature -> Develop)
*   **전략:** **Squash and Merge**
*   **이유:** develop 브랜치에는 기능 단위의 깔끔한 단일 커밋만 남기기 위함.
*   **Gemini 명령:** "feature #10번 기능 완성했어. develop으로 Squash Merge 해줘."
*   **Git 동작:**
    1.  `git checkout develop`
    2.  `git merge --squash feature/#10-signup`
    3.  `git commit -m "feat: #10 회원가입 기능 구현 완료 (API, 테스트 포함)"`
    4.  `git branch -D feature/#10-signup`

---

## 4. 자동화 (CI/CD)

*   **동작 시점:** 원격 저장소(`origin`)에 Push 또는 PR 생성 시.
*   **수행 내용:** 빌드, 테스트, 코드 스타일 체크, JaCoCo 커버리지 분석.
*   **개발자 할 일:** 로컬에서 `./gradlew clean build`로 사전 검증 권장.
