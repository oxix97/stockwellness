BEGIN;
INSERT INTO market(code,name,country_code,currency,timezone) VALUES
 ('KOSPI','KOSPI','KR','KRW','Asia/Seoul'),('KOSDAQ','KOSDAQ','KR','KRW','Asia/Seoul'),
 ('NASDAQ','NASDAQ','US','USD','America/New_York'),('NYSE','NYSE','US','USD','America/New_York'),
 ('AMEX','AMEX','US','USD','America/New_York');
COMMIT;
