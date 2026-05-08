# 비즈니스 로직 및 배치 프로세서 수정 (#262)

## 🎯 작업 목표
새로운 섹터 지표 모델에 맞춰 계산 로직과 배치 프로세서를 수정합니다.

## 🛠 구현 세부사항
- `TechnicalCalculator`의 섹터 지표 처리 로직 최적화
- `SectorInsightItemProcessor` 및 `SectorEodBatchService` 모델 매핑 수정
- 불필요한(0건) 지표 계산 로직 호출 제거

## 🔗 상위 이슈
- Parent: #260

## ✅ 완료 조건
- [ ] `stockwellness-batch` 모듈 빌드 성공
- [ ] 섹터 데이터 수집 배치 정상 작동 확인
