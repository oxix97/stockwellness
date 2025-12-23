package org.stockwellness.application.service.mapper;

import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.external.krx.dto.KrxListedInfoResponse;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * StockMapper
 * <p>
 * 역할: 외부 API DTO(KrxStockPriceItem)를 도메인 Entity(Stock, StockHistory)로 변환
 * 특징: API의 String 데이터를 안전하게 Parsing하여 타입 불일치 오류 방지
 */
@Component
public class StockMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * [Master Data] DTO -> Stock Entity 변환
     * <p>
     * 매일 배치가 돌 때, 시세 정보에 포함된 상장주식수(listedShares)나
     * 시장구분(marketCategory) 등의 변경 사항을 마스터 엔티티에 반영하기 위함입니다.
     */
    public Stock toStockEntity(KrxListedInfoResponse.Item item) {
        Long totalShares = parseLongSafe(item.listedShares());

        return Stock.create(
                item.isinCode(),        // isinCd (PK)
                item.itemName(),        // itmsNm
                item.ticker(),          // srtnCd
                MarketType.fromString(item.marketCategory()),  // mrktCtg (Enum 변환 로직은 Stock 내부 혹은 별도 처리)
                totalShares,             // lstgStCnt
                item.corporationNo(),
                item.corporationName()
        );
    }

    /**
     * [Time-Series Data] DTO -> StockHistory Entity 변환
     * <p>
     * API의 모든 금액/비율 필드를 BigDecimal로 변환하여 정밀도를 보장합니다.
     */
    public StockHistory toHistoryEntity(KrxListedInfoResponse.Item item) {
        return StockHistory.create(
                item.isinCode(),                            // isinCd
                parseDateSafe(item.baseDate()),             // basDt
                parseBigDecimalSafe(item.closePrice()),     // clpr
                parseBigDecimalSafe(item.openPrice()),      // mkp
                parseBigDecimalSafe(item.highPrice()),      // hipr
                parseBigDecimalSafe(item.lowPrice()),       // lopr
                parseBigDecimalSafe(item.priceChange()),    // vs
                parseBigDecimalSafe(item.fluctuationRate()),// fltRt
                parseLongSafe(item.tradingVolume()),        // trqu
                parseBigDecimalSafe(item.tradingValue()),   // trPrc
                parseBigDecimalSafe(item.marketCap())       // mrktTotAmt
        );
    }
    /**
     * 날짜 파싱 (YYYYMMDD -> LocalDate)
     */
    private LocalDate parseDateSafe(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Base Date(basDt) cannot be null or empty.");
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }

    /**
     * 숫자(금액/비율) 파싱 (String -> BigDecimal)
     * <p>API가 빈 문자열("")이나 null을 줄 경우 0으로 처리</p>
     */
    private BigDecimal parseBigDecimalSafe(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            String cleanValue = value.replace(",", "");
            return new BigDecimal(cleanValue);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 정수(수량) 파싱 (String -> Long)
     */
    private Long parseLongSafe(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            String cleanValue = value.replace(",", "");
            return Long.parseLong(cleanValue);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}