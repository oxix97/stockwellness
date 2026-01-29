# Specification: Member Management (Retrieve, Update, Withdraw) - User Implemented

## Overview
회원 정보 조회, 수정, 탈퇴 기능을 **사용자가 직접 구현**합니다. 본 트랙의 핵심 목표는 사용자가 작성한 코드에 대해 **Hexagonal Architecture 준수 여부, 비즈니스 로직의 정합성, 코드 품질 및 테스트 커버리지를 검토하고 보완**하는 것입니다.

## Functional Requirements (User Implementation Scope)

### 1. 회원 정보 조회 (`GET /api/v1/members/me`)
- 응답 구조에 `portfolioSummary`를 포함하여 향후 확장성 확보.

### 2. 회원 정보 수정 (`PATCH /api/v1/members/me`)
- 닉네임 중복 체크 로직 및 유효성 검사 구현.
- `Optional` 필드 처리를 통한 부분 업데이트 구현.

### 3. 회원 탈퇴 (`DELETE /api/v1/members/me`)
- Soft Delete (`status = DEACTIVATED`) 구현.

## Role & Responsibilities
- **User**: 위 기능 요구사항에 따른 실제 코드 작성 (Domain, Application, Adapter).
- **Conductor (AI)**:
    - 작성된 코드의 아키텍처 원칙 준수 여부 리뷰.
    - 놓치기 쉬운 엣지 케이스(Edge Case) 및 예외 처리 검토.
    - 테스트 코드(Unit/Integration)의 완성도 점검 및 보완 제안.

## Acceptance Criteria
- [ ] 사용자가 구현한 코드가 `functional requirements`를 충족하는지 검증되었다.
- [ ] Hexagonal Architecture의 의존성 규칙(Dependency Rule)이 위배되지 않았다.
- [ ] 닉네임 중복, 유효하지 않은 입력 등 예외 상황에 대한 처리가 적절하다.
- [ ] 주요 비즈니스 로직에 대한 테스트 코드가 존재하며 통과한다.
