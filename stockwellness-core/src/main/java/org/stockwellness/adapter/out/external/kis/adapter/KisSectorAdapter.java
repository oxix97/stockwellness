package org.stockwellness.adapter.out.external.kis.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.*;

import java.util.List;

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
                .body(new ParameterizedTypeReference<>() {
                });
    }

    /**
     * 국내업종 구분별전체시세
     */
    public KisPriceResponse<KisSectorPriceDetail, List<KisSectorPriceSummary>> fetchAllSectorPrices(String inputIscd, String mrktClsCode) {
//        FID_MRKT_CLS_CODE K, Q, K2의 bstp_cls_code만 추출
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
    public KisResponse<InvestorTradingDaily> fetchInvestorTradingDaily(
            String startDate,
            String endDate
    ) {
        return kisApiClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_DATE_1", startDate)
                        .queryParam("FID_INPUT_DATE_2", endDate)
                        .queryParam("FID_INPUT_ISCD", "0001")
                        .queryParam("FID_INPUT_ISCD_1", "0001")
                        .queryParam("FID_INPUT_ISCD_2", "0001")
                        .build())
                .header("tr_id", "FHPTJ04040000")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    /**
     * 국내주식 등락률 TOP 30
     */


    /**
     * 국내주식 시가 총액 상위 30
     */

}
