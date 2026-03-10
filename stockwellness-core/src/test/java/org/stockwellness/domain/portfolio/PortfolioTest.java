package org.stockwellness.domain.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.portfolio.exception.InvalidPortfolioException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Portfolio 도메인 단위 테스트")
class PortfolioTest {

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("포트폴리오 기본 정보(이름, 설명)를 수정할 수 있다")
        void update_basic_info_success() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "기존 이름", "기존 설명");

            // when
            portfolio.updateBasicInfo("새 이름", "새 설명");

            // then
            assertThat(portfolio.getName()).isEqualTo("새 이름");
            assertThat(portfolio.getDescription()).isEqualTo("새 설명");
        }

        @Test
        @DisplayName("포트폴리오에 아이템을 업데이트할 수 있다")
        void update_items_success() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "소액 포트", "");
            List<PortfolioItem> items = List.of(PortfolioItem.createStock("AAPL", BigDecimal.ONE, BigDecimal.valueOf(150000), "KRW"));

            // when
            portfolio.updateItems(items);

            // then
            assertThat(portfolio.getItems()).hasSize(1);
            assertThat(portfolio.calculateTotalPurchaseAmount()).isEqualByComparingTo(BigDecimal.valueOf(150000));
        }
    }

    @Nested
    @DisplayName("실패 케이스 (엣지 케이스 중심)")
    class FailureCases {

        @Test
        @DisplayName("엣지 케이스: 개별 아이템 수량이 딱 0이면 생성 시점에 예외가 발생한다")
        void fail_item_zero_quantity() {
            // when & then
            assertThatThrownBy(() -> PortfolioItem.createStock("AAPL", BigDecimal.ZERO, BigDecimal.valueOf(100), "KRW"))
                    .isInstanceOf(InvalidPortfolioException.class);
        }

        @Test
        @DisplayName("엣지 케이스: 개별 아이템 수량이 음수(-1)이면 생성 시점에 예외가 발생한다")
        void fail_item_negative_quantity() {
            // when & then
            assertThatThrownBy(() -> PortfolioItem.createCash(BigDecimal.valueOf(-1), "KRW"))
                    .isInstanceOf(InvalidPortfolioException.class);
        }

        @Test
        @DisplayName("엣지 케이스: 매입단가가 음수이면 생성 시점에 예외가 발생한다")
        void fail_item_negative_price() {
            // when & then
            assertThatThrownBy(() -> PortfolioItem.createStock("AAPL", BigDecimal.ONE, BigDecimal.valueOf(-100), "KRW"))
                    .isInstanceOf(InvalidPortfolioException.class);
        }
    }
}
