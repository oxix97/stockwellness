package org.stockwellness.batch.job.stock.master;

import org.stockwellness.domain.stock.KosdaqItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 코스닥 종목 마스터 파일(kosdaq_code.mst) 파서
 *
 * <h2>파일 구조 (실데이터 검증 완료)</h2>
 * <p>각 행은 CP949 인코딩 기준으로 줄바꿈(\n) 제외 정확히 {@code 281바이트}로 구성됩니다.
 *
 * <pre>
 * ┌──────────────────────────────────┬──────────────────────────────┐
 * │        Part 1 (60 bytes)         │      Part 2 (221 bytes)      │
 * │  단축코드(9) + 표준코드(12) + 한글명 │  64개 고정폭 필드             │
 * └──────────────────────────────────┴──────────────────────────────┘
 * </pre>
 *
 * <h2>주의사항</h2>
 * <ul>
 *   <li>한글명은 CP949 기준 가변 길이이므로 반드시 <b>바이트 배열</b>로 슬라이싱해야 합니다.
 *   <li>그룹코드 필드는 공백 없이 {@code 2바이트}입니다.
 * </ul>
 */
public class KosdaqMstParser {

    private static final Charset CP949 = Charset.forName("EUC-KR");

    /**
     * Part2 고정 바이트 길이 (줄바꿈 제외)
     */
    private static final int PART2_LENGTH = 221;

    /**
     * Part2 각 필드의 바이트 너비 (총합 = 221).
     *
     * <p><b>공식 표준 스펙 준수:</b>
     * <ul>
     *   <li>Part 1은 60바이트(인덱스 0~59)이며, Part 2는 인덱스 60부터 시작합니다.
     *   <li>그룹코드(2)는 공백 없이 바로 시작합니다. (ex: "ST")
     * </ul>
     */
    private static final int[] FIELD_WIDTHS = {
            //  0: 그룹코드(2)
            2,
            //  1: 시가총액규모
            1,
            //  2: 지수업종대분류     3: 지수업종중분류      4: 지수업종소분류
            4, 4, 4,
            //  5: 벤처기업여부(코스닥 전용)   6: 저유동성
            1, 1,
            //  7: KRX              8: ETP상품구분         9: KRX100
            1, 1, 1,
            // 10: KRX자동차        11: KRX반도체
            1, 1,
            // 12: KRX바이오        13: KRX은행            14: SPAC
            1, 1, 1,
            // 15: KRX에너지화학     16: KRX철강
            1, 1,
            // 17: 단기과열         18: KRX미디어통신       19: KRX건설
            1, 1, 1,
            // 20: 투자주의환기(코스닥 전용)  21: KRX증권
            1, 1,
            // 22: KRX선박          23: KRX섹터보험         24: KRX섹터운송
            1, 1, 1,
            // 25: KOSDAQ150(코스닥 전용)
            1,
            // 26: 기준가(9)
            9,
            // 27: 매매수량단위(5)   28: 시간외수량단위(5)
            5, 5,
            // 29: 거래정지         30: 정리매매            31: 관리종목
            1, 1, 1,
            // 32: 시장경고(2)      33: 경고예고            34: 불성실공시        35: 우회상장
            2, 1, 1, 1,
            // 36: 락구분(2)        37: 액면변경(2)         38: 증자구분(2)
            2, 2, 2,
            // 39: 증거금비율(3)    40: 신용가능             41: 신용기간(3)
            3, 1, 3,
            // 42: 전일거래량(12)
            12,
            // 43: 액면가(12)       44: 상장일자(8)
            12, 8,
            // 45: 상장주수(15)     46: 자본금(21)
            15, 21,
            // 47: 결산월(2)        48: 공모가(7)            49: 우선주
            2, 7, 1,
            // 50: 공매도과열       51: 이상급등
            1, 1,
            // 52: KRX300
            1,
            // 53: 매출액(9)        54: 영업이익(9)          55: 경상이익(9)
            9, 9, 9,
            // 56: 당기순이익(5)    57: ROE(9)               58: 기준년월(8)
            5, 9, 8,
            // 59: 시가총액(9)      60: 그룹사코드(3) ← 2에서 3으로 원복 (합계 221 맞춤)
            9, 3,
            // 61: 회사신용한도초과  62: 담보대출가능          63: 대주가능
            1, 1, 1
    };

    static {
        int sum = Arrays.stream(FIELD_WIDTHS).sum();
        if (sum != PART2_LENGTH) {
            throw new IllegalStateException(
                    "FIELD_WIDTHS 합계 오류: expected=%d, actual=%d".formatted(PART2_LENGTH, sum));
        }
    }

    public static List<KosdaqItem> parse(Path filePath) throws IOException {
        List<KosdaqItem> result = new ArrayList<>();
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

    public static List<KosdaqItem> parseLines(List<String> lines) {
        return lines.stream().map(KosdaqMstParser::parseLine).toList();
    }

    private static KosdaqItem parseLine(String line) {
        byte[] lineBytes = line.getBytes(CP949);
        int totalBytes = lineBytes.length;

        byte[] part1Bytes = Arrays.copyOf(lineBytes, totalBytes - PART2_LENGTH);
        String shortCode = decode(part1Bytes, 0, 9).strip();
        String isinCode = decode(part1Bytes, 9, 21).strip();
        String koreanName = decode(part1Bytes, 21, part1Bytes.length).strip();

        byte[] part2Bytes = Arrays.copyOfRange(lineBytes, totalBytes - PART2_LENGTH, totalBytes);
        String[] f = splitFixedWidth(part2Bytes);

        return new KosdaqItem(
                shortCode, isinCode, koreanName,
                f[0], f[1], f[2], f[3], f[4],
                f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12], f[13], f[14],
                f[15], f[16], f[17], f[18], f[19], f[20], f[21], f[22], f[23], f[24], f[25],
                f[26], f[27], f[28], f[29], f[30], f[31], f[32], f[33], f[34], f[35],
                f[36], f[37], f[38], f[39], f[40], f[41],
                f[42], f[43], f[44], f[45], f[46], f[47], f[48], f[49],
                f[50], f[51], f[52],
                f[53], f[54], f[55], f[56], f[57], f[58],
                f[59], f[60], f[61], f[62], f[63]
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
