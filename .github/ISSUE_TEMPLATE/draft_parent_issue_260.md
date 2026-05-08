# [Refactor] 섹터 기술 지표 모델 다이어트 및 DB 최적화 (#260)

## 🎯 작업 목표 및 동기
섹터(`sector_insight`) 테이블에서 실제 사용되지 않는 기술 지표 컬럼(ma60, ma120, adx 등)을 제거하고, 전용 임베디드 모델인 `SectorTechnicalIndicators`로 분리하여 데이터 저장 효율성과 도메인 모델의 명확성을 높입니다.

## 🛠 구현 세부사항
- `TechnicalIndicators` 공통 모델에서 섹터 필수 필드만 추출한 `SectorTechnicalIndicators` 생성
- `SectorInsight` 엔티티 필드 교체 및 관련 생성자/팩토리 메서드 수정
- `stockwellness-batch`의 섹터 지표 계산 로직 다이어트
- DB 물리 컬럼 삭제 마이그레이션 실행

## 🔗 하위 테스크 (Sub-Tasks)
- [ ] #261 도메인 모델 분리 및 엔티티 적용
- [ ] #262 비즈니스 로직 및 배치 프로세서 수정
- [ ] #263 DB 마이그레이션 (미사용 컬럼 삭제)
- [ ] #264 최종 통합 테스트 및 PR 작성

## ✅ 완료 조건 (Definition of Done)
- [x] 불필요한 DB 컬럼 8개 제거 완료
- [x] 섹터 전용 기술 지표 모델 적용 완료
- [x] 전체 프로젝트 빌드 및 테스트 통과
- [x] API 응답 정합성 확인
