package org.stockwellness.adapter.out.external.kis.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.KisDailySectorDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisSectorAdapter {

    private final RestClient kisApiClient;

    public KisResponse<KisDailySectorDetail> fetchDailySectorDatil() {
        return kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-index-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", "0001")
                        .build())
                .header("tr_id", "FHPUP02100000")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
