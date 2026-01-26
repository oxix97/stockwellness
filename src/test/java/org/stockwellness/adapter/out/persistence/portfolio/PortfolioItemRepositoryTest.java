package org.stockwellness.adapter.out.persistence.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.config.TestJpaConfig;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import({TestJpaConfig.class, QueryDslConfig.class})
@DataJpaTest
@DisplayName("PortfolioItem Repository 테스트")
class PortfolioItemRepositoryTest {

    @Autowired
    PortfolioItemRepository portfolioItemRepository;

    @Autowired
    PortfolioRepository portfolioRepository;

    @Autowired
    TestEntityManager em;

    @BeforeEach
    void setUp() {
        // [Given] 테스트 데이터 준비
        // 사용자 A: AAPL 5조각 + 현금 3조각
        Portfolio p1 = Portfolio.create(1L, "A의 포트폴리오", "");
        p1.updateItems(List.of(
                PortfolioItem.createStock("AAPL", 5),
                PortfolioItem.createCash(3)
        ));
        portfolioRepository.save(p1);

        // 사용자 B: AAPL 2조각 + TSLA 6조각
        Portfolio p2 = Portfolio.create(2L, "B의 포트폴리오", "");
        p2.updateItems(List.of(
                PortfolioItem.createStock("AAPL", 2),
                PortfolioItem.createStock("TSLA", 6)
        ));
        portfolioRepository.save(p2);

        // 실제 DB 반영 및 캐시 초기화 (쿼리 검증용)
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("성공 케이스 (Success)")
    class Success {

        @Test
        @DisplayName("조회: 특정 종목 코드(AAPL)를 가진 모든 아이템을 조회한다")
        void find_all_by_stock_code_found() {
            // when
            List<PortfolioItem> items = portfolioItemRepository.findAllByIsinCode("AAPL");

            // then
            assertThat(items).hasSize(2); // 2개의 아이템이 나와야 함
            assertThat(items)
                    .extracting("IsinCode")
                    .containsOnly("AAPL");
            assertThat(items)
                    .extracting("pieceCount")
                    .containsExactlyInAnyOrder(5, 2); // 각각 5조각, 2조각
        }

        @Test
        @DisplayName("조회: 한 명만 보유한 종목(TSLA) 조회 시 해당 아이템만 반환한다")
        void find_all_by_stock_code_single() {
            // when
            List<PortfolioItem> items = portfolioItemRepository.findAllByIsinCode("TSLA");

            // then
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getPortfolio().getMemberId()).isEqualTo(2L); // B의 포트폴리오인지 확인
        }
    }

    @Nested
    @DisplayName("결과 없음 (Empty Case)")
    class Empty {

        @Test
        @DisplayName("조회: 아무도 보유하지 않은 종목(NVDA) 조회 시 빈 리스트를 반환한다")
        void find_all_by_stock_code_not_found() {
            // when
            List<PortfolioItem> items = portfolioItemRepository.findAllByIsinCode("NVDA");

            // then
            assertThat(items).isNotNull(); // null이면 안 됨
            assertThat(items).isEmpty();   // 빈 리스트여야 함
        }
    }
}