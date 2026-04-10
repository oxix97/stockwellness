package org.stockwellness.batch.job.investortradedetail.step.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.util.StringUtils;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateSource;

import java.util.*;

@Slf4j
public class StockInvestorTradeDetailReader implements ItemReader<InvestorTradeDetailUpdateSource> {
    private static final String BUY_RANK_SORT = "0";
    private static final String SELL_RANK_SORT = "1";

    /**
     * KIS FID_RANK_SORT_CLS_CODE (정렬기준코드)
     * 0: 순매수 상위, 1: 순매도 상위
     * 순매수 상위를 먼저 수집하고, 같은 티커가 순매도 상위에 다시 등장해도 최초 값은 유지한다.
     */
    private static final List<String> SORTED_VALUES = List.of(BUY_RANK_SORT, SELL_RANK_SORT);

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
        int duplicateCount = 0;

        List<String> marketCodes = List.of("0001", "1001"); // KOSPI, KOSDAQ

        for (String market : marketCodes) {
            for (String sorted : SORTED_VALUES) {
                log.debug(
                        "[투자주체 수급 보정 Reader] KIS 랭킹 조회. market={}, sorted={}, label={}",
                        market,
                        sorted,
                        BUY_RANK_SORT.equals(sorted) ? "순매수 상위" : "순매도 상위"
                );
                List<InvestorTradeDetail> details = kisDailyPriceAdapter.fetchForeignInstitutionData(market, sorted);
                for (InvestorTradeDetail detail : details) {
                String ticker = normalizeTicker(detail.mkscShrnIscd());
                if (!StringUtils.hasText(ticker)) {
                    continue;
                }
                if (StringUtils.hasText(targetTicker) && !targetTicker.equals(ticker)) {
                    continue;
                }
                InvestorTradeDetailUpdateSource source = new InvestorTradeDetailUpdateSource(
                        ticker,
                        detail.orgnNtbyQty(),
                        detail.frgnNtbyQty(),
                        detail.fundNtbyQty(),
                        detail.ivtrNtbyQty(),
                        detail.etcCorpNtbyVol(),
                        detail.ntbyQty(),
                        detail.orgnNtbyTrPbmn(),
                        detail.frgnNtbyTrPbmn(),
                        detail.fundNtbyTrPbmn(),
                        detail.ivtrNtbyTrPbmn(),
                        detail.etcCorpNtbyTrPbmn(),
                        "0" // totalNetBuyingAmtText - 상세 수급 API에서 합계 금액은 제공되지 않으므로 0으로 설정
                );
                if (merged.putIfAbsent(ticker, source) != null) {
                    duplicateCount++;
                    log.debug(
                            "[투자주체 수급 보정 Reader] 중복 티커는 최초 응답을 유지합니다. ticker={}, sorted={}",
                            ticker,
                            sorted
                    );
                }
            }
        }
    }

        List<InvestorTradeDetailUpdateSource> items = new ArrayList<>(merged.values());
        log.info(
                "[투자주체 수급 보정 Reader] 대상 종목 수집 완료. count={}, duplicatesSkipped={}, targetTicker={}",
                items.size(),
                duplicateCount,
                targetTicker
        );
        return items;
    }

    private static String normalizeTicker(String ticker) {
        if (!StringUtils.hasText(ticker)) {
            return null;
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
