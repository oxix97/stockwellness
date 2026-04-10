package org.stockwellness.batch.job.investortradedetail.step.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateSource;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.fixture.StockFixture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockInvestorTradeDetailProcessorTest {

    @Mock
    private StockRepository stockRepository;

    @Test
    @DisplayName("백만원 단위 금액을 실제 금액으로 변환해 업데이트 명령을 만든다")
    void process_convertsPbmnAndCreatesCommand() throws Exception {
        Stock stock = StockFixture.createSamsung();
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(stockRepository.findByTicker("005930")).willReturn(Optional.of(stock));

        StockInvestorTradeDetailProcessor processor =
                new StockInvestorTradeDetailProcessor(stockRepository, LocalDate.of(2026, 4, 8));

        InvestorTradeDetailUpdateCommand result = processor.process(
                new InvestorTradeDetailUpdateSource("005930", "1200", "300", "12.5", "3")
        );

        assertThat(result).isNotNull();
        assertThat(result.stockId()).isEqualTo(1L);
        assertThat(result.baseDate()).isEqualTo(LocalDate.of(2026, 4, 8));
        assertThat(result.netInstitutionalBuyingQty()).isEqualTo(1200L);
        assertThat(result.netForeignBuyingQty()).isEqualTo(300L);
        assertThat(result.netInstitutionalBuyingAmt()).isEqualByComparingTo(new BigDecimal("12500000.0"));
        assertThat(result.netForeignBuyingAmt()).isEqualByComparingTo(new BigDecimal("3000000"));
    }

    @Test
    @DisplayName("빈 금액은 0으로 처리한다")
    void process_returnsZeroWhenAmountIsBlank() throws Exception {
        Stock stock = StockFixture.createSamsung();
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(stockRepository.findByTicker("005930")).willReturn(Optional.of(stock));

        StockInvestorTradeDetailProcessor processor =
                new StockInvestorTradeDetailProcessor(stockRepository, LocalDate.of(2026, 4, 8));

        InvestorTradeDetailUpdateCommand result = processor.process(
                new InvestorTradeDetailUpdateSource("005930", "", null, "", null)
        );

        assertThat(result).isNotNull();
        assertThat(result.netInstitutionalBuyingQty()).isZero();
        assertThat(result.netForeignBuyingQty()).isZero();
        assertThat(result.netInstitutionalBuyingAmt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.netForeignBuyingAmt()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("금액 파싱에 실패하면 건너뛴다")
    void process_skipsWhenAmountIsInvalid() throws Exception {
        Stock stock = StockFixture.createSamsung();
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(stockRepository.findByTicker("005930")).willReturn(Optional.of(stock));

        StockInvestorTradeDetailProcessor processor =
                new StockInvestorTradeDetailProcessor(stockRepository, LocalDate.of(2026, 4, 8));

        InvestorTradeDetailUpdateCommand result = processor.process(
                new InvestorTradeDetailUpdateSource("005930", "10", "3", "invalid", "3")
        );

        assertThat(result).isNull();
    }
}
