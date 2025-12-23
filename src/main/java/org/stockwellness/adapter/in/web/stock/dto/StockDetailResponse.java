package org.stockwellness.adapter.in.web.stock.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주식 상세 정보 응답 DTO
 * <p>Stock(마스터) + 최신 StockHistory(시세)를 결합하여 제공</p>
 */
public record StockDetailResponse(
        // --- Master Data ---
        String isinCode,
        String ticker,
        String name,
        String marketType,
        String sector,      // 업종 (추후 확장 대비)
        Long totalShares,   // 상장주식수

        // --- Latest Price Data ---
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate baseDate,        // 기준일 (최신 데이터 날짜)

        BigDecimal closePrice,     // 현재가(종가)
        BigDecimal priceChange,    // 대비
        BigDecimal fluctuationRate,// 등락률
        BigDecimal openPrice,      // 시가
        BigDecimal highPrice,      // 고가
        BigDecimal lowPrice,       // 저가
        Long volume,               // 거래량
        BigDecimal tradingValue,   // 거래대금
        BigDecimal marketCap,      // 시가총액

        // --- Technical Indicators (Pre-calculated) ---
        BigDecimal rsi14,
        BigDecimal ma20
) {
    /**
     * Entity 결합 팩토리 메서드
     * history가 null일 경우(신규 상장 등) 안전하게 처리
     */
    public static StockDetailResponse of(Stock stock, StockHistory history) {
        if (history == null) {
            // 시세 데이터가 없는 경우 마스터 정보만 반환
            return new StockDetailResponse(
                    stock.getIsinCode(), stock.getTicker(), stock.getName(),
                    stock.getMarketType().name(), null, stock.getTotalShares(),
                    null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0L, BigDecimal.ZERO, BigDecimal.ZERO, null, null
            );
        }

        return new StockDetailResponse(
                stock.getIsinCode(),
                stock.getTicker(),
                stock.getName(),
                stock.getMarketType().name(),
                null, // Sector 정보는 현재 엔티티에 없으므로 null
                stock.getTotalShares(),

                history.getBaseDate(),
                history.getClosePrice(),
                history.getPriceChange(),
                history.getFluctuationRate(),
                history.getOpenPrice(),
                history.getHighPrice(),
                history.getLowPrice(),
                history.getVolume(),
                history.getTradingValue(),
                history.getMarketCap(),
                history.getRsi14(),
                history.getMovingAverage20()
        );
    }
}