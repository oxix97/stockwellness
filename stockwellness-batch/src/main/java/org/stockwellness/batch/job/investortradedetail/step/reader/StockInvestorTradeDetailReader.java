package org.stockwellness.batch.job.investortradedetail.step.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.util.StringUtils;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateSource;
import org.stockwellness.domain.stock.BenchmarkType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class StockInvestorTradeDetailReader implements ItemReader<InvestorTradeDetailUpdateSource> {

    private static final List<String> MARKET_INDEX_CODES = List.of(
            BenchmarkType.KOSPI.getTicker(),
            BenchmarkType.KOSDAQ.getTicker()
    );
    private static final List<String> SORTED_VALUES = List.of("0", "1");

    private final ListItemReader<InvestorTradeDetailUpdateSource> delegate;

    public StockInvestorTradeDetailReader(KisDailyPriceAdapter kisDailyPriceAdapter, String targetTicker) {
        this.delegate = new ListItemReader<>(loadItems(kisDailyPriceAdapter, normalizeTicker(targetTicker)));
    }

    @Override
    public InvestorTradeDetailUpdateSource read() {
        return delegate.read();
    }

    private static List<InvestorTradeDetailUpdateSource> loadItems(
            KisDailyPriceAdapter kisDailyPriceAdapter,
            String targetTicker
    ) {
        Map<String, InvestorTradeDetailUpdateSource> merged = new LinkedHashMap<>();

        for (String marketIndexCode : MARKET_INDEX_CODES) {
            for (String sorted : SORTED_VALUES) {
                List<InvestorTradeDetail> details = kisDailyPriceAdapter.fetchForeignInstitutionData(marketIndexCode, sorted);
                for (InvestorTradeDetail detail : details) {
                    String ticker = normalizeTicker(detail.mkscShrnIscd());
                    if (!StringUtils.hasText(ticker)) {
                        continue;
                    }
                    if (StringUtils.hasText(targetTicker) && !targetTicker.equals(ticker)) {
                        continue;
                    }
                    merged.putIfAbsent(
                            ticker,
                            new InvestorTradeDetailUpdateSource(
                                    ticker,
                                    detail.orgnNtbyTrPbmn(),
                                    detail.frgnNtbyTrPbmn()
                            )
                    );
                }
            }
        }

        List<InvestorTradeDetailUpdateSource> items = new ArrayList<>(merged.values());
        log.info("[투자주체 보정 Reader] 대상 종목 수집 완료. count={}, targetTicker={}", items.size(), targetTicker);
        return items;
    }

    private static String normalizeTicker(String ticker) {
        if (!StringUtils.hasText(ticker)) {
            return null;
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
