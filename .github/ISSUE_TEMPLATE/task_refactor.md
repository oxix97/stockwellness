name: "⚙️ 작업 및 리팩토링 (Task/Refactor)"
description: "기능 추가, 버그 수정(소규모), 리팩토링 및 유지보수 작업을 관리합니다."
labels: ["task"]
body:
  - type: dropdown
    id: task_type
    attributes:
      label: "작업 유형"
      description: "해당하는 작업 유형을 선택해 주세요 (style 제외)."
      options:
        - "feat (새로운 기능 추가)"
        - "fix (버그 수정)"
        - "refactor (코드 리팩토링)"
        - "test (테스트 추가 및 수정)"
        - "chore (기타 설정 및 유지보수)"
    validations:
      required: true
  - type: textarea
    id: goal
    attributes:
      label: "작업 목표 및 동기"
      description: "유지보수성 향상 또는 성능 최적화의 이유를 작성해 주세요."
    validations:
      required: true
  - type: textarea
    id: implementation_details
    attributes:
      label: "구현 세부사항"
      description: "수정될 특정 도메인(Domain), 어댑터(Adapter), 포트(Port) 등을 명시해 주세요."
  - type: checkboxes
    id: dod
    attributes:
      label: "완료 조건 (Definition of Done)"
      description: "작업 완료를 위해 반드시 수행해야 하는 항목입니다."
      options:
        - label: "TDD 기반 테스트 작성 완료 (Red-Green-Refactor)"
        - label: "코드 커버리지 80% 이상 확보 (Jacoco 보고서 확인)"
        - label: "프로젝트 코드 스타일 가이드 준수"
        - label: "모듈 간 경계(Hexagonal Architecture) 준수 여부 확인"
        - label: "관련 문서 업데이트 여부 확인"
