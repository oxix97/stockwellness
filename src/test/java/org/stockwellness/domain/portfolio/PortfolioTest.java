package org.stockwellness.domain.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Portfolio 도메인 테스트")
class PortfolioTest {

    @Nested
    @DisplayName("성공 케이스 (Success)")
    class Success {

        @Test
        @DisplayName("정상: 총 조각 수가 8개(MAX)일 때 포트폴리오가 정상 업데이트된다")
        void update_valid_max_pieces() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "풀 매수 포트폴리오", "꽉 채움");
            List<PortfolioItem> items = List.of(
                    PortfolioItem.createStock("AAPL", 4), // 4조각
                    PortfolioItem.createStock("TSLA", 2), // 2조각
                    PortfolioItem.createCash(2)           // 2조각 (총 8)
            );

            // when
            portfolio.updateItems(items);

            // then
            assertThat(portfolio.getItems()).hasSize(3);
            assertThat(portfolio.getTotalPieces()).isEqualTo(8);
        }

        @Test
        @DisplayName("정상: 총 조각 수가 1개(MIN)일 때도 포트폴리오 구성이 가능하다")
        void update_valid_min_piece() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "소액 적립식", "한 조각만");
            List<PortfolioItem> items = List.of(
                    PortfolioItem.createStock("NVDA", 1) // 1조각 (총 1)
            );

            // when
            portfolio.updateItems(items);

            // then
            assertThat(portfolio.getItems()).hasSize(1);
            assertThat(portfolio.getTotalPieces()).isEqualTo(1);
        }

        @Test
        @DisplayName("정상: 총 조각 수가 1~8개 사이(예: 5개)일 때 정상 동작한다")
        void update_valid_middle_range() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "밸런스형", "적당히");
            List<PortfolioItem> items = List.of(
                    PortfolioItem.createStock("O", 3),   // 리츠 3조각
                    PortfolioItem.createCash(2)          // 현금 2조각 (총 5)
            );

            // when
            portfolio.updateItems(items);

            // then
            assertThat(portfolio.getTotalPieces()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("실패 케이스 (Failure)")
    class Failure {

        @Test
        @DisplayName("실패: 총 조각 수가 8개를 초과하면 BusinessException이 발생한다")
        void fail_over_max_pieces() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "욕심쟁이", "초과");
            List<PortfolioItem> items = List.of(
                    PortfolioItem.createStock("AAPL", 5),
                    PortfolioItem.createStock("MSFT", 4) // 총 9조각 (MAX 8 초과)
            );

            // when & then
            assertThatThrownBy(() -> portfolio.updateItems(items))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INVALID_PORTFOLIO_TOTAL_PIECE.getMessage());
        }

        @Test
        @DisplayName("실패: 아이템 리스트가 비어있으면(0개) BusinessException이 발생한다")
        void fail_zero_pieces() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "빈 깡통", "");
            List<PortfolioItem> items = List.of(); // 총 0조각

            // when & then
            assertThatThrownBy(() -> portfolio.updateItems(items))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INVALID_PORTFOLIO_TOTAL_PIECE.getMessage());
        }

        @Test
        @DisplayName("실패: 개별 아이템 조각 수가 1 미만이면 생성 시점에 예외가 발생한다")
        void fail_invalid_item_piece() {
            // when & then
            // PortfolioItem 생성 시점에 검증
            assertThatThrownBy(() -> PortfolioItem.createStock("AMZN", 0))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INVALID_ITEM_PIECE_COUNT.getMessage());
        }

        @Test
        @DisplayName("실패: 개별 아이템 조각 수가 음수면 생성 시점에 예외가 발생한다")
        void fail_negative_item_piece() {
            // when & then
            assertThatThrownBy(() -> PortfolioItem.createCash(-1))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ErrorCode.INVALID_ITEM_PIECE_COUNT.getMessage());
        }
    }
}