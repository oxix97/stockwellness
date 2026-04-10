package org.stockwellness.adapter.out.external.kis.adapter;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.*;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
import org.stockwellness.application.port.out.stock.SectorDailyDetailSnapshot;
import org.stockwellness.application.port.out.stock.SectorDailySnapshot;
import org.stockwellness.application.port.out.stock.SectorDataPort;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisSectorAdapter implements SectorDataPort {

    private static final String INVESTOR_DAILY_PATH = "/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market";
    private static final String INVESTOR_DAILY_TR_ID = "FHPTJ04040000";
    private static final String DOMESTIC_MARKET_DIVISION_CODE = "U";
    private static final String KOSPI_MARKET_CODE = "KSP";
    private static final String KOSDAQ_MARKET_CODE = "KSQ";
    private static final BigDecimal PBMN_TO_WON_MULTIPLIER = BigDecimal.valueOf(1_000_000L);

    private final RestClient kisApiClient;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    @Override
    @Retry(name = "kisRetry")
    public SectorDailySnapshot fetchDailySectorDetail(String indexCode, LocalDate date) {
        log.debug("섹터 상세 정보 조회: {} (기준일: {})", indexCode, date);

        KisPriceResponse<KisDailySectorDetail, List<KisDailySectorDetail>> response = kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-index-daily-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build())
                .header("tr_id", "FHPUP02120000")
                .retrieve()
                .body(new ParameterizedTypeReference<KisPriceResponse<KisDailySectorDetail, List<KisDailySectorDetail>>>() {
                });

        response = requireSuccessfulPriceResponse(response, "섹터 상세 정보 조회", indexCode);
        if (response == null || response.output1() == null) {
            throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);
        }

        // Note: DTO에 날짜 필드가 없는 경우 최신 데이터를 반환하도록 함
        KisDailySectorDetail detail = (response.output2() != null && !response.output2().isEmpty())
                ? response.output2().get(0)
                : response.output1();
        return new SectorDailySnapshot(
                indexCode,
                date,
                detail.sectorIndexPrice() != null ? new BigDecimal(detail.sectorIndexPrice()) : BigDecimal.ZERO,
                detail.sectorIndexPriceChangeRate() != null ? new BigDecimal(detail.sectorIndexPriceChangeRate()) : BigDecimal.ZERO
        );
    }

    /**
     * 시장별 투자자매매동향(일별)
     */
    @Override
    @Retry(name = "kisRetry")
    public List<InvestorTradingSnapshot> fetchInvestorTradingDaily(
            String indexCode,
            LocalDate date,
            int days
    ) {
        String latestDate = date.format(DATE_FMT);
        String fromDate = date.minusMonths(1).format(DATE_FMT);
        String marketCode = resolveMarketCode(indexCode);

        KisResponse<List<InvestorTradingDaily>> response = kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(INVESTOR_DAILY_PATH)
                        // U: 업종/시장 지수 조회 구분값
                        .queryParam("FID_COND_MRKT_DIV_CODE", DOMESTIC_MARKET_DIVISION_CODE)
                        .queryParam("FID_INPUT_DATE_1", latestDate)
                        .queryParam("FID_INPUT_DATE_2", fromDate)
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_INPUT_ISCD_1", marketCode)
                        .queryParam("FID_INPUT_ISCD_2", indexCode)
                        .build())
                .header("tr_id", INVESTOR_DAILY_TR_ID)
                .retrieve()
                .body(new ParameterizedTypeReference<KisResponse<List<InvestorTradingDaily>>>() {
                });

        response = requireSuccessfulResponse(response, "시장별 투자자매매동향 조회", indexCode);
        if (response == null || response.output() == null || response.output().isEmpty()) {
            return Collections.emptyList();
        }

        List<InvestorTradingSnapshot> snapshots = response.output().stream()
                .limit(days)
                .map(detail -> new InvestorTradingSnapshot(
                        toLocalDate(detail.stckBsopDate()) != null ? toLocalDate(detail.stckBsopDate()) : date,
                        null,
                        null,
                        null,
                        null,
                        null,
                        toPbmnAmount(detail.orgnNtbyTrPbmn()),
                        toPbmnAmount(detail.frgnNtbyTrPbmn())
                ))
                .toList();

        if (snapshots.stream().allMatch(snapshot ->
                BigDecimal.ZERO.compareTo(snapshot.netInstitutionalBuyingAmt()) == 0
                        && BigDecimal.ZERO.compareTo(snapshot.netForeignBuyingAmt()) == 0)) {
            log.debug("시장별 투자자매매동향 응답이 모두 0입니다. indexCode={}, requestedDate={}, responseSize={}",
                    indexCode, date, snapshots.size());
        }

        return snapshots;
    }

    @Override
    @Retry(name = "kisRetry")
    public List<BigDecimal> fetchHistoricalIndexPrices(String indexCode, LocalDate endDate, int days) {
        KisPriceResponse<KisDailySectorDetail, List<KisDailySectorDetail>> response = kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-index-daily-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build())
                .header("tr_id", "FHPUP02120000")
                .retrieve()
                .body(new ParameterizedTypeReference<KisPriceResponse<KisDailySectorDetail, List<KisDailySectorDetail>>>() {
                });

        response = requireSuccessfulPriceResponse(response, "섹터 히스토리 시세 조회", indexCode);
        if (response == null || response.output2() == null) {
            return Collections.emptyList();
        }

        // [중요] KIS API는 최신순(DESC)으로 데이터를 주므로, 지표 계산을 위해 과거순(ASC)으로 반전시킴
        List<BigDecimal> prices = response.output2().stream()
                .map(d -> d.sectorIndexPrice() != null ? new BigDecimal(d.sectorIndexPrice()) : BigDecimal.ZERO)
                .limit(days)
                .collect(Collectors.toList());

        Collections.reverse(prices);
        return prices;
    }

    @Override
    @Retry(name = "kisRetry")
    public SectorDailyDetailSnapshot fetchTodaySectorDetail(String indexCode, LocalDate date) {
        KisResponse<KisDailySectorInfo> response = kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-index-category-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build())
                .header("tr_id", "FHPUP02100000")
                .retrieve()
                .body(new ParameterizedTypeReference<KisResponse<KisDailySectorInfo>>() {
                });

        response = requireSuccessfulResponse(response, "섹터 당일 상세 조회", indexCode);
        if (response == null || response.output() == null) {
            throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);
        }

        KisDailySectorInfo detail = response.output();
        List<InvestorTradingSnapshot> trading = fetchInvestorTradingDaily(indexCode, date, 1);
        InvestorTradingSnapshot latestTrading = trading.stream().filter(Objects::nonNull).findFirst().orElse(null);

        return new SectorDailyDetailSnapshot(
                indexCode,
                date,
                toBigDecimal(detail.bstpNmixPrpr()),
                toBigDecimal(detail.bstpNmixPrdyVrss()),
                detail.prdyVrssSign(),
                toBigDecimal(detail.bstpNmixPrdyCtrt()),
                toLong(detail.acmlVol()),
                toLong(detail.prdyVol()),
                toLong(detail.acmlTrPbmn()),
                toLong(detail.prdyTrPbmn()),
                toBigDecimal(detail.bstpNmixOprc()),
                toBigDecimal(detail.bstpNmixHgpr()),
                toBigDecimal(detail.bstpNmixLwpr()),
                toInteger(detail.ascnIssuCnt()),
                toInteger(detail.uplmIssuCnt()),
                toInteger(detail.stnrIssuCnt()),
                toInteger(detail.downIssuCnt()),
                toInteger(detail.lslmIssuCnt()),
                toBigDecimal(detail.dryyBstpNmixHgpr()),
                toBigDecimal(detail.dryyHgprVrssPrprRate()),
                toLocalDate(detail.dryyBstpNmixHgprDate()),
                toBigDecimal(detail.dryyBstpNmixLwpr()),
                toBigDecimal(detail.dryyLwprVrssPrprRate()),
                toLocalDate(detail.dryyBstpNmixLwprDate()),
                toLong(detail.totalAskpRsqn()),
                toLong(detail.totalBidpRsqn()),
                toBigDecimal(detail.selnRsqnRate()),
                toBigDecimal(detail.shnuRsqnRate()),
                toLong(detail.ntbyRsqn()),
                latestTrading != null && latestTrading.netForeignBuyingAmt() != null ? latestTrading.netForeignBuyingAmt().longValue() : 0L,
                latestTrading != null && latestTrading.netInstitutionalBuyingAmt() != null ? latestTrading.netInstitutionalBuyingAmt().longValue() : 0L
        );

    }

    private <T> KisResponse<T> requireSuccessfulResponse(KisResponse<T> response, String operation, String indexCode) {
        if (response == null) {
            throw new KisApiException(null, null, operation + " 응답이 비어 있습니다.");
        }
        if (response.rtCd() != null && !"0".equals(response.rtCd())) {
            log.error("{} 실패. indexCode={}, msgCd={}, msg1={}", operation, indexCode, response.msgCd(), response.msg1());
            throw KisApiException.from(response.rtCd(), response.msgCd(), response.msg1());
        }
        return response;
    }

    private <T1, T2> KisPriceResponse<T1, T2> requireSuccessfulPriceResponse(
            KisPriceResponse<T1, T2> response,
            String operation,
            String indexCode
    ) {
        if (response == null) {
            throw new KisApiException(null, null, operation + " 응답이 비어 있습니다.");
        }
        if (response.rtCd() != null && !"0".equals(response.rtCd())) {
            log.error("{} 실패. indexCode={}, msgCd={}, msg1={}", operation, indexCode, response.msgCd(), response.msg1());
            throw KisApiException.from(response.rtCd(), response.msgCd(), response.msg1());
        }
        return response;
    }

    private BigDecimal toPbmnAmount(String value) {
        if (isBlank(value)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value).multiply(PBMN_TO_WON_MULTIPLIER);
    }

    private String resolveMarketCode(String indexCode) {
        int parsedIndexCode = Integer.parseInt(indexCode.trim());

        if (parsedIndexCode >= 1 && parsedIndexCode < 1000) {
            return KOSPI_MARKET_CODE;
        }
        if (parsedIndexCode >= 1000 && parsedIndexCode < 2000) {
            return KOSDAQ_MARKET_CODE;
        }

        throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);
    }

    private BigDecimal toBigDecimal(String value) {
        if (isBlank(value)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    private Long toLong(String value) {
        if (isBlank(value)) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private Integer toInteger(String value) {
        if (isBlank(value)) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    private LocalDate toLocalDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        return LocalDate.parse(value, DATE_FMT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
