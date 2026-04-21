package org.stockwellness.application.port.out.external.kis;

import org.stockwellness.adapter.out.external.kis.dto.*;
import org.stockwellness.domain.stock.Stock;

import java.time.LocalDate;
import java.util.List;

public interface KisPricePort {
    /**
     * 멀티 종목 시세 조회 (최대 30종목)
     */
    List<KisMultiStockPriceDetail> fetchMultiStockPrices(List<String> tickers);

    /**
     * 주식 기간별 시세(일/주/월/년)
     */
    List<KisDailyPriceDetail> fetchDailyPrices(Stock stock, LocalDate startDate, LocalDate endDate);

    /**
     * 주식 일자별 투자자 매매 추이 (확정치)
     */
    List<KisInvestorPriceDetail> fetchInvestorPrices(Stock stock, LocalDate startDate, LocalDate endDate);

    /**
     * 국내 업종/지수 기간별 시세
     */
    List<BenchmarkPriceData> fetchIndexDailyPrices(String indexCode, LocalDate startDate, LocalDate endDate);

    /**
     * 해외 지수 일자별 시세 조회
     */
    List<BenchmarkPriceData> fetchOverseasIndexDailyPrices(String indexCode, LocalDate startDate, LocalDate endDate);

    /**
     * 국내기관_외국인 매매종목가집계
     */
    List<InvestorTradeDetail> fetchForeignInstitutionData(String code);
}
