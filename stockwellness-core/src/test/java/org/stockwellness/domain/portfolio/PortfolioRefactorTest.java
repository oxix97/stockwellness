package org.stockwellness.domain.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Portfolio 도메인 리팩터링 테스트")
class PortfolioRefactorTest {

    @Test
    @DisplayName("수량과 매입단가를 가진 주식 아이템을 생성할 수 있다")
    void create_stock_item_with_quantity_and_price() {
        // given
        String symbol = "AAPL";
        BigDecimal quantity = new BigDecimal("10.5");
        BigDecimal purchasePrice = new BigDecimal("150.00");
        String currency = "USD";

        // when
        PortfolioItem item = PortfolioItem.createStock(symbol, quantity, purchasePrice, currency);

        // then
        assertThat(item.getSymbol()).isEqualTo(symbol);
        assertThat(item.getQuantity()).isEqualByComparingTo(quantity);
        assertThat(item.getPurchasePrice()).isEqualByComparingTo(purchasePrice);
        assertThat(item.getCurrency()).isEqualTo(currency);
        assertThat(item.getAssetType()).isEqualTo(AssetType.STOCK);
    }

    @Test
    @DisplayName("현금 아이템을 생성할 수 있다")
    void create_cash_item() {
        // given
        BigDecimal amount = new BigDecimal("1000000");
        String currency = "KRW";

        // when
        PortfolioItem item = PortfolioItem.createCash(amount, currency);

        // then
        assertThat(item.getSymbol()).isEqualTo("CASH");
        assertThat(item.getQuantity()).isEqualByComparingTo(amount);
        assertThat(item.getPurchasePrice()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(item.getCurrency()).isEqualTo(currency);
        assertThat(item.getAssetType()).isEqualTo(AssetType.CASH);
    }

    @Test
    @DisplayName("포트폴리오에 아이템들을 업데이트하면 총 자산 가치(매입가 기준)를 계산할 수 있다")
    void update_portfolio_items_and_calculate_total_purchase_amount() {
        // given
        Portfolio portfolio = Portfolio.create(1L, "My Portfolio", "Description");
        PortfolioItem stock = PortfolioItem.createStock("AAPL", new BigDecimal("10"), new BigDecimal("150"), "USD");
        PortfolioItem cash = PortfolioItem.createCash(new BigDecimal("500"), "USD");

        // when
        portfolio.updateItems(List.of(stock, cash));

        // then
        // 10 * 150 + 500 = 1500 + 500 = 2000
        assertThat(portfolio.calculateTotalPurchaseAmount()).isEqualByComparingTo(new BigDecimal("2000"));
    }
}
