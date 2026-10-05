package org.stockwellness.adapter.out.external.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisHolidayEntry(
        @JsonProperty("bass_dt") String baseDate,
        @JsonProperty("opnd_yn") String marketOpen
) {}
