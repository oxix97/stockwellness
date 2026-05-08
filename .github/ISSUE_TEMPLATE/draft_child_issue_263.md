# DB 마이그레이션 (미사용 컬럼 삭제) (#263)

## 🎯 작업 목표
`sector_insight` 테이블의 실제 데이터가 없는 불필요한 컬럼들을 물리적으로 삭제합니다.

## 🛠 구현 세부사항
- 제거 대상: `ma60`, `ma120`, `macd`, `macd_signal`, `adx`, `plus_di`, `minus_di`, `is_macd_cross`
- 로컬 DB 직접 SQL 실행 및 스키마 확인

## 🔗 상위 이슈
- Parent: #260

## ✅ 완료 조건
- [ ] 컬럼 삭제 SQL 실행 완료
- [ ] DB 스키마에서 해당 컬럼 제거 확인
