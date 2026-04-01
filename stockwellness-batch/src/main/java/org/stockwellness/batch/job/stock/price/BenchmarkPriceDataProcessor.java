package org.stockwellness.batch.job.stock.price;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * [Processor] 수집된 시세 데이터를 BenchmarkPrice 엔티티로 변환하고 필요한 등락률을 계산하는 클래스
 */
@RequiredArgsConstructor
public class BenchmarkPriceDataProcessor implements ItemProcessor<BenchmarkPriceDataWrapper, BenchmarkPrice> {

    private final BenchmarkPricePort benchmarkPricePort;

    // 지수별 전일 종가 상태 유지 (시계열 데이터의 등락률 계산을 위한 메모리 캐시)
    private final Map<String, BigDecimal> prevCloseMap = new HashMap<>();

    /**
     * 개별 시세 데이터를 처리하여 엔티티로 변환합니다.
     */
    @Override
    public BenchmarkPrice process(BenchmarkPriceDataWrapper item) {
        BenchmarkType type = item.type();
        BenchmarkPriceData detail = item.data();
        String ticker = type.getTicker();

        BigDecimal close = detail.closePrice();
        BigDecimal changeRate = detail.prdyCtrt(); // API에서 제공하는 기본 대비율

        // 1. 해당 지수의 전일 종가(T-1) 확보 시도
        BigDecimal prevClose = prevCloseMap.get(ticker);

        // 2. 데이터가 해외 지수(isOverseas == true)인 경우에만 수동 계산(가공) 로직 수행
        if (type.isOverseas()) {
            // 전일 종가가 메모리(Map)에 없으면 DB에서 가장 최근 일자 데이터를 조회
            if (prevClose == null) {
                prevClose = benchmarkPricePort.findLatestBefore(ticker, detail.baseDate())
                        .map(BenchmarkPrice::getClosePrice)
                        .orElse(null);
            }

            // 전일 종가가 있는 경우 현재가와 비교하여 등락률 계산
            if (prevClose != null && prevClose.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = close.subtract(prevClose);
                // 공식: (현재가 - 전일종가) / 전일종가 * 100
                changeRate = diff.divide(prevClose, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            } else {
                // 직전 데이터가 전혀 없는 경우 등락률은 0으로 처리
                changeRate = BigDecimal.ZERO;
            }
        }

        // 3. 도메인 엔티티 생성 (이름, 티커, 날짜, 종가 기준)
        BenchmarkPrice bp = BenchmarkPrice.of(type.getName(), ticker, detail.baseDate(), close);
        
        // 4. 상세 시세 정보 업데이트 (시가, 고가, 저가, 종가, 대비율, 거래량)
        bp.updatePrices(
                detail.openPrice(), detail.highPrice(), detail.lowPrice(),
                close, changeRate, detail.volume()
        );

        // 5. 다음 영업일 데이터의 등락률 계산을 위해 현재 종가를 맵에 저장
        prevCloseMap.put(ticker, close);

        return bp;
    }
}
