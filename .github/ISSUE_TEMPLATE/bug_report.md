name: "🐛 버그 리포트 (Bug Report)"
description: "발생한 문제에 대해 자세히 보고해 주세요."
labels: ["bug"]
body:
  - type: checkboxes
    id: module
    attributes:
      label: "발생 모듈"
      description: "문제가 발생한 모듈을 선택해 주세요."
      options:
        - label: "stockwellness-api"
        - label: "stockwellness-batch"
        - label: "stockwellness-core"
        - label: "Infrastructure (Kafka/Redis/DB/K8s)"
  - type: textarea
    id: reproduction
    attributes:
      label: "재현 단계"
      description: "버그를 재현하기 위한 구체적인 순서를 작성해 주세요."
      placeholder: "1. /api/v1/portfolio 호출\n2. 특정 자산의 수익률 계산 오차 확인..."
    validations:
      required: true
  - type: textarea
    id: expected_vs_actual
    attributes:
      label: "예상 결과 vs 실제 결과"
      description: "특히 자산 배분 계산식이나 데이터 오차를 명시해 주세요."
      placeholder: "예상: RSI 지수 45.2\n실제: RSI 지수 102.5 (임계값 초과)"
    validations:
      required: true
  - type: textarea
    id: logs
    attributes:
      label: "로그 및 스크린샷"
      description: "관련 로그나 에러 스택트레이스를 첨부해 주세요."
      placeholder: "Spring AOP 로그 또는 Kafka 컨슈머 에러 로그 등"
  - type: textarea
    id: environment
    attributes:
      label: "환경 정보"
      description: "문제가 발생한 환경을 입력해 주세요 (Local/Docker/K8s 등)."
      placeholder: "Docker Compose (Compose.yaml 사용)"
