package org.stockwellness.adapter.out.external.kis.adapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.kis.config.ResilienceConfig;
import org.stockwellness.adapter.out.external.kis.dto.KisHolidayEntry;
import org.stockwellness.adapter.out.external.kis.dto.KisHolidayResponse;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.application.port.out.external.kis.KisHolidayPort;

@Slf4j
@Component
public class KisHolidayAdapter implements KisHolidayPort {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;
    private final Retry retry = Retry.of("kisRetry", ResilienceConfig.kisRetryConfig());

    public KisHolidayAdapter(@Qualifier("kisApiClient") RestClient client, ObjectMapper objectMapper,
            RateLimiter rateLimiter) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public List<KisHolidayEntry> fetchHolidays(LocalDate baseDate) {
        if (baseDate == null) throw new IllegalArgumentException("KIS holiday base date is required");
        Supplier<List<KisHolidayEntry>> operation = () -> rateLimiter.executeSupplier(() -> {
            KisHolidayResponse response = client.get()
                    .uri(builder -> builder.path("/uapi/domestic-stock/v1/quotations/chk-holiday")
                            .queryParam("BASS_DT", baseDate.format(DateTimeFormatter.BASIC_ISO_DATE))
                            .queryParam("CTX_AREA_NK", "")
                            .queryParam("CTX_AREA_FK", "")
                            .build())
                    .header("tr_id", "CTCA0903R")
                    .header("custtype", "P")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !"0".equals(response.rtCd())) {
                throw KisApiException.from(response == null ? null : response.rtCd(),
                        response == null ? null : response.msgCd(), response == null ? null : response.msg1());
            }
            JsonNode output = response.output();
            if (output == null || output.isNull()) return List.of();
            if (output.isArray()) return objectMapper.convertValue(output,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, KisHolidayEntry.class));
            return List.of(objectMapper.convertValue(output, KisHolidayEntry.class));
        });
        return Retry.decorateSupplier(retry, operation).get();
    }
}
