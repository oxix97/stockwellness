-- V7: PortfolioStats 테이블에 누적 수익률(inception_return) 및 벤치마크 수익률(benchmark_return) 컬럼 추가
ALTER TABLE portfolio_stats ADD COLUMN inception_return numeric(19, 4);
ALTER TABLE portfolio_stats ADD COLUMN benchmark_return numeric(19, 4);

-- 기본값은 0으로 채움
UPDATE portfolio_stats SET inception_return = 0, benchmark_return = 0;
