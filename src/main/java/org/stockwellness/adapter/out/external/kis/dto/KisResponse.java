package org.stockwellness.adapter.out.external.kis.dto;

public record KisResponse<T>(
        String rtCd,
        String msgCd,
        String msg1,
        T output
) {

}