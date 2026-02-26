package org.stockwellness.adapter.out.external.kis.adapter;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.*;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.application.port.out.stock.SectorDataPort;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisSectorAdapter implements SectorDataPort {

    private final RestClient kisApiClient;

    /**
     * 국내업종 현재지수
     */
    @Override
    @Retry(name = "kisRetry")
    public KisDailySectorDetail fetchDailySectorDetail(String indexCode) {
        KisResponse<KisDailySectorDetail> response = kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-index-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .build())
                .header("tr_id", "FHPUP02100000")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        
        if (response == null || response.output() == null) {
            throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);
        }
        return response.output();
    }

    /**
     * 국내업종 구분별전체시세
     */
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
                .body(new ParameterizedTypeReference<>() {
                });
    }

    /**
     * 시장별 투자자매매동향(일별)
     */
    @Override
    @Retry(name = "kisRetry")
    public List<InvestorTradingDaily> fetchInvestorTradingDaily(
            String indexCode,
            int days
    ) {
        String endDate = LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String startDate = LocalDate.now().minusMonths(2).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);

        KisResponse<List<InvestorTradingDaily>> response = kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_DATE_1", startDate)
                        .queryParam("FID_INPUT_DATE_2", endDate)
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_INPUT_ISCD_1", indexCode)
                        .queryParam("FID_INPUT_ISCD_2", indexCode)
                        .build())
                .header("tr_id", "FHPTJ04040000")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        
        if (response == null || response.output() == null || response.output().isEmpty()) {
            throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);
        }
        
        return response.output().stream().limit(days).toList();
    }

    @Override
    @Retry(name = "kisRetry")
    public List<BigDecimal> fetchHistoricalIndexPrices(String indexCode, int days) {
        KisPriceResponse<KisDailySectorDetail, List<KisDailySectorDetail>> response = kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-index-daily-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .build())
                .header("tr_id", "FHPUP02120000")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || response.output2() == null || response.output2().isEmpty()) {
            throw new SectorDomainException(ErrorCode.SECTOR_HISTORY_NOT_FOUND);
        }

        return response.output2().stream()
                .map(d -> d.sectorIndexPrice() != null ? new BigDecimal(d.sectorIndexPrice()) : BigDecimal.ZERO)
                .limit(days)
                .toList();
    }

    @Override
    @Retry(name = "kisRetry")
    public List<SectorApiDto> fetchTodaySectorData() {
        List<SectorApiDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // KOSPI (0001)
        fetchAndAddSectors(result, "0001", "K", today);
        // KOSDAQ (1001)
        fetchAndAddSectors(result, "1001", "Q", today);

        return result;
    }

    private void fetchAndAddSectors(List<SectorApiDto> result, String iscd, String mrktCls, LocalDate today) {
        try {
            KisPriceResponse<KisSectorPriceDetail, List<KisSectorPriceSummary>> response = fetchAllSectorPrices(iscd, mrktCls);
            if (response != null && response.output2() != null) {
                response.output2().forEach(s -> result.add(new SectorApiDto(
                        s.bstpClsCode(),
                        s.htsKorIsnm(),
                        today,
                        new BigDecimal(s.bstpNmixPrpr()),
                        new BigDecimal(s.bstpNmixPrdyCtrt()),
                        0L, 
                        0L
                )));
            }
        } catch (Exception e) {
            log.error("Failed to fetch sector prices for {}: {}", iscd, e.getMessage());
        }
    }

}
