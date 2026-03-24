-- portfolio_item 테이블에서 JPA 엔티티에서 제거된 레거시 컬럼을 정리합니다.
-- piece_count: PortfolioItem.quantity 로 대체됨 (@Deprecated getPieceCount() 참조)
-- isin_code:   PortfolioItem.symbol 로 대체됨 (@Deprecated getIsinCode() 참조)

ALTER TABLE portfolio_item
    ALTER COLUMN piece_count DROP NOT NULL;

ALTER TABLE portfolio_item
    ALTER COLUMN isin_code DROP NOT NULL;
