package org.stockwellness.adapter.out.external.kis.adapter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.dto.*;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.global.config.ResilienceConfig;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisDailyPriceAdapter {

    private final RestClient kisApiClient;
    private final Retry kisRetry = Retry.of("kisRetry", ResilienceConfig.kisRetryConfig());
    private final RateLimiter kisRateLimiter;

    /**
     * 멀티 종목 시세 조회 (최대 30종목)
     */
    public List<KisMultiStockPriceDetail> fetchMultiStockPrices(List<String> tickers) {
        return executeWithRetry(() -> {
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

            response = requireSuccessfulResponse(response, "멀티 종목 시세 조회", String.join(",", tickers));
            return (response.output2() != null) ? response.output2() : Collections.emptyList();
        });
    }

    /**
     * 주식 기간별 시세(일/주/월/년)
     */
    public List<KisDailyPriceDetail> fetchDailyPrices(Stock stock, LocalDate startDate, LocalDate endDate) {
        String path = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
        String ticker = stock.getTicker();
        return executeWithRetry(() -> {
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

            response = requireSuccessfulResponse(response, "주식 시세 조회", ticker);
            return (response.output2() != null) ? response.output2() : Collections.emptyList();
        });
    }

    /**
     * 주식 일자별 투자자 매매 추이 (확정치)
     */
    public List<KisInvestorPriceDetail> fetchInvestorPrices(Stock stock, LocalDate startDate, LocalDate endDate) {
        String path = "/uapi/domestic-stock/v1/quotations/inquire-investor";
        String ticker = stock.getTicker();
        return executeWithRetry(() -> {
            KisResponse<List<KisInvestorPriceDetail>> response = kisApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .queryParam("FID_INPUT_DATE_1", startDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_INPUT_DATE_2", endDate.format(BASIC_ISO_DATE))
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_ORG_ADJ_PRC", "1")
                            .build())
                    .header("tr_id", "FHKST01010900")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            response = requireSuccessfulResponse(response, "투자자 매매 추이 조회", ticker);
            return (response.output() != null) ? response.output() : Collections.emptyList();
        });
    }

    /**
     * 국내 업종/지수 기간별 시세
     */
    public List<BenchmarkPriceData> fetchIndexDailyPrices(String indexCode, LocalDate startDate, LocalDate endDate) {
        return executeWithRetry(() -> {
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

            response = requireSuccessfulResponse(response, "국내 지수 시세 조회", indexCode);
            int outputSize = response.output2() == null ? 0 : response.output2().size();
            log.info("[KIS 어댑터] 국내 지수 시세 조회 응답 indexCode={}, outputSize={}", indexCode, outputSize);

            if (response.output2() == null || response.output2().isEmpty()) {
                return Collections.emptyList();
            }

            return response.output2().stream()
                    .filter(price -> price.baseDate() != null)
                    .filter(price -> !price.baseDate().isBefore(startDate) && !price.baseDate().isAfter(endDate))
                    .sorted(Comparator.comparing(BenchmarkPriceData::baseDate).reversed())
                    .map(BenchmarkPriceData.class::cast)
                    .toList();
        });
    }

    /**
     * 해외 지수 일자별 시세 조회
     */
    public List<BenchmarkPriceData> fetchOverseasIndexDailyPrices(String indexCode, LocalDate startDate, LocalDate endDate) {
        return executeWithRetry(() -> {
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

            response = requireSuccessfulResponse(response, "해외 지수 시세 조회", indexCode);
            int outputSize = response.output2() == null ? 0 : response.output2().size();
            log.info("[KIS 어댑터] 해외 지수 시세 조회 응답 indexCode={}, outputSize={}", indexCode, outputSize);

            if (response.output2() == null || response.output2().isEmpty()) {
                return Collections.emptyList();
            }

            return response.output2().stream()
                    .filter(price -> price.baseDate() != null)
                    .filter(price -> !price.baseDate().isBefore(startDate) && !price.baseDate().isAfter(endDate))
                    .sorted(Comparator.comparing(KisOverseasIndexDailyPrice::baseDate).reversed())
                    .map(BenchmarkPriceData.class::cast)
                    .toList();
        });
    }

    /**
     * 주식현재가 투자자
     */
    public List<InvestorTradeDetail> fetchForeignInstitutionData(String ticker) {
        return executeWithRetry(() -> {
            KisResponse<List<InvestorTradeDetail>> response = kisApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-investor")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", ticker)
                            .build())
                    .header("tr_id", "FHKST01010900")
                    .header("custtype", "P")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            response = requireSuccessfulResponse(response, "국내기관 외국인 매매종목가 집계", ticker);
            return (response.output() != null) ? response.output() : Collections.emptyList();
        });
    }

    private <T> T requireSuccessfulResponse(T response, String operation, String targetId) {
        if (response == null) {
            throw new KisApiException(null, null, "%s 응답이 비어 있습니다. (대상: %s)".formatted(operation, targetId));
        }

        String rtCd = null;
        String msgCd = null;
        String msg1 = null;

        if (response instanceof KisPriceResponse<?, ?> priceResponse) {
            rtCd = priceResponse.rtCd();
            msgCd = priceResponse.msgCd();
            msg1 = priceResponse.msg1();
        } else if (response instanceof KisResponse<?> baseResponse) {
            rtCd = baseResponse.rtCd();
            msgCd = baseResponse.msgCd();
            msg1 = baseResponse.msg1();
        }

        if (rtCd != null && !"0".equals(rtCd)) {
            log.error("[KIS 어댑터] {} 실패 (대상: {}, rtCd: {}, msgCd: {}, msg1: {})",
                    operation, targetId, rtCd, msgCd, msg1);
            throw KisApiException.from(rtCd, msgCd, msg1);
        }
        return response;
    }

    private <T> T executeWithRetry(Supplier<T> supplier) {
        return Retry.decorateSupplier(kisRetry, () -> kisRateLimiter.executeSupplier(supplier)).get();
    }
}
