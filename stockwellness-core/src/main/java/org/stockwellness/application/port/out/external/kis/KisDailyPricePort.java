package org.stockwellness.application.port.out.external.kis;

import org.stockwellness.adapter.out.external.kis.dto.*;
import org.stockwellness.domain.stock.Stock;
import java.time.LocalDate;
import java.util.List;

public interface KisDailyPricePort {
    List<KisMultiStockPriceDetail> fetchMultiStockPrices(List<String> tickers);
    List<KisDailyPriceDetail> fetchDailyPrices(Stock stock, LocalDate startDate, LocalDate endDate);
    List<KisInvestorPriceDetail> fetchInvestorPrices(Stock stock, LocalDate startDate, LocalDate endDate);
    List<BenchmarkPriceData> fetchIndexDailyPrices(String indexCode, LocalDate startDate, LocalDate endDate);
    List<BenchmarkPriceData> fetchOverseasIndexDailyPrices(String indexCode, LocalDate startDate, LocalDate endDate);
    List<InvestorTradeDetail> fetchForeignInstitutionData(String code);
}
