# 구현 계획 (Implementation Plan): StockPriceBatchConfig 수급 데이터 버그 수정

## 목표
- `StockPriceBatchConfig` 배치가 `InvestorSupplyDemand` 데이터를 저장할 때 0으로 저장되는 현상의 원인 파악 및 수정.
- 단위/통합 테스트 작성 및 수동 데이터베이스 검증.
- (문서 참조: 한국어 진행, 정해진 커밋 메시지 컨벤션 준수)

## Phase 1: 현상 분석 및 원인 파악 (분석)
- [ ] Task: `StockPriceBatchConfig` 및 관련 데이터 수집/변환 클래스 확인
    - [ ] `stockwellness-batch` 모듈 내 주가 수집 배치 흐름(`ItemReader`, `ItemProcessor`, `ItemWriter`) 분석
    - [ ] KIS API(`/uapi/domestic-stock/v1/quotations/inquire-investor`) 응답 데이터를 파싱하는 DTO 클래스 매핑 로직 점검
    - [ ] 도메인 엔티티(`InvestorSupplyDemand`) 생성 및 초기화 로직 점검
- [ ] Task: Conductor - User Manual Verification 'Phase 1 완료' (Protocol in workflow.md)

## Phase 2: 테스트 주도 디버깅 및 구현 (Red -> Green -> Refactor)
- [ ] Task: 실패하는 테스트 작성 (Red)
    - [ ] 수급 데이터 파싱 및 엔티티 매핑을 검증하는 단위/통합 테스트 작성 (실제 KIS API 응답 JSON 기반)
    - [ ] 테스트가 실패하여 현재의 버그(값이 0으로 나옴)를 재현하는지 확인
- [ ] Task: 테스트를 통과하는 최소 구현 (Green)
    - [ ] 파싱 오류 또는 매핑 누락 부분을 찾아 올바르게 수정 (예: 필드명 오타, 타입 불일치, JSON 어노테이션 문제 등)
- [ ] Task: 리팩터링 및 커버리지 확인
    - [ ] 중복 로직 제거 및 코드 컨벤션 적용
    - [ ] 해당 모듈 테스트 실행 및 커버리지(>80%) 확보
- [ ] Task: Conductor - User Manual Verification 'Phase 2 완료' (Protocol in workflow.md)

## Phase 3: 로컬 검증 및 마무리를 위한 준비 (QA)
- [ ] Task: 수동 검증 수행
    - [ ] 로컬 인프라(Docker Compose) 가동
    - [ ] 배치 직접 실행 (`./gradlew :stockwellness-batch:bootRun`)
    - [ ] DB에 적재된 `InvestorSupplyDemand` 데이터 확인 (0이 아닌 정상 값인지)
    - [ ] Postman 또는 curl로 호출한 원본 KIS API 데이터와 비교
- [ ] Task: 커밋 메시지 초안 작성
    - [ ] 문서 컨벤션(`<type>(<scope>): <한글 설명>`)에 맞는 커밋 메시지 작성 후 사용자에게 제안 및 승인 요청
- [ ] Task: Conductor - User Manual Verification 'Phase 3 완료' (Protocol in workflow.md)