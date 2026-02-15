package org.stockwellness.batch.job.stock.master;

import org.springframework.stereotype.Component;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

@Component
public class StockMasterParser {
    private static final String ENCODING = "Cp949";

    public Stock parseLine(String line, MarketType marketType) {
        try {
            byte[] bytes = line.getBytes(ENCODING);
            if (bytes.length < 100) return null;

            // 오프셋 기반 문자열 추출
            String ticker = new String(bytes, 0, 9, ENCODING).trim();
            String stdCode = new String(bytes, 9, 12, ENCODING).trim();
            String name = new String(bytes, 21, 40, ENCODING).trim();

            // 마켓별 섹터 위치 분기
            String rawSector = (marketType == MarketType.KOSPI)
                    ? new String(bytes, 144, 3, ENCODING).trim()
                    : new String(bytes, 116, 3, ENCODING).trim();

            // Builder 대신 정적 팩터리 메서드 사용
            return Stock.ofMasterFile(ticker, stdCode, name, marketType, rawSector);

        } catch (Exception e) {
            return null;
        }
    }
}