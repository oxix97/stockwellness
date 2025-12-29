package org.stockwellness.adapter.out.persistence.portfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.stockwellness.config.TestJpaConfig;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestJpaConfig.class)
@DataJpaTest
@DisplayName("Portfolio Repository 테스트")
class PortfolioRepositoryTest {

    @Autowired
    PortfolioJpaRepository portfolioRepository;

    @Autowired
    TestEntityManager em;

    private Portfolio defaultPortfolio;
    private final Long myMemberId = 1L;
    private final Long otherMemberId = 99L;

    @BeforeEach
    void setUp() {
        // [Given] 공통 데이터 세팅: 멤버 1의 유효한 포트폴리오
        Portfolio portfolio = Portfolio.create(myMemberId, "노후 대비", "안정형 자산 배분");
        List<PortfolioItem> items = List.of(
                PortfolioItem.createStock("AAPL", 4), // 4조각
                PortfolioItem.createCash(4)           // 4조각 (Total 8)
        );
        portfolio.updateItems(items);

        // 영속화 (ID 생성)
        defaultPortfolio = portfolioRepository.save(portfolio);

        // 쿼리 로그 확인 및 정확한 조회를 위해 영속성 컨텍스트 초기화
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("성공 케이스 (Success)")
    class Success {

        @Test
        @DisplayName("저장: Cascade 옵션으로 인해 포트폴리오 저장 시 아이템도 함께 저장된다")
        void save_cascade_items() {
            // given
            Portfolio newPortfolio = Portfolio.create(2L, "새 포트폴리오", "");
            newPortfolio.updateItems(List.of(PortfolioItem.createStock("TSLA", 8)));

            // when
            Portfolio saved = portfolioRepository.save(newPortfolio);
            em.flush();
            em.clear();

            // then
            Portfolio found = portfolioRepository.findById(saved.getId()).orElseThrow();
            assertThat(found.getItems()).hasSize(1);
            assertThat(found.getItems().get(0).getIsinCode()).isEqualTo("TSLA");
        }

        @Test
        @DisplayName("조회: findWithItems... 사용 시 Fetch Join으로 아이템을 즉시 로딩한다")
        void find_with_items_fetch_join() {
            // when
            // setUp에서 저장한 ID로 조회
            Optional<Portfolio> result = portfolioRepository.findWithItems(defaultPortfolio.getId(), myMemberId);

            // then
            assertThat(result).isPresent();
            // LazyLoading 예외가 발생하지 않고 아이템에 접근 가능해야 함
            assertThat(result.get().getItems()).hasSize(2);
            assertThat(result.get().getItems()).extracting("isinCode")
                    .containsExactlyInAnyOrder("AAPL", "KRW");
        }

        @Test
        @DisplayName("목록 조회: 특정 회원의 포트폴리오 목록을 조회한다")
        void find_all_by_member_id() {
            // given (setUp 데이터 외에 하나 더 추가)
            Portfolio secondPortfolio = Portfolio.create(myMemberId, "두번째 포트", "");
            secondPortfolio.updateItems(List.of(PortfolioItem.createCash(8)));
            portfolioRepository.save(secondPortfolio);

            // 다른 사람 데이터 추가 (조회되면 안 됨)
            Portfolio otherPortfolio = Portfolio.create(otherMemberId, "남의 포트", "");
            otherPortfolio.updateItems(List.of(PortfolioItem.createCash(8)));
            portfolioRepository.save(otherPortfolio);

            em.flush();
            em.clear();

            // when
            List<Portfolio> myPortfolios = portfolioRepository.findAllByMemberId(myMemberId);

            // then
            assertThat(myPortfolios).hasSize(2); // setUp(1) + given(1)
            assertThat(myPortfolios).extracting("name")
                    .containsExactlyInAnyOrder("노후 대비", "두번째 포트");
        }

        @Test
        @DisplayName("중복 확인: 해당 멤버에게 이미 존재하는 이름인지 확인한다")
        void exists_by_name_true() {
            // when
            boolean exists = portfolioRepository.existsByMemberIdAndName(myMemberId, "노후 대비");

            // then
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("실패/예외 케이스 (Failure)")
    class Failure {

        @Test
        @DisplayName("조회 실패: 포트폴리오 ID가 존재해도 소유자(MemberId)가 다르면 조회되지 않는다")
        void find_fail_owner_mismatch() {
            // when
            // setUp에 저장된 포트폴리오(MemberId=1)를 MemberId=99로 조회 시도
            Optional<Portfolio> result = portfolioRepository.findByIdAndMemberId(defaultPortfolio.getId(), otherMemberId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("조회 실패: 존재하지 않는 포트폴리오 ID로 조회하면 Empty를 반환한다")
        void find_fail_not_found_id() {
            // when
            Long nonExistentId = 9999L;
            Optional<Portfolio> result = portfolioRepository.findWithItems(nonExistentId, myMemberId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("중복 확인: 다른 멤버가 가진 같은 이름은 중복으로 치지 않는다")
        void exists_by_name_false_diff_member() {
            // when
            // setUp에 있는 "노후 대비"는 MemberId=1의 것. MemberId=99가 조회하면 false여야 함.
            boolean exists = portfolioRepository.existsByMemberIdAndName(otherMemberId, "노후 대비");

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("중복 확인: 같은 멤버지만 다른 이름이면 false를 반환한다")
        void exists_by_name_false_diff_name() {
            // when
            boolean exists = portfolioRepository.existsByMemberIdAndName(myMemberId, "완전 새로운 이름");

            // then
            assertThat(exists).isFalse();
        }
    }
}