# 도메인 모델 분리 및 엔티티 적용 (#261)

## 🎯 작업 목표
섹터 전용 임베디드 클래스 `SectorTechnicalIndicators`를 생성하고 `SectorInsight` 엔티티에 적용합니다.

## 🛠 구현 세부사항
- `SectorTechnicalIndicators.java` 생성 (필수 필드 8개: ma5, ma20, rsi14, bollinger 3종, golden/dead cross)
- `SectorInsight.java` 필드 타입 변경 및 영향도 수정
- `SectorInsightTest.java` 단위 테스트 작성

## 🔗 상위 이슈
- Parent: #260

## ✅ 완료 조건
- [ ] `SectorTechnicalIndicators` 클래스 생성 완료
- [ ] `SectorInsight` 엔티티 내 타입 교체 완료
- [ ] 단위 테스트 통과
