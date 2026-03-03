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
 * <h2>파일 구조</h2>
 * <p>각 행은 CP949 인코딩 기준으로 정확히 {@code 282바이트}로 구성됩니다.
 *
 * <pre>
 * ┌──────────────────────────────────┬──────────────────────────────┐
 * │        Part 1 (60 bytes)         │      Part 2 (222 bytes)      │
 * │  단축코드(9) + 표준코드(12) + 한글명 │  64개 고정폭 필드             │
 * └──────────────────────────────────┴──────────────────────────────┘
 * </pre>
 *
 * <h2>코스피 파서({@code KospiMstParser})와의 차이점</h2>
 * <table border="1">
 *   <tr><th>항목</th><th>코스피</th><th>코스닥</th></tr>
 *   <tr><td>총 행 바이트</td><td>288</td><td>282</td></tr>
 *   <tr><td>Part2 바이트</td><td>228</td><td>222</td></tr>
 *   <tr><td>필드 수</td><td>70</td><td>64</td></tr>
 *   <tr><td>그룹코드 너비</td><td>3bytes</td><td>3bytes (동일)</td></tr>
 *   <tr><td>전일거래량 너비</td><td>12bytes</td><td>12bytes (동일)</td></tr>
 *   <tr><td>ROE 형식</td><td>소수점 포함 문자열</td><td>정수형 문자열</td></tr>
 * </table>
 *
 * <h2>주의사항</h2>
 * <ul>
 *   <li>한글명은 CP949 기준 가변 길이이므로 반드시 <b>바이트 배열</b>로 슬라이싱해야 합니다.
 *   <li>그룹코드 필드는 {@code " ST"} 형태로 앞에 공백 1byte가 포함된 <b>3바이트</b>입니다.
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * List<KosdaqItem> items = KosdaqMstParser.parse(Path.of("kosdaq_code.mst"));
 * items.stream()
 *      .filter(KosdaqItem::isStock)
 *      .filter(KosdaqItem::isTradable)
 *      .filter(KosdaqItem::isKosdaq150)
 *      .forEach(item -> System.out.println(item.shortCode() + " " + item.koreanName()));
 * }</pre>
 */
public class KosdaqMstParser {

    private static final Charset CP949 = Charset.forName("EUC-KR");

    /**
     * Part2 고정 바이트 길이
     */
    private static final int PART2_LENGTH = 222;

    /**
     * Part2 각 필드의 바이트 너비 (총합 = 222).
     *
     * <p><b>실제 파일 스펙 주의사항:</b>
     * <ul>
     *   <li>인덱스 0(그룹코드)은 <b>3바이트</b>입니다 — 원본 명세(2)와 다릅니다.
     *       실제 데이터는 {@code " ST"}, {@code " EF"} 형태로 앞에 공백 1byte가 포함됩니다.
     * </ul>
     */
    private static final int[] FIELD_WIDTHS = {
            //  0: 그룹코드(3) ← 원본 명세 2에서 수정 (앞에 공백 1byte 포함)
            3,
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
            // 42: 전일거래량(12)   43: 액면가(12)           44: 상장일자(8)
            12, 12, 8,
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
            // 59: 시가총액(9)      60: 그룹사코드(3)
            9, 3,
            // 61: 회사신용한도초과  62: 담보대출가능          63: 대주가능
            1, 1, 1
    };

    static {
        // 합계 검증 (클래스 로딩 시 1회 실행)
        int sum = Arrays.stream(FIELD_WIDTHS).sum();
        if (sum != PART2_LENGTH) {
            throw new IllegalStateException(
                    "FIELD_WIDTHS 합계 오류: expected=%d, actual=%d".formatted(PART2_LENGTH, sum));
        }
    }

    // ── 공개 API ─────────────────────────────────────────────────────────────

