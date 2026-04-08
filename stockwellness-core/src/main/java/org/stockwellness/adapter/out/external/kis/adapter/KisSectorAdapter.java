package org.stockwellness.adapter.out.external.kis.adapter;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.*;
import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.application.port.out.stock.SectorDailySnapshot;
import org.stockwellness.application.port.out.stock.SectorDataPort;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisSectorAdapter implements SectorDataPort {

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
        String startDate = date.minusMonths(1).format(DATE_FMT);
        String endDate = date.format(DATE_FMT);

        KisResponse<List<InvestorTradingDaily>> response = kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_DATE_1", startDate)
                        .queryParam("FID_INPUT_DATE_2", endDate)
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_INPUT_ISCD_1", "")
                        .queryParam("FID_INPUT_ISCD_2", "")
                        .build())
                .header("tr_id", "FHPTJ04040000")
                .retrieve()
                .body(new ParameterizedTypeReference<KisResponse<List<InvestorTradingDaily>>>() {
                });

        if (response == null || !"0".equals(response.rtCd()) || response.output() == null) {
            return Collections.emptyList();
        }

        return response.output().stream()
                .limit(days)
                .map(detail -> new InvestorTradingSnapshot(
                        date,
                        null,
                        null,
                        null,
                        null,
                        null,
                        detail.orgnNtbyTrPbmn() != null ? new BigDecimal(detail.orgnNtbyTrPbmn()).multiply(BigDecimal.valueOf(1_000_000L)) : BigDecimal.ZERO,
                        detail.frgnNtbyTrPbmn() != null ? new BigDecimal(detail.frgnNtbyTrPbmn()).multiply(BigDecimal.valueOf(1_000_000L)) : BigDecimal.ZERO
                ))
                .toList();
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

        if (response == null || !"0".equals(response.rtCd()) || response.output2() == null) {
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
    public List<SectorApiDto> fetchTodaySectorData() {
        List<SectorApiDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        fetchAndAddSectors(result, "0001", "K", today);
        fetchAndAddSectors(result, "1001", "Q", today);
        return result;
    }

    private void fetchAndAddSectors(List<SectorApiDto> result, String iscd, String mrktCls, LocalDate today) {
        try {
            KisPriceResponse<KisSectorPriceDetail, List<KisSectorPriceSummary>> response = fetchAllSectorPrices(iscd, mrktCls);
            if (response != null && response.output2() != null) {
                response.output2().forEach(s -> {
                    String sectorCode = s.bstpClsCode();

                    // 수급 데이터 추가 조회 (외인/기관 순매수 금액)
                    List<InvestorTradingSnapshot> trading = fetchInvestorTradingDaily(sectorCode, today, 1);
                    long netForeign = 0L;
                    long netInst = 0L;

                    if (!trading.isEmpty()) {
                        InvestorTradingSnapshot latest = trading.get(0);
                        netForeign = latest.netForeignBuyingAmt() != null ? latest.netForeignBuyingAmt().longValue() : 0L;
                        netInst = latest.netInstitutionalBuyingAmt() != null ? latest.netInstitutionalBuyingAmt().longValue() : 0L;
                    }

                    result.add(new SectorApiDto(
                            sectorCode,
                            s.htsKorIsnm(),
                            today,
                            new BigDecimal(s.bstpNmixPrpr() != null ? s.bstpNmixPrpr() : "0"),
                            new BigDecimal(s.bstpNmixPrdyCtrt() != null ? s.bstpNmixPrdyCtrt() : "0"),
                            netForeign,
                            netInst
                    ));
                });
            }
        } catch (Exception e) {
            log.error("섹터 시세 및 수급 조회 실패 (업종코드: {}): {}", iscd, e.getMessage());
        }
    }

    @Retry(name = "kisRetry")
    public KisPriceResponse<KisSectorPriceDetail, List<KisSectorPriceSummary>> fetchAllSectorPrices(String inputIscd, String mrktClsCode) {
        return kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-index-category-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", inputIscd)
                        .queryParam("FID_COND_SCR_DIV_CODE", "20214")
                        .queryParam("FID_MRKT_CLS_CODE", mrktClsCode)
                        .queryParam("FID_BLNG_CLS_CODE", "0")
                        .build())
                .header("tr_id", "FHPUP02140000")
                .retrieve()
                .body(new ParameterizedTypeReference<KisPriceResponse<KisSectorPriceDetail, List<KisSectorPriceSummary>>>() {
                });
    }
}
