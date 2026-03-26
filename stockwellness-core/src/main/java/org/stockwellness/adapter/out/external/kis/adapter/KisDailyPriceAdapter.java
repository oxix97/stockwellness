package org.stockwellness.adapter.out.external.kis.adapter;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.*;
import org.stockwellness.domain.stock.Stock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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

        } catch (Exception e) {
            log.error("멀티 종목 시세 조회 실패 (종목: {}): {}", tickers, e.getMessage());
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
        } catch (Exception e) {
            log.error("시세 조회 실패 (ticker: {}, path: {}): {}", ticker, path, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 국내업종 일자별 지수 최대 100개
     */
    @Retry(name = "kisRetry")
    public List<BenchmarkPriceData> fetchIndexDailyPrices(String indexCode, LocalDate startDate) {
        try {
            KisPriceResponse<SectorIndexSummary, List<SectorDailyPrice>> response = kisApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-index-category-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                            .queryParam("FID_INPUT_ISCD", indexCode)
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_INPUT_DATE_1", startDate.format(BASIC_ISO_DATE))
                            .build())
                    .header("tr_id", "FHPUP02120000")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.output2() == null) {
                return Collections.emptyList();
            }
            return new ArrayList<>(response.output2());
        } catch (Exception e) {
            log.error("국내 지수 시세 조회 실패 (indexCode: {}): {}", indexCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 해외 지수 일자별 지수 조회
     */
    @Retry(name = "kisRetry")
    public List<BenchmarkPriceData> fetchOverseasIndexDailyPrices(String indexCode, LocalDate startDate) {
        try {
            KisPriceResponse<OverseasIndexSummary, List<KisOverseasIndexDailyPrice>> response = kisApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/overseas-stock/v1/quotations/inquire-index-dailyprice")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                            .queryParam("FID_INPUT_ISCD", indexCode)
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

            // startDate 이후 데이터만 필터링
            return response.output2().stream()
                    .filter(d -> !d.baseDate().isBefore(startDate))
                    .map(d -> (BenchmarkPriceData) d)
                    .toList();
        } catch (Exception e) {
            log.error("해외 지수 시세 조회 실패 (indexCode: {}): {}", indexCode, e.getMessage());
            return Collections.emptyList();
        }
    }
}