    /**
     * 파일 경로를 받아 전체 종목 목록을 반환합니다.
     *
     * @param filePath kosdaq_code.mst 파일 경로
     * @return 파싱된 {@link KosdaqItem} 목록
     * @throws IOException 파일 읽기 실패 시
     */
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
                    String preview = line.substring(0, Math.min(30, line.length()));
                    System.err.printf("[WARN] 파싱 실패 (line=%d, preview='%s'): %s%n",
                            lineNumber, preview, e.getMessage());
                }
            }
        }

        return result;
    }

    // ── 내부 파싱 로직 ────────────────────────────────────────────────────────

    /**
     * 한 행을 CP949 바이트 기준으로 파싱하여 {@link KosdaqItem}을 반환합니다.
     */
    private static KosdaqItem parseLine(String line) {
        byte[] lineBytes = line.getBytes(CP949);
        int totalBytes = lineBytes.length;

        // ── Part 1: 가변 길이 영역 ──────────────────────────────────────────
        byte[] part1Bytes = Arrays.copyOf(lineBytes, totalBytes - PART2_LENGTH);

        String shortCode = decode(part1Bytes, 0, 9).strip();
        String isinCode = decode(part1Bytes, 9, 21).strip();
        String koreanName = decode(part1Bytes, 21, part1Bytes.length).strip();

        // ── Part 2: 고정 222바이트 영역 ─────────────────────────────────────
        byte[] part2Bytes = Arrays.copyOfRange(lineBytes, totalBytes - PART2_LENGTH, totalBytes);
        String[] f = splitFixedWidth(part2Bytes);

        return new KosdaqItem(
                shortCode, isinCode, koreanName,

                // Part2-A: 분류 정보
                f[0],   // groupCode
                f[1],   // marketCapSize
                f[2],   // sectorLarge
                f[3],   // sectorMedium
                f[4],   // sectorSmall

                // Part2-B: 지수 편입 / 종목 특성 플래그
                f[5],   // venture          (코스닥 전용)
                f[6],   // lowLiquidity
                f[7],   // krx
                f[8],   // etpType
                f[9],   // krx100
                f[10],  // krxAutomobile
                f[11],  // krxSemiconductor
                f[12],  // krxBio
                f[13],  // krxBank
                f[14],  // spac
                f[15],  // krxEnergyChem
                f[16],  // krxSteel
                f[17],  // shortTermOverheat
                f[18],  // krxMediaTelecom
                f[19],  // krxConstruction
                f[20],  // investCaution    (코스닥 전용)
                f[21],  // krxFinance
                f[22],  // krxShipping
                f[23],  // krxInsurance
                f[24],  // krxTransport
                f[25],  // kosdaq150        (코스닥 전용)

                // Part2-C: 거래 정보
                f[26],  // basePrice
                f[27],  // tradingUnit
                f[28],  // afterHoursTradingUnit
                f[29],  // tradingHalt
                f[30],  // clearingTrade
                f[31],  // administeredStock
                f[32],  // marketWarningLevel
                f[33],  // warningNotice
                f[34],  // unfaithfulDisclosure
                f[35],  // backdoorListing
                f[36],  // exRightType
                f[37],  // parValueChangeType
                f[38],  // capitalIncreaseType
                f[39],  // marginRatio
                f[40],  // creditAvailable
                f[41],  // creditPeriodDays

                // Part2-D: 종목 기본 정보
                f[42],  // previousVolume
                f[43],  // parValue
                f[44],  // listingDate
                f[45],  // listedShares
                f[46],  // capital
                f[47],  // fiscalMonth
                f[48],  // ipoPrice
                f[49],  // preferredStockType

                // Part2-E: 시장 감시 플래그
                f[50],  // shortSellOverheat
                f[51],  // abnormalSurge
                f[52],  // krx300

                // Part2-F: 재무 정보
                f[53],  // revenue
                f[54],  // operatingProfit
                f[55],  // ordinaryProfit
                f[56],  // netIncome
                f[57],  // roe              (정수형, 코스피와 포맷 다름)
                f[58],  // financialBaseYearMonth

                // Part2-G: 시장 통계
                f[59],  // marketCap
                f[60],  // conglomerateCode
                f[61],  // creditLimitExceeded
                f[62],  // collateralLoanAvailable
                f[63]   // stockLendingAvailable
        );
    }

    public static List<KosdaqItem> parseLines(List<String> lines) {
        return lines.stream()
                .map(KosdaqMstParser::parseLine)
                .toList();
    }

    /**
     * 바이트 배열을 {@link #FIELD_WIDTHS}에 따라 고정폭으로 분할합니다.
     *
     * @param data Part2 바이트 배열 (정확히 222바이트)
     * @return 각 필드를 strip()한 문자열 배열 (길이 64)
     */
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

    /**
     * 바이트 배열의 특정 구간을 CP949 문자열로 디코딩합니다.
     */
    private static String decode(byte[] bytes, int from, int to) {
        int end = Math.min(to, bytes.length);
        return new String(bytes, from, end - from, CP949);
    }

    // ── main (간단한 동작 확인용) ─────────────────────────────────────────────

    public static void main(String[] args) throws IOException {
        Path path = args.length > 0 ? Path.of(args[0]) : Path.of("kosdaq_code.mst");

        List<KosdaqItem> items = parse(path);

        System.out.printf("총 종목 수: %,d%n%n", items.size());

        System.out.printf("%-8s %-6s %-20s %-4s %-8s %-12s %-6s %12s%n",
                "단축코드", "그룹", "한글명", "규모", "상장일자", "액면가", "ROE(%)", "시가총액(억)");
        System.out.println("-".repeat(88));

        items.stream()
                .filter(KosdaqItem::isStock)
                .filter(KosdaqItem::isTradable)
                .limit(10)
                .forEach(item -> System.out.printf(
                        "%-8s %-6s %-20s %-4s %-8s %12s %6d %,12d%n",
                        item.shortCode(),
                        item.groupCode(),
                        item.koreanName(),
                        item.marketCapSize(),
                        item.listingDate(),
                        item.parValue().replaceAll("^0+", ""),
                        item.roeAsLong(),
                        item.marketCapAsLong()
                ));

        long stockCount = items.stream().filter(KosdaqItem::isStock).count();
        long tradable = items.stream().filter(KosdaqItem::isStock).filter(KosdaqItem::isTradable).count();
        long venture = items.stream().filter(KosdaqItem::isVenture).count();
        long kosdaq150 = items.stream().filter(KosdaqItem::isKosdaq150).count();

        System.out.printf("%n주식(ST) 전체: %,d   거래가능: %,d   벤처기업: %,d   KOSDAQ150: %,d%n",
                stockCount, tradable, venture, kosdaq150);
    }
}
