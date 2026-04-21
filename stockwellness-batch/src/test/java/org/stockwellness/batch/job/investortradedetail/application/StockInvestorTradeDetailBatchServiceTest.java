package org.stockwellness.batch.job.investortradedetail.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.application.service.batch.StockInvestorTradeDetailBatchService;
import org.stockwellness.application.port.out.stock.StockPricePort;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockInvestorTradeDetailBatchServiceTest {

    @Mock
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Mock
    private StockPricePort stockPricePort;

    @Test
    @DisplayName("code 0과 code 1 응답을 합집합으로 병합한다")
    void fetchMergedDetails_mergesUnionOfBothResponses() {
        given(kisDailyPriceAdapter.fetchForeignInstitutionData("0"))
                .willReturn(List.of(detail("005930", "삼성전자"), detail("000660", "SK하이닉스")));
        given(kisDailyPriceAdapter.fetchForeignInstitutionData("1"))
                .willReturn(List.of(detail("000660", "SK하이닉스"), detail("035420", "NAVER")));

        StockInvestorTradeDetailBatchService service = new StockInvestorTradeDetailBatchService(kisDailyPriceAdapter, stockPricePort);

        List<InvestorTradeDetail> result = service.fetchMergedDetails();

        assertThat(result).extracting(InvestorTradeDetail::mkscShrnIscd)
                .containsExactly("005930", "000660", "035420");
    }

    @Test
    @DisplayName("시장 기준일은 요청한 날짜의 stock_price 데이터가 있으면 그대로 사용한다")
    void resolveMarketBaseDate_usesRequestedDateWhenStockPriceExists() {
        given(stockPricePort.existsByBaseDate(LocalDate.of(2026, 4, 12)))
                .willReturn(true);

        StockInvestorTradeDetailBatchService service = new StockInvestorTradeDetailBatchService(kisDailyPriceAdapter, stockPricePort);

        LocalDate result = service.resolveMarketBaseDate(LocalDate.of(2026, 4, 12));

        assertThat(result).isEqualTo(LocalDate.of(2026, 4, 12));
    }

    @Test
    @DisplayName("요청 기준일의 stock_price가 없으면 배치를 실패시킨다")
    void resolveMarketBaseDate_throwsWhenRequestedDateMissing() {
        given(stockPricePort.existsByBaseDate(LocalDate.of(2026, 4, 12)))
                .willReturn(false);

        StockInvestorTradeDetailBatchService service = new StockInvestorTradeDetailBatchService(kisDailyPriceAdapter, stockPricePort);

        assertThatThrownBy(() -> service.resolveMarketBaseDate(LocalDate.of(2026, 4, 12)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("요청 기준일의 stock_price 데이터");
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
}
