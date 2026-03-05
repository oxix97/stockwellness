package org.stockwellness.batch.job.stock.master;

import org.stockwellness.domain.stock.KospiItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 코스피 종목 마스터 파일(kospi_code.mst) 파서
 *
 * <h2>파일 구조 (실데이터 검증 완료)</h2>
 * <p>각 행은 CP949 인코딩 기준으로 줄바꿈(\n) 제외 정확히 {@code 288바이트}로 구성됩니다.
 *
 * <pre>
 * ┌─────────────────────────────────┬──────────────────────────────┐
 * │       Part 1 (61 bytes)         │      Part 2 (227 bytes)      │
 * │  단축코드(9) 표준코드(12) 한글명  │  70개 고정폭 필드             │
 * └─────────────────────────────────┴──────────────────────────────┘
 * </pre>
 *
 * <h2>주의사항</h2>
 * <ul>
 *   <li>한글명은 CP949 기준 가변 길이이므로 반드시 <b>바이트 배열</b>로 슬라이싱해야 합니다.
 *   <li>Part 2의 시작점은 인덱스 61이며, 첫 필드인 그룹코드는 공백 없이 {@code 2바이트}입니다.
 * </ul>
 */
public class KospiMstParser {

    private static final Charset CP949 = Charset.forName("EUC-KR");

    /**
     * Part2 고정 바이트 길이 (줄바꿈 제외)
     */
    private static final int PART2_LENGTH = 227;

    /**
     * Part2 각 필드의 바이트 너비 (총합 = 227).
     *
     * <p><b>공식 문서(파이썬) 스펙 준수:</b>
     * <ul>
     *   <li>Part 1은 61바이트이며, 한글명 뒤의 마지막 공백 1바이트를 포함합니다.
     *   <li>Part 2는 인덱스 61부터 시작하며, 그룹코드(2)는 공백 없이 바로 시작합니다. (ex: "ST")
     * </ul>
     */
    private static final int[] FIELD_WIDTHS = {
            //  0: 그룹코드(2)
            2,
            //  1: 시가총액규모
            1,
            //  2: 지수업종대분류      3: 지수업종중분류      4: 지수업종소분류
            4, 4, 4,
            //  5: 제조업             6: 저유동성            7: 지배구조지수종목
            1, 1, 1,
            //  8: KOSPI200섹터업종   9: KOSPI100           10: KOSPI50
            1, 1, 1,
            // 11: KRX              12: ETP               13: ELW발행          14: KRX100
            1, 1, 1, 1,
            // 15: KRX자동차        16: KRX반도체          17: KRX바이오         18: KRX은행
            1, 1, 1, 1,
            // 19: SPAC             20: KRX에너지화학      21: KRX철강           22: 단기과열
            1, 1, 1, 1,
            // 23: KRX미디어통신     24: KRX건설           25: Non1
            1, 1, 1,
            // 26: KRX증권          27: KRX선박            28: KRX섹터_보험      29: KRX섹터_운송
            1, 1, 1, 1,
            // 30: SRI
            1,
            // 31: 기준가(9)        32: 매매수량단위(5)     33: 시간외수량단위(5)
            9, 5, 5,
            // 34: 거래정지         35: 정리매매           36: 관리종목
            1, 1, 1,
            // 37: 시장경고(2)      38: 경고예고           39: 불성실공시        40: 우회상장
            2, 1, 1, 1,
            // 41: 락구분(2)        42: 액면변경(2)        43: 증자구분(2)
            2, 2, 2,
            // 44: 증거금비율(3)    45: 신용가능           46: 신용기간(3)
            3, 1, 3,
            // 47: 전일거래량(12)
            12,
            // 48: 액면가(12)       49: 상장일자(8)
            12, 8,
            // 50: 상장주수(15)     51: 자본금(21)
            15, 21,
            // 52: 결산월(2)        53: 공모가(7)          54: 우선주
            2, 7, 1,
            // 55: 공매도과열       56: 이상급등           57: KRX300           58: KOSPI
            1, 1, 1, 1,
            // 59: 매출액(9)        60: 영업이익(9)        61: 경상이익(9)
            9, 9, 9,
            // 62: 당기순이익(5)    63: ROE(9)             64: 기준년월(8)
            5, 9, 8,
            // 65: 시가총액(9)      66: 그룹사코드(3)
            9, 3,
            // 67: 회사신용한도초과  68: 담보대출가능       69: 대주가능
            1, 1, 1
    };

    static {
        int sum = Arrays.stream(FIELD_WIDTHS).sum();
        if (sum != PART2_LENGTH) {
            throw new IllegalStateException(
                    "FIELD_WIDTHS 합계 오류: expected=%d, actual=%d".formatted(PART2_LENGTH, sum));
        }
    }

    public static List<KospiItem> parse(Path filePath) throws IOException {
        List<KospiItem> result = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(filePath, CP949)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                try {
                    result.add(parseLine(line));
                } catch (Exception e) {
                    System.err.printf("[WARN] 파싱 실패 (line=%d): %s%n", lineNumber, e.getMessage());
                }
            }
        }
        return result;
    }

    public static List<KospiItem> parseLines(List<String> lines) {
        return lines.stream().map(KospiMstParser::parseLine).toList();
    }

    private static KospiItem parseLine(String line) {
        byte[] lineBytes = line.getBytes(CP949);
        int totalBytes = lineBytes.length;

        byte[] part1Bytes = Arrays.copyOf(lineBytes, totalBytes - PART2_LENGTH);
        String shortCode = decode(part1Bytes, 0, 9).strip();
        String isinCode = decode(part1Bytes, 9, 21).strip();
        String koreanName = decode(part1Bytes, 21, part1Bytes.length).strip();

        byte[] part2Bytes = Arrays.copyOfRange(lineBytes, totalBytes - PART2_LENGTH, totalBytes);
        String[] f = splitFixedWidth(part2Bytes);

        return new KospiItem(
                shortCode, isinCode, koreanName,
                f[0], f[1], f[2], f[3], f[4],
                f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12], f[13], f[14],
                f[15], f[16], f[17], f[18], f[19], f[20], f[21], f[22], f[23], f[24],
                f[25], f[26], f[27], f[28], f[29], f[30],
                f[31], f[32], f[33], f[34], f[35], f[36], f[37], f[38], f[39], f[40],
                f[41], f[42], f[43], f[44], f[45], f[46],
                f[47], f[48], f[49], f[50], f[51], f[52], f[53], f[54],
                f[55], f[56], f[57], f[58],
                f[59], f[60], f[61], f[62], f[63], f[64],
                f[65], f[66], f[67], f[68], f[69]
        );
    }

    private static String[] splitFixedWidth(byte[] data) {
        String[] fields = new String[FIELD_WIDTHS.length];
        int offset = 0;
        for (int i = 0; i < FIELD_WIDTHS.length; i++) {
            int end = Math.min(offset + FIELD_WIDTHS[i], data.length);
            fields[i] = new String(data, offset, end - offset, CP949).strip();
            offset += FIELD_WIDTHS[i];
        }
        return fields;
    }

    private static String decode(byte[] bytes, int from, int to) {
        int end = Math.min(to, bytes.length);
        return new String(bytes, from, end - from, CP949);
    }
}
