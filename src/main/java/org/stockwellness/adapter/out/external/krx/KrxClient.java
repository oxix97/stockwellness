package org.stockwellness.adapter.out.external.krx;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.stockwellness.adapter.out.external.krx.dto.KrxListedInfoResponse;
import org.stockwellness.adapter.out.external.krx.dto.KrxStockPriceResponse;

import java.net.URI;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@RequiredArgsConstructor
public class KrxClient {
    private final RestClient restClient;

    @Value("${krx.service-key}")
    private String serviceKey;

    @Value("${krx.base-url}")
    private String baseUrl;

    public KrxStockPriceResponse stockPriceInfo(String isinCd) {
        URI uri = uriBuilder("/GetStockSecuritiesInfoService/getStockPriceInfo")
                .queryParam("isinCd", isinCd)
                .build(true)
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(KrxStockPriceResponse.class);
    }

    /**
     * 금융위원회_주식목록
     */
    public KrxListedInfoResponse stockInfos(String date) {
        URI uri = uriBuilder("/GetKrxListedInfoService/getItemInfo")
                .queryParam("basDt",date)
                .build(true)
                .toUri();
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(KrxListedInfoResponse.class);
    }

    private UriComponentsBuilder uriBuilder(String path) {
        String encodeKey = URLEncoder.encode(serviceKey, UTF_8);
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(path)
                .queryParam("serviceKey", encodeKey)
                .queryParam("resultType", "json")
                .queryParam("numOfRows", 3000)
                .queryParam("pageNo", 1);
    }
}
