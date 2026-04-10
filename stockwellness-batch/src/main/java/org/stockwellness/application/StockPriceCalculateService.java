package org.stockwellness.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class StockPriceCalculateService {

    private final StockPort stockPort;
    private final StockPricePort stockPricePort;

    /**
     * 모든 활성 종목에 대해 최근 시세 데이터를 기반으로 기술적 지표를 계산하여 업데이트합니다.
     * 약 3,000개 이상의 종목을 처리하기 위해 병렬 스트림으로 계산 부하를 분산하고 벌크 저장을 수행합니다.
     */
    @Transactional
    public void calculateStockPrice() {
        List<Stock> stocks = stockPort.findAllByActiveStocks();
        log.info("[지표 계산 시작] 총 종목 수: {}", stocks.size());

        List<StockPrice> updatedPrices = stocks.parallelStream()
                .map(this::processIndicatorsForStock)
                .filter(Objects::nonNull)
                .toList();

        if (!updatedPrices.isEmpty()) {
            stockPricePort.saveAll(updatedPrices);
            log.info("[지표 계산 완료] 업데이트된 시세 수: {}", updatedPrices.size());
        }
    }

    private StockPrice processIndicatorsForStock(Stock stock) {
        try {
            // 1. 최근 시세 데이터 조회 (지표 계산을 위해 충분한 120개 이상의 데이터 확보)
            List<StockPrice> recentPrices = stockPricePort.findRecent120Prices(stock.getId());
            if (recentPrices.size() < 5) {
                return null;
            }

            // 2. 계산용 데이터 추출 (ta4j 라이브러리 규격에 맞춤)
            List<BigDecimal> highs = recentPrices.stream()
                    .map(p -> p.getHighPrice() != null ? p.getHighPrice() : p.getClosePrice())
                    .toList();
            List<BigDecimal> lows = recentPrices.stream()
                    .map(p -> p.getLowPrice() != null ? p.getLowPrice() : p.getClosePrice())
                    .toList();
            List<BigDecimal> closes = recentPrices.stream()
                    .map(StockPrice::getClosePrice)
                    .toList();
            List<LocalDate> dates = recentPrices.stream()
                    .map(p -> p.getId().getBaseDate())
                    .toList();

            // 3. 지표 시리즈 계산
            List<TechnicalIndicators> results = TechnicalIndicatorCalculator.calculateSeries(highs, lows, closes, dates);
            if (results.isEmpty()) {
                return null;
            }

            // 4. 최신 시세 엔티티에 지표 결과 업데이트
            TechnicalIndicators latestIndicators = results.getLast();
            StockPrice latestPrice = recentPrices.getLast();
            latestPrice.updateIndicators(latestIndicators);

            return latestPrice;
        } catch (Exception e) {
            log.error("[지표 계산 오류] stockTicker={}, error={}", stock.getTicker(), e.getMessage());
            return null;
        }
    }
}
