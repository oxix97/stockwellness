-- V7: PortfolioStats 테이블에 누적 수익률(inception_return) 및 벤치마크 수익률(benchmark_return) 컬럼 추가
DO $$
BEGIN
    -- 컬럼 추가 (inception_return)
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'portfolio_stats' AND column_name = 'inception_return') THEN
        ALTER TABLE portfolio_stats ADD COLUMN inception_return numeric(19, 4);
        UPDATE portfolio_stats SET inception_return = 0 WHERE inception_return IS NULL;
    END IF;

    -- 컬럼 추가 (benchmark_return)
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'portfolio_stats' AND column_name = 'benchmark_return') THEN
        ALTER TABLE portfolio_stats ADD COLUMN benchmark_return numeric(19, 4);
        UPDATE portfolio_stats SET benchmark_return = 0 WHERE benchmark_return IS NULL;
    END IF;
END $$;
