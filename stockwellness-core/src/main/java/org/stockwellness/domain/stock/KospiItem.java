package org.stockwellness.domain.stock;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 코스피 종목 마스터(kospi_code.mst) 단일 레코드
 *
 * <p>파일 구조
 * <ul>
 *   <li>Part1 (가변 길이, CP949 바이트 기준): 단축코드(9) + 표준코드(12) + 한글명(나머지)
 *   <li>Part2 (고정 228바이트): 70개 고정폭 필드
 * </ul>
 *
 * <p>플래그 필드 값 의미: {@code "Y"} = 해당, {@code "N"} = 비해당, {@code " "} = 해당없음
 */
public record KospiItem(

        // ── Part 1: 식별 정보 ─────────────────────────────────────────────

        /** 단축코드 (6자리, e.g. "000810") */
        String shortCode,

        /** 표준코드 (12자리 ISIN, e.g. "KR7000810002") */
        String isinCode,

        /** 한글 종목명 (e.g. "삼성화재") */
        String koreanName,

        // ── Part 2-A: 분류 정보 ──────────────────────────────────────────

        /** 그룹코드 (ST=주식, EF=ETF, BC=수익증권 등, 2자) */
        String groupCode,

        /** 시가총액 규모 구분 (1=대형, 2=중형, 3=소형, 0=해당없음) */
        String marketCapSize,

        /** 지수업종 대분류 코드 */
        String sectorLarge,

        /** 지수업종 중분류 코드 */
        String sectorMedium,

        /** 지수업종 소분류 코드 */
        String sectorSmall,

        // ── Part 2-B: 지수 편입 플래그 ───────────────────────────────────

        /** 제조업 여부 (Y/N) */
        String manufacturing,

        /** 저유동성 종목 여부 (Y/N) */
        String lowLiquidity,

        /** 지배구조 지수 편입 여부 (Y/N) */
        String corporateGovernanceIndex,

        /** KOSPI200 섹터업종 편입 여부 (Y/N) */
        String kospi200Sector,

        /** KOSPI100 편입 여부 (Y/N) */
        String kospi100,

        /** KOSPI50 편입 여부 (Y/N) */
        String kospi50,

        /** KRX 편입 여부 (Y/N) */
        String krx,

        /** ETP(상장지수상품) 여부 (Y/N) */
        String etp,

        /** ELW 발행 여부 (Y/N) */
        String elwIssued,

        /** KRX100 편입 여부 (Y/N) */
        String krx100,

        /** KRX 자동차 지수 편입 여부 (Y/N) */
        String krxAutomobile,

        /** KRX 반도체 지수 편입 여부 (Y/N) */
        String krxSemiconductor,

        /** KRX 바이오 지수 편입 여부 (Y/N) */
        String krxBio,

        /** KRX 은행 지수 편입 여부 (Y/N) */
        String krxBank,

        /** 기업인수목적회사(SPAC) 여부 (Y/N) */
        String spac,

        /** KRX 에너지화학 지수 편입 여부 (Y/N) */
        String krxEnergyChem,

        /** KRX 철강 지수 편입 여부 (Y/N) */
        String krxSteel,

        /** 단기과열 종목 지정 여부 (Y/N) */
        String shortTermOverheat,

        /** KRX 미디어통신 지수 편입 여부 (Y/N) */
        String krxMediaTelecom,

        /** KRX 건설 지수 편입 여부 (Y/N) */
        String krxConstruction,

        /** 예비 필드 (Non1) */
        String reserved1,

        /** KRX 증권 지수 편입 여부 (Y/N) */
        String krxFinance,

        /** KRX 선박 지수 편입 여부 (Y/N) */
        String krxShipping,

        /** KRX 섹터 보험 편입 여부 (Y/N) */
        String krxInsurance,

        /** KRX 섹터 운송 편입 여부 (Y/N) */
        String krxTransport,

        /** SRI(사회책임투자) 지수 편입 여부 (Y/N) */
        String sri,

        // ── Part 2-C: 거래 정보 ──────────────────────────────────────────

        /** 기준가 (원, 9자리) */
        String basePrice,

        /** 매매수량단위 (주, 5자리) */
        String tradingUnit,

        /** 시간외 매매수량단위 (주, 5자리) */
        String afterHoursTradingUnit,

        /** 거래정지 여부 (Y/N) */
        String tradingHalt,

        /** 정리매매 여부 (Y/N) */
        String clearingTrade,

        /** 관리종목 지정 여부 (Y/N) */
        String administeredStock,

        /** 시장경고 단계 (00=정상, 01=주의, 02=경고, 03=위험예고) */
        String marketWarningLevel,

        /** 경고예고 여부 (Y/N) */
        String warningNotice,

        /** 불성실공시 법인 지정 여부 (Y/N) */
        String unfaithfulDisclosure,

        /** 우회상장 여부 (Y/N) */
        String backdoorListing,

        /** 락 구분 (00=해당없음, 01=권리락, 02=배당락, 03=분배락) */
        String exRightType,

        /** 액면변경 구분 (00=해당없음, 01=분할, 02=병합) */
        String parValueChangeType,

        /** 증자구분 (00=해당없음, 01=유상, 02=무상, 03=유무상) */
        String capitalIncreaseType,

        /** 증거금비율 (%, 3자리, e.g. "040" = 40%) */
        String marginRatio,

        /** 신용거래 가능 여부 (Y/N) */
        String creditAvailable,

        /** 신용거래 기간 (일, 3자리) */
        String creditPeriodDays,

        // ── Part 2-D: 종목 기본 정보 ─────────────────────────────────────

        /** 전일 거래량 (주, 13자리) */
        String previousVolume,

        /** 액면가 (원, 12자리, e.g. "000000005000" = 5,000원) */
        String parValue,

        /** 상장일자 (YYYYMMDD, 8자리) */
        String listingDate,

        /** 상장 주수 (주, 15자리) */
        String listedShares,

        /** 자본금 (원, 21자리) */
        String capital,

        /** 결산월 (2자리, e.g. "12") */
        String fiscalMonth,

        /** 공모가 (원, 7자리) */
        String ipoPrice,

        /** 우선주 구분 (0=보통주, 1=우선주, 2=우선주B, ...) */
        String preferredStockType,

        // ── Part 2-E: 시장 감시 플래그 ───────────────────────────────────

        /** 공매도 과열 종목 지정 여부 (Y/N) */
        String shortSellOverheat,

        /** 이상 급등 종목 지정 여부 (Y/N) */
        String abnormalSurge,

        /** KRX300 편입 여부 (Y/N) */
        String krx300,

        /** KOSPI 편입 여부 (Y/N) */
        String kospi,

        // ── Part 2-F: 재무 정보 ──────────────────────────────────────────

        /** 매출액 (억원, 9자리) */
        String revenue,

        /** 영업이익 (억원, 9자리) */
        String operatingProfit,

        /** 경상이익 (억원, 9자리) */
        String ordinaryProfit,

        /** 당기순이익 (억원, 5자리) */
        String netIncome,

        /** ROE (자기자본이익률, %, 소수점 포함 9자리, e.g. "000008.70") */
        String roe,

        /** 재무 기준년월 (YYYYMM, 8자리) */
        String financialBaseYearMonth,

        // ── Part 2-G: 시장 통계 ──────────────────────────────────────────

        /** 시가총액 (억원, 9자리) */
        String marketCap,

        /** 그룹사 코드 (3자리) */
        String conglomerateCode,

        /** 회사 신용한도 초과 여부 (Y/N) */
        String creditLimitExceeded,

        /** 담보대출 가능 여부 (Y/N) */
        String collateralLoanAvailable,

        /** 대주 가능 여부 (Y/N) */
        String stockLendingAvailable

) {

    /**
     * 그룹코드가 주식(ST)인지 여부
     */
    public boolean isStock() {
        return "ST".equals(groupCode);
    }

    /**
     * 그룹코드가 ETF(EF)인지 여부
     */
    public boolean isEtf() {
        return "EF".equals(groupCode);
    }

    /**
     * 거래 가능 종목 여부 (거래정지·정리매매·관리종목 모두 아닌 경우)
     */
    public boolean isTradable() {
        return !"Y".equals(tradingHalt)
                && !"Y".equals(clearingTrade)
                && !"Y".equals(administeredStock);
    }

    /**
     * 상장일자를 {@code LocalDate}로 변환
     *
     * @return 상장일자, 파싱 실패 시 {@code null}
     */
    public LocalDate listingLocalDate() {
        try {
            return LocalDate.parse(listingDate, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ROE를 {@code double}로 변환
     *
     * @return ROE 값, 파싱 실패 시 {@code 0.0}
     */
    public double roeAsDouble() {
        try {
            return Double.parseDouble(roe.strip());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 시가총액을 {@code long}(억원)으로 변환
     *
     * @return 시가총액, 파싱 실패 시 {@code 0L}
     */
    public long marketCapAsLong() {
        try {
            return Long.parseLong(marketCap.strip());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}