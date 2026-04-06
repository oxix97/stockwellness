  ALTER TABLE benchmark_price
  ADD COLUMN IF NOT EXISTS name VARCHAR(30);

  UPDATE benchmark_price
  SET name = CASE ticker
      WHEN '0001' THEN '코스피'
      WHEN '1001' THEN '코스닥'
      WHEN '2001' THEN '코스피 200'
      WHEN 'SPX' THEN 'S&P 500'
      WHEN 'IXIC' THEN '나스닥'
      ELSE ticker
  END
  WHERE name IS NULL;


