package org.stockwellness.adapter.out.external.kis.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
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

    public List<KisDailyPriceDetail> fetchDailyPrices(Stock stock, LocalDate startDate, LocalDate endDate) {
        try {
            // ParameterizedTypeReference를 사용하여 제네릭 타입을 런타임까지 보존
            KisPriceResponse<KisStockInfo, List<KisDailyPriceDetail>> response = kisApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stock.getTicker())
                            .queryParam("FID_INPUT_DATE_1", startDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_INPUT_DATE_2", endDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "1")
                            .build())
                    .header("tr_id", "FHKST03010100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.output2() == null) {
                return Collections.emptyList();
            }

            return response.output2();

        } catch (Exception e) {
            log.error("Failed to fetch price for {}: {}", stock.getTicker(), e.getMessage());
            return Collections.emptyList();
        }
    }
}