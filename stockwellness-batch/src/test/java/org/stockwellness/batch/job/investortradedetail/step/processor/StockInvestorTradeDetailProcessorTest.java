package org.stockwellness.batch.job.investortradedetail.step.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.adapter.batch.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.adapter.batch.investortradedetail.step.processor.StockInvestorTradeDetailProcessor;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;

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
    @DisplayName("KIS 상세 응답을 upsert 명령으로 변환한다")
    void process_mapsInvestorTradeDetailToCommand() throws Exception {
        Stock stock = stock(1L, "005930");
        given(stockRepository.findByTicker("005930")).willReturn(Optional.of(stock));

        StockInvestorTradeDetailProcessor processor =
                new StockInvestorTradeDetailProcessor(stockRepository, LocalDate.of(2026, 4, 10));

        InvestorTradeDetailUpdateCommand result = processor.process(detail("005930", "삼성전자"));

        assertThat(result).isNotNull();
        assertThat(result.stockId()).isEqualTo(1L);
        assertThat(result.baseDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(result.name()).isEqualTo("삼성전자");
        assertThat(result.ticker()).isEqualTo("005930");
        assertThat(result.frgnNtbyQty()).isEqualTo(10L);
        assertThat(result.orgnNtbyQty()).isEqualTo(20L);
        assertThat(result.ivtrNtbyQty()).isEqualTo(30L);
        assertThat(result.frgnNtbyTrPbmn()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(result.orgnNtbyTrPbmn()).isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    @DisplayName("종목 마스터에 없는 ticker는 건너뛴다")
    void process_returnsNullWhenTickerMissing() throws Exception {
        given(stockRepository.findByTicker("999999")).willReturn(Optional.empty());

        StockInvestorTradeDetailProcessor processor =
                new StockInvestorTradeDetailProcessor(stockRepository, LocalDate.of(2026, 4, 10));

        InvestorTradeDetailUpdateCommand result = processor.process(detail("999999", "없는종목"));

        assertThat(result).isNull();
    }

    private InvestorTradeDetail detail(String ticker, String name) {
        return new InvestorTradeDetail(
                name,
                ticker,
                "70000",
                "2",
                "1000",
                "1.5",
                "100000",
                "30",
                "10",
                "20",
                "30",
                "40",
                "50",
                "60",
                "70",
                "80",
                "90",
                "100",
                "200",
                "300",
                "400",
                "500",
                "600",
                "700",
                "800",
                "900"
        );
    }

    private Stock stock(Long id, String ticker) {
        Stock stock = Stock.of(
                ticker,
                "KR" + ticker,
                "테스트종목",
                MarketType.KOSPI,
                Currency.KRW,
                StockSector.empty(),
                StockStatus.ACTIVE
        );
        try {
            var idField = AbstractEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(stock, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return stock;
    }
}
