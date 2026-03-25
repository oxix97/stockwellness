package org.stockwellness.adapter.out.external.kis.adapter;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisPriceResponse;
import org.stockwellness.adapter.out.external.kis.dto.KisStockInfo;
import org.stockwellness.domain.stock.Stock;

import java.time.LocalDate;
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
        return fetchPricesInternal("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice", "J", stock.getTicker(), startDate, endDate, "FHKST03010100");
    }

    /**
     * 지수 기간별 시세(일/주/월/년)
     */
    @Retry(name = "kisRetry")
    public List<KisDailyPriceDetail> fetchIndexDailyPrices(String ticker, LocalDate startDate, LocalDate endDate) {
        return fetchPricesInternal("/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice", "U", ticker, startDate, endDate, "FHKST03010200");
    }

    private List<KisDailyPriceDetail> fetchPricesInternal(String path, String marketDiv, String ticker, LocalDate startDate, LocalDate endDate, String trId) {
        try {
            KisPriceResponse<KisStockInfo, List<KisDailyPriceDetail>> response = kisApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("FID_COND_MRKT_DIV_CODE", marketDiv)
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .queryParam("FID_INPUT_DATE_1", startDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_INPUT_DATE_2", endDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "1")
                            .build())
                    .header("tr_id", trId)
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
}
