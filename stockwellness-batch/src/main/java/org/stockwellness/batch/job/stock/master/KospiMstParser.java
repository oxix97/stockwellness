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
 * <h2>파일 구조</h2>
 * <p>각 행은 CP949 인코딩 기준으로 정확히 {@code 288바이트}로 구성됩니다.
 *
 * <pre>
 * ┌─────────────────────────────────┬──────────────────────────────┐
 * │       Part 1 (60 bytes)         │      Part 2 (228 bytes)      │
 * │  단축코드(9) 표준코드(12) 한글명  │  70개 고정폭 필드             │
 * └─────────────────────────────────┴──────────────────────────────┘
 * </pre>
 *
 * <h2>주의사항</h2>
 * <ul>
 *   <li>한글명은 CP949 기준 가변 길이이므로 반드시 <b>바이트 배열</b>로 슬라이싱해야 합니다.
 *   <li>Part2 {@code 전일거래량} 필드는 원본 명세의 12바이트가 아닌 <b>13바이트</b>입니다.
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * List<KospiItem> items = KospiMstParser.parse(Path.of("kospi_code.mst"));
 * items.stream()
 *      .filter(KospiItem::isStock)
 *      .filter(KospiItem::isTradable)
 *      .forEach(item -> System.out.println(item.shortCode() + " " + item.koreanName()));
 * }</pre>
 */
public class KospiMstParser {

    private static final Charset CP949 = Charset.forName("EUC-KR");

    /**
     * Part2 고정 바이트 길이
     */
    private static final int PART2_LENGTH = 228;

    /**
     * Part2 각 필드의 바이트 너비 (총합 = 228).
     *
     * <p><b>실제 파일 스펙 주의사항:</b>
     * <ul>
     *   <li>인덱스 0(그룹코드)가 <b>3바이트</b>입니다 — 원본 명세(2)와 다릅니다.
     *       실제 데이터는 {@code " ST"}, {@code " EF"} 형태로 앞에 공백 1byte가 포함됩니다.
     *   <li>인덱스 47(전일거래량)은 원본 명세대로 <b>12바이트</b>입니다.
     * </ul>
     */
    private static final int[] FIELD_WIDTHS = {
            //  0: 그룹코드(3) ← 원본 명세 2에서 수정 (앞에 공백 1byte 포함)
            3,
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
     * @param filePath kospi_code.mst 파일 경로
     * @return 파싱된 {@link KospiItem} 목록
     * @throws IOException 파일 읽기 실패 시
     */
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
     * 한 행을 CP949 바이트 기준으로 파싱하여 {@link KospiItem}을 반환합니다.
     */
    private static KospiItem parseLine(String line) {
        byte[] lineBytes = line.getBytes(CP949);
        int totalBytes = lineBytes.length;

        // ── Part 1: 가변 길이 영역 ──────────────────────────────────────────
        byte[] part1Bytes = Arrays.copyOf(lineBytes, totalBytes - PART2_LENGTH);

        String shortCode = decode(part1Bytes, 0, 9).strip();
        String isinCode = decode(part1Bytes, 9, 21).strip();
        String koreanName = decode(part1Bytes, 21, part1Bytes.length).strip();

        // ── Part 2: 고정 228바이트 영역 ────────────────────────────────────
        byte[] part2Bytes = Arrays.copyOfRange(lineBytes, totalBytes - PART2_LENGTH, totalBytes);
        String[] f = splitFixedWidth(part2Bytes);

        return new KospiItem(
                shortCode, isinCode, koreanName,

                // Part2-A: 분류 정보
                f[0],   // groupCode
                f[1],   // marketCapSize
                f[2],   // sectorLarge
                f[3],   // sectorMedium
                f[4],   // sectorSmall

                // Part2-B: 지수 편입 플래그
                f[5],   // manufacturing
                f[6],   // lowLiquidity
                f[7],   // corporateGovernanceIndex
                f[8],   // kospi200Sector
                f[9],   // kospi100
                f[10],  // kospi50
                f[11],  // krx
                f[12],  // etp
                f[13],  // elwIssued
                f[14],  // krx100
                f[15],  // krxAutomobile
                f[16],  // krxSemiconductor
                f[17],  // krxBio
                f[18],  // krxBank
                f[19],  // spac
                f[20],  // krxEnergyChem
                f[21],  // krxSteel
                f[22],  // shortTermOverheat
                f[23],  // krxMediaTelecom
                f[24],  // krxConstruction
                f[25],  // reserved1
                f[26],  // krxFinance
                f[27],  // krxShipping
                f[28],  // krxInsurance
                f[29],  // krxTransport
                f[30],  // sri

                // Part2-C: 거래 정보
                f[31],  // basePrice
                f[32],  // tradingUnit
                f[33],  // afterHoursTradingUnit
                f[34],  // tradingHalt
                f[35],  // clearingTrade
                f[36],  // administeredStock
                f[37],  // marketWarningLevel
                f[38],  // warningNotice
                f[39],  // unfaithfulDisclosure
                f[40],  // backdoorListing
                f[41],  // exRightType
                f[42],  // parValueChangeType
                f[43],  // capitalIncreaseType
                f[44],  // marginRatio
                f[45],  // creditAvailable
                f[46],  // creditPeriodDays

                // Part2-D: 종목 기본 정보
                f[47],  // previousVolume      (13bytes, 수정된 너비)
                f[48],  // parValue
                f[49],  // listingDate
                f[50],  // listedShares
                f[51],  // capital
                f[52],  // fiscalMonth
                f[53],  // ipoPrice
                f[54],  // preferredStockType

                // Part2-E: 시장 감시 플래그
                f[55],  // shortSellOverheat
                f[56],  // abnormalSurge
                f[57],  // krx300
                f[58],  // kospi

                // Part2-F: 재무 정보
                f[59],  // revenue
                f[60],  // operatingProfit
                f[61],  // ordinaryProfit
                f[62],  // netIncome
                f[63],  // roe
                f[64],  // financialBaseYearMonth

                // Part2-G: 시장 통계
                f[65],  // marketCap
                f[66],  // conglomerateCode
                f[67],  // creditLimitExceeded
                f[68],  // collateralLoanAvailable
                f[69]   // stockLendingAvailable
        );
    }

    public static List<KospiItem> parseLines(List<String> lines) {
        return lines.stream().map(KospiMstParser::parseLine).toList();
    }

    /**
     * 바이트 배열을 {@link #FIELD_WIDTHS}에 따라 고정폭으로 분할합니다.
     *
     * @param data Part2 바이트 배열 (정확히 228바이트)
     * @return 각 필드를 strip()한 문자열 배열 (길이 70)
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

}
