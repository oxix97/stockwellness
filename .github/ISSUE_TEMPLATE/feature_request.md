name: "🚀 기능 제안 (Feature Request)"
description: "새로운 기능을 제안해 주세요."
labels: ["enhancement"]
body:
  - type: textarea
    id: user_story
    attributes:
      label: "사용자 스토리 (User Story)"
      description: "개인 투자자 입장에서 왜 이 기능이 필요한지 설명해 주세요."
      placeholder: "자산 관리자로서, 나는 리밸런싱 알림을 받고 싶다. 왜냐하면..."
    validations:
      required: true
  - type: textarea
    id: contribution
    attributes:
      label: "제안 배경 및 기대 효과"
      description: "자산 배분 전략이나 이성적 투자 결정에 어떻게 기여하는지 설명해 주세요."
    validations:
      required: true
  - type: textarea
    id: tech_impact
    attributes:
      label: "기술적 고려사항"
      description: "Batch 파이프라인, Redis 캐싱, Kafka 이벤트 흐름 등 고려해야 할 점"
      placeholder: "새로운 기술 지표를 계산해야 하므로 Spring Batch 작업 추가 필요..."
  - type: textarea
    id: visuals
    attributes:
      label: "시각화 제안 (Optional)"
      description: "차트나 데이터가 어떻게 보여야 하는지에 대한 설명"
