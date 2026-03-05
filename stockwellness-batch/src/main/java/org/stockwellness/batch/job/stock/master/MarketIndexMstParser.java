package org.stockwellness.batch.job.stock.master;

import lombok.extern.slf4j.Slf4j;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 업종/지수 마스터 파일(idxcode.mst) 파서
 *
 * <h2>파일 구조 (45 bytes + \n)</h2>
 * <pre>
 * - idx_div  (1 byte)  : 시장구분 (0:KOSPI, 1:KOSDAQ 등)
 * - idx_code (4 bytes) : 업종 코드 (Stock 엔티티와 매핑되는 핵심 키)
 * - idx_name (40 bytes): 업종명 (CP949)
 * </pre>
 */
@Slf4j
public class MarketIndexMstParser {

    private static final Charset CP949 = Charset.forName("EUC-KR");
    private static final int RECORD_LENGTH = 45; // Newline 제외 데이터 길이

    public static List<MarketIndex> parse(Path filePath) throws IOException {
        byte[] allBytes = Files.readAllBytes(filePath);
        List<MarketIndex> result = new ArrayList<>();

        int offset = 0;
        while (offset + RECORD_LENGTH <= allBytes.length) {
            byte[] record = Arrays.copyOfRange(allBytes, offset, offset + RECORD_LENGTH);
            
            try {
                result.add(parseRecord(record));
            } catch (Exception e) {
                log.warn("[MarketIndex] 파싱 실패 (offset={}): {}", offset, e.getMessage());
            }

            // 다음 레코드로 이동 (45바이트 데이터 + 1바이트 줄바꿈)
            offset += (RECORD_LENGTH + 1);
        }

        return result;
    }

    private static MarketIndex parseRecord(byte[] record) {
        // 1. 시장 구분 (1)
        String div = new String(record, 0, 1, CP949).trim();
        
        // 2. 업종 코드 (4) -> Stock의 mediumCode와 매핑되는 4자리
        String code = new String(record, 1, 4, CP949).trim();
        
        // 3. 업종명 (40)
        String name = new String(record, 5, 40, CP949).trim();

        if (code.isEmpty() || name.isEmpty()) {
            throw new IllegalArgumentException("필수 데이터 누락");
        }

        // MarketIndex.of(rawCode, indexName) 호출
        // StockSector 로직에서 trim()을 사용하므로 여기서도 정규화하여 저장
        return MarketIndex.of(code, name);
    }
}
