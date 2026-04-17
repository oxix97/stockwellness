package org.stockwellness.application.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInvestorTradeDetailBatchService {

    private static final List<String> RANK_CODES = List.of("0", "1");

    private final KisDailyPriceAdapter kisDailyPriceAdapter;
    private final StockPricePort stockPricePort;

    public List<InvestorTradeDetail> fetchMergedDetails() {
        Map<String, InvestorTradeDetail> merged = new LinkedHashMap<>();

        for (String code : RANK_CODES) {
            for (InvestorTradeDetail detail : kisDailyPriceAdapter.fetchForeignInstitutionData(code)) {
                String ticker = detail.mkscShrnIscd();
                if (ticker == null || ticker.isBlank()) {
                    continue;
                }
                merged.merge(ticker, detail, this::mergeDetail);
            }
        }

        return new ArrayList<>(merged.values());
    }

    public LocalDate resolveMarketBaseDate() {
        return resolveMarketBaseDate(DateUtil.today());
    }

    public LocalDate resolveMarketBaseDate(LocalDate referenceDate) {
        LocalDate marketBaseDate = stockPricePort.findLatestDateOnOrBefore(referenceDate)
                .orElseThrow(() -> new IllegalStateException(
                        "stock_price 기준 영업일을 찾을 수 없어 stockInvestorTradeDetailJob을 실행할 수 없습니다."
                ));
        log.info("[투자자 상세 배치] 시장 기준일을 결정했습니다. referenceDate={}, marketBaseDate={}", referenceDate, marketBaseDate);
        return marketBaseDate;
    }

    private InvestorTradeDetail mergeDetail(InvestorTradeDetail existing, InvestorTradeDetail incoming) {
        return new InvestorTradeDetail(
                choose(existing.htsKorIsnm(), incoming.htsKorIsnm()),
                choose(existing.mkscShrnIscd(), incoming.mkscShrnIscd()),
                choose(existing.stckPrpr(), incoming.stckPrpr()),
                choose(existing.prdyVrssSign(), incoming.prdyVrssSign()),
                choose(existing.prdyVrss(), incoming.prdyVrss()),
                choose(existing.prdyCtrt(), incoming.prdyCtrt()),
                choose(existing.acmlVol(), incoming.acmlVol()),
                choose(existing.ntbyQty(), incoming.ntbyQty()),
                choose(existing.frgnNtbyQty(), incoming.frgnNtbyQty()),
                choose(existing.orgnNtbyQty(), incoming.orgnNtbyQty()),
                choose(existing.ivtrNtbyQty(), incoming.ivtrNtbyQty()),
                choose(existing.bankNtbyQty(), incoming.bankNtbyQty()),
                choose(existing.insuNtbyQty(), incoming.insuNtbyQty()),
                choose(existing.mrbnNtbyQty(), incoming.mrbnNtbyQty()),
                choose(existing.fundNtbyQty(), incoming.fundNtbyQty()),
                choose(existing.etcOrgtNtbyVol(), incoming.etcOrgtNtbyVol()),
                choose(existing.etcCorpNtbyVol(), incoming.etcCorpNtbyVol()),
                choose(existing.frgnNtbyTrPbmn(), incoming.frgnNtbyTrPbmn()),
                choose(existing.orgnNtbyTrPbmn(), incoming.orgnNtbyTrPbmn()),
                choose(existing.ivtrNtbyTrPbmn(), incoming.ivtrNtbyTrPbmn()),
                choose(existing.bankNtbyTrPbmn(), incoming.bankNtbyTrPbmn()),
                choose(existing.insuNtbyTrPbmn(), incoming.insuNtbyTrPbmn()),
                choose(existing.mrbnNtbyTrPbmn(), incoming.mrbnNtbyTrPbmn()),
                choose(existing.fundNtbyTrPbmn(), incoming.fundNtbyTrPbmn()),
                choose(existing.etcOrgtNtbyTrPbmn(), incoming.etcOrgtNtbyTrPbmn()),
                choose(existing.etcCorpNtbyTrPbmn(), incoming.etcCorpNtbyTrPbmn())
        );
    }

    private String choose(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }
}
