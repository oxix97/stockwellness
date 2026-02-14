package org.stockwellness.adapter.out.external.kis.dto;

public record KisPriceResponse<T1,T2>(
        String rtCd,
        String msgCd,
        String msg1,
        T1 output1,
        T2 output2
) {

}
