package org.stockwellness.domain.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.portfolio.exception.PortfolioDomainException;

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
        @DisplayName("경계값 테스트: 총 조각 수가 딱 1개일 때 정상 업데이트된다")
        void update_valid_min_boundary() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "소액 포트", "");
            List<PortfolioItem> items = List.of(PortfolioItem.createStock("AAPL", 1));

            // when
            portfolio.updateItems(items);

            // then
            assertThat(portfolio.getTotalPieces()).isEqualTo(1);
        }

        @Test
        @DisplayName("경계값 테스트: 총 조각 수가 딱 8개일 때 정상 업데이트된다")
        void update_valid_max_boundary() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "풀 매수 포트", "");
            List<PortfolioItem> items = List.of(
                    PortfolioItem.createStock("AAPL", 4),
                    PortfolioItem.createCash(4)
            );

            // when
            portfolio.updateItems(items);

            // then
            assertThat(portfolio.getTotalPieces()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("실패 케이스 (엣지 케이스 중심)")
    class FailureCases {

        @Test
        @DisplayName("엣지 케이스: 총 조각 수가 0개이면 PortfolioDomainException이 발생한다")
        void fail_zero_pieces() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "빈 포트", "");
            List<PortfolioItem> items = List.of(); // 0개

            // when & then
            assertThatThrownBy(() -> portfolio.updateItems(items))
                    .isInstanceOf(PortfolioDomainException.class)
                    .hasMessageContaining("1개 이상");
        }

        @Test
        @DisplayName("엣지 케이스: 총 조각 수가 딱 9개(MAX+1)이면 PortfolioDomainException이 발생한다")
        void fail_exactly_over_max() {
            // given
            Portfolio portfolio = Portfolio.create(1L, "초과 포트", "");
            List<PortfolioItem> items = List.of(
                    PortfolioItem.createStock("AAPL", 5),
                    PortfolioItem.createStock("TSLA", 4) // 총 9개
            );

            // when & then
            assertThatThrownBy(() -> portfolio.updateItems(items))
                    .isInstanceOf(PortfolioDomainException.class)
                    .hasMessageContaining("8개 이하");
        }

        @Test
        @DisplayName("엣지 케이스: 개별 아이템 조각 수가 딱 0개이면 생성 시점에 예외가 발생한다")
        void fail_item_zero_piece() {
            // when & then
            assertThatThrownBy(() -> PortfolioItem.createStock("AAPL", 0))
                    .isInstanceOf(PortfolioDomainException.class)
                    .hasMessageContaining("최소 1조각 이상");
        }

        @Test
        @DisplayName("엣지 케이스: 개별 아이템 조각 수가 음수(-1)이면 생성 시점에 예외가 발생한다")
        void fail_item_negative_piece() {
            // when & then
            assertThatThrownBy(() -> PortfolioItem.createCash(-1))
                    .isInstanceOf(PortfolioDomainException.class)
                    .hasMessageContaining("최소 1조각 이상");
        }
    }
}
