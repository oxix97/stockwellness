package org.stockwellness.adapter.out.external.kis.adapter;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.stockwellness.adapter.out.external.kis.dto.*;
import org.stockwellness.domain.stock.Stock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisDailyPriceAdapter {

    private final RestClient kisApiClient;

    /**
     * 멀티 종목 시세 조회 (최대 30종목)
     */
    @Retry(name = "kisRetry")
    public List<KisMultiStockPriceDetail> fetchMultiStockPrices(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) return Collections.emptyList();

        try {
            KisPriceResponse<Object, List<KisMultiStockPriceDetail>> response = kisApiClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/uapi/domestic-stock/v1/quotations/intstock-multprice");
                        for (int i = 0; i < Math.min(tickers.size(), 30); i++) {
                            int idx = i + 1;
                            uriBuilder.queryParam("FID_COND_MRKT_DIV_CODE_" + idx, "J");
                            uriBuilder.queryParam("FID_INPUT_ISCD_" + idx, tickers.get(i));
                        }
                        return uriBuilder.build();
                    })
                    .header("tr_id", "FHKST11300006")
                    .header("custtype", "P")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.output2() == null) {
                return Collections.emptyList();
            }

            return response.output2();

        } catch (RestClientException | IllegalStateException e) {
            log.error("[KIS 어댑터] 멀티 종목 시세 조회 실패 (종목: {}): {}", tickers, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 주식 기간별 시세(일/주/월/년)
     */
    @Retry(name = "kisRetry")
    public List<KisDailyPriceDetail> fetchDailyPrices(Stock stock, LocalDate startDate, LocalDate endDate) {
        String path = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
        String ticker = stock.getTicker();
        try {
            KisPriceResponse<KisStockInfo, List<KisDailyPriceDetail>> response = kisApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .queryParam("FID_INPUT_DATE_1", startDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_INPUT_DATE_2", endDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "1")
                            .build())
                    .header("tr_id", "FHKST03010100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.output2() == null) {
                return Collections.emptyList();
            }
            return response.output2();
        } catch (RestClientException | IllegalStateException e) {
            log.error("[KIS 어댑터] 주식 시세 조회 실패 (티커: {}, 경로: {}): {}", ticker, path, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 국내 업종/지수 기간별 시세
     */
    @Retry(name = "kisRetry")
    public List<BenchmarkPriceData> fetchIndexDailyPrices(String indexCode, LocalDate startDate, LocalDate endDate) {
        try {
            KisPriceResponse<SectorIndexSummary, List<SectorDailyPrice>> response = kisApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                            .queryParam("FID_INPUT_DATE_1", startDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_INPUT_DATE_2", endDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_INPUT_ISCD", indexCode)
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .build())
                    .header("tr_id", "FHKUP03500100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.output2() == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(response.output2());
        } catch (RestClientException | IllegalStateException e) {
            log.error("[KIS 어댑터] 국내 지수 시세 조회 실패 (지수코드: {}): {}", indexCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 해외 지수 일자별 시세 조회
     */
    @Retry(name = "kisRetry")
    public List<BenchmarkPriceData> fetchOverseasIndexDailyPrices(String indexCode, LocalDate startDate, LocalDate endDate) {
        try {
            KisPriceResponse<OverseasIndexSummary, List<KisOverseasIndexDailyPrice>> response = kisApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/overseas-price/v1/quotations/inquire-daily-chartprice")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "N")
                            .queryParam("FID_INPUT_ISCD", indexCode)
                            .queryParam("FID_INPUT_DATE_1", startDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_INPUT_DATE_2", endDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .build())
                    .header("tr_id", "FHKST03030100")
                    .header("custtype", "P")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.output2() == null) {
                return Collections.emptyList();
            }

            return response.output2().stream()
                    .sorted(Comparator.comparing(KisOverseasIndexDailyPrice::baseDate))
                    .map(BenchmarkPriceData.class::cast)
                    .toList();
        } catch (RestClientException | IllegalStateException e) {
            log.error("[KIS 어댑터] 해외 지수 시세 조회 실패 (지수코드: {}): {}", indexCode, e.getMessage());
            return Collections.emptyList();
        }
    }
}
