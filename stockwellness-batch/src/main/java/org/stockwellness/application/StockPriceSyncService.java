package org.stockwellness.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.application.port.out.stock.StockInvestorTradePort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockInvestorTrade;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.global.common.util.ParsingUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class StockPriceSyncService {

    private final StockPort stockPort;
    private final StockPricePort stockPricePort;
    private final StockInvestorTradePort investorTradePort;
    private final KisDailyPriceAdapter kisPriceAdapter;

    private static final BigDecimal PBMN_MULTIPLIER = BigDecimal.valueOf(1_000_000L);

    /**
     * 당일 종목 가격 반영<br>
     * 1. ACTIVE 상태의 종목을 전부 가져온다. <br>
     * 2. 최대 30개씩 끊어서 fetchMultiStockPrices 에 List<String> tickers 로 전달 <br>
     * 3. KisMultiStockPriceDetail -> StockPrice로 변환해서 저장 <br>
     */
    @Transactional
    public void saveDailyStockPrices() {
        List<Stock> stocks = stockPort.findAllByActiveStocks();
        log.info("[KIS 시세 저장] 시작 - 대상 종목 수: {}", stocks.size());

        int pageSize = 30;
        int totalStocks = stocks.size();
        LocalDate baseDate = LocalDate.now();

        for (int i = 0; i < totalStocks; i += pageSize) {
            int end = Math.min(i + pageSize, totalStocks);
            List<Stock> subList = stocks.subList(i, end);
            List<String> tickers = subList.stream().map(Stock::getTicker).toList();

            List<KisMultiStockPriceDetail> dtos = stockPricePort.fetchMultiStockPrices(tickers);

            Map<String, Stock> stockMap = subList.stream()
                    .collect(Collectors.toMap(Stock::getTicker, Function.identity()));

            List<StockPrice> stockPrices = toStockPrices(dtos, stockMap, baseDate);
            stockPricePort.saveAll(stockPrices);

            // 멀티 시세 API에서 제공하는 기본 수급 정보도 별도 테이블에 저장
            List<StockInvestorTrade> trades = toInvestorTrades(dtos, stockMap, baseDate);
            investorTradePort.saveAll(trades);
        }
        log.info("[KIS 시세 저장] 완료");
    }

    /**
     * 외인/기관 순매수 상위 데이터를 기반으로 상세 수급 정보 동기화
     */
    @Transactional
    public void syncInvestorTradeDetails() {
        log.info("[KIS 상세 수급 동기화] 시작");
        LocalDate baseDate = LocalDate.now();
        
        // "0": 매수 상위, "1": 매도 상위
        processTradeDetails("0", baseDate);
        processTradeDetails("1", baseDate);
        
        log.info("[KIS 상세 수급 동기화] 완료");
    }

    private void processTradeDetails(String type, LocalDate baseDate) {
        List<InvestorTradeDetail> details = kisPriceAdapter.fetchForeignInstitutionData("0000", type);
        
        List<StockInvestorTrade> trades = details.stream()
                .map(detail -> {
                    return stockPort.loadStockByTicker(detail.mkscShrnIscd())
                            .map(stock -> toStockInvestorTrade(stock, baseDate, detail))
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();

        if (!trades.isEmpty()) {
            investorTradePort.saveAll(trades);
        }
    }

    private StockInvestorTrade toStockInvestorTrade(Stock stock, LocalDate baseDate, InvestorTradeDetail detail) {
        BigDecimal instAmt = ParsingUtil.toBigDecimal(detail.orgnNtbyTrPbmn()).multiply(PBMN_MULTIPLIER);
        BigDecimal frgnAmt = ParsingUtil.toBigDecimal(detail.frgnNtbyTrPbmn()).multiply(PBMN_MULTIPLIER);
        BigDecimal pensionAmt = ParsingUtil.toBigDecimal(detail.fundNtbyTrPbmn()).multiply(PBMN_MULTIPLIER);
        BigDecimal trustAmt = ParsingUtil.toBigDecimal(detail.ivtrNtbyTrPbmn()).multiply(PBMN_MULTIPLIER);
        BigDecimal etcCorpAmt = ParsingUtil.toBigDecimal(detail.etcCorpNtbyTrPbmn()).multiply(PBMN_MULTIPLIER);
        // 전체 순매수 금액: 수량 * 현재가로 근사치 계산
        BigDecimal totalAmt = ParsingUtil.toBigDecimal(detail.ntbyQty()).multiply(ParsingUtil.toBigDecimal(detail.stckPrpr()));

        Long instQty = ParsingUtil.toLong(detail.orgnNtbyQty());
        Long frgnQty = ParsingUtil.toLong(detail.frgnNtbyQty());
        Long pensionQty = ParsingUtil.toLong(detail.fundNtbyQty());
        Long trustQty = ParsingUtil.toLong(detail.ivtrNtbyQty());
        Long etcCorpQty = ParsingUtil.toLong(detail.etcCorpNtbyVol());
        Long totalQty = ParsingUtil.toLong(detail.ntbyQty());

        return StockInvestorTrade.of(stock, baseDate,
                instAmt, frgnAmt, pensionAmt, trustAmt, etcCorpAmt, totalAmt,
                instQty, frgnQty, pensionQty, trustQty, etcCorpQty, totalQty);
    }

    private List<StockPrice> toStockPrices(List<KisMultiStockPriceDetail> dtos, Map<String, Stock> stockMap, LocalDate baseDate) {
        return dtos.stream()
                .filter(dto -> stockMap.containsKey(dto.ticker()))
                .map(dto -> {
                    Stock stock = stockMap.get(dto.ticker());
                    return StockPrice.of(
                            stock,
                            baseDate,
                            dto.openPrice(),
                            dto.highPrice(),
                            dto.lowPrice(),
                            dto.closePrice(),
                            dto.closePrice(),
                            dto.previousClosePrice(),
                            dto.accumulatedVolume(),
                            dto.accumulatedTradingValue(),
                            dto.netInstitutionalBuyingAmt(),
                            dto.netForeignBuyingAmt(),
                            TechnicalIndicators.empty()
                    );
                })
                .toList();
    }

    private List<StockInvestorTrade> toInvestorTrades(List<KisMultiStockPriceDetail> dtos, Map<String, Stock> stockMap, LocalDate baseDate) {
        return dtos.stream()
                .filter(dto -> stockMap.containsKey(dto.ticker()))
                .map(dto -> {
                    Stock stock = stockMap.get(dto.ticker());
                    BigDecimal instAmt = dto.netInstitutionalBuyingAmt();
                    BigDecimal frgnAmt = dto.netForeignBuyingAmt();
                    BigDecimal totalAmt = instAmt.add(frgnAmt);

                    // 멀티 시세 API에서는 상세 항목이 없으므로 나머지는 기본값(0)
                    return StockInvestorTrade.of(stock, baseDate,
                            instAmt, frgnAmt, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, totalAmt,
                            0L, 0L, 0L, 0L, 0L, 0L);
                })
                .toList();
    }
}
