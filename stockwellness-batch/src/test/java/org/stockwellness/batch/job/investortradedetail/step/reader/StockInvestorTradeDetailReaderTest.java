package org.stockwellness.batch.job.investortradedetail.step.reader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateSource;
import org.stockwellness.domain.stock.BenchmarkType;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class StockInvestorTradeDetailReaderTest {

    @Mock
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    @DisplayName("KOSPI/KOSDAQ의 순매수/순매도 상위를 병합하고 중복 티커는 최초 응답을 유지한다")
    void read_mergesAllMarketResponses() throws Exception {
        given(kisDailyPriceAdapter.fetchForeignInstitutionData(BenchmarkType.KOSPI.getTicker(), "0"))
                .willReturn(List.of(investorTradeDetail("005930", "10", "20")));
        given(kisDailyPriceAdapter.fetchForeignInstitutionData(BenchmarkType.KOSPI.getTicker(), "1"))
                .willReturn(List.of(investorTradeDetail("005930", "999", "999")));
        given(kisDailyPriceAdapter.fetchForeignInstitutionData(BenchmarkType.KOSDAQ.getTicker(), "0"))
                .willReturn(List.of(investorTradeDetail("035720", "30", "40")));
        given(kisDailyPriceAdapter.fetchForeignInstitutionData(BenchmarkType.KOSDAQ.getTicker(), "1"))
                .willReturn(Collections.emptyList());

        StockInvestorTradeDetailReader reader = new StockInvestorTradeDetailReader(kisDailyPriceAdapter, null);

        InvestorTradeDetailUpdateSource first = reader.read();
        InvestorTradeDetailUpdateSource second = reader.read();
        InvestorTradeDetailUpdateSource third = reader.read();

        assertThat(first.ticker()).isEqualTo("005930");
        assertThat(first.institutionalBuyingQtyText()).isEqualTo("100");
        assertThat(first.foreignBuyingQtyText()).isEqualTo("200");
        assertThat(first.institutionalBuyingAmtText()).isEqualTo("10");
        assertThat(first.foreignBuyingAmtText()).isEqualTo("20");
        assertThat(second.ticker()).isEqualTo("035720");
        assertThat(third).isNull();

        verify(kisDailyPriceAdapter).fetchForeignInstitutionData(BenchmarkType.KOSPI.getTicker(), "0");
        verify(kisDailyPriceAdapter).fetchForeignInstitutionData(BenchmarkType.KOSPI.getTicker(), "1");
        verify(kisDailyPriceAdapter).fetchForeignInstitutionData(BenchmarkType.KOSDAQ.getTicker(), "0");
        verify(kisDailyPriceAdapter).fetchForeignInstitutionData(BenchmarkType.KOSDAQ.getTicker(), "1");
        verifyNoMoreInteractions(kisDailyPriceAdapter);
    }

    @Test
    @DisplayName("targetTicker가 있으면 해당 종목만 남긴다")
    void read_filtersByTargetTicker() throws Exception {
        given(kisDailyPriceAdapter.fetchForeignInstitutionData(BenchmarkType.KOSPI.getTicker(), "0"))
                .willReturn(List.of(
                        investorTradeDetail("005930", "10", "20"),
                        investorTradeDetail("000660", "30", "40")
                ));
        given(kisDailyPriceAdapter.fetchForeignInstitutionData(BenchmarkType.KOSPI.getTicker(), "1"))
                .willReturn(Collections.emptyList());
        given(kisDailyPriceAdapter.fetchForeignInstitutionData(BenchmarkType.KOSDAQ.getTicker(), "0"))
                .willReturn(Collections.emptyList());
        given(kisDailyPriceAdapter.fetchForeignInstitutionData(BenchmarkType.KOSDAQ.getTicker(), "1"))
                .willReturn(Collections.emptyList());

        StockInvestorTradeDetailReader reader = new StockInvestorTradeDetailReader(kisDailyPriceAdapter, "005930");

        InvestorTradeDetailUpdateSource first = reader.read();
        InvestorTradeDetailUpdateSource second = reader.read();

        assertThat(first.ticker()).isEqualTo("005930");
        assertThat(second).isNull();

        verify(kisDailyPriceAdapter).fetchForeignInstitutionData(BenchmarkType.KOSPI.getTicker(), "0");
        verify(kisDailyPriceAdapter).fetchForeignInstitutionData(BenchmarkType.KOSPI.getTicker(), "1");
        verify(kisDailyPriceAdapter).fetchForeignInstitutionData(BenchmarkType.KOSDAQ.getTicker(), "0");
        verify(kisDailyPriceAdapter).fetchForeignInstitutionData(BenchmarkType.KOSDAQ.getTicker(), "1");
        verifyNoMoreInteractions(kisDailyPriceAdapter);
    }

    private InvestorTradeDetail investorTradeDetail(String ticker, String institutionalAmount, String foreignAmount) {
        return new InvestorTradeDetail(
                "테스트",
                ticker,
                null,
                null,
                null,
                null,
                null,
                null,
                "200",
                "100",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                foreignAmount,
                institutionalAmount,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
